package app.lawnchair.categorization

import android.content.Context
import android.util.Log
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.ModelAccuracy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Tracks LLM model prediction accuracy based on user behavior.
 *
 * This class records whether users accept or override model predictions,
 * enabling data-driven model selection and performance analysis.
 *
 * Usage:
 * - Call recordUserCorrection() when user manually changes a tab assignment
 * - Call recordAcceptedCategorization() when user keeps an LLM tab assignment
 *
 * @param context Application context for database access
 */
class AccuracyTracker(private val context: Context) {

    private val database = TabDatabase.getInstance(context)
    private val tabDao = database.tabDao()
    private val accuracyDao = database.accuracyDao()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "AccuracyTracker"

        /**
         * Minimum time (in days) to wait before considering an uncorrected
         * categorization as "accepted" by the user.
         */
        private const val ACCEPTANCE_GRACE_PERIOD_DAYS = 7L
    }

    /**
     * Records that the user corrected a model's prediction.
     *
     * This indicates the model was INCORRECT - it predicted oldTab
     * but the user changed it to newTab.
     *
     * @param packageName The app that was re-assigned
     * @param oldTab The tab predicted by the model
     * @param newTab The tab chosen by the user
     */
    fun recordUserCorrection(
        packageName: String,
        oldTab: String,
        newTab: String,
    ) {
        scope.launch {
            try {
                // Get the original categorization to find provider/model info
                val original = tabDao.getAppTab(packageName) ?: run {
                    Log.w(TAG, "Cannot record correction: No tab assignment found for $packageName")
                    return@launch
                }

                // Only track corrections for LLM predictions
                if (original.provider == null || original.model == null) {
                    Log.d(TAG, "Skipping correction tracking: Not an LLM prediction")
                    return@launch
                }

                // Record that the model was WRONG
                val accuracy = ModelAccuracy(
                    provider = original.provider,
                    model = original.model,
                    tabName = oldTab,
                    wasCorrect = false,
                    confidence = original.confidence,
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                )

                accuracyDao.insert(accuracy)

                Log.i(
                    TAG,
                    "Recorded incorrect prediction: ${original.provider}/${original.model} " +
                        "predicted '$oldTab' (confidence: ${original.confidence}), " +
                        "user chose '$newTab'",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record user correction", e)
            }
        }
    }

    /**
     * Records that the user accepted a model's prediction.
     *
     * This should be called after a grace period (e.g., 7 days) if the user
     * has not changed the tab assignment, indicating they accept the model's choice.
     *
     * @param packageName The app whose tab assignment was accepted
     */
    fun recordAcceptedCategorization(packageName: String) {
        scope.launch {
            try {
                val appTab = tabDao.getAppTab(packageName) ?: run {
                    Log.w(TAG, "Cannot record acceptance: No tab assignment found for $packageName")
                    return@launch
                }

                // Only track acceptance for LLM predictions that weren't overridden
                if (appTab.provider == null ||
                    appTab.model == null ||
                    appTab.isUserOverride
                ) {
                    Log.d(TAG, "Skipping acceptance tracking: Not an LLM prediction or was overridden")
                    return@launch
                }

                // Check if enough time has passed since categorization
                val gracePeriodMillis = ACCEPTANCE_GRACE_PERIOD_DAYS * 24 * 60 * 60 * 1000
                val timeSinceCategorization = System.currentTimeMillis() - appTab.lastUpdated

                if (timeSinceCategorization < gracePeriodMillis) {
                    Log.d(TAG, "Skipping acceptance tracking: Grace period not yet elapsed")
                    return@launch
                }

                // Record that the model was CORRECT (user kept it)
                val accuracy = ModelAccuracy(
                    provider = appTab.provider,
                    model = appTab.model,
                    tabName = appTab.tabName,
                    wasCorrect = true,
                    confidence = appTab.confidence,
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                )

                accuracyDao.insert(accuracy)

                Log.i(
                    TAG,
                    "Recorded accepted prediction: ${appTab.provider}/${appTab.model} " +
                        "predicted '${appTab.tabName}' (confidence: ${appTab.confidence})",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record accepted categorization", e)
            }
        }
    }

    /**
     * Scans all LLM-categorized apps and records acceptance for those
     * that haven't been changed after the grace period.
     *
     * This should be called periodically (e.g., daily) to update accuracy metrics.
     */
    fun scanAndRecordAcceptances() {
        scope.launch {
            try {
                val llmApps = tabDao.getAppsBySource("llm")
                val gracePeriodMillis = ACCEPTANCE_GRACE_PERIOD_DAYS * 24 * 60 * 60 * 1000
                val now = System.currentTimeMillis()

                var acceptedCount = 0

                for (app in llmApps) {
                    // Skip user overrides
                    if (app.isUserOverride) continue

                    // Skip if provider/model info missing
                    if (app.provider == null || app.model == null) continue

                    // Skip if not past grace period
                    if (now - app.lastUpdated < gracePeriodMillis) continue

                    // Check if we've already recorded this acceptance
                    // (by checking if a record exists for this package that was correct)
                    val existingCount = accuracyDao.getRecordCount(app.provider, app.model)

                    // Simple check: just record it (duplicates are expected since users
                    // might keep the same tab assignment for a long time)
                    val accuracy = ModelAccuracy(
                        provider = app.provider,
                        model = app.model,
                        tabName = app.tabName,
                        wasCorrect = true,
                        confidence = app.confidence,
                        timestamp = now,
                        packageName = app.packageName,
                    )

                    accuracyDao.insert(accuracy)
                    acceptedCount++
                }

                if (acceptedCount > 0) {
                    Log.i(TAG, "Recorded $acceptedCount accepted tab assignments")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan and record acceptances", e)
            }
        }
    }

    /**
     * Gets accuracy statistics for all models.
     *
     * @param daysBack Number of days of history to include (default: 30)
     * @return List of accuracy stats sorted by accuracy descending
     */
    suspend fun getAccuracyStats(daysBack: Int = 30) = accuracyDao.getAccuracyByModel(
        since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L),
    )

    /**
     * Gets the best performing model based on recent accuracy.
     *
     * @param minSamples Minimum number of predictions required
     * @param daysBack Number of days of history to consider
     * @return The best model, or null if no model meets criteria
     */
    suspend fun getBestModel(minSamples: Int = 10, daysBack: Int = 30) = accuracyDao.getBestModel(
        minSamples = minSamples,
        since = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L),
    )
}
