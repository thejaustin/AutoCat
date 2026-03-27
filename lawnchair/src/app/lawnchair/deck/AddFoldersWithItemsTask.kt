package app.lawnchair.deck

import android.content.Intent
import android.os.UserHandle
import android.util.Log
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherModel
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.AllAppsList
import com.android.launcher3.model.BgDataModel
import com.android.launcher3.model.ModelTaskController
import com.android.launcher3.model.WorkspaceItemSpaceFinder
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.util.IntArray
import com.android.launcher3.util.PackageManagerHelper

/**
 * Custom model task to add folders with their items to the workspace.
 * This properly handles adding folders and then adding items to those folders.
 *
 * Uses WorkspaceItemSpaceFinder to automatically find optimal placement,
 * avoiding hardcoded coordinates and ensuring folders don't overlap.
 */
class AddFoldersWithItemsTask(
    private val folders: List<FolderInfo>,
    private val onComplete: (() -> Unit)? = null,
) : LauncherModel.ModelUpdateTask {

    companion object {
        private const val TAG = "AddFoldersWithItems"
    }

    override fun execute(
        taskController: ModelTaskController,
        dataModel: BgDataModel,
        apps: AllAppsList,
    ) {
        val context = taskController.context

        val idp = InvariantDeviceProfile.INSTANCE.get(context)
        val model = LauncherAppState.getInstance(context).model
        val itemSpaceFinder = WorkspaceItemSpaceFinder(dataModel, idp, model)

        if (folders.isEmpty()) {
            Log.w(TAG, "No folders to add")
            return
        }

        val addedItemsFinal = ArrayList<ItemInfo>()
        val addedWorkspaceScreensFinal = IntArray()
        var foldersAdded = 0
        var itemsAdded = 0

        synchronized(dataModel) {
            val workspaceScreens = dataModel.itemsIdMap.collectWorkspaceScreens(context)
            val modelWriter = taskController.getModelWriter()

            folders.forEach { folderInfo ->
                try {
                    // Find space for the folder using automatic placement
                    val coords = itemSpaceFinder.findSpaceForItem(
                        workspaceScreens,
                        addedWorkspaceScreensFinal,
                        addedItemsFinal,
                        folderInfo.spanX,
                        folderInfo.spanY,
                        context,
                    )
                    val screenId = coords[0]
                    val cellX = coords[1]
                    val cellY = coords[2]

                    Log.d(TAG, "Placing folder '${folderInfo.title}' at screen=$screenId, cell=($cellX,$cellY)")

                    // Add folder to database
                    modelWriter.addItemToDatabase(
                        folderInfo,
                        LauncherSettings.Favorites.CONTAINER_DESKTOP,
                        screenId,
                        cellX,
                        cellY,
                    )

                    // Now add items to the folder
                    // Items need to be added with proper rank/position
                    val folderContents = folderInfo.getContents()
                    folderContents.forEachIndexed { index, item ->
                        if (item is WorkspaceItemInfo) {
                            // Check if item already exists on workspace
                            if (shortcutExists(dataModel, item.intent, item.user)) {
                                Log.d(TAG, "Skipping duplicate item: ${item.title}")
                                return@forEachIndexed
                            }

                            // Add item to folder using folder's ID as container
                            // Use rank as position - folder will arrange items
                            modelWriter.addOrMoveItemInDatabase(
                                item,
                                folderInfo.id,
                                0, // screenId is 0 for items in folders
                                index % 4, // cellX - approximate grid position
                                index / 4, // cellY - approximate grid position
                            )
                            itemsAdded++
                            Log.d(TAG, "Added '${item.title}' to folder at rank $index")
                        }
                    }

                    addedItemsFinal.add(folderInfo)
                    foldersAdded++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add folder '${folderInfo.title}': ${e.message}", e)
                }
            }
        }

        Log.i(TAG, "Successfully added $foldersAdded folders with $itemsAdded items")

        // Schedule callback to bind items
        if (addedItemsFinal.isNotEmpty()) {
            taskController.scheduleCallbackTask { callbacks ->

                callbacks.bindItemsAdded(addedItemsFinal)

                // Notify completion after items are bound
                onComplete?.invoke()
            }
        } else {
            Log.w(TAG, "No items were added to the workspace")
            // No items to add, notify completion immediately
            onComplete?.invoke()
        }
    }

    /**
     * Returns true if the shortcut already exists on the workspace.
     * Based on AddWorkspaceItemsTask.shortcutExists
     */
    private fun shortcutExists(
        dataModel: BgDataModel,
        intent: Intent?,
        user: UserHandle,
    ): Boolean {
        if (intent == null) {
            return true
        }

        val compPkgName: String?
        val intentWithPkg: String
        val intentWithoutPkg: String

        if (intent.component != null) {
            compPkgName = intent.component!!.packageName
            if (intent.`package` != null) {
                intentWithPkg = intent.toUri(0)
                intentWithoutPkg = Intent(intent).apply { `package` = null }.toUri(0)
            } else {
                intentWithPkg = Intent(intent).apply { `package` = compPkgName }.toUri(0)
                intentWithoutPkg = intent.toUri(0)
            }
        } else {
            compPkgName = null
            intentWithPkg = intent.toUri(0)
            intentWithoutPkg = intent.toUri(0)
        }

        val isLauncherAppTarget = PackageManagerHelper.isLauncherAppTarget(intent)

        synchronized(dataModel) {
            dataModel.itemsIdMap.forEach { existingItem ->
                if (existingItem is WorkspaceItemInfo) {
                    val existingIntent = existingItem.intent
                    if (existingIntent != null && existingItem.user == user) {
                        val copyIntent = Intent(existingIntent)
                        copyIntent.sourceBounds = intent.sourceBounds
                        val s = copyIntent.toUri(0)
                        if (intentWithPkg == s || intentWithoutPkg == s) {
                            return true
                        }

                        // Check for existing promise icon with same package name
                        if (isLauncherAppTarget &&
                            existingItem.isPromise() &&
                            existingItem.hasStatusFlag(WorkspaceItemInfo.FLAG_AUTOINSTALL_ICON) &&
                            existingItem.targetComponent != null &&
                            compPkgName != null &&
                            compPkgName == existingItem.targetComponent!!.packageName
                        ) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}
