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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Service for syncing app categorizations to app drawer folders.
 *
 * When enabled, this service automatically creates and maintains folders
 * in the app drawer based on app categories assigned by the categorization system.
 *
 * Uses the caddy folder implementation (FolderService + Room DB).
 */
class CategoryFolderSyncService(
    private val context: Context,
) {

    private val prefs = PreferenceManager.getInstance(context)
    private val drawerFolderService = FolderService.INSTANCE.get(context)
    private val reloadHelper = ReloadHelper(context)

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
     * Syncs all categorized apps to drawer folders.
     *
     * Creates folders for each category and adds apps to them.
     * Only affects apps that have been categorized.
     *
     * @param categorizations Map of package name → category name
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
            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "SYNC",
                message = "Starting folder sync for ${categorizations.size} categorizations",
            )

            // Group apps by category, excluding "Other" and system categories
            val appsByCategory = categorizations.entries
                .filter { (_, category) ->
                    // Exclude "Other" category and empty categories
                    category.isNotEmpty() && category != "Other"
                }
                .groupBy(
                    keySelector = { it.value },
                    valueTransform = { it.key },
                )

            android.util.Log.d(TAG, "Categorizations: ${categorizations.size} total, ${appsByCategory.size} categories")
            appsByCategory.forEach { (category, packages) ->
                android.util.Log.d(TAG, "Category '$category': ${packages.size} packages - ${packages.take(3)}" + if (packages.size > 3) "..." else "")
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
            val categoryIconMap = customTabs.associate { it.name to it.icon }

            // Create/update folder for each category (FAST - parallel friendly)
            appsByCategory.forEach { (category, packageNames) ->
                val folderName = getFolderName(category)
                val folderIcon = categoryIconMap[category]

                LLMLogger.logDebug(
                    provider = "CategoryFolderSync",
                    operation = "SYNC_DRAWER_FOLDER",
                    message = "Syncing drawer folder: $folderName",
                    details = mapOf(
                        "category" to category,
                        "appCount" to packageNames.size,
                        "icon" to (folderIcon ?: "none"),
                    ),
                )

                // Find apps for this category (FAST - map lookup)
                val apps = packageNames.flatMap { packageName ->
                    val matchedApps = appsByPackage[packageName] ?: emptyList()
                    if (matchedApps.isEmpty()) {
                        android.util.Log.w(TAG, "No apps found for package: $packageName in category: $category")
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

            val result = SyncResult(
                success = true,
                message = "Synced $appsMovedToFolders apps to $foldersCreated drawer folders",
                foldersCreated = foldersCreated,
                appsMovedToFolders = appsMovedToFolders,
            )

            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "SYNC_DRAWER",
                message = result.message,
            )

            // Reload app drawer to display new/updated folders
            reloadHelper.reloadGrid()
            android.util.Log.d(TAG, "Triggered app drawer reload to display folders")

            result
        } catch (e: Exception) {
            LLMLogger.logError(
                provider = "CategoryFolderSync",
                operation = "SYNC_DRAWER",
                error = e,
            )

            SyncResult(
                success = false,
                message = "Drawer folder sync failed: ${e.message}",
                foldersCreated = 0,
                appsMovedToFolders = 0,
                error = e,
            )
        }
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
     * Syncs a single category to a folder.
     */
    suspend fun syncCategoryToFolder(
        category: String,
        packageNames: List<String>,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isSyncEnabled()) return@withContext false

        val categorizations = packageNames.associateWith { category }
        val result = syncCategoriesToFolders(categorizations)
        result.success
    }

    /**
     * When user deletes a folder in settings, optionally remove category assignments
     */
    suspend fun onFolderDeleted(folderId: Int, categoryName: String, removeCategories: Boolean = false) {
        if (removeCategories) {
            // Remove all AppCategory entries for this category
            val dao = TabDatabase.getInstance(context).categoryDao()
            val apps = dao.getAppsByCategory(categoryName)
            
            if (apps.isNotEmpty()) {
                dao.deleteAppsByPackageNames(apps.map { it.packageName })
            }
            
            android.util.Log.d(TAG, "Removed category assignments for: $categoryName")
        } else {
            android.util.Log.d(TAG, "Folder deleted but category assignments preserved")
        }
    }

    /**
     * When user manually adds/removes apps from a folder, update categories
     */
    suspend fun onFolderItemsChanged(folderId: Int, categoryName: String, newAppPackages: List<String>) {
        val dao = TabDatabase.getInstance(context).categoryDao()
        
        // Update AppCategory table to match folder contents
        val existingApps = dao.getAppsByCategory(categoryName).map { it.packageName }

        // Apps added to folder
        val added = newAppPackages - existingApps.toSet()
        // Apps removed from folder
        val removed = existingApps - newAppPackages.toSet()

        added.forEach { packageName ->
            dao.insertAppCategory(
                app.lawnchair.data.tab.entities.AppTab(
                    packageName = packageName,
                    category = categoryName,
                    confidence = 1.0f,
                    source = app.lawnchair.data.tab.entities.AppTab.SOURCE_USER,  // User manually assigned
                    isUserOverride = true
                )
            )
        }

        if (removed.isNotEmpty()) {
            dao.deleteAppsByPackageNames(removed.toList())
        }

        android.util.Log.d(TAG, "Folder sync: added ${added.size}, removed ${removed.size} apps")
    }

    /**
     * Gets the folder name for a category.
     * Extracts the last part of a nested category (e.g. "Games > Puzzle" -> "Puzzle").
     */
    private fun getFolderName(category: String): String {
        return category.substringAfterLast(" > ")
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
