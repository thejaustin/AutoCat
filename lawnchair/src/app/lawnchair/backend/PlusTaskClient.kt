package app.lawnchair.backend

import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import app.lawnchair.shizuku.IUserService
import rikka.shizuku.Shizuku

/** The recent-task calls AutoCat needs, whichever privileged service hosts them. */
interface TaskOps {
    /** Recent tasks newest first: int[] "taskIds", String[] "components". Empty bundle on failure. */
    fun getRecentTasks(maxTasks: Int, userId: Int): Bundle
    fun getTaskThumbnail(taskId: Int): Bundle
    fun startTaskFromRecents(taskId: Int, options: Bundle?): Boolean
    fun removeTask(taskId: Int): Boolean
}

/** [TaskOps] served by AutoCat's own [IUserService] (root or Shizuku hosted). */
class UserServiceTaskOps(private val service: IUserService) : TaskOps {
    override fun getRecentTasks(maxTasks: Int, userId: Int): Bundle = service.getRecentTasks(maxTasks, userId)
    override fun getTaskThumbnail(taskId: Int): Bundle = service.getTaskThumbnail(taskId)
    override fun startTaskFromRecents(taskId: Int, options: Bundle?): Boolean = service.startTaskFromRecents(taskId, options)
    override fun removeTask(taskId: Int): Boolean = service.removeTask(taskId)
}

/**
 * Hand-written client for ShizukuPlus's `af.shizuku.server.ITaskManagerPlus`, reached through
 * `IShizukuService.getTaskManagerPlus()` (transaction 130) on the Shizuku binder.
 *
 * This exists because AutoCat pins stock `dev.rikka.shizuku:api`. It can be replaced by the
 * generated stub (and `ShizukuPlusAPI.Tasks`) once the Plus API artifact is published.
 * Transaction codes follow the declaration order in ITaskManagerPlus.aidl.
 */
class PlusTaskClient private constructor(private val binder: IBinder) : TaskOps {

    val isAlive get() = binder.pingBinder()

    fun linkToDeath(onDied: () -> Unit): Boolean = try {
        binder.linkToDeath({ onDied() }, 0)
        true
    } catch (_: Throwable) {
        false
    }

    /** The Plus server ignores [userId] and uses the calling app's user. */
    override fun getRecentTasks(maxTasks: Int, userId: Int): Bundle = callBundle(TX_GET_RECENT_TASKS) {
        writeInt(maxTasks)
        writeInt(0)
    }

    override fun getTaskThumbnail(taskId: Int): Bundle = callBundle(TX_GET_TASK_THUMBNAIL) { writeInt(taskId) }

    override fun startTaskFromRecents(taskId: Int, options: Bundle?): Boolean = callBoolean(TX_START_TASK_FROM_RECENTS) {
        writeInt(taskId)
        if (options != null) {
            writeInt(1)
            options.writeToParcel(this, 0)
        } else {
            writeInt(0)
        }
    }

    override fun removeTask(taskId: Int): Boolean = callBoolean(TX_REMOVE_TASK) { writeInt(taskId) }

    /** True when the server answers with a real result rather than an empty failure bundle. */
    fun probe(): Boolean = getRecentTasks(1, 0).containsKey(KEY_TASK_IDS)

    private inline fun transact(code: Int, write: Parcel.() -> Unit, read: Parcel.() -> Unit) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR)
            data.write()
            binder.transact(code, data, reply, 0)
            reply.readException()
            reply.read()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    private inline fun callBundle(code: Int, crossinline write: Parcel.() -> Unit): Bundle {
        var result = Bundle.EMPTY
        try {
            transact(code, { write() }) {
                if (readInt() != 0) {
                    val bundle = Bundle.CREATOR.createFromParcel(this)
                    bundle.classLoader = PlusTaskClient::class.java.classLoader
                    // Unparcel now: the reply parcel is recycled when this returns
                    bundle.keySet()
                    result = bundle
                }
            }
        } catch (_: Throwable) {
            return Bundle.EMPTY
        }
        return result
    }

    private inline fun callBoolean(code: Int, crossinline write: Parcel.() -> Unit): Boolean {
        var result = false
        try {
            transact(code, { write() }) { result = readInt() != 0 }
        } catch (_: Throwable) {
            return false
        }
        return result
    }

    companion object {
        private const val DESCRIPTOR = "af.shizuku.server.ITaskManagerPlus"
        private const val SHIZUKU_DESCRIPTOR = "moe.shizuku.server.IShizukuService"
        private const val TX_GET_TASK_MANAGER_PLUS = 130
        private const val TX_GET_RECENT_TASKS = IBinder.FIRST_CALL_TRANSACTION
        private const val TX_GET_TASK_THUMBNAIL = IBinder.FIRST_CALL_TRANSACTION + 1
        private const val TX_START_TASK_FROM_RECENTS = IBinder.FIRST_CALL_TRANSACTION + 2
        private const val TX_REMOVE_TASK = IBinder.FIRST_CALL_TRANSACTION + 3
        private const val KEY_TASK_IDS = "taskIds"

        /** Asks the Shizuku server for its Plus task binder. Null when stock, disabled or on any failure. */
        fun obtain(): PlusTaskClient? {
            val shizukuBinder = try {
                if (!Shizuku.pingBinder()) return null
                Shizuku.getBinder() ?: return null
            } catch (_: Throwable) {
                return null
            }
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            return try {
                data.writeInterfaceToken(SHIZUKU_DESCRIPTOR)
                if (!shizukuBinder.transact(TX_GET_TASK_MANAGER_PLUS, data, reply, 0)) return null
                reply.readException()
                reply.readStrongBinder()?.let(::PlusTaskClient)
            } catch (_: Throwable) {
                null
            } finally {
                data.recycle()
                reply.recycle()
            }
        }
    }
}
