package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.deck.AddFoldersWithItemsTask
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.ReloadHelper
import com.android.launcher3.LauncherAppState
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.pm.UserCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
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
) {

    private val prefs by lazy { PreferenceManager.getInstance(context) }
    private val drawerFolderService by lazy { FolderService.INSTANCE.get(context) }
    private val reloadHelper by lazy { ReloadHelper(context) }

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

            // Group apps by tab, excluding "Other" and system categories
            val appsByTab = categorizations.entries
                .filter { (_, tabName) ->
                    // Exclude "Other" tab and empty tab names
                    tabName.isNotEmpty() && tabName != "Other"
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
                withTimeout(10000) {
                    // 10 second timeout
                    drawerFolderService.getAllFolders()
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.w(TAG, "Timeout getting folders, continuing with empty list")
                emptyList()
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
            val customTabs = TabDatabase.getInstance(context).categoryDao().getAllCustomCategories()
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
                syncToHomeScreen(appsByTab, appsByPackage)
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
                        icon = folderIcon,
                    )
                    android.util.Log.d(TAG, "Updated drawer folder: $folderName (${apps.size} apps)")
                } else {
                    // Create new folder with ID
                    val newFolder = FolderInfo().apply {
                        title = folderName
                        // Launcher3 auto-generates ID if not set
                    }
                    drawerFolderService.saveFolderInfo(newFolder)

                    // Use database query instead of timeout-based getAllFolders() - much faster
                    kotlinx.coroutines.delay(100) // Small delay for DB write
                    val folderId = try {
                        // Get folder ID from Room directly (no timeout)
                        val allFolders = drawerFolderService.getAllFolders()
                        allFolders.find { it.title.toString() == folderName }?.id
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "Failed to get folder ID for $folderName", e)
                        null
                    }

                    if (folderId != null) {
                        drawerFolderService.updateFolderWithItems(
                            folderInfoId = folderId,
                            title = folderName,
                            appInfos = apps,
                            icon = folderIcon,
                        )
                        android.util.Log.d(TAG, "Created drawer folder: $folderName (${apps.size} apps)")
                        foldersCreated++
                    }
                }

                appsMovedToFolders += apps.size
            }
        }

        return Pair(foldersCreated, appsMovedToFolders)
    }

    /**
     * Syncs apps to home screen folders.
     * Uses AddFoldersWithItemsTask for automatic placement.
     */
    private fun syncToHomeScreen(
        appsByTab: Map<String, List<String>>,
        appsByPackage: Map<String, List<AppInfo>>,
    ): Pair<Int, Int> {
        val foldersToAdd = mutableListOf<FolderInfo>()
        var appsAdded = 0

        appsByTab.forEach { (tabName, packageNames) ->
            val folderName = getFolderName(tabName)
            val apps = packageNames.flatMap { appsByPackage[it] ?: emptyList() }

            if (apps.isNotEmpty()) {
                val folderInfo = FolderInfo().apply {
                    title = folderName
                }

                apps.forEach { appInfo ->
                    val workspaceItem = WorkspaceItemInfo(appInfo)
                    folderInfo.add(workspaceItem, false)
                    appsAdded++
                }

                foldersToAdd.add(folderInfo)
            }
        }

        if (foldersToAdd.isNotEmpty()) {
            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "SYNC_HOME_SCREEN",
                message = "Adding ${foldersToAdd.size} folders to home screen",
            )
            val model = LauncherAppState.getInstance(context).model
            val task = AddFoldersWithItemsTask(foldersToAdd)
            model.enqueueModelUpdateTask(task)
        }

        return Pair(foldersToAdd.size, appsAdded)
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
                withTimeout(10000) {
                    drawerFolderService.getAllFolders()
                }
            } catch (e: TimeoutCancellationException) {
                android.util.Log.w(TAG, "Timeout getting folders to remove")
                emptyList()
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
            val dao = TabDatabase.getInstance(context).categoryDao()
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
        val dao = TabDatabase.getInstance(context).categoryDao()

        // Update AppTab table to match folder contents
        val existingApps = dao.getAppsByTab(tabName).map { it.packageName }

        // Apps added to folder
        val added = newAppPackages - existingApps.toSet()
        // Apps removed from folder
        val removed = existingApps - newAppPackages.toSet()

        added.forEach { packageName ->
            dao.insertAppCategory(
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
