package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.ReloadHelper
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.pm.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Service for syncing app tabs to app drawer folders.
 *
 * When enabled, this service automatically creates and maintains folders
 * in the app drawer based on app tabs assigned by the categorization system.
 *
 * Uses the caddy folder implementation (FolderService + Room DB).
 */
class CategoryFolderSyncService(
    private val context: Context,
    private val prefs: PreferenceManager = PreferenceManager.getInstance(context),
    private val drawerFolderService: FolderService = FolderService.INSTANCE.get(context),
    private val reloadHelper: ReloadHelper = ReloadHelper(context)
) {

    // Mutex to serialize folder sync operations and prevent race conditions
    private val syncMutex = Mutex()

    companion object {
        private const val TAG = "CategoryFolderSync"
    }

    /**
     * Checks if folder sync is enabled in preferences
     */
    fun isSyncEnabled(): Boolean {
        return prefs.autoCatSyncFolders.get()
    }

    /**
     * Gets the current folder sync mode from preferences
     */
    private fun getSyncMode(): FolderSyncMode {
        val modeString = prefs.autoCatFolderSyncMode.get()
        return FolderSyncMode.fromString(modeString)
    }

    /**
     * Checks if drawer folder sync is enabled
     */
    private fun shouldSyncToDrawer(): Boolean {
        val mode = getSyncMode()
        return mode == FolderSyncMode.DRAWER || mode == FolderSyncMode.BOTH
    }

    /**
     * Checks if home screen folder sync is enabled
     */
    private fun shouldSyncToHomeScreen(): Boolean {
        val mode = getSyncMode()
        return mode == FolderSyncMode.HOME_SCREEN || mode == FolderSyncMode.BOTH
    }

    /**
     * Syncs all categorized apps to drawer folders.
     *
     * Creates folders for each category and adds apps to them.
     * Only affects apps that have been categorized.
     *
     * @param categorizations Map of package name → tab name
     * @param allApps Optional list of all apps to avoid recreating AppInfo objects
     * @return SyncResult with statistics
     */
    suspend fun syncCategoriesToFolders(
        categorizations: Map<String, String>,
        allApps: List<com.android.launcher3.model.data.AppInfo>? = null,
    ): SyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            if (!isSyncEnabled()) {
                LLMLogger.logInfo(
                    provider = "CategoryFolderSync",
                    operation = "SYNC",
                    message = "Folder sync is disabled, skipping",
                )
                return@withContext SyncResult(
                    success = false,
                    message = "Folder sync is disabled",
                    foldersCreated = 0,
                    appsMovedToFolders = 0,
                )
            }

            try {
                val syncMode = getSyncMode()
                LLMLogger.logInfo(
                    provider = "CategoryFolderSync",
                    operation = "SYNC",
                    message = "Starting folder sync for ${categorizations.size} categorizations (mode: ${syncMode.displayName})",
                )

                if (!shouldSyncToDrawer() && !shouldSyncToHomeScreen()) {
                    LLMLogger.logWarning(
                        provider = "CategoryFolderSync",
                        operation = "SYNC",
                        message = "No sync mode enabled",
                    )
                    return@withContext SyncResult(
                        success = false,
                        message = "No sync mode enabled",
                        foldersCreated = 0,
                        appsMovedToFolders = 0,
                    )
                }

                // Group apps by tab, excluding uncategorized/system categories
                val appsByTab = categorizations.entries
                    .filter { (_, tabName) ->
                        CategorizationConstants.isCategorized(tabName)
                    }
                    .groupBy(
                        keySelector = { it.value },
                        valueTransform = { it.key },
                    )

                android.util.Log.d(TAG, "Categorizations: ${categorizations.size} total, ${appsByTab.size} tabs")
                appsByTab.forEach { (tabName, packages) ->
                    android.util.Log.d(TAG, "Tab '$tabName': ${packages.size} packages - ${packages.take(3)}" + if (packages.size > 3) "..." else "")
                }

                var foldersCreated = 0
                var appsMovedToFolders = 0

                // Get existing drawer folders (with timeout to prevent hanging)
                val existingFolders = try {
                    withTimeout(CategorizationConstants.FOLDER_OPERATION_TIMEOUT_MS) {
                        drawerFolderService.getAllFolders()
                    }
                } catch (e: TimeoutCancellationException) {
                    android.util.Log.e(TAG, "Timeout getting existing folders, aborting sync to prevent duplicates")
                    return@withContext SyncResult(
                        success = false,
                        message = "Timeout reading existing folders, sync aborted",
                        foldersCreated = 0,
                        appsMovedToFolders = 0,
                    )
                }
                val existingFolderMap = existingFolders.associateBy { it.title.toString() }

                // Build app lookup map for fast access
                val appsByPackage = if (allApps != null) {
                    // Use provided apps (FAST - no system calls)
                    allApps.filter { it.componentName != null }
                        .groupBy { it.componentName!!.packageName }
                } else {
                    // Fallback: create AppInfo from scratch (SLOW)
                    android.util.Log.w(TAG, "No apps provided, creating AppInfo from scratch (slow)")
                    val userCache = UserCache.INSTANCE.get(context)
                    val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)

                    categorizations.keys.flatMap { packageName ->
                        userCache.userProfiles.flatMap { userHandle ->
                            try {
                                launcherApps?.getActivityList(packageName, userHandle)
                                    ?.map { AppInfo(context, it, userHandle) } ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }.filter { it.componentName != null }
                        .groupBy { it.componentName!!.packageName }
                }

                // Build icon map
                val customTabs = TabDatabase.getInstance(context).tabDao().getAllCustomTabs()
                val tabIconMap = customTabs.associate { it.name to it.icon }

                // Sync to drawer folders if enabled
                if (shouldSyncToDrawer()) {
                    syncToDrawer(appsByTab, appsByPackage, existingFolderMap, tabIconMap)
                        .also { result ->
                            foldersCreated += result.first
                            appsMovedToFolders += result.second
                        }
                }

                // Sync to home screen folders if enabled
                if (shouldSyncToHomeScreen()) {
                    syncToHomeScreen(appsByTab, appsByPackage, tabIconMap)
                        .also { result ->
                            foldersCreated += result.first
                            appsMovedToFolders += result.second
                        }
                }

                val result = SyncResult(
                    success = true,
                    message = "Synced $appsMovedToFolders apps to $foldersCreated folders (mode: ${syncMode.displayName})",
                    foldersCreated = foldersCreated,
                    appsMovedToFolders = appsMovedToFolders,
                )

                LLMLogger.logInfo(
                    provider = "CategoryFolderSync",
                    operation = "SYNC",
                    message = result.message,
                )

                // Reload app drawer to display new/updated folders
                reloadHelper.reloadGrid()
                android.util.Log.d(TAG, "Triggered app drawer reload to display folders")

                result
            } catch (e: Exception) {
                LLMLogger.logError(
                    provider = "CategoryFolderSync",
                    operation = "SYNC",
                    error = e,
                )

                SyncResult(
                    success = false,
                    message = "Folder sync failed: ${e.message}",
                    foldersCreated = 0,
                    appsMovedToFolders = 0,
                    error = e,
                )
            }
        }
    }

    /**
     * Syncs apps to drawer folders.
     * Returns Pair<foldersCreated, appsMovedToFolders>
     */
    private suspend fun syncToDrawer(
        appsByTab: Map<String, List<String>>,
        appsByPackage: Map<String, List<AppInfo>>,
        existingFolderMap: Map<String, FolderInfo>,
        tabIconMap: Map<String, String?>,
    ): Pair<Int, Int> {
        var foldersCreated = 0
        var appsMovedToFolders = 0

        // Create/update folder for each tab (FAST - parallel friendly)
        appsByTab.forEach { (tabName, packageNames) ->
            val folderName = getFolderName(tabName)
            val folderIcon = tabIconMap[tabName]

            LLMLogger.logDebug(
                provider = "CategoryFolderSync",
                operation = "SYNC_DRAWER_FOLDER",
                message = "Syncing drawer folder: $folderName",
                details = mapOf(
                    "tab" to tabName,
                    "appCount" to packageNames.size,
                    "icon" to (folderIcon ?: "none"),
                ),
            )

            // Find apps for this category (FAST - map lookup)
            val apps = packageNames.flatMap { packageName ->
                val matchedApps = appsByPackage[packageName] ?: emptyList()
                if (matchedApps.isEmpty()) {
                    android.util.Log.w(TAG, "No apps found for package: $packageName in tab: $tabName")
                }
                matchedApps
            }

            if (apps.isNotEmpty()) {
                android.util.Log.d(TAG, "Folder '$folderName': ${apps.size} apps from ${packageNames.size} packages")
                // Check if folder exists
                val existingFolder = existingFolderMap[folderName]

                if (existingFolder != null) {
                    // Update existing folder
                    drawerFolderService.updateFolderWithItems(
                        folderInfoId = existingFolder.id,
                        title = folderName,
                        appInfos = apps,
                    )
                    android.util.Log.d(TAG, "Updated drawer folder: $folderName (${apps.size} apps)")
                } else {
                    // Create new folder with ID
                    val newFolder = FolderInfo().apply {
                        title = folderName
                        // Launcher3 auto-generates ID if not set
                    }
                    val folderIdLong = drawerFolderService.saveFolderInfo(newFolder)
                    val folderId = folderIdLong.toInt()

                    if (folderId > 0) {
                        drawerFolderService.updateFolderWithItems(
                            folderInfoId = folderId,
                            title = folderName,
                            appInfos = apps,
                        )
                        android.util.Log.d(TAG, "Created drawer folder: $folderName (${apps.size} apps)")
                        foldersCreated++
                    } else {
                        android.util.Log.w(TAG, "Failed to get valid folder ID for $folderName (id=$folderId)")
                    }
                }

                appsMovedToFolders += apps.size
            }
        }

        return Pair(foldersCreated, appsMovedToFolders)
    }

    /**
     * Syncs apps to home screen folders.
     *
     * Uses AddFoldersWithItemsTask which automatically finds optimal placement
     * for folders on the workspace, avoiding hardcoded coordinates.
     */
    private suspend fun syncToHomeScreen(
        appsByTab: Map<String, List<String>>,
        appsByPackage: Map<String, List<AppInfo>>,
        tabIconMap: Map<String, String?>,
    ): Pair<Int, Int> {
        var foldersCreated = 0
        var appsMovedToFolders = 0

        val launcher = app.lawnchair.AutoCatLauncher.instance
        if (launcher == null) {
            android.util.Log.e(TAG, "Launcher instance is null, cannot sync to home screen")
            return Pair(0, 0)
        }

        val foldersToAdd = mutableListOf<FolderInfo>()

        appsByTab.forEach { (tabName, packageNames) ->
            val folderName = getFolderName(tabName)

            // Find apps for this category
            val apps = packageNames.flatMap { packageName ->
                appsByPackage[packageName] ?: emptyList()
            }

            if (apps.size >= 2) {
                // Create folder info for multiple apps
                val folderInfo = FolderInfo().apply {
                    title = folderName
                }

                var appsAddedToFolder = 0
                apps.forEach { app ->
                    val workspaceItem = app.makeWorkspaceItem(context)
                    if (workspaceItem != null) {
                        folderInfo.add(workspaceItem)
                        appsMovedToFolders++
                        appsAddedToFolder++
                    }
                }

                if (folderInfo.getContents().isNotEmpty()) {
                    foldersToAdd.add(folderInfo)
                    foldersCreated++
                    android.util.Log.d(TAG, "Prepared home screen folder '$folderName' with $appsAddedToFolder apps")
                }
            } else if (apps.size == 1) {
                // Single app - add directly to workspace via ItemInstallQueue
                val app = apps.first()
                com.android.launcher3.model.ItemInstallQueue.INSTANCE.get(context)
                    .queueItem(app.targetPackage, app.user)
                appsMovedToFolders++
                android.util.Log.d(TAG, "Queued single app '${app.targetPackage}' for workspace")
            }
        }

        if (foldersToAdd.isNotEmpty()) {
            android.util.Log.i(TAG, "Adding ${foldersToAdd.size} folders to home screen with $appsMovedToFolders apps")

            // Add folders with automatic placement (no hardcoded coordinates)
            launcher.model.enqueueModelUpdateTask(
                app.lawnchair.deck.AddFoldersWithItemsTask(foldersToAdd) {
                    android.util.Log.i(TAG, "Home screen folder sync completed successfully")
                },
            )
        } else {
            android.util.Log.d(TAG, "No folders to add to home screen")
        }

        return Pair(foldersCreated, appsMovedToFolders)
    }

    /**
     * Removes all synced folders created by this service.
     */
    suspend fun removeAllSyncedFolders(): Int = withContext(Dispatchers.IO) {
        try {
            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "REMOVE_DRAWER_FOLDERS",
                message = "Removing all drawer folders",
            )

            val folders = try {
                withTimeout(CategorizationConstants.FOLDER_OPERATION_TIMEOUT_MS) {
                    drawerFolderService.getAllFolders()
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.e(TAG, "Timeout getting folders to remove, aborting")
                return@withContext 0
            }

            folders.forEach { folder ->
                drawerFolderService.deleteFolderInfo(folder.id)
            }

            // Reload app drawer to reflect removed folders
            reloadHelper.reloadGrid()

            android.util.Log.i(TAG, "Removed ${folders.size} drawer folders")
            folders.size
        } catch (e: Exception) {
            LLMLogger.logError(
                provider = "CategoryFolderSync",
                operation = "REMOVE_DRAWER_FOLDERS",
                error = e,
            )
            0
        }
    }

    /**
     * Syncs a single tab to a folder.
     */
    suspend fun syncTabToFolder(
        tabName: String,
        packageNames: List<String>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isSyncEnabled()) return@withContext false

        val categorizations = packageNames.associateWith { tabName }
        val result = syncCategoriesToFolders(categorizations)
        result.success
    }

    /**
     * When user deletes a folder in settings, optionally remove tab assignments
     */
    suspend fun onFolderDeleted(folderId: Int, tabName: String, removeTabs: Boolean = false) {
        if (removeTabs) {
            // Remove all AppTab entries for this tab
            val dao = TabDatabase.getInstance(context).tabDao()
            val apps = dao.getAppsByTab(tabName)

            if (apps.isNotEmpty()) {
                dao.deleteAppsByPackageNames(apps.map { it.packageName })
            }

            android.util.Log.d(TAG, "Removed tab assignments for: $tabName")
        } else {
            android.util.Log.d(TAG, "Folder deleted but tab assignments preserved")
        }
    }

    /**
     * When user manually adds/removes apps from a folder, update categories
     */
    suspend fun onFolderItemsChanged(folderId: Int, tabName: String, newAppPackages: List<String>) {
        val dao = TabDatabase.getInstance(context).tabDao()

        // Update AppTab table to match folder contents
        val existingApps = dao.getAppsByTab(tabName).map { it.packageName }

        // Apps added to folder
        val added = newAppPackages - existingApps.toSet()
        // Apps removed from folder
        val removed = existingApps - newAppPackages.toSet()

        added.forEach { packageName ->
            dao.insertAppTab(
                app.lawnchair.data.tab.entities.AppTab(
                    packageName = packageName,
                    tabName = tabName,
                    confidence = 1.0f,
                    source = app.lawnchair.data.tab.entities.AppTab.SOURCE_USER, // User manually assigned
                    isUserOverride = true,
                ),
            )
        }

        if (removed.isNotEmpty()) {
            dao.deleteAppsByPackageNames(removed.toList())
        }

        android.util.Log.d(TAG, "Folder sync: added ${added.size}, removed ${removed.size} apps")
    }

    /**
     * Gets the folder name for a tab.
     * Extracts the last part of a nested tab (e.g. "Games > Puzzle" -> "Puzzle").
     */
    private fun getFolderName(tabName: String): String {
        return tabName.substringAfterLast(" > ")
    }

    /**
     * Result of a folder sync operation
     */
    data class SyncResult(
        val success: Boolean,
        val message: String,
        val foldersCreated: Int,
        val appsMovedToFolders: Int,
        val error: Throwable? = null,
    )
}
