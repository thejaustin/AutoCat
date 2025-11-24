package app.lawnchair.categorization.stages

import android.content.Context
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
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
 * Supports multiple LLM providers with automatic fallback:
 * 1. Google AI (Gemini) - Primary
 * 2. Claude (Anthropic) - Fallback 1
 * 3. OpenAI (GPT) - Fallback 2
 * 4. Perplexity - Fallback 3
 *
 * Stage 2 confidence: 0.7-0.9 depending on LLM confidence
 */
class LLMCategorizer(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val primaryProvider: LLMProvider = GoogleAIProvider(context),
) {

    // Initialize all available providers for fallback
    private val fallbackProviders = listOf(
        ClaudeProvider(context),
        OpenAIProvider(context),
        PerplexityProvider(context),
    )

    /**
     * Attempts to categorize an app using LLM analysis with fallback support.
     *
     * Tries providers in order: primary provider, then fallbacks.
     *
     * @param appInfo App metadata including name and package
     * @return true if app was categorized, false if LLM couldn't determine a category
     */
    suspend fun categorize(appInfo: AppInfo): Boolean {
        // Get available custom categories
        val customCategories = categoryDao.getVisibleCustomCategories()

        if (customCategories.isEmpty()) {
            android.util.Log.d(TAG, "No custom categories available for LLM categorization")
            return false
        }

        val categoryNames = customCategories.map { it.name }

        // Try primary provider first
        val allProviders = listOf(primaryProvider) + fallbackProviders

        for (provider in allProviders) {
            try {
                // Check if provider is available
                if (!provider.isAvailable()) {
                    android.util.Log.d(TAG, "LLM provider ${provider.name} not available, trying next")
                    continue
                }

                android.util.Log.d(TAG, "Trying provider: ${provider.name}")

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
                        "${provider.name} confidence too low for ${appInfo.packageName}: ${result.confidence}",
                    )
                    continue
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
                    "${provider.name} categorized ${appInfo.packageName} as ${result.category} " +
                        "(confidence: ${result.confidence}, reason: ${result.reasoning})",
                )

                return true
            } catch (e: LLMException) {
                android.util.Log.e(TAG, "${provider.name} error for ${appInfo.packageName}: ${e.message}")
                // Try next provider
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected error with ${provider.name}", e)
                // Try next provider
            }
        }

        android.util.Log.w(TAG, "All LLM providers failed for ${appInfo.packageName}")
        return false
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
