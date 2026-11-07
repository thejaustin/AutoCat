package app.lawnchair.shizuku

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

/**
 * Runs in a separate process as the shell user, started by Shizuku. Task access goes
 * through hidden framework APIs by reflection, because their signatures change between
 * Android versions.
 */
class UserService(context: Context) : IUserService.Stub() {

    private val activityTaskManager: Any by lazy {
        Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null)!!
    }

    override fun runCommand(command: Array<String>, env: Array<String>?): Int {
        return try {
            Runtime.getRuntime().exec(command, env).waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: ${command.joinToString(" ")}", e)
            -1
        }
    }

    override fun getRecentTasks(maxTasks: Int, userId: Int): Bundle {
        return try {
            val slice = activityTaskManager.javaClass
                .getMethod("getRecentTasks", Int::class.java, Int::class.java, Int::class.java)
                .invoke(activityTaskManager, maxTasks, ActivityManager.RECENT_IGNORE_UNAVAILABLE, userId)
            val tasks = slice!!.javaClass.getMethod("getList").invoke(slice) as List<*>
            val recent = tasks.filterIsInstance<ActivityManager.RecentTaskInfo>()
            Bundle().apply {
                putIntArray(KEY_TASK_IDS, recent.map { it.taskId }.toIntArray())
                putStringArray(
                    KEY_COMPONENTS,
                    recent.map { (it.topActivity ?: it.baseIntent.component)?.flattenToString().orEmpty() }
                        .toTypedArray(),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't read recent tasks", e)
            Bundle()
        }
    }

    override fun getTaskThumbnail(taskId: Int): Bundle {
        return try {
            val snapshot = getTaskSnapshot(taskId) ?: return Bundle()
            val buffer = snapshotBuffer(snapshot) ?: return Bundle()
            Bundle().apply { putParcelable(KEY_BUFFER, buffer) }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't read a thumbnail for task $taskId", e)
            Bundle()
        }
    }

    override fun startTaskFromRecents(taskId: Int, options: Bundle?): Boolean {
        return try {
            val result = activityTaskManager.javaClass
                .getMethod("startActivityFromRecents", Int::class.java, Bundle::class.java)
                .invoke(activityTaskManager, taskId, options) as Int
            result >= 0
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't open task $taskId", e)
            false
        }
    }

    override fun removeTask(taskId: Int): Boolean {
        return try {
            activityTaskManager.javaClass.getMethod("removeTask", Int::class.java)
                .invoke(activityTaskManager, taskId) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't remove task $taskId", e)
            false
        }
    }

    /**
     * Android 17 moved snapshots to ITaskSnapshotManager; older versions have them on
     * IActivityTaskManager.
     */
    private fun getTaskSnapshot(taskId: Int): Any? {
        val atm = activityTaskManager
        val snapshotManager = atm.javaClass.methods
            .firstOrNull { it.name == "getTaskSnapshotManager" && it.parameterCount == 0 }
            ?.invoke(atm)
        if (snapshotManager != null) {
            return snapshotManager.javaClass
                .getMethod("getTaskSnapshot", Int::class.java, Long::class.java, Int::class.java)
                .invoke(snapshotManager, taskId, 0L, RESOLUTION_HIGH)
        }
        val method = atm.javaClass.methods.firstOrNull { it.name == "getTaskSnapshot" } ?: return null
        val args: List<Any?> = method.parameterTypes.mapIndexed { index, type ->
            when {
                index == 0 -> taskId

                // isLowResolution: a phone-sized thumbnail is plenty
                type == Boolean::class.java -> index == 1

                else -> null
            }
        }
        return method.invoke(atm, *args.toTypedArray())
    }

    private fun snapshotBuffer(snapshot: Any): HardwareBuffer? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bitmap = runCatching {
                snapshot.javaClass.getMethod("wrapToBitmap").invoke(snapshot) as Bitmap?
            }.getOrNull()
            if (bitmap != null && bitmap.config == Bitmap.Config.HARDWARE) {
                return bitmap.hardwareBuffer
            }
        }
        return runCatching {
            snapshot.javaClass.getMethod("getHardwareBuffer").invoke(snapshot) as HardwareBuffer?
        }.getOrNull()
    }

    override fun destroy() {
        exitProcess(0)
    }

    companion object {
        private const val TAG = "UserService"
        private const val RESOLUTION_HIGH = 1

        const val KEY_TASK_IDS = "taskIds"
        const val KEY_COMPONENTS = "components"
        const val KEY_BUFFER = "buffer"
    }
}
