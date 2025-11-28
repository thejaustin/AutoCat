package app.lawnchair.categorization.stages

import android.content.Context
import app.lawnchair.categorization.llm.AppBatchInfo
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.category.CategoryDao
import app.lawnchair.data.category.entities.AppCategory
import app.lawnchair.preferences.PreferenceManager

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
) {

    // Initialize providers once to avoid overhead in loops
    private val googleProvider = GoogleAIProvider(context)
    private val providers = mapOf(
        "google_ai" to googleProvider,
        "claude" to ClaudeProvider(context),
        "openai" to OpenAIProvider(context),
        "perplexity" to PerplexityProvider(context),
    )

    /**
     * Attempts to categorize an app using LLM analysis with fallback support.
     *
     * Tries providers in order: primary provider (from settings), then fallbacks.
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

        // Get user's preferred provider
        val prefManager = PreferenceManager.getInstance(context)
        val preferredProviderId = prefManager.llmProviderPreference.get()

        // Order providers: Preferred first, then others as fallback
        val primary = providers[preferredProviderId] ?: googleProvider
        val fallbacks = providers.values.filter { it.name != primary.name }
        val allProviders = listOf(primary) + fallbacks

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
     * Uses LLM batch API when enabled, falls back to sequential processing.
     *
     * @param apps List of apps to categorize
     * @return Number of apps successfully categorized
     */
    suspend fun categorizeBatch(apps: List<AppInfo>): Int {
        val prefManager = PreferenceManager.getInstance(context)

        // Check if batching is enabled
        val batchingEnabled = prefManager.llmEnableBatching.get()

        return if (batchingEnabled) {
            categorizeBatchAPI(apps)
        } else {
            categorizeBatchSequential(apps)
        }
    }

    /**
     * Categorizes apps using the batch API (efficient).
     */
    private suspend fun categorizeBatchAPI(apps: List<AppInfo>): Int {
        if (apps.isEmpty()) return 0

        // Get available custom categories
        val customCategories = categoryDao.getVisibleCustomCategories()
        if (customCategories.isEmpty()) {
            android.util.Log.d(TAG, "No custom categories available for LLM categorization")
            return 0
        }

        val categoryNames = customCategories.map { it.name }

        // Get user's preferred provider
        val prefManager = PreferenceManager.getInstance(context)
        val preferredProviderId = prefManager.llmProviderPreference.get()

        // Order providers: Preferred first, then others as fallback
        val primary = providers[preferredProviderId] ?: googleProvider
        val fallbacks = providers.values.filter { it.name != primary.name }
        val allProviders = listOf(primary) + fallbacks

        // Get model info to calculate batch size
        val modelInfo = primary.getCurrentModel()
        val batchSize = if (modelInfo != null) {
            val prefBatchSize = prefManager.llmBatchSize.get()
            if (prefBatchSize > 0) {
                prefBatchSize // User-specified
            } else {
                // Auto-calculate
                app.lawnchair.categorization.llm.BatchCalculator.calculateOptimalBatchSize(
                    modelInfo = modelInfo,
                    totalApps = apps.size,
                    categories = categoryNames,
                ).batchSize
            }
        } else {
            20 // Default batch size
        }

        var categorizedCount = 0

        // Process apps in batches
        apps.chunked(batchSize).forEach { batch ->
            val batchInfo = batch.map { app ->
                AppBatchInfo(
                    packageName = app.packageName,
                    appName = app.label,
                    appDescription = null, // TODO: Add description from metadata
                )
            }

            // Try providers in order
            for (provider in allProviders) {
                try {
                    if (!provider.isAvailable()) continue

                    android.util.Log.d(TAG, "Batch categorizing ${batch.size} apps with ${provider.name}")

                    val results = provider.categorizeAppBatch(batchInfo, categoryNames)

                    // Save successful categorizations
                    results.forEach { (packageName, result) ->
                        if (result.confidence >= MIN_CONFIDENCE) {
                            val appCategory = AppCategory(
                                packageName = packageName,
                                category = result.category,
                                confidence = result.confidence,
                                source = AppCategory.SOURCE_LLM,
                                isUserOverride = false,
                            )
                            categoryDao.insertAppCategory(appCategory)
                            categorizedCount++

                            android.util.Log.d(
                                TAG,
                                "Batch: $packageName → ${result.category} (${result.confidence})",
                            )
                        }
                    }

                    // Successfully categorized batch, break to next batch
                    break
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "${provider.name} batch error", e)
                    // Try next provider
                }
            }

            // Rate limiting between batches
            kotlinx.coroutines.delay(RATE_LIMIT_DELAY_MS)
        }

        return categorizedCount
    }

    /**
     * Categorizes apps sequentially (fallback method).
     */
    private suspend fun categorizeBatchSequential(apps: List<AppInfo>): Int {
        var categorizedCount = 0

        // Check preference for rate limiting
        val prefManager = PreferenceManager.getInstance(context)
        val preferredProviderId = prefManager.llmProviderPreference.get()
        // If Google AI is preferred (or default), use the slow rate limit.
        val isGooglePreferred = preferredProviderId == "google_ai" || preferredProviderId.isEmpty()

        for (app in apps) {
            try {
                if (categorize(app)) {
                    categorizedCount++
                }

                if (isGooglePreferred) {
                    // Google AI Free Tier: 15 RPM = 4s delay
                    kotlinx.coroutines.delay(RATE_LIMIT_DELAY_MS)
                } else {
                    // Other providers: fast delay
                    kotlinx.coroutines.delay(200)
                }
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
