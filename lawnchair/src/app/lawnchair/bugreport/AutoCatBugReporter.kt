package app.lawnchair.bugreport

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import app.lawnchair.AutoCatApp
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.util.MainThreadInitializedObject
import app.lawnchair.util.requireSystemService
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import com.patrykmichalik.opto.core.firstBlocking
import com.patrykmichalik.opto.core.setBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class AutoCatBugReporter(private val context: Context) {

    private val notificationManager: NotificationManager = context.requireSystemService()
    private val logsFolder by lazy { File(context.cacheDir, "logs").apply { mkdirs() } }
    private val appName by lazy { context.getString(R.string.derived_app_name) }

    init {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                BugReportReceiver.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.bugreport_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                BugReportReceiver.STATUS_CHANNEL_ID,
                context.getString(R.string.status_channel_name),
                NotificationManager.IMPORTANCE_NONE,
            ),
        )

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val report = sendNotification(throwable)
            if (report != null) {
                PreferenceManager2.getInstance(context).lastCrashId.setBlocking(report.id)
                if (PreferenceManager2.getInstance(context).autoCrashReporting.firstBlocking()) {
                    context.startService(
                        android.content.Intent(context, UploaderService::class.java)
                            .putExtra("report", report),
                    )
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        removeOldLogs()
    }

    fun getReport(id: Int): BugReport? {
        val folder = File(logsFolder, String.format("%x", id))
        val file = folder.listFiles()?.firstOrNull { it.extension == "txt" } ?: return null
        val contents = file.readText()
        val lines = contents.lines()
        val title = lines.firstOrNull() ?: ""
        val remainingContents = lines.drop(1).joinToString("\n")
        return BugReport(id, BugReport.TYPE_UNCAUGHT_EXCEPTION, "", remainingContents, file)
    }

    private fun removeOldLogs() {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        logsFolder.listFiles()?.forEach { file ->
            if (file.lastModified() < sevenDaysAgo) {
                file.deleteRecursively()
            }
        }
    }

    private fun sendNotification(throwable: Throwable): BugReport? {
        val bugReport = Report(BugReport.TYPE_UNCAUGHT_EXCEPTION, throwable)
            .generateBugReport() ?: return null

        val notifications = notificationManager.activeNotifications
        val hasNotification = notifications.any { it.id == bugReport.id }
        if (hasNotification || notifications.size > 3) {
            return bugReport
        }
        BugReportReceiver.notify(context, bugReport)
        return bugReport
    }

    inner class Report(val error: String, val throwable: Throwable? = null) {

        private val fileName = "$appName bug report ${SimpleDateFormat.getDateTimeInstance().format(Date())}"

        fun generateBugReport(): BugReport? {
            val contents = writeContents()
            val contentsWithHeader = "$fileName\n$contents"
            val id = contents.hashCode()
            val reportFile = save(contentsWithHeader, id)

            return BugReport(id, error, getDescription(throwable ?: return null), contentsWithHeader, reportFile)
        }

        private fun getDescription(throwable: Throwable): String {
            return "${throwable::class.java.name}: ${throwable.message}"
        }

        private fun save(contents: String, id: Int): File? {
            val dest = File(logsFolder, String.format("%x", id))
            dest.mkdirs()

            val file = File(dest, "$fileName.txt")
            if (!file.createNewFile()) return null
            file.writeText(contents)
            return file
        }

        private fun writeContents() = StringBuilder()
            .appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            .appendLine("commit: ${BuildConfig.COMMIT_HASH}")
            .appendLine("build.brand: ${Build.BRAND}")
            .appendLine("build.device: ${Build.DEVICE}")
            .appendLine("build.display: ${Build.DISPLAY}")
            .appendLine("build.fingerprint: ${Build.FINGERPRINT}")
            .appendLine("build.hardware: ${Build.HARDWARE}")
            .appendLine("build.id: ${Build.ID}")
            .appendLine("build.manufacturer: ${Build.MANUFACTURER}")
            .appendLine("build.model: ${Build.MODEL}")
            .appendLine("build.security.level: ${Build.VERSION.SECURITY_PATCH}")
            .appendLine("build.product: ${Build.PRODUCT}")
            .appendLine("build.type: ${Build.TYPE}")
            .appendLine("version.codename: ${Build.VERSION.CODENAME}")
            .appendLine("version.incremental: ${Build.VERSION.INCREMENTAL}")
            .appendLine("version.release: ${Build.VERSION.RELEASE}")
            .appendLine("version.sdk_int: ${Build.VERSION.SDK_INT}")
            .appendLine("display.density_dpi: ${context.resources.displayMetrics.densityDpi}")
            .appendLine("isRecentsEnabled: ${AutoCatApp.isRecentsEnabled}")
            .appendLine()
            .appendLine("error: $error")
            .also {
                if (throwable != null) {
                    it
                        .appendLine()
                        .appendLine(Log.getStackTraceString(throwable))
                }
            }
            .toString()
    }

    fun getLogs(): List<File> {
        return logsFolder.listFiles()?.flatMap { it.listFiles().orEmpty().toList() }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    companion object {
        val INSTANCE = MainThreadInitializedObject(::AutoCatBugReporter)
    }
}
