package app.lawnchair.backend

import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import app.lawnchair.autoCatApp
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.root.RootHelperManager
import app.lawnchair.shizuku.ShizukuManager
import app.lawnchair.util.MainThreadInitializedObject
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Chooses which privileged backend serves a [Capability], honouring the per-backend toggles
 * and the [BackendMode] the user picked in settings.
 *
 * Availability checks that can block (root shell, binder transaction, package lookup) only
 * happen in suspend functions on the IO dispatcher and are cached. The `...Now` functions
 * read cached state only and are safe on the main thread.
 */
class BackendSelector(context: Context) {

    private val context = context.applicationContext
    private val prefs = PreferenceManager.getInstance(this.context)
    private val shizuku get() = ShizukuManager.getInstance(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var rootProbe: Boolean? = null

    @Volatile private var rootProbeAt = 0L

    @Volatile private var plusProbe: Boolean? = null

    /** Cached Plus task binder; dropped when it or the Shizuku server dies. */
    @Volatile private var plusTasks: PlusTaskClient? = null

    init {
        // A new or dead server may be a different flavour of Shizuku
        Shizuku.addBinderReceivedListener {
            plusProbe = null
            plusTasks = null
        }
        Shizuku.addBinderDeadListener {
            plusProbe = null
            plusTasks = null
        }
    }

    /** Warms the root and Plus caches off the main thread so the next [resolveNow] sees them. */
    fun refreshInBackground() {
        scope.launch {
            Backend.entries.forEach { isAvailable(it) }
        }
    }

    fun mode(): BackendMode = BackendMode.fromString(prefs.backendMode.get())

    fun isEnabled(backend: Backend): Boolean = when (backend) {
        Backend.Root -> prefs.backendRootEnabled.get()
        Backend.ShizukuPlus -> prefs.backendShizukuPlusEnabled.get()
        Backend.Shizuku -> prefs.backendShizukuEnabled.get()
        Backend.Dhizuku -> prefs.backendDhizukuEnabled.get()
        Backend.Accessibility -> prefs.backendAccessibilityEnabled.get()
    }

    // region availability

    private suspend fun awaitShizukuBinder(timeoutMs: Long = 3_000) {
        var waited = 0L
        while (waited < timeoutMs && !(runCatching { Shizuku.pingBinder() }.getOrDefault(false))) {
            delay(POLL_MS)
            waited += POLL_MS
        }
    }

    private fun shizukuUsableNow(): Boolean = try {
        shizuku.hasPermission()
    } catch (_: Throwable) {
        false
    }

    private fun rootUsableNow(): Boolean = rootProbe == true || Shell.isAppGrantedRoot() == true

    private fun accessibilityUsableNow(): Boolean = context.autoCatApp.isAccessibilityServiceBound()

    /** Cached, never blocks. Root and ShizukuPlus may read false until a suspend check ran. */
    fun isAvailableNow(backend: Backend): Boolean = when (backend) {
        Backend.Root -> rootUsableNow()
        Backend.ShizukuPlus -> shizukuUsableNow() && plusProbe == true
        Backend.Shizuku -> shizukuUsableNow()
        Backend.Dhizuku -> false
        Backend.Accessibility -> accessibilityUsableNow()
    }

    /** Whether the device really has the backend, ignoring the user's toggle. May block, so IO only. */
    suspend fun isAvailable(backend: Backend, forceRefresh: Boolean = false): Boolean = when (backend) {
        Backend.Root -> probeRoot(forceRefresh)
        Backend.ShizukuPlus -> shizukuUsableNow() && probePlus(forceRefresh)
        Backend.Shizuku -> shizukuUsableNow()
        Backend.Dhizuku -> false
        Backend.Accessibility -> accessibilityUsableNow()
    }

    private suspend fun probeRoot(force: Boolean): Boolean {
        if (rootUsableNow() && !force) return true
        val cached = rootProbe
        // A denied root may be granted later, so re-probe a cached "no" after a while
        if (!force && cached == false && SystemClock.elapsedRealtime() - rootProbeAt < ROOT_RETRY_MS) return false
        return withContext(Dispatchers.IO) {
            val granted = try {
                Shell.getShell().isRoot
            } catch (_: Throwable) {
                false
            }
            rootProbe = granted
            rootProbeAt = SystemClock.elapsedRealtime()
            granted
        }
    }

    private suspend fun probePlus(force: Boolean): Boolean {
        plusProbe?.let { if (!force) return it }
        return withContext(Dispatchers.IO) {
            ShizukuPlusProbe.isShizukuPlus().also { plusProbe = it }
        }
    }

    private fun dhizukuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(DHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Throwable) {
        false
    }

    /** Live status for the settings screen. Ignores toggles, describes the device. */
    suspend fun status(backend: Backend, forceRefresh: Boolean = false): BackendStatus {
        if (isAvailable(backend, forceRefresh)) return BackendStatus.Available
        return when (backend) {
            Backend.Dhizuku -> if (withContext(Dispatchers.IO) { dhizukuInstalled() }) {
                BackendStatus.Detected
            } else {
                BackendStatus.Unavailable
            }

            // Plus is running but AutoCat has no Shizuku permission yet
            Backend.ShizukuPlus -> if (Shizuku.pingBinder() && probePlus(forceRefresh)) {
                BackendStatus.Detected
            } else {
                BackendStatus.Unavailable
            }

            else -> BackendStatus.Unavailable
        }
    }

    suspend fun statusAll(forceRefresh: Boolean = false): Map<Backend, BackendStatus> = Backend.entries.associateWith { status(it, forceRefresh) }

    // endregion

    // region resolution

    /**
     * The backends to try for [capability], best first. [modeOverride] replaces the user's mode
     * for callers with a legacy per-feature choice.
     */
    suspend fun resolve(capability: Capability, modeOverride: BackendMode? = null): Resolution {
        val usable = capability.supportedBackends
            .filter { isEnabled(it) && isAvailable(it) }
            .toSet()
        return pick(capability, modeOverride ?: mode(), usable)
    }

    /** Main-thread-safe [resolve] from cached state. */
    fun resolveNow(capability: Capability, modeOverride: BackendMode? = null): Resolution {
        val usable = capability.supportedBackends
            .filter { isEnabled(it) && isAvailableNow(it) }
            .toSet()
        return pick(capability, modeOverride ?: mode(), usable)
    }

    private fun pick(capability: Capability, mode: BackendMode, usable: Set<Backend>): Resolution {
        val pinned = mode.pinned
        if (pinned != null) {
            return if (pinned in usable) {
                Resolution(capability, listOf(pinned))
            } else {
                Resolution(capability, emptyList(), pinnedUnavailable = pinned)
            }
        }
        // ShizukuPlus and Shizuku are the same transport: trying both would just repeat a failure
        val ordered = capability.supportedBackends.filter { it in usable }
            .filterNot { it == Backend.Shizuku && Backend.ShizukuPlus in usable }
        return Resolution(capability, ordered)
    }

    // endregion

    /** The Plus task client, from cache or freshly obtained and probed. Null falls through to the UserService. */
    private suspend fun plusTaskClient(): PlusTaskClient? {
        plusTasks?.let { if (it.isAlive) return it }
        plusTasks = null
        // On a cold start the binder arrives a moment after the activity opens; without waiting,
        // the Plus path would always be skipped and the UserService used instead.
        awaitShizukuBinder()
        if (!shizukuUsableNow()) return null
        val client = PlusTaskClient.obtain() ?: return null
        if (!client.probe()) return null
        client.linkToDeath { if (plusTasks === client) plusTasks = null }
        plusTasks = client
        return client
    }

    /**
     * The task calls for [Capability.TaskControl]: ShizukuPlus's task API when a Plus server is
     * detected and answers, otherwise the IUserService hosted by root or Shizuku. Null when no
     * backend can provide it.
     */
    suspend fun taskService(): TaskOps? {
        for (backend in resolve(Capability.TaskControl).backends) {
            val ops: TaskOps? = when (backend) {
                Backend.Root -> try {
                    RootHelperManager.INSTANCE.get(context).getTaskService()?.let(::UserServiceTaskOps)
                } catch (_: Exception) {
                    // Includes RootNotAvailableException and remote failures: try the next backend
                    null
                }

                Backend.ShizukuPlus -> withContext(Dispatchers.IO) { plusTaskClient() }
                    ?: shizuku.awaitService()?.let(::UserServiceTaskOps)

                Backend.Shizuku -> shizuku.awaitService()?.let(::UserServiceTaskOps)

                else -> null
            }
            if (ops != null) return ops
        }
        return null
    }

    companion object {
        private const val ROOT_RETRY_MS = 30_000L
        private const val POLL_MS = 100L
        const val DHIZUKU_PACKAGE = "com.rosan.dhizuku"

        @JvmField
        val INSTANCE = MainThreadInitializedObject(::BackendSelector)

        @JvmStatic
        fun getInstance(context: Context): BackendSelector = INSTANCE.get(context)
    }
}
