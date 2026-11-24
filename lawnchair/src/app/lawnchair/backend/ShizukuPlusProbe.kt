package app.lawnchair.backend

import android.os.IBinder
import android.os.Parcel
import rikka.shizuku.Shizuku

/**
 * Detects ShizukuPlus from stock `dev.rikka.shizuku:api` with the same raw transaction as
 * the Plus API's `Shizuku.isCustomApiEnabled()`. Any failure means "not Plus".
 */
internal object ShizukuPlusProbe {
    private const val DESCRIPTOR = "moe.shizuku.server.IShizukuService"
    private const val TRANSACTION_IS_CUSTOM_API_ENABLED = 10002

    fun isShizukuPlus(): Boolean {
        val binder: IBinder = try {
            if (!Shizuku.pingBinder()) return false
            Shizuku.getBinder() ?: return false
        } catch (_: Throwable) {
            return false
        }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(DESCRIPTOR)
            if (binder.transact(TRANSACTION_IS_CUSTOM_API_ENABLED, data, reply, 0)) {
                reply.readException()
                reply.readInt() != 0
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
