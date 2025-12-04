package app.lawnchair.categorization.stages

import android.content.Context
import app.lawnchair.categorization.CategorizationProgress
import app.lawnchair.categorization.learning.UserCorrectionLearner
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
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

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

    // User correction learner for improving accuracy
    private val learner = UserCorrectionLearner.getInstance(context, categoryDao)

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

        // Check if we have a strong learned hint for this app
        val hint = learner.getHintForPackage(appInfo.packageName)
        if (hint != null && categoryNames.contains(hint.category)) {
            // Apply learned categorization directly (skip LLM)
            val appCategory = AppCategory(
                packageName = appInfo.packageName,
                category = hint.category,
                confidence = hint.confidence,
                source = AppCategory.SOURCE_LLM,
                isUserOverride = false,
                reasoning = "Based on ${hint.sampleCount} previous user corrections for similar apps",
            )
            categoryDao.insertAppCategory(appCategory)

            android.util.Log.d(
                TAG,
                "Applied learned hint for ${appInfo.packageName}: ${hint.category} " +
                    "(confidence: ${hint.confidence}, pattern: ${hint.pattern})",
            )
            return true
        }

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
                    appDescription = appInfo.description,
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
                    reasoning = result.reasoning,
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
     * @param onProgress Optional callback for progress updates
     * @return Number of apps successfully categorized
     */
    suspend fun categorizeBatch(
        apps: List<AppInfo>,
        onProgress: ((CategorizationProgress) -> Unit)? = null,
    ): Int {
        val prefManager = PreferenceManager.getInstance(context)

        // Check if batching is enabled
        val batchingEnabled = prefManager.llmEnableBatching.get()

        return if (batchingEnabled) {
            categorizeBatchAPI(apps, onProgress)
        } else {
            categorizeBatchSequential(apps)
        }
    }

    /**
     * Categorizes apps using the batch API (efficient).
     */
    private suspend fun categorizeBatchAPI(
        apps: List<AppInfo>,
        onProgress: ((CategorizationProgress) -> Unit)? = null,
    ): Int {
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
        var failedBatches = 0
        var totalApiCalls = 0

        // Calculate total batches for progress tracking
        val batches = apps.chunked(batchSize)
        val totalBatches = batches.size
        var currentBatchIndex = 0
        val startTime = System.currentTimeMillis()

        android.util.Log.d(
            TAG,
            "Starting batch categorization: ${apps.size} apps, $totalBatches batches of ~$batchSize apps",
        )

        // Process batches in parallel chunks to maximize throughput
        // while respecting rate limits
        val batchChunks = batches.chunked(PARALLEL_BATCH_LIMIT)

        for (batchChunk in batchChunks) {
            // Process multiple batches concurrently
            val results = coroutineScope {
                batchChunk.map { batch ->
                    async {
                        val batchIndex = ++currentBatchIndex

                        // Update progress at start of batch
                        onProgress?.invoke(
                            CategorizationProgress(
                                isRunning = true,
                                currentStage = "LLM Categorization",
                                processedCount = categorizedCount,
                                totalCount = apps.size,
                                currentBatch = batchIndex,
                                totalBatches = totalBatches,
                                batchSize = batch.size,
                                currentProvider = primary.name,
                                estimatedTimeMs = calculateEstimatedTime(
                                    batchIndex,
                                    totalBatches,
                                    startTime,
                                ),
                            ),
                        )

                        val batchInfo = batch.map { app ->
                            AppBatchInfo(
                                packageName = app.packageName,
                                appName = app.label,
                                appDescription = app.description,
                            )
                        }

                        // Try providers in order with retry logic
                        var batchSuccess = false
                        var lastError: String? = null

                        for (provider in allProviders) {
                            if (!provider.isAvailable()) {
                                android.util.Log.d(
                                    TAG,
                                    "Batch $batchIndex: Skipping ${provider.name} (not available)",
                                )
                                continue
                            }

                            // Retry with exponential backoff
                            val batchStartTime = System.currentTimeMillis()
                            val apiResults = retryWithBackoff(
                                maxRetries = MAX_RETRIES,
                                initialDelayMs = INITIAL_RETRY_DELAY_MS,
                            ) {
                                android.util.Log.d(
                                    TAG,
                                    "Batch $batchIndex/$totalBatches: Categorizing ${batch.size} apps with ${provider.name}",
                                )
                                provider.categorizeAppBatch(batchInfo, categoryNames)
                            }
                            val batchDuration = System.currentTimeMillis() - batchStartTime

                            // If retry succeeded, return results
                            if (apiResults != null) {
                                batchSuccess = true
                                android.util.Log.d(
                                    TAG,
                                    "Batch $batchIndex: SUCCESS with ${provider.name} " +
                                        "(${apiResults.size} results in ${batchDuration}ms)",
                                )
                                return@async Triple(apiResults, batch.size, true)
                            } else {
                                lastError = "${provider.name} failed after $MAX_RETRIES retries"
                                android.util.Log.w(TAG, "Batch $batchIndex: $lastError")
                            }
                        }

                        if (!batchSuccess) {
                            android.util.Log.e(
                                TAG,
                                "Batch $batchIndex: FAILED - All providers exhausted. Last error: $lastError",
                            )
                        }

                        return@async Triple(null, 0, false)
                    }
                }.awaitAll()
            }

            // Process results from parallel batches
            results.forEach { (apiResults, batchSize, success) ->
                totalApiCalls++

                if (success && apiResults != null) {
                    apiResults.forEach { (packageName, result) ->
                        if (result.confidence >= MIN_CONFIDENCE) {
                            val appCategory = AppCategory(
                                packageName = packageName,
                                category = result.category,
                                confidence = result.confidence,
                                source = AppCategory.SOURCE_LLM,
                                isUserOverride = false,
                                reasoning = result.reasoning,
                            )
                            categoryDao.insertAppCategory(appCategory)
                            categorizedCount++

                            android.util.Log.d(
                                TAG,
                                "Saved: $packageName → ${result.category} (${result.confidence})",
                            )
                        }
                    }
                } else {
                    failedBatches++
                }
            }

            // Update progress after chunk completion
            onProgress?.invoke(
                CategorizationProgress(
                    isRunning = true,
                    currentStage = "LLM Categorization",
                    processedCount = categorizedCount,
                    totalCount = apps.size,
                    currentBatch = currentBatchIndex,
                    totalBatches = totalBatches,
                    batchSize = batchSize,
                    currentProvider = primary.name,
                    estimatedTimeMs = calculateEstimatedTime(
                        currentBatchIndex,
                        totalBatches,
                        startTime,
                    ),
                ),
            )

            // Rate limiting between chunks (not individual batches)
            if (batchChunk.size == PARALLEL_BATCH_LIMIT) {
                kotlinx.coroutines.delay(RATE_LIMIT_DELAY_MS)
            }
        }

        // Log final summary
        val totalDuration = System.currentTimeMillis() - startTime
        val successRate = if (totalApiCalls > 0) {
            ((totalApiCalls - failedBatches).toFloat() / totalApiCalls * 100).toInt()
        } else {
            0
        }

        android.util.Log.d(
            TAG,
            "Batch categorization complete: $categorizedCount/${apps.size} apps categorized " +
                "($successRate% batch success rate, $failedBatches/$totalBatches batches failed, " +
                "duration: ${totalDuration / 1000}s)",
        )

        return categorizedCount
    }

    /**
     * Calculates estimated time remaining based on current progress.
     */
    private fun calculateEstimatedTime(
        currentBatch: Int,
        totalBatches: Int,
        startTime: Long,
    ): Long {
        if (currentBatch == 0) return 0

        val elapsed = System.currentTimeMillis() - startTime
        val avgTimePerBatch = elapsed / currentBatch
        val remainingBatches = totalBatches - currentBatch

        return avgTimePerBatch * remainingBatches
    }

    /**
     * Retries an operation with exponential backoff.
     *
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds (doubles each retry)
     * @param operation The operation to retry
     * @return Result of the operation, or null if all retries failed
     */
    private suspend fun <T> retryWithBackoff(
        maxRetries: Int,
        initialDelayMs: Long,
        operation: suspend () -> T,
    ): T? {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    android.util.Log.w(
                        TAG,
                        "Attempt ${attempt + 1}/$maxRetries failed, retrying in ${currentDelay}ms: ${e.message}",
                    )
                    delay(currentDelay)
                    currentDelay = (currentDelay * 2.0.pow(1.0)).toLong() // Exponential backoff
                }
            }
        }

        android.util.Log.e(TAG, "All $maxRetries retry attempts failed", lastException)
        return null
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

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L // 1 second, doubles each retry

        // Parallel processing configuration
        private const val PARALLEL_BATCH_LIMIT = 3 // Process 3 batches concurrently
    }
}
