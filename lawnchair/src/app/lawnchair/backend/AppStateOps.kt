package app.lawnchair.backend

import android.content.Context
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.shizuku.ShizukuManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface AppStateResult {
    data object Success : AppStateResult
    data class Failed(val reason: String) : AppStateResult

    /** No privileged backend is in play (none available, or the user opted out); caller falls back to the user. */
    data object NoBackend : AppStateResult
}

/**
 * Archive, disable, enable and install through the [BackendSelector]. A pinned or disabled
 * backend is never replaced silently: if the choice can't be served the result is [AppStateResult.Failed].
 *
 * The older "Archival method" preference still applies: `none` opts out of privileged access,
 * `root` and `shizuku` pin that backend for these operations, anything else follows the global mode.
 */
class AppStateOps(private val context: Context) {

    private val selector = BackendSelector.getInstance(context)
    private val shizuku = ShizukuManager.getInstance(context)
    private val prefs = PreferenceManager.getInstance(context)

    /** Which backend will do archiving etc., for callers that need to describe it. Null means no privileged one. */
    suspend fun resolve(): Resolution {
        val override = when (prefs.archivalMethod.get()) {
            "none" -> return Resolution(Capability.AppState, emptyList())
            "root" -> BackendMode.Root
            "shizuku" -> BackendMode.Shizuku
            else -> null
        }
        return selector.resolve(Capability.AppState, override)
    }

    suspend fun archive(packageName: String) = execute { PackageOps.archive(it, packageName) }

    suspend fun uninstall(packageName: String, keepData: Boolean) = execute { PackageOps.uninstall(it, packageName, keepData) }

    suspend fun disable(packageName: String) = execute { PackageOps.disable(it, packageName) }

    suspend fun enable(packageName: String) = execute { PackageOps.enable(it, packageName) }

    suspend fun install(file: File) = execute { PackageOps.install(it, file) }

    private suspend fun execute(
        allowed: (Backend) -> Boolean = { true },
        op: (PmExecutor) -> PmResult,
    ): AppStateResult = withContext(Dispatchers.IO) {
        val resolution = resolve().let { it.copy(backends = it.backends.filter(allowed)) }
        if (resolution.isEmpty) {
            return@withContext if (resolution.pinnedUnavailable != null) {
                AppStateResult.Failed(resolution.failureReason())
            } else {
                AppStateResult.NoBackend
            }
        }
        var lastError = "no backend could run the command"
        for (backend in resolution.backends) {
            val executor = executorFor(backend)
            if (executor == null) {
                lastError = "${backend.name} service isn't connected"
                continue
            }
            val result = op(executor)
            if (result.success) return@withContext AppStateResult.Success
            lastError = "${backend.name}: ${result.error.ifBlank { "command failed" }}"
        }
        AppStateResult.Failed(lastError)
    }

    private suspend fun executorFor(backend: Backend): PmExecutor? = when (backend) {
        Backend.Root -> RootPmExecutor
        Backend.ShizukuPlus, Backend.Shizuku -> shizuku.awaitService()?.let(::ServicePmExecutor)
        else -> null
    }
}
