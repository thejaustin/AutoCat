package app.lawnchair.data.folder.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.util.Log
import app.lawnchair.categorization.CategoryFolderSyncService
import app.lawnchair.data.AppDatabase
import app.lawnchair.data.Converters
import app.lawnchair.data.folder.FolderInfoEntity
import app.lawnchair.data.toEntity
import com.android.launcher3.AppFilter
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.launcher3.util.SafeCloseable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppSingleton
import javax.inject.Inject

@LauncherAppSingleton
class FolderService @Inject constructor(@ApplicationContext val context: Context) : SafeCloseable {

    private val database by lazy { AppDatabase.INSTANCE.get(context) }
    private val folderDao by lazy { database.folderDao() }
    private val launcherApps by lazy { context.getSystemService(LauncherApps::class.java) }
    private val userCache by lazy { UserCache.INSTANCE.get(context) }
    private val appFilter by lazy { AppFilter(context) }
    private val converters = Converters()

    fun getFoldersFlow(): Flow<List<FolderInfo>> {
        return folderDao.getAllFolders().map { folderEntities ->
            folderEntities.mapNotNull { folderEntity ->
                getFolderInfo(folderEntity.id, true)
            }
        }
    }

    suspend fun updateFolderWithItems(folderInfoId: Int, title: String, appInfos: List<AppInfo>, icon: String? = null) = withContext(Dispatchers.IO) {
        folderDao.insertFolderWithItems(
            FolderInfoEntity(id = folderInfoId, title = title, icon = icon),
            appInfos.mapIndexed { index, appInfo ->
                appInfo.toEntity(folderInfoId).copy(rank = index)
            }.toList(),
        )
        // Bidirectional sync: Update categories when folder contents change
        // TODO: Implement CategoryFolderSyncService
        // CategoryFolderSyncService.getInstance(context).onFolderItemsChanged(
        //     folderId = folderInfoId,
        //     categoryName = title,
        //     newAppPackages = appInfos.mapNotNull { it.componentName?.packageName },
        // )
    }

    suspend fun saveFolderInfo(folderInfo: FolderInfo) = withContext(Dispatchers.IO) {
        folderDao.insertFolder(
            FolderInfoEntity(
                title = folderInfo.title.toString(),
                icon = folderInfo.icon,
                coverMode = folderInfo.coverMode,
                coverAppComponent = folderInfo.coverApp?.flattenToString(),
            ),
        )
    }

    suspend fun updateFolderInfo(folderInfo: FolderInfo, hide: Boolean = false) = withContext(Dispatchers.IO) {
        folderDao.updateFolderInfo(folderInfo.id, folderInfo.title.toString(), hide)
    }

    suspend fun updateFolderCover(folderId: Int, coverMode: Boolean, coverApp: ComponentName?) = withContext(Dispatchers.IO) {
        folderDao.updateFolderCover(folderId, coverMode, coverApp?.flattenToString())
    }

    suspend fun deleteFolderInfo(id: Int) = withContext(Dispatchers.IO) {
        // Bidirectional sync: Notify when folder is deleted
        // TODO: Implement CategoryFolderSyncService
        // val folder = getFolderInfo(id, true)
        // if (folder != null) {
        //     CategoryFolderSyncService.getInstance(context).onFolderDeleted(
        //         folderId = id,
        //         categoryName = folder.title.toString(),
        //         removeCategories = false, // Don't delete categories by default when folder is deleted
        //     )
        // }
        folderDao.deleteFolder(id)
    }

    suspend fun getFolderInfo(folderId: Int, hasId: Boolean = false): FolderInfo? = withContext(Dispatchers.Default) {
        folderDao.getFolderWithItems(folderId)?.let {
            mapToFolderInfo(it, hasId)
        }
    }

    private fun mapToFolderInfo(folderWithItems: FolderWithItems, hasId: Boolean): FolderInfo? {
        return try {
            val domainFolderInfo = FolderInfo().apply {
                // if no id, launcher automatically creates an id for this
                if (hasId) id = folderWithItems.folder.id
                title = folderWithItems.folder.title
                icon = folderWithItems.folder.icon
                // New fields for Folder Cover Mode
                coverMode = folderWithItems.folder.coverMode
                coverApp = folderWithItems.folder.coverAppComponent?.let { ComponentName.unflattenFromString(it) }
            }

            folderWithItems.items.sortedBy { it.rank }.forEach { itemEntity ->
                // Consider caching toItemInfo results if componentKey lookups are slow
                // and items don't change frequently without folder data changing
                toItemInfo(itemEntity.componentKey)?.let { appInfo ->
                    domainFolderInfo.add(appInfo, false)
                }
            }
            domainFolderInfo
        } catch (e: Exception) {
            Log.e("FolderService", "Failed to map FolderWithItems for id: ${folderWithItems.folder.id}", e)
            null
        }
    }

    private fun toItemInfo(componentKey: String?): AppInfo? {
        launcherApps?.let { service ->
            return userCache.userProfiles.asSequence()
                .flatMap { service.getActivityList(null, it) }
                .filter { appFilter.shouldShowApp(it.componentName) }
                .map { AppInfo(context, it, it.user) }
                .filter { converters.fromComponentKey(it.componentKey) == componentKey }
                .firstOrNull()
        }
        return null
    }

    suspend fun getAllFolders(): List<FolderInfo> = withContext(Dispatchers.IO) {
        try {
            val folderEntities = folderDao.getAllFolders().firstOrNull() ?: emptyList()
            folderEntities.mapNotNull { folderEntity ->
                getFolderInfo(folderEntity.id, true)
            }
        } catch (e: Exception) {
            Log.e("FolderService", "Failed to get all folders", e)
            emptyList()
        }
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject(::FolderService)
    }
}
