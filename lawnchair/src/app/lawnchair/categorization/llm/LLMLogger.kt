package app.lawnchair.categorization.llm

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Centralized logging system for LLM provider operations.
 *
 * Provides structured logging with categorization, in-memory buffering,
 * and export capabilities for debugging LLM API issues.
 */
object LLMLogger {

    private const val MAX_LOG_ENTRIES = 200
    private val logBuffer = ConcurrentLinkedQueue<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    // Expose logs as a flow for real-time UI updates
    private val _logFlow = kotlinx.coroutines.flow.MutableSharedFlow<LogEntry>(
        replay = MAX_LOG_ENTRIES,
        extraBufferCapacity = 50,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val logFlow = _logFlow.asSharedFlow()

    enum class LogLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
    }

    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val provider: String,
        val operation: String,
        val message: String,
        val details: Map<String, Any>? = null,
        val exception: Throwable? = null,
    ) {
        fun toFormattedString(): String {
            val timeStr = dateFormat.format(Date(timestamp))
            val detailsStr = details?.let { d ->
                "\n  Details: ${d.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
            } ?: ""
            val exceptionStr = exception?.let { e ->
                "\n  Exception: ${e.javaClass.simpleName}: ${e.message}\n${e.stackTraceToString()}"
            } ?: ""

            return "[$timeStr] [${level.name}] [$provider] $operation: $message$detailsStr$exceptionStr"
        }
    }

    /**
     * Logs an API request
     */
    fun logRequest(
        provider: String,
        endpoint: String,
        requestBody: String? = null,
        headers: Map<String, String>? = null,
    ) {
        val details = mutableMapOf<String, Any>(
            "endpoint" to endpoint,
        )
        requestBody?.let { details["bodyLength"] = it.length }
        headers?.let { details["headers"] = it.keys.joinToString(", ") }

        addLog(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                level = LogLevel.DEBUG,
                provider = provider,
                operation = "API_REQUEST",
                message = "Sending request to $endpoint",
                details = details,
            ),
        )
    }

    /**
     * Logs an API response
     */
    fun logResponse(
        provider: String,
        statusCode: Int,
        responseBody: String? = null,
        durationMs: Long,
    ) {
        val level = if (statusCode in 200..299) LogLevel.INFO else LogLevel.ERROR
        val details = mutableMapOf<String, Any>(
            "statusCode" to statusCode,
            "durationMs" to durationMs,
        )
        responseBody?.let { details["bodyLength"] = it.length }

        addLog(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                provider = provider,
                operation = "API_RESPONSE",
                message = "Received response: $statusCode (${durationMs}ms)",
                details = details,
            ),
        )
    }

    /**
     * Logs an error
     */
    fun logError(
        provider: String,
        operation: String,
        error: Throwable,
        context: Map<String, Any>? = null,
    ) {
        addLog(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                level = LogLevel.ERROR,
                provider = provider,
                operation = operation,
                message = error.message ?: "Unknown error",
                details = context,
                exception = error,
            ),
        )
    }

    /**
     * Logs a warning
     */
    fun logWarning(
        provider: String,
        operation: String,
        message: String,
        details: Map<String, Any>? = null,
    ) {
        addLog(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                level = LogLevel.WARNING,
                provider = provider,
                operation = operation,
                message = message,
                details = details,
            ),
        )
    }

    /**
     * Logs informational message
     */
    fun logInfo(
        provider: String,
        operation: String,
        message: String,
        details: Map<String, Any>? = null,
    ) {
        addLog(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                level = LogLevel.INFO,
                provider = provider,
                operation = operation,
                message = message,
                details = details,
            ),
        )
    }

    /**
     * Logs debug message
     */
    fun logDebug(
        provider: String,
        operation: String,
        message: String,
        details: Map<String, Any>? = null,
    ) {
        addLog(
            LogEntry(
                timestamp = System.currentTimeMillis(),
                level = LogLevel.DEBUG,
                provider = provider,
                operation = operation,
                message = message,
                details = details,
            ),
        )
    }

    /**
     * Adds a log entry to the buffer
     */
    private fun addLog(entry: LogEntry) {
        // Add to buffer
        logBuffer.offer(entry)

        // Trim buffer if too large
        while (logBuffer.size > MAX_LOG_ENTRIES) {
            logBuffer.poll()
        }

        // Emit to flow
        scope.launch {
            try {
                _logFlow.emit(entry)
            } catch (e: Exception) {
                // Ignore errors during logging to prevent crashes
                android.util.Log.e("LLMLogger", "Failed to emit log", e)
            }
        }

        // Also log to Android logcat
        val tag = "LLM:${entry.provider}"
        val message = "${entry.operation}: ${entry.message}"
        when (entry.level) {
            LogLevel.DEBUG -> android.util.Log.d(tag, message, entry.exception)
            LogLevel.INFO -> android.util.Log.i(tag, message, entry.exception)
            LogLevel.WARNING -> android.util.Log.w(tag, message, entry.exception)
            LogLevel.ERROR -> android.util.Log.e(tag, message, entry.exception)
        }
    }

    /**
     * Gets recent log entries
     */
    fun getRecentLogs(count: Int = MAX_LOG_ENTRIES): List<LogEntry> {
        return logBuffer.toList().takeLast(count)
    }

    /**
     * Gets logs filtered by provider
     */
    fun getLogsByProvider(provider: String, count: Int = MAX_LOG_ENTRIES): List<LogEntry> {
        return logBuffer.filter { it.provider == provider }.takeLast(count)
    }

    /**
     * Gets logs filtered by level
     */
    fun getLogsByLevel(level: LogLevel, count: Int = MAX_LOG_ENTRIES): List<LogEntry> {
        return logBuffer.filter { it.level == level }.takeLast(count)
    }

    /**
     * Clears all logs
     */
    fun clearLogs() {
        logBuffer.clear()
        android.util.Log.d("LLMLogger", "Logs cleared")
    }

    /**
     * Exports logs to a file
     */
    fun exportLogs(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "llm_logs_$timestamp.txt"
        val file = File(context.getExternalFilesDir(null), filename)

        file.bufferedWriter().use { writer ->
            writer.write("AutoCat LLM Logs Export\n")
            writer.write("Generated: ${dateFormat.format(Date())}\n")
            writer.write("Total entries: ${logBuffer.size}\n")
            writer.write("=" * 80 + "\n\n")

            logBuffer.forEach { entry ->
                writer.write(entry.toFormattedString())
                writer.write("\n\n")
            }
        }

        android.util.Log.i("LLMLogger", "Logs exported to ${file.absolutePath}")
        return file
    }

    /**
     * Gets summary statistics
     */
    fun getStatistics(): LogStatistics {
        val logs = logBuffer.toList()
        val byLevel = logs.groupBy { it.level }.mapValues { it.value.size }
        val byProvider = logs.groupBy { it.provider }.mapValues { it.value.size }
        val errors = logs.filter { it.level == LogLevel.ERROR }

        return LogStatistics(
            totalEntries = logs.size,
            debugCount = byLevel[LogLevel.DEBUG] ?: 0,
            infoCount = byLevel[LogLevel.INFO] ?: 0,
            warningCount = byLevel[LogLevel.WARNING] ?: 0,
            errorCount = byLevel[LogLevel.ERROR] ?: 0,
            providerCounts = byProvider,
            recentErrors = errors.takeLast(10),
        )
    }

    data class LogStatistics(
        val totalEntries: Int,
        val debugCount: Int,
        val infoCount: Int,
        val warningCount: Int,
        val errorCount: Int,
        val providerCounts: Map<String, Int>,
        val recentErrors: List<LogEntry>,
    )

    /**
     * Cancels the coroutine scope and cleans up resources.
     * Should be called when shutting down to prevent memory leaks.
     */
    fun cleanup() {
        scope.cancel()
        android.util.Log.d("LLMLogger", "LLMLogger cleaned up, coroutine scope cancelled")
    }
}

/**
 * Helper function to repeat a string
 */
private operator fun String.times(count: Int): String {
    return this.repeat(count)
}
