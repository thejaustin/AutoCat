package app.lawnchair.root

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import app.lawnchair.shizuku.IUserService
import app.lawnchair.util.MainThreadInitializedObject
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class RootHelperManager(private val context: Context) {
    private val scope = MainScope() + CoroutineName("RootHelperManager")
    private val isAvailableDeferred by lazy {
        scope.async(Dispatchers.IO) {
            Shell.rootAccess()
        }
    }
    private var rootHelperDeferred: Deferred<IRootHelper>? = null

    suspend fun isAvailable() = isAvailableDeferred.await()

    private var taskServiceDeferred: Deferred<IUserService>? = null

    @Throws(RootNotAvailableException::class)
    suspend fun getService(): IRootHelper {
        if (!isAvailable()) throw RootNotAvailableException()

        if (rootHelperDeferred == null) {
            rootHelperDeferred = scope.async {
                bindImpl(Intent(context, RootHelper::class.java), IRootHelper.Stub::asInterface) {
                    rootHelperDeferred = null
                }
            }
        }
        return rootHelperDeferred!!.await()
    }

    /**
     * The recent-tasks service hosted in the root process. Untested without a granted su.
     * Throws [RootNotAvailableException] if root is missing or the bind doesn't complete in time,
     * so the caller can fall through to another backend.
     */
    @Throws(RootNotAvailableException::class)
    suspend fun getTaskService(): IUserService {
        if (!isAvailable()) throw RootNotAvailableException()

        val deferred = taskServiceDeferred ?: scope.async {
            bindImpl(Intent(context, RootTaskService::class.java), IUserService.Stub::asInterface) {
                taskServiceDeferred = null
            }
        }.also { taskServiceDeferred = it }
        val service = try {
            withTimeoutOrNull(BIND_TIMEOUT_MS) { deferred.await() }
        } catch (_: Exception) {
            null
        }
        if (service == null) {
            deferred.cancel()
            taskServiceDeferred = null
            throw RootNotAvailableException()
        }
        return service
    }

    private suspend fun <T> bindImpl(intent: Intent, asInterface: (IBinder) -> T, onDisconnected: () -> Unit): T {
        return withContext(Dispatchers.IO) {
            suspendCoroutine {
                val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder) {
                        it.resume(asInterface(service))
                    }

                    override fun onServiceDisconnected(name: ComponentName) {
                        onDisconnected()
                    }
                }
                RootService.bind(intent, connection)
            }
        }
    }

    companion object {
        private const val BIND_TIMEOUT_MS = 8_000L
        val INSTANCE = MainThreadInitializedObject(::RootHelperManager)
    }
}
