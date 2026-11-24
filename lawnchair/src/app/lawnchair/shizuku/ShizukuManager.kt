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
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

@LauncherAppSingleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceManager: PreferenceManager,
) {

    private var userService: IUserService? = null
    private var serviceReady = CompletableDeferred<IUserService>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null && service.pingBinder()) {
                val connected = IUserService.Stub.asInterface(service)
                userService = connected
                serviceReady.complete(connected)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
            serviceReady = CompletableDeferred()
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, UserService::class.java.name),
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(true)
        .version(2)

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

    /** Whether the user lets AutoCat use any Shizuku-family backend (Privileged access toggles). */
    fun isShizukuEnabled(): Boolean {
        return preferenceManager.backendShizukuEnabled.get() || preferenceManager.backendShizukuPlusEnabled.get()
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

    /**
     * The shell service, binding it first if needed. Returns null when Shizuku isn't running,
     * permission hasn't been granted, or the service doesn't connect within [timeoutMs].
     */
    suspend fun awaitService(timeoutMs: Long = 5_000): IUserService? {
        userService?.let { return it }
        if (!isShizukuAvailable() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        bindService()
        return withTimeoutOrNull(timeoutMs) { serviceReady.await() }
    }

    fun hasPermission(): Boolean {
        return isShizukuAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun unbindService() {
        Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        userService = null
    }

    companion object {
        private const val TAG = "ShizukuManager"
        private const val REQUEST_CODE_PERMISSION = 1001

        @JvmField
        val INSTANCE = DaggerSingletonObject(LauncherAppComponent::getShizukuManager)

        @JvmStatic
        fun getInstance(context: Context): ShizukuManager {
            return INSTANCE.get(context)
                ?: throw IllegalStateException("ShizukuManager not initialized. Ensure Dagger component is set up.")
        }
    }
}
