package app.lawnchair.root

import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.lawnchair.shizuku.UserService
import com.topjohnwu.superuser.ipc.RootService

class RootHelper : RootService() {
    override fun onBind(intent: Intent) = RootHelperBackend(this)
}

/**
 * Hosts [RootUserService] in its own service, so its binder can't be mixed up with
 * [RootHelper]'s (libsu tracks root services per component).
 */
class RootTaskService : RootService() {
    override fun onBind(intent: Intent): IBinder = RootUserService(this)
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
