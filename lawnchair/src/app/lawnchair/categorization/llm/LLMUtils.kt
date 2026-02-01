package app.lawnchair.categorization.llm

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Utility functions for LLM operations.
 */
object LLMUtils {
    private const val TAG = "LLMUtils"

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
            message.contains("connectivity", ignoreCase = true) -> true
            // Server errors (5xx)
            message.contains("500") || message.contains("502") || message.contains("503") || message.contains("504") -> true
            // Gemini-specific overloaded error
            message.contains("overloaded", ignoreCase = true) -> true
            else -> false
        }
    }

    /**
     * Checks if an exception is a rate limit (429) error.
     */
    private fun isRateLimitException(e: Exception): Boolean {
        val message = e.message ?: ""
        return message.contains("429") || message.contains("quota", ignoreCase = true)
    }
}
