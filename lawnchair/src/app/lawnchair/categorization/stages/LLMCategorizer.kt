package app.lawnchair.categorization.stages

import android.content.Context
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.category.CategoryDao
import app.lawnchair.data.category.entities.AppCategory

/**
 * LLM-based categorizer that uses AI to assign apps to custom categories.
 *
 * This is the second stage of categorization (after built-in categories).
 * It uses an LLM to intelligently match apps to user-defined custom categories
 * based on app name, package, and description.
 *
 * Stage 2 confidence: 0.7-0.9 depending on LLM confidence
 */
class LLMCategorizer(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val provider: LLMProvider = GoogleAIProvider(context),
) {

    /**
     * Attempts to categorize an app using LLM analysis.
     *
     * @param appInfo App metadata including name and package
     * @return true if app was categorized, false if LLM couldn't determine a category
     */
    suspend fun categorize(appInfo: AppInfo): Boolean {
        try {
            // Get available custom categories
            val customCategories = categoryDao.getVisibleCustomCategories()

            if (customCategories.isEmpty()) {
                android.util.Log.d(TAG, "No custom categories available for LLM categorization")
                return false
            }

            // Check if provider is available
            if (!provider.isAvailable()) {
                android.util.Log.w(TAG, "LLM provider ${provider.name} not available")
                return false
            }

            val categoryNames = customCategories.map { it.name }

            // Call LLM to categorize
            val result = provider.categorizeApp(
                appName = appInfo.label,
                appPackage = appInfo.packageName,
                appDescription = null, // TODO: Add description from metadata provider
                availableCategories = categoryNames,
            )

            // Only accept if confidence is above threshold
            if (result.confidence < MIN_CONFIDENCE) {
                android.util.Log.d(
                    TAG,
                    "LLM confidence too low for ${appInfo.packageName}: ${result.confidence}",
                )
                return false
            }

            // Save to database
            val appCategory = AppCategory(
                packageName = appInfo.packageName,
                category = result.category,
                confidence = result.confidence,
                source = AppCategory.SOURCE_LLM,
                isUserOverride = false,
            )

            categoryDao.insertAppCategory(appCategory)

            android.util.Log.d(
                TAG,
                "LLM categorized ${appInfo.packageName} as ${result.category} " +
                    "(confidence: ${result.confidence}, reason: ${result.reasoning})",
            )

            return true
        } catch (e: LLMException) {
            android.util.Log.e(TAG, "LLM categorization error for ${appInfo.packageName}", e)
            return false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Unexpected error during LLM categorization", e)
            return false
        }
    }

    /**
     * Categorizes multiple apps in batch.
     *
     * Note: This processes apps sequentially to respect API rate limits.
     * Future: Implement smart batching and rate limiting.
     *
     * @param apps List of apps to categorize
     * @return Number of apps successfully categorized
     */
    suspend fun categorizeBatch(apps: List<AppInfo>): Int {
        var categorizedCount = 0

        for (app in apps) {
            try {
                if (categorize(app)) {
                    categorizedCount++
                }

                // Add small delay to respect rate limits (Google AI: 15 req/min)
                kotlinx.coroutines.delay(RATE_LIMIT_DELAY_MS)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error categorizing ${app.packageName}", e)
                // Continue with next app
            }
        }

        return categorizedCount
    }

    companion object {
        private const val TAG = "LLMCategorizer"

        // Minimum confidence to accept LLM categorization
        private const val MIN_CONFIDENCE = 0.7f

        // Delay between API calls to respect rate limits (4 seconds = 15/min)
        private const val RATE_LIMIT_DELAY_MS = 4000L
    }
}
