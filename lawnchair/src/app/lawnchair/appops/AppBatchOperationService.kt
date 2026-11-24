package app.lawnchair.appops

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import app.lawnchair.backend.AppStateOps
import app.lawnchair.backend.AppStateResult
import app.lawnchair.backend.Backend
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for performing batch app operations (archive, uninstall).
 * Privileged work goes through [AppStateOps], which asks the BackendSelector and respects the
 * user's backend toggles and pinned choice (a pinned backend that is unavailable fails loudly).
 * With no privileged backend in play it falls back to:
 * 1. API 35 PackageInstaller.requestArchive() (if installer-of-record)
 * 2. System intent (user confirms each)
 */
class AppBatchOperationService(private val context: Context) {

    private val ops = AppStateOps(context)

    companion object {
        private const val TAG = "AppBatchOpService"
        private const val ARCHIVE_REQUEST_CODE_BASE = 9000

        private fun AppStateResult.toOperationResult(): OperationResult = when (this) {
            AppStateResult.Success -> OperationResult.Success
            is AppStateResult.Failed -> OperationResult.Failed(reason)
            AppStateResult.NoBackend -> OperationResult.RequiresUserConfirmation
        }
    }

    /**
     * Result of a single app operation.
     */
    sealed class OperationResult {
        data object Success : OperationResult()
        data class Failed(val reason: String) : OperationResult()
        data object Skipped : OperationResult()
        data object RequiresUserConfirmation : OperationResult()
    }

    /**
     * Callback for batch operation progress.
     */
    data class BatchProgress(
        val current: Int,
        val total: Int,
        val currentPackage: String,
        val results: Map<String, OperationResult>,
    )

    /**
     * Determines the best available archive method based on device capabilities.
     */
    suspend fun getArchiveMethod(): ArchiveMethod = withContext(Dispatchers.IO) {
        val resolution = ops.resolve()
        if (resolution.pinnedUnavailable != null) {
            return@withContext ArchiveMethod.Unavailable(resolution.failureReason())
        }
        when (resolution.backends.firstOrNull()) {
            Backend.Root -> if (Build.VERSION.SDK_INT >= 35) ArchiveMethod.RootArchive else ArchiveMethod.RootUninstallKeepData
            Backend.ShizukuPlus, Backend.Shizuku -> ArchiveMethod.ShizukuArchive
            else -> if (Build.VERSION.SDK_INT >= 35) ArchiveMethod.Api35Archive else ArchiveMethod.IntentFallback
        }
    }

    /**
     * Archive a single app using the best available method.
     * Returns the operation result.
     */
    suspend fun archiveApp(packageName: String): OperationResult = withContext(Dispatchers.IO) {
        if (isSystemApp(packageName)) {
            return@withContext OperationResult.Skipped
        }

        val method = getArchiveMethod()
        executeArchive(packageName, method)
    }

