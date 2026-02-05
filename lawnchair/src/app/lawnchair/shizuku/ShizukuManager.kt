package app.lawnchair.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.dagger.ApplicationContext
import com.android.launcher3.dagger.LauncherAppComponent
import com.android.launcher3.dagger.LauncherAppSingleton
import com.android.launcher3.util.DaggerSingletonObject
import rikka.shizuku.Shizuku
import javax.inject.Inject

@LauncherAppSingleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager
) {

    private var userService: IUserService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null && service.pingBinder()) {
                userService = IUserService.Stub.asInterface(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, UserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(true)
        .version(1)

    init {
        Shizuku.addBinderReceivedListenerSticky {
            if (isShizukuEnabled()) {
                bindService()
            }
        }
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_PERMISSION && grantResult == PackageManager.PERMISSION_GRANTED) {
                bindService()
            }
        }
    }

    fun isShizukuEnabled(): Boolean {
        return preferenceManager.archivalMethod.get() == "shizuku"
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun checkAndRequestPermission(): Boolean {
        if (!isShizukuAvailable()) return false

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            return false
        } else {
            Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
            return false
        }
    }

    fun bindService() {
        if (!isShizukuAvailable() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return
        }
        Shizuku.bindUserService(userServiceArgs, serviceConnection)
    }

    fun unbindService() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        userService = null
    }

    fun archiveApp(packageName: String): Boolean {
        return runCommand(arrayOf("pm", "uninstall", "-k", packageName))
    }

    fun disableApp(packageName: String): Boolean {
        return runCommand(arrayOf("pm", "disable-user", "--user", "0", packageName))
    }

    fun enableApp(packageName: String): Boolean {
         return runCommand(arrayOf("pm", "enable", packageName))
    }

    private fun runCommand(command: Array<String>): Boolean {
        if (userService == null) {
            bindService()
            if (userService == null) return false
        }

        return try {
            userService?.runCommand(command, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSION = 1001

        @JvmField
        val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getShizukuManager)

        @JvmStatic
        fun getInstance(context: Context): ShizukuManager {
            return INSTANCE.get(context)!!
        }
    }
}
