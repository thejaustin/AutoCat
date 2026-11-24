package app.lawnchair.root

import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.lawnchair.shizuku.UserService
import com.topjohnwu.superuser.ipc.RootService

class RootHelper : RootService() {
    override fun onBind(intent: Intent): IBinder = when (intent.action) {
        ACTION_TASKS -> RootUserService(this)
        else -> RootHelperBackend(this)
    }

    companion object {
        /** Binds the root-hosted IUserService (recent tasks) instead of [RootHelperBackend]. */
        const val ACTION_TASKS = "app.lawnchair.root.action.TASKS"
    }
}

/**
 * The same task logic as the Shizuku-hosted [UserService], running inside the root service.
 *
 * UNTESTED: written without a working `su` (root is denied on the dev device). The shared
 * implementation is exercised through Shizuku; the only new part is this hosting.
 */
class RootUserService(context: Context) : UserService(context) {
    /** The root service process is shared with [RootHelperBackend], so don't exit it. */
    override fun destroy() = Unit
}
