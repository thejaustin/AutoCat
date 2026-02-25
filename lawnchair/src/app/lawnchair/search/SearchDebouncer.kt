package app.lawnchair.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debouncer for search queries.
 *
 * Prevents excessive search operations on every keystroke by waiting
 * for a pause in typing before executing the search.
 *
 * Features:
 * - Configurable delay duration
 * - Automatic cancellation of pending searches
 * - Thread-safe operation
 * - Main thread delivery for UI updates
 *
 * Usage:
 * ```kotlin
 * val debouncer = SearchDebouncer(lifecycleScope)
 *
 * searchView.addTextChangedListener { text ->
 *     debouncer.debounce(text.toString()) { query ->
 *         // Execute search with query
 *         searchAlgorithm.doSearch(query, callback)
 *     }
 * }
 * ```
 */
class SearchDebouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long = DEFAULT_DELAY_MS,
) {

    companion object {
        private const val DEFAULT_DELAY_MS = 300L
    }

    private var debounceJob: Job? = null

    /**
     * Debounce a search query.
     *
     * @param query The search query to debounce
     * @param action The action to execute after debounce delay
     */
    fun debounce(query: String, action: suspend (String) -> Unit) {
        // Cancel previous pending search
        debounceJob?.cancel()

        // Skip empty queries immediately
        if (query.isBlank()) {
            action("")
            return
        }

        // Schedule new search with delay
        debounceJob = scope.launch(Dispatchers.Default) {
            delay(delayMs)
            withContext(Dispatchers.Main) {
                action(query)
            }
        }
    }

    /**
     * Debounce with custom delay.
     *
     * @param query The search query
     * @param customDelayMs Custom delay in milliseconds
     * @param action The action to execute
     */
    fun debounce(query: String, customDelayMs: Long, action: suspend (String) -> Unit) {
        debounceJob?.cancel()

        if (query.isBlank()) {
            scope.launch(Dispatchers.Main) {
                action("")
            }
            return
        }

        debounceJob = scope.launch(Dispatchers.Default) {
            delay(customDelayMs)
            withContext(Dispatchers.Main) {
                action(query)
            }
        }
    }

    /**
     * Execute search immediately without debouncing.
     * Use this for explicit user actions like "Search" button.
     *
     * @param query The search query
     * @param action The action to execute
     */
    fun immediate(query: String, action: suspend (String) -> Unit) {
        debounceJob?.cancel()
        scope.launch(Dispatchers.Main) {
            action(query)
        }
    }

    /**
     * Cancel any pending search.
     * Call this when search is no longer needed (e.g., view destroyed).
     */
    fun cancel() {
        debounceJob?.cancel()
        debounceJob = null
    }

    /**
     * Check if a search is currently pending.
     */
    fun isPending(): Boolean = debounceJob?.isActive == true
}
