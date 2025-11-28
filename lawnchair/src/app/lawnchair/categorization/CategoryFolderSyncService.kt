package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.preferences.PreferenceManager
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
     * @return SyncResult with statistics
     */
    suspend fun syncCategoriesToFolders(
        categorizations: Map<String, String>,
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

            // Get UserCache for creating AppInfo
            val userCache = UserCache.INSTANCE.get(context)
            val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)

            // Create/update folder for each category
            appsByCategory.forEach { (category, packageNames) ->
                val folderName = getFolderName(category)

                LLMLogger.logDebug(
                    provider = "CategoryFolderSync",
                    operation = "SYNC_DRAWER_FOLDER",
                    message = "Syncing drawer folder: $folderName",
                    details = mapOf(
                        "category" to category,
                        "appCount" to packageNames.size,
                    ),
                )

                // Find apps for this category
                val apps = mutableListOf<AppInfo>()
                packageNames.forEach { packageName ->
                    // Get AppInfo for each package
                    userCache.userProfiles.forEach { userHandle ->
                        try {
                            val activities = launcherApps?.getActivityList(packageName, userHandle)
                            activities?.forEach { launcherActivityInfo ->
                                apps.add(AppInfo(context, launcherActivityInfo, userHandle))
                            }
                        } catch (e: Exception) {
                            android.util.Log.w(TAG, "Failed to get AppInfo for $packageName", e)
                        }
                    }
                }

                if (apps.isNotEmpty()) {
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
                        // Create new folder
                        val newFolder = FolderInfo().apply {
                            title = folderName
                        }
                        drawerFolderService.saveFolderInfo(newFolder)

                        // Get the created folder ID and add items (with timeout)
                        val createdFolder = try {
                            withTimeout(10000) {
                                drawerFolderService.getAllFolders()
                            }
                        } catch (e: TimeoutCancellationException) {
                            android.util.Log.w(TAG, "Timeout getting created folder")
                            emptyList()
                        }.find { it.title.toString() == folderName }

                        if (createdFolder != null) {
                            drawerFolderService.updateFolderWithItems(
                                folderInfoId = createdFolder.id,
                                title = folderName,
                                appInfos = apps,
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
     * Gets the folder name for a category.
     */
    private fun getFolderName(category: String): String {
        return category
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
