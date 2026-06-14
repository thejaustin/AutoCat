package app.lawnchair.util

import android.content.Context
import android.os.Process
import android.os.UserHandle
import app.lawnchair.LawnchairLauncher
import app.lawnchair.launcher
import com.android.launcher3.BuildConfig
import com.android.quickstep.views.RecentsView
import com.android.systemui.shared.recents.model.Task
import com.android.systemui.shared.system.ActivityManagerWrapper

/**
 * Helper utilities for managing recent tasks.
 */
object RecentHelper {

    /**
     * Clears all task stacks from recent apps, except:
     * - Locked apps (via [isAppLocked])
     * - The launcher itself
     * - Tasks with locked state (via [TaskUtilLockState])
     *
     * If an error occurs during selective removal, falls back to removing all recent tasks.
     */
    fun clearAllTaskStacks(context: Context) {
        try {
            val launcher = context.launcher
            val recentsView = launcher.getOverviewPanel<RecentsView<LawnchairLauncher, *>>()
            val taskViewCount = recentsView.getTaskViewCount()
            val currentUserId = Process.myUserHandle().identifier
            for (i in 0..taskViewCount) {
                try {
                    val rawTasks = ActivityManagerWrapper.getInstance()
                        .getRecentTasks(i, currentUserId)
                    for (recentTaskInfo in rawTasks) {
                        var packageName = recentTaskInfo.baseIntent.component?.packageName
                        val taskKey = Task.TaskKey(recentTaskInfo)
                        val taskId = taskKey.id
                        packageName = packageName?.replace("unknown", "")
                        if (!packageName.isNullOrEmpty()) {
                            packageName += "#" + UserHandle.getUserId(taskId)
                            val component = taskKey.baseIntent.component
                            val taskLockState = component?.let {
                                TaskUtilLockState.getTaskLockState(context, it, taskKey)
                            } ?: false

                            val shouldRemoveTask = !isAppLocked(packageName, context) &&
                                !packageName.contains(BuildConfig.APPLICATION_ID) &&
                                !taskLockState

                            if (shouldRemoveTask) {
                                ActivityManagerWrapper.getInstance().removeTask(taskId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (exception: Exception) {
            ActivityManagerWrapper.getInstance().removeAllRecentTasks()
        }
    }

    fun isAppLocked(packageName: String, context: Context): Boolean {
        val pref = context.getSharedPreferences(
            LawnchairLockedStateController.TASK_LOCK_STATE,
            Context.MODE_PRIVATE,
        )

        val lockedApps = pref.getStringSet(
            LawnchairLockedStateController.TASK_LOCK_LIST_KEY_WITH_USERID,
            emptySet(),
        )

        return lockedApps?.any { it.contains(packageName) } ?: false
    }
}
