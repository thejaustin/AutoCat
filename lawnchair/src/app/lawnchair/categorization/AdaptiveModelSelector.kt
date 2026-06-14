package app.lawnchair.categorization

import android.content.Context
import android.util.Log
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.ModelAccuracyStats
import app.lawnchair.preferences.PreferenceManager

/**
 * Intelligently selects the best performing LLM provider based on accuracy data.
 *
 * This service analyzes accuracy metrics to automatically choose the most accurate
 * model, enabling data-driven provider selection instead of manual configuration.
 *
 * @param context Application context for database and preferences access
 */
class AdaptiveModelSelector(private val context: Context) {

    private val database = TabDatabase.getInstance(context)
    private val accuracyDao = database.accuracyDao()
    private val prefs = PreferenceManager.getInstance(context)

    companion object {
        private const val TAG = "AdaptiveModelSelector"

        /**
         * Minimum number of predictions required to consider a model for auto-selection.
         * This prevents selecting models with insufficient data.
         */
        private const val MIN_SAMPLES = 10

        /**
         * Number of days of historical data to consider for accuracy calculation.
         */
        private const val LOOKBACK_DAYS = 30

        /**
         * Minimum accuracy threshold (percentage) to consider a model.
         * Models below this threshold won't be auto-selected.
         */
        private const val MIN_ACCURACY_THRESHOLD = 70f
    }

    /**
     * Determines if auto-selection is currently enabled.
     */
    fun isAutoSelectEnabled(): Boolean = prefs.llmAutoSelectBestModel.get()

    /**
     * Gets the best performing provider based on recent accuracy data.
     *
     * @param minSamples Minimum number of predictions required (default: 10)
     * @param daysBack Number of days of history to consider (default: 30)
     * @return Provider ID of the best performing model, or null if:
     *         - Auto-select is disabled
     *         - No models meet the minimum sample requirement
     *         - No models meet the minimum accuracy threshold
     */
    suspend fun getBestProvider(
        minSamples: Int = MIN_SAMPLES,
        daysBack: Int = LOOKBACK_DAYS,
    ): String? {
        // Check if auto-selection is enabled
        if (!isAutoSelectEnabled()) {
            Log.d(TAG, "Auto-selection is disabled")
            return null
        }

        try {
            val since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
            val stats = accuracyDao.getAccuracyByModel(since)

            if (stats.isEmpty()) {
                Log.d(TAG, "No accuracy data available for auto-selection")
                return null
            }

            // Find the best performing model
            val bestModel = stats
                .filter { it.total >= minSamples && it.accuracy >= MIN_ACCURACY_THRESHOLD }
                .maxByOrNull { it.accuracy }

            if (bestModel != null) {
                Log.i(
                    TAG,
                    "Auto-selected best provider: ${bestModel.provider}/${bestModel.model} " +
                        "(${bestModel.accuracy.toInt()}% accuracy, ${bestModel.total} samples)",
                )
                return bestModel.provider
            } else {
                Log.d(
                    TAG,
                    "No models meet selection criteria (min $minSamples samples, " +
                        "min ${MIN_ACCURACY_THRESHOLD}% accuracy)",
                )
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting best provider", e)
            return null
        }
    }

    /**
     * Gets detailed accuracy statistics for all models.
     *
     * @param daysBack Number of days of history to include
     * @return List of accuracy stats sorted by accuracy descending
     */
    suspend fun getAccuracyStats(daysBack: Int = LOOKBACK_DAYS): List<ModelAccuracyStats> {
        return try {
            val since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
            accuracyDao.getAccuracyByModel(since)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting accuracy stats", e)
            emptyList()
        }
    }

    /**
     * Gets the best performing model for a specific provider.
     *
     * @param provider Provider ID (e.g., "google_ai", "claude")
     * @param minSamples Minimum number of predictions required
     * @param daysBack Number of days of history to consider
     * @return Model ID of the best performing model for this provider, or null
     */
    suspend fun getBestModelForProvider(
        provider: String,
        minSamples: Int = MIN_SAMPLES,
        daysBack: Int = LOOKBACK_DAYS,
    ): String? {
        try {
            val since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
            val stats = accuracyDao.getAccuracyByProvider(provider, since)

            val bestModel = stats
                .filter { it.total >= minSamples && it.accuracy >= MIN_ACCURACY_THRESHOLD }
                .maxByOrNull { it.accuracy }

            if (bestModel != null) {
                Log.d(
                    TAG,
                    "Best model for $provider: ${bestModel.model} " +
                        "(${bestModel.accuracy.toInt()}% accuracy)",
                )
                return bestModel.model
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting best model for provider $provider", e)
            return null
        }
    }

    /**
     * Gets a human-readable summary of the current auto-selection state.
     *
     * @return Summary string describing auto-selection status and best model
     */
    suspend fun getAutoSelectionSummary(): String {
        if (!isAutoSelectEnabled()) {
            return "Auto-selection is disabled. Using manually configured provider."
        }

        val bestProvider = getBestProvider()
        if (bestProvider == null) {
            return "Auto-selection is enabled, but insufficient data available. " +
                "Using fallback to manually configured provider."
        }

        val stats = getAccuracyStats()
        val bestModel = stats.find { it.provider == bestProvider }

        return if (bestModel != null) {
            "Auto-selected: ${formatProviderName(bestProvider)} " +
                "(${bestModel.accuracy.toInt()}% accuracy, ${bestModel.total} predictions)"
        } else {
            "Auto-selection active"
        }
    }

    /**
     * Formats provider ID to human-readable name.
     */
    private fun formatProviderName(provider: String): String = when (provider) {
        "google_ai" -> "Google AI (Gemini)"
        "claude" -> "Anthropic Claude"
        "openai" -> "OpenAI (GPT)"
        "perplexity" -> "Perplexity"
        else -> provider.replaceFirstChar { it.uppercase() }
    }
}
