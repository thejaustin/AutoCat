package app.lawnchair.categorization.llm

import android.util.Log
import kotlin.math.max

enum class CircuitState {
    CLOSED, // Normal operation, requests allowed
    OPEN, // Provider disabled due to failures, requests blocked
    HALF_OPEN, // Testing if provider recovered, one request allowed
}

data class CircuitBreakerStatus(
    var state: CircuitState = CircuitState.CLOSED,
    var failureCount: Int = 0,
    var lastFailureTime: Long = 0,
    var lastSuccessTime: Long = 0,
)

class ProviderCircuitBreaker {
    private val circuits = mutableMapOf<String, CircuitBreakerStatus>()

    // Configuration constants
    private val failureThreshold = 3 // Number of failures before circuit opens
    private val timeoutMs = 60_000L // Time in milliseconds before a half-open retry
    private val halfOpenDurationMs = 10_000L // Duration for half-open state after a successful check

    fun isAvailable(providerName: String): Boolean {
        val circuit = circuits.getOrPut(providerName) { CircuitBreakerStatus() }

        return when (circuit.state) {
            CircuitState.CLOSED -> true

            // Always allow if closed

            CircuitState.OPEN -> {
                // Check if enough time has passed to retry (transition to HALF_OPEN)
                val elapsed = System.currentTimeMillis() - circuit.lastFailureTime
                if (elapsed >= timeoutMs) {
                    circuit.state = CircuitState.HALF_OPEN
                    Log.i(TAG, "Circuit breaker HALF_OPEN for $providerName (retrying after ${elapsed / 1000}s)")
                    true // Allow one request to test recovery
                } else {
                    Log.w(TAG, "Circuit breaker OPEN for $providerName (retry in ${(timeoutMs - elapsed) / 1000}s)")
                    false // Still open, block request
                }
            }

            CircuitState.HALF_OPEN -> {
                // Allow one request through to test. If this request succeeds, close the circuit.
                // If it fails, open the circuit again.
                // For now, always return true as the request itself will trigger success/failure.
                true
            }
        }
    }

    fun recordSuccess(providerName: String) {
        val circuit = circuits.getOrPut(providerName) { CircuitBreakerStatus() }
        if (circuit.state == CircuitState.HALF_OPEN) {
            // If in half-open, a success means recovery, so close the circuit
            circuit.state = CircuitState.CLOSED
            circuit.failureCount = 0
            circuit.lastSuccessTime = System.currentTimeMillis()
            Log.i(TAG, "Circuit breaker CLOSED for $providerName (recovered successfully)")
        } else if (circuit.state == CircuitState.CLOSED) {
            // Already closed, just update success time
            circuit.lastSuccessTime = System.currentTimeMillis()
        }
    }

    fun recordFailure(providerName: String, error: Throwable) {
        val circuit = circuits.getOrPut(providerName) { CircuitBreakerStatus() }
        circuit.failureCount++
        circuit.lastFailureTime = System.currentTimeMillis()

        if (circuit.state == CircuitState.HALF_OPEN || circuit.failureCount >= failureThreshold) {
            circuit.state = CircuitState.OPEN
            Log.e(TAG, "Circuit breaker OPEN for $providerName after ${circuit.failureCount} failures (last error: ${error.message})")
        }
    }

    fun reset(providerName: String) {
        circuits.remove(providerName)
        Log.i(TAG, "Circuit breaker reset for $providerName")
    }

    fun resetAll() {
        circuits.clear()
        Log.i(TAG, "All circuit breakers reset")
    }

    companion object {
        private const val TAG = "ProviderCircuitBreaker"
    }
}
