package app.lawnchair.gestures.handlers

import android.content.Context
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.model.data.FolderInfo

class OpenFolderGestureHandler(context: Context) : GestureHandler(context) {

    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        // Fallback or do nothing if no item info
    }

    override suspend fun onTrigger(launcher: LawnchairLauncher, itemInfo: Any?) {
        if (!launcher.isStarted || itemInfo !is FolderInfo) return

        val workspace = launcher.workspace
        val folderIcon = workspace.getFirstMatch { info, view ->
            info == itemInfo && view is FolderIcon
        } as? FolderIcon

        if (folderIcon != null) {
            folderIcon.post {
                if (!folderIcon.folder.isOpen) {
                    folderIcon.folder.animateOpen()
                }
            }
        }
    }
}
