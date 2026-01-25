package app.lawnchair.appops

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for performing batch app operations (archive, uninstall).
 * Uses a privilege fallback hierarchy similar to SleepGestureHandler:
 * 1. API 35 PackageInstaller.requestArchive() (if installer-of-record)
 * 2. Root shell: pm archive (Android 15+)
 * 3. Root shell: pm uninstall -k (keeps data)
 * 4. System intent fallback (user confirms each)
 */
class AppBatchOperationService(private val context: Context) {

    companion object {
        private const val TAG = "AppBatchOpService"
        private const val ARCHIVE_REQUEST_CODE_BASE = 9000
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
        val hasRoot = try {
            Shell.getShell().isRoot
        } catch (e: Exception) {
            false
        }

        when {
            Build.VERSION.SDK_INT >= 35 && hasRoot -> ArchiveMethod.RootArchive
            hasRoot -> ArchiveMethod.RootUninstallKeepData
            Build.VERSION.SDK_INT >= 35 -> ArchiveMethod.Api35Archive
            else -> ArchiveMethod.IntentFallback
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
    suspend fun uninstallApp(packageName: String, keepData: Boolean = false): OperationResult =
        withContext(Dispatchers.IO) {
            if (isSystemApp(packageName)) {
                return@withContext OperationResult.Skipped
            }

            val hasRoot = try {
                Shell.getShell().isRoot
            } catch (e: Exception) {
                false
            }

            if (hasRoot) {
                val flag = if (keepData) "-k" else ""
                val result = Shell.cmd("pm uninstall $flag $packageName").exec()
                if (result.isSuccess) {
                    OperationResult.Success
                } else {
                    OperationResult.Failed(result.err.joinToString("\n"))
                }
            } else {
                OperationResult.RequiresUserConfirmation
            }
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

    private fun executeArchive(packageName: String, method: ArchiveMethod): OperationResult {
        return when (method) {
            is ArchiveMethod.Api35Archive -> executeApi35Archive(packageName)
            is ArchiveMethod.RootArchive -> executeRootArchive(packageName)
            is ArchiveMethod.RootUninstallKeepData -> executeRootUninstallKeepData(packageName)
            is ArchiveMethod.IntentFallback -> OperationResult.RequiresUserConfirmation
        }
    }

    private fun executeApi35Archive(packageName: String): OperationResult {
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
                // Not the installer of record — fall back to root or intent
                Log.w(TAG, "requestArchive failed (not installer of record): ${e.message}")
                val hasRoot = try { Shell.getShell().isRoot } catch (_: Exception) { false }
                if (hasRoot) {
                    executeRootArchive(packageName)
                } else {
                    OperationResult.RequiresUserConfirmation
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestArchive failed", e)
                OperationResult.Failed(e.message ?: "Unknown error")
            }
        } else {
            OperationResult.Failed("API 35 required")
        }
    }

    private fun executeRootArchive(packageName: String): OperationResult {
        // `pm archive` is available on Android 15+ (API 35+) with root
        return if (Build.VERSION.SDK_INT >= 35) {
            val result = Shell.cmd("pm archive $packageName").exec()
            if (result.isSuccess) {
                OperationResult.Success
            } else {
                // Fallback to uninstall -k if pm archive is not available on this ROM
                Log.w(TAG, "pm archive failed, falling back to uninstall -k: ${result.err}")
                executeRootUninstallKeepData(packageName)
            }
        } else {
            executeRootUninstallKeepData(packageName)
        }
    }

    private fun executeRootUninstallKeepData(packageName: String): OperationResult {
        val result = Shell.cmd("pm uninstall -k $packageName").exec()
        return if (result.isSuccess) {
            OperationResult.Success
        } else {
            OperationResult.Failed(result.err.joinToString("\n"))
        }
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
