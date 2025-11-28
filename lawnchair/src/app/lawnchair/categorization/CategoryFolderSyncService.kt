package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.data.folder.FolderInfoEntity
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.ModelWriter
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.util.ComponentKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for syncing app categorizations to launcher folders.
 *
 * Supports two folder modes:
 * 1. **App Drawer Folders** (default) - Uses FolderService for drawer organization
 * 2. **Home Screen Folders** - Uses ModelWriter for workspace organization
 *
 * When enabled, this service automatically creates and maintains folders
 * based on app categories assigned by the categorization system.
 */
class CategoryFolderSyncService(
    private val context: Context,
) {

    private val prefs = PreferenceManager.getInstance(context)

    // Drawer folder service
    private val drawerFolderService = FolderService.INSTANCE.get(context)

    // Home screen folder service
    private val appState = LauncherAppState.getInstance(context)
    private val model = appState.model
    private val dataModel: BgDataModel
        get() = model.modelDbController.bgDataModel

    companion object {
        private const val TAG = "CategoryFolderSync"

        // Workspace container ID for home screen
        private const val CONTAINER_DESKTOP = LauncherSettings.Favorites.CONTAINER_DESKTOP

        // Special tag to mark folders created by AutoCat
        private const val AUTOCAT_FOLDER_TAG = "autocat_synced"
    }

    /**
     * Folder sync mode
     */
    enum class FolderSyncMode {
        DRAWER, // Sync to app drawer folders (default, caddy implementation)
        HOME_SCREEN, // Sync to home screen folders
        BOTH, // Sync to both locations
    }

    /**
     * Checks if folder sync is enabled in preferences
     */
    fun isSyncEnabled(): Boolean {
        return prefs.autoCatSyncFolders.get()
    }

    /**
     * Gets the configured folder sync mode.
     * Defaults to DRAWER (the caddy implementation).
     */
    private fun getSyncMode(): FolderSyncMode {
        // For now, default to drawer mode
        // TODO: Add preference for this
        return FolderSyncMode.DRAWER
    }

    /**
     * Syncs all categorized apps to folders.
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

        val mode = getSyncMode()

        LLMLogger.logInfo(
            provider = "CategoryFolderSync",
            operation = "SYNC",
            message = "Starting folder sync (mode: $mode) for ${categorizations.size} categorizations",
        )

        return@withContext when (mode) {
            FolderSyncMode.DRAWER -> syncToDrawerFolders(categorizations)

            FolderSyncMode.HOME_SCREEN -> syncToHomeScreenFolders(categorizations)

            FolderSyncMode.BOTH -> {
                val drawerResult = syncToDrawerFolders(categorizations)
                val homeResult = syncToHomeScreenFolders(categorizations)
                SyncResult(
                    success = drawerResult.success && homeResult.success,
                    message = "Drawer: ${drawerResult.message}; Home: ${homeResult.message}",
                    foldersCreated = drawerResult.foldersCreated + homeResult.foldersCreated,
                    appsMovedToFolders = drawerResult.appsMovedToFolders + homeResult.appsMovedToFolders,
                )
            }
        }
    }

    /**
     * Syncs categorizations to app drawer folders (caddy implementation).
     */
    private suspend fun syncToDrawerFolders(
        categorizations: Map<String, String>,
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Group apps by category
            val appsByCategory = categorizations.entries.groupBy(
                keySelector = { it.value },
                valueTransform = { it.key },
            )

            var foldersCreated = 0
            var appsMovedToFolders = 0

            // Get existing drawer folders
            val existingFolders = drawerFolderService.getAllFolders()
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

                        // Get the created folder ID and add items
                        val createdFolder = drawerFolderService.getAllFolders()
                            .find { it.title.toString() == folderName }

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
     * Syncs categorizations to home screen folders.
     */
    private suspend fun syncToHomeScreenFolders(
        categorizations: Map<String, String>,
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Get ModelWriter to make changes
            val modelWriter = model.modelDbController.getModelWriter(false)

            // Group apps by category
            val appsByCategory = categorizations.entries.groupBy(
                keySelector = { it.value },
                valueTransform = { it.key },
            )

            var foldersCreated = 0
            var appsMovedToFolders = 0

            synchronized(dataModel) {
                // Create a folder for each category
                appsByCategory.forEach { (category, packageNames) ->
                    val folderName = getFolderName(category)

                    // Find existing folder with this name or create new one
                    val folder = findOrCreateHomeScreenFolder(modelWriter, folderName)

                    // Find workspace items for these packages
                    val itemsToAdd = findWorkspaceItems(packageNames)

                    if (itemsToAdd.isNotEmpty()) {
                        LLMLogger.logDebug(
                            provider = "CategoryFolderSync",
                            operation = "SYNC_HOME_FOLDER",
                            message = "Adding ${itemsToAdd.size} apps to home screen folder: $folderName",
                            details = mapOf(
                                "category" to category,
                                "appCount" to itemsToAdd.size,
                            ),
                        )

                        // Add items to folder
                        itemsToAdd.forEach { item ->
                            if (!folder.contents.contains(item)) {
                                folder.add(item, false)
                                item.container = folder.id
                                modelWriter.updateItemInDatabase(item)
                                appsMovedToFolders++
                            }
                        }

                        // Update folder in database
                        modelWriter.updateItemInDatabase(folder)
                        foldersCreated++
                    }
                }
            }

            val result = SyncResult(
                success = true,
                message = "Synced $appsMovedToFolders apps to $foldersCreated home screen folders",
                foldersCreated = foldersCreated,
                appsMovedToFolders = appsMovedToFolders,
            )

            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "SYNC_HOME",
                message = result.message,
            )

            result
        } catch (e: Exception) {
            LLMLogger.logError(
                provider = "CategoryFolderSync",
                operation = "SYNC_HOME",
                error = e,
            )

            SyncResult(
                success = false,
                message = "Home screen folder sync failed: ${e.message}",
                foldersCreated = 0,
                appsMovedToFolders = 0,
                error = e,
            )
        }
    }

    /**
     * Finds or creates a home screen folder with the given name.
     */
    private fun findOrCreateHomeScreenFolder(modelWriter: ModelWriter, folderName: String): FolderInfo {
        // Look for existing folder with this name
        val existingFolder = dataModel.folders.values.find { folder ->
            folder.title?.toString() == folderName && isAutoCatFolder(folder)
        }

        if (existingFolder != null) {
            android.util.Log.d(TAG, "Found existing home screen folder: $folderName")
            return existingFolder
        }

        // Create new folder
        android.util.Log.d(TAG, "Creating new home screen folder: $folderName")
        val folder = FolderInfo()
        folder.title = folderName
        folder.itemType = LauncherSettings.Favorites.ITEM_TYPE_FOLDER

        // Mark as AutoCat folder
        markAsAutoCatFolder(folder)

        // Find an empty spot on the home screen
        val (screenId, cellX, cellY) = findEmptyCell()

        // Add folder to database
        modelWriter.addItemToDatabase(
            folder,
            CONTAINER_DESKTOP,
            screenId,
            cellX,
            cellY,
        )

        return folder
    }

    /**
     * Finds workspace items matching the given package names.
     */
    private fun findWorkspaceItems(packageNames: List<String>): List<WorkspaceItemInfo> {
        val items = mutableListOf<WorkspaceItemInfo>()

        for (item in dataModel.itemsIdMap.values) {
            if (item is WorkspaceItemInfo) {
                val packageName = item.intent?.component?.packageName
                if (packageName in packageNames) {
                    // Only include items on home screen (not already in folders)
                    if (item.container == CONTAINER_DESKTOP) {
                        items.add(item)
                    }
                }
            }
        }

        return items
    }

    /**
     * Finds an empty cell on the home screen for a new folder.
     */
    private fun findEmptyCell(): Triple<Int, Int, Int> {
        // For now, use a simple strategy: screen 0, first available spot
        // In a real implementation, this should use the occupancy matrix
        val screenId = 0
        val cellX = 0
        val cellY = 0
        return Triple(screenId, cellX, cellY)
    }

    /**
     * Marks a folder as created by AutoCat sync.
     */
    private fun markAsAutoCatFolder(folder: FolderInfo) {
        folder.options = folder.options or 0x80000000.toInt()
    }

    /**
     * Checks if a folder was created by AutoCat sync.
     */
    private fun isAutoCatFolder(folder: FolderInfo): Boolean {
        return (folder.options and 0x80000000.toInt()) != 0
    }

    /**
     * Removes all synced folders created by this service.
     */
    suspend fun removeAllSyncedFolders(): Int = withContext(Dispatchers.IO) {
        val mode = getSyncMode()
        when (mode) {
            FolderSyncMode.DRAWER -> removeAllDrawerFolders()

            FolderSyncMode.HOME_SCREEN -> removeAllHomeScreenFolders()

            FolderSyncMode.BOTH -> {
                val drawer = removeAllDrawerFolders()
                val home = removeAllHomeScreenFolders()
                drawer + home
            }
        }
    }

    /**
     * Removes all drawer folders.
     */
    private suspend fun removeAllDrawerFolders(): Int = withContext(Dispatchers.IO) {
        try {
            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "REMOVE_DRAWER_FOLDERS",
                message = "Removing all drawer folders",
            )

            val folders = drawerFolderService.getAllFolders()
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
     * Removes all home screen folders.
     */
    private suspend fun removeAllHomeScreenFolders(): Int = withContext(Dispatchers.IO) {
        try {
            LLMLogger.logInfo(
                provider = "CategoryFolderSync",
                operation = "REMOVE_HOME_FOLDERS",
                message = "Removing all home screen folders",
            )

            val modelWriter = model.modelDbController.getModelWriter(false)
            var removed = 0

            synchronized(dataModel) {
                val foldersToRemove = dataModel.folders.values.filter { isAutoCatFolder(it) }

                foldersToRemove.forEach { folder ->
                    // Move all items out of folder first
                    val items = ArrayList(folder.contents)
                    items.forEach { item ->
                        if (item is WorkspaceItemInfo) {
                            item.container = CONTAINER_DESKTOP
                            modelWriter.updateItemInDatabase(item)
                        }
                    }

                    // Delete the folder
                    modelWriter.deleteItemFromDatabase(folder, "AutoCat sync removal")
                    removed++
                }
            }

            android.util.Log.i(TAG, "Removed $removed home screen folders")
            removed
        } catch (e: Exception) {
            LLMLogger.logError(
                provider = "CategoryFolderSync",
                operation = "REMOVE_HOME_FOLDERS",
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