    /**
     * Unarchive (Restore) a single app.
     */
    suspend fun unarchiveApp(packageName: String): OperationResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val installer = context.packageManager.packageInstaller
                val intent = Intent("app.lawnchair.UNARCHIVE_STATUS")
                    .setPackage(context.packageName)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ARCHIVE_REQUEST_CODE_BASE + packageName.hashCode() + 1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                installer.requestUnarchive(packageName, pendingIntent.intentSender)
                OperationResult.Success
            } catch (e: Exception) {
                Log.e(TAG, "requestUnarchive failed", e)
                OperationResult.Failed(e.message ?: "Unknown error")
            }
        } else {
            OperationResult.RequiresUserConfirmation
        }
    }

    /**
     * Archive all non-system apps belonging to a specific tab / category.
     */
    suspend fun archiveCategoryApps(
        categoryName: String,
        onProgress: (BatchProgress) -> Unit,
    ): Map<String, OperationResult> = withContext(Dispatchers.IO) {
        val tabDao = app.lawnchair.data.tab.TabDatabase.getInstance(context).categoryDao()
        val categoryApps = tabDao.getAppsByTab(categoryName).map { it.packageName }
        archiveApps(categoryApps, onProgress)
    }

    /**
     * Archive multiple apps with progress reporting.
     */
    suspend fun archiveApps(
        packages: List<String>,
        onProgress: (BatchProgress) -> Unit,
    ): Map<String, OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, OperationResult>()
        val method = getArchiveMethod()
        val filteredPackages = packages.filter { !isSystemApp(it) }
        val skippedCount = packages.size - filteredPackages.size

        // Mark skipped system apps
        packages.filter { isSystemApp(it) }.forEach { pkg ->
            results[pkg] = OperationResult.Skipped
        }

        filteredPackages.forEachIndexed { index, pkg ->
            onProgress(
                BatchProgress(
                    current = index + 1,
                    total = filteredPackages.size,
                    currentPackage = pkg,
                    results = results.toMap(),
                ),
            )

            val result = executeArchive(pkg, method)
            results[pkg] = result
        }

        results
    }

    /**
     * Uninstall a single app.
     * @param keepData If true, keeps app data (similar to archive behavior).
     */
    suspend fun uninstallApp(packageName: String, keepData: Boolean = false): OperationResult = withContext(Dispatchers.IO) {
        if (isSystemApp(packageName)) {
            return@withContext OperationResult.Skipped
        }

        ops.uninstall(packageName, keepData).toOperationResult()
    }

    /**
     * Uninstall multiple apps with progress reporting.
     */
    suspend fun uninstallApps(
        packages: List<String>,
        keepData: Boolean = false,
        onProgress: (BatchProgress) -> Unit,
    ): Map<String, OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, OperationResult>()
        val filteredPackages = packages.filter { !isSystemApp(it) }

        packages.filter { isSystemApp(it) }.forEach { pkg ->
            results[pkg] = OperationResult.Skipped
        }

        filteredPackages.forEachIndexed { index, pkg ->
            onProgress(
                BatchProgress(
                    current = index + 1,
                    total = filteredPackages.size,
                    currentPackage = pkg,
                    results = results.toMap(),
                ),
            )

            val result = uninstallApp(pkg, keepData)
            results[pkg] = result
        }

        results
    }

    /**
     * Filter out system apps from a list of package names.
     */
    fun filterSystemApps(packages: List<String>): List<String> {
        return packages.filter { !isSystemApp(it) }
    }

    /**
     * Disable a single app.
     */
    suspend fun disableApp(packageName: String): OperationResult = withContext(Dispatchers.IO) {
        ops.disable(packageName).toOperationResult()
    }

    /**
     * Enable a single app.
     */
    suspend fun enableApp(packageName: String): OperationResult = withContext(Dispatchers.IO) {
        ops.enable(packageName).toOperationResult()
    }

    /**
     * Get the count of system apps in a package list (for UI display).
     */
    fun countSystemApps(packages: List<String>): Int {
        return packages.count { isSystemApp(it) }
    }

    /**
     * Check if a package is a system app.
     */
    fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get the display label for a package.
     */
    fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private suspend fun executeArchive(packageName: String, method: ArchiveMethod): OperationResult {
        return when (method) {
            is ArchiveMethod.Api35Archive -> executeApi35Archive(packageName)

            is ArchiveMethod.RootArchive,
            is ArchiveMethod.ShizukuArchive,
            is ArchiveMethod.RootUninstallKeepData,
            -> ops.archive(packageName).toOperationResult()

            is ArchiveMethod.IntentFallback -> OperationResult.RequiresUserConfirmation

            is ArchiveMethod.Unavailable -> OperationResult.Failed(method.reason)
        }
    }

    private suspend fun executeApi35Archive(packageName: String): OperationResult {
        return if (Build.VERSION.SDK_INT >= 35) {
            try {
                val installer = context.packageManager.packageInstaller
                val intent = Intent("app.lawnchair.ARCHIVE_STATUS")
                    .setPackage(context.packageName)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ARCHIVE_REQUEST_CODE_BASE + packageName.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                installer.requestArchive(packageName, pendingIntent.intentSender)
                OperationResult.Success
            } catch (e: SecurityException) {
                // Not the installer of record: only a privileged backend can still do it
                Log.w(TAG, "requestArchive failed (not installer of record): ${e.message}")
                ops.archive(packageName).toOperationResult()
            } catch (e: Exception) {
                Log.e(TAG, "requestArchive failed", e)
                OperationResult.Failed(e.message ?: "Unknown error")
            }
        } else {
            OperationResult.Failed("API 35 required")
        }
    }

    /**
     * Unarchive (Restore) multiple apps with progress reporting.
     */
    suspend fun unarchiveApps(
        packages: List<String>,
        onProgress: (BatchProgress) -> Unit,
    ): Map<String, OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, OperationResult>()

        packages.forEachIndexed { index, pkg ->
            onProgress(
                BatchProgress(
                    current = index + 1,
                    total = packages.size,
                    currentPackage = pkg,
                    results = results.toMap(),
                ),
            )

            val result = unarchiveApp(pkg)
            results[pkg] = result
        }

        results
    }

    /**
     * Enable multiple apps with progress reporting.
     */
    suspend fun enableApps(
        packages: List<String>,
        onProgress: (BatchProgress) -> Unit,
    ): Map<String, OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, OperationResult>()

        packages.forEachIndexed { index, pkg ->
            onProgress(
                BatchProgress(
                    current = index + 1,
                    total = packages.size,
                    currentPackage = pkg,
                    results = results.toMap(),
                ),
            )

            val result = enableApp(pkg)
            results[pkg] = result
        }

        results
    }

    /**
     * Install an APK file using the best available method.
     */
    suspend fun installApk(file: File): OperationResult = withContext(Dispatchers.IO) {
        ops.install(file).toOperationResult()
    }

    /**
     * Disable multiple apps with progress reporting.
     */
    suspend fun disableApps(
        packages: List<String>,
        onProgress: (BatchProgress) -> Unit,
    ): Map<String, OperationResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, OperationResult>()

        packages.forEachIndexed { index, pkg ->
            onProgress(
                BatchProgress(
                    current = index + 1,
                    total = packages.size,
                    currentPackage = pkg,
                    results = results.toMap(),
                ),
            )

            val result = disableApp(pkg)
            results[pkg] = result
        }

        results
    }

    /**
     * Create an intent to launch the system uninstall dialog for a package.
     * Used as fallback when root is not available.
     */
    fun createUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = android.net.Uri.fromParts("package", packageName, null)
            putExtra(Intent.EXTRA_RETURN_RESULT, true)
        }
    }
}
