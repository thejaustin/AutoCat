package app.lawnchair.categorization.llm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import kotlin.math.pow
import kotlinx.coroutines.delay

/**
 * Utility functions for LLM operations.
 */
object LLMUtils {
    private const val TAG = "LLMUtils"

    /**
     * Checks if the device is currently connected to Wi-Fi.
     */
    fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Checks if the device is currently charging.
     */
    fun isCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Retries an operation with exponential backoff.
     *
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds (doubles each retry)
     * @param operation The operation to retry
     * @return Result of the operation
     * @throws Exception if all retries fail, throws the last exception encountered
     */
    suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        onRetry: (attempt: Int, exception: Exception, nextDelayMs: Long) -> Unit = { _, _, _ -> },
        operation: suspend () -> T,
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                // Check if we should retry
                val shouldRetry = isRetryableException(e)
                val isLastAttempt = attempt == maxRetries - 1

                if (shouldRetry && !isLastAttempt) {
                    // Calculate delay with exponential backoff
                    // delay = initialDelay * 2^attempt
                    var currentDelay = (initialDelayMs * 2.0.pow(attempt.toDouble())).toLong()

                    // If it's a 429 (Rate Limit), add extra jitter or increase delay
                    if (isRateLimitException(e)) {
                        currentDelay *= 2 // Double the delay for rate limits
                        Log.w(TAG, "Rate limit hit (429), increasing backoff to ${currentDelay}ms")
                    }

                    onRetry(attempt + 1, e, currentDelay)
                    delay(currentDelay)
                } else {
                    // Not retryable or last attempt, throw
                    throw e
                }
            }
        }

        throw lastException ?: Exception("All retry attempts failed")
    }

    /**
     * Checks if an exception is retryable (e.g., network error, rate limit, server error).
     */
    private fun isRetryableException(e: Exception): Boolean {
        val message = e.message ?: ""
        return when {
            // Rate limits (429)
            isRateLimitException(e) -> true

            // Network timeouts/connectivity
            message.contains("timeout", ignoreCase = true) -> true

            message.contains("network", ignoreCase = true) -> true

            message.contains("connection", ignoreCase = true) -> true

            message.contains("SSL", ignoreCase = true) -> true

            // Server errors (5xx)
            message.contains("500", ignoreCase = true) -> true

            message.contains("502", ignoreCase = true) -> true

            message.contains("503", ignoreCase = true) -> true

            message.contains("504", ignoreCase = true) -> true

            // Transient errors
            message.contains("temporarily", ignoreCase = true) -> true

            message.contains("try again", ignoreCase = true) -> true

            else -> false
        }
    }

    /**
     * Checks if an exception is a rate limit (HTTP 429).
     */
    fun isRateLimitException(e: Exception): Boolean {
        val message = e.message ?: ""
        return message.contains("429", ignoreCase = true) ||
            message.contains("rate limit", ignoreCase = true) ||
            message.contains("too many requests", ignoreCase = true)
    }

    /**
     * Generates a user-friendly error message for LLM failures.
     */
    fun getUserFriendlyErrorMessage(e: Exception, provider: String): String {
        return when {
            isRateLimitException(e) ->
                "Rate limit reached. Please wait a moment and try again, or reduce batch size."

            e.message?.contains("timeout", ignoreCase = true) == true ->
                "Connection timed out. Please check your internet connection and try again."

            e.message?.contains("401", ignoreCase = true) == true ||
                e.message?.contains("unauthorized", ignoreCase = true) == true ->
                "Invalid API key. Please check your $provider API key in settings."

            e.message?.contains("403", ignoreCase = true) == true ->
                "Access denied. Please verify your API key has the correct permissions."

            e.message?.contains("404", ignoreCase = true) == true ->
                "Model not found. Please check the model selection in settings."

            e.message?.contains("500", ignoreCase = true) == true ||
                e.message?.contains("502", ignoreCase = true) == true ||
                e.message?.contains("503", ignoreCase = true) == true ->
                "$provider service is temporarily unavailable. Please try again later."

            e.message?.contains("network", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true ->
                "Network error. Please check your internet connection."

            else -> "An error occurred with $provider. Please try again or switch to a different provider."
        }
    }

    /**
     * Suggests next steps based on the error type.
     */
    fun getSuggestedAction(e: Exception, provider: String): String {
        return when {
            isRateLimitException(e) ->
                "Wait 30 seconds, then retry with a smaller batch size"

            e.message?.contains("401", ignoreCase = true) == true ||
                e.message?.contains("403", ignoreCase = true) == true ->
                "Go to Settings > AI Engine and verify your API key"

            e.message?.contains("network", ignoreCase = true) == true ->
                "Check Wi-Fi/mobile data connection"

            e.message?.contains("timeout", ignoreCase = true) == true ->
                "Try again - the service may be slow"

            else -> "Try switching to a different provider or contact support"
        }
    }
}
