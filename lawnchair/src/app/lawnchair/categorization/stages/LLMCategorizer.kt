package app.lawnchair.categorization.stages

import android.content.Context
import app.lawnchair.categorization.AdaptiveModelSelector
import app.lawnchair.categorization.CategorizationProgress
import app.lawnchair.categorization.learning.UserCorrectionLearner
import app.lawnchair.categorization.llm.AppBatchInfo
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.ConfidenceCalibrator
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.LLMUtils
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.categorization.llm.ProviderCircuitBreaker
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.AppTab
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
    private val categoryDao: TabDao,
) {

    // Initialize providers once to avoid overhead in loops
    private val googleProvider by lazy { GoogleAIProvider(context) }
    private val providers by lazy {
        mapOf(
            "google_ai" to googleProvider,
            "claude" to ClaudeProvider(context),
            "openai" to OpenAIProvider(context),
            "perplexity" to PerplexityProvider(context),
        )
    }

    // User correction learner for improving accuracy
    private val learner by lazy { UserCorrectionLearner.getInstance(context, categoryDao) }
    private val circuitBreaker = ProviderCircuitBreaker()
    private val adaptiveSelector by lazy { AdaptiveModelSelector(context) }

    private fun checkConstraints(): Boolean {
        val prefManager = PreferenceManager.getInstance(context)

        // Check Wi-Fi constraint
        if (prefManager.llmOnlyOnWifi.get() && !LLMUtils.isConnectedToWifi(context)) {
            android.util.Log.d(TAG, "Skipping LLM categorization: Wi-Fi connection required")
            return false
        }

        // Check charging constraint
        if (prefManager.llmOnlyWhileCharging.get() && !LLMUtils.isCharging(context)) {
            android.util.Log.d(TAG, "Skipping LLM categorization: device must be charging")
            return false
        }

        // Check battery level threshold constraint
        val currentBattery = LLMUtils.getBatteryLevel(context)
        val minBattery = prefManager.llmMinBatteryLevel.get()
        if (currentBattery < minBattery) {
            android.util.Log.d(
                TAG,
                "Skipping LLM categorization: battery level ($currentBattery%) is below threshold ($minBattery%)",
            )
            return false
        }

        return true
    }

    /**
     * Attempts to categorize an app using LLM analysis with fallback support.
     *
     * Tries providers in order: primary provider (from settings), then fallbacks.
     *
     * @param appInfo App metadata including name and package
     * @return true if app was categorized, false if LLM couldn't determine a category
     */
    suspend fun categorize(appInfo: AppInfo): Boolean {
        if (!checkConstraints()) return false

        // Get available custom categories
        val customCategories = categoryDao.getVisibleCustomCategories()

        if (customCategories.isEmpty()) {
            android.util.Log.d(TAG, "No custom categories available for LLM categorization")
            return false
        }

        val tabNames = customCategories.map { it.name }

        // Check if we have a strong learned hint for this app
        val hint = learner.getHintForPackage(appInfo.packageName)
        if (hint != null && tabNames.contains(hint.tabName)) {
            // Apply learned categorization directly (skip LLM)
            val appTab = AppTab(
                packageName = appInfo.packageName,
                tabName = hint.tabName,
                confidence = hint.confidence,
                source = AppTab.SOURCE_LLM,
                isUserOverride = false,
                reasoning = "Based on ${hint.sampleCount} previous user corrections for similar apps",
            )
            categoryDao.insertAppCategory(appTab)

            android.util.Log.d(
                TAG,
                "Applied learned hint for ${appInfo.packageName}: ${hint.tabName} " +
                    "(confidence: ${hint.confidence}, pattern: ${hint.pattern})",
            )
            return true
        }

        // Get user's preferred provider (or auto-selected best provider)
        val prefManager = PreferenceManager.getInstance(context)
        val preferredProviderId = if (adaptiveSelector.isAutoSelectEnabled()) {
            val bestProvider = adaptiveSelector.getBestProvider()
            if (bestProvider != null) {
                android.util.Log.d(TAG, "Auto-selected provider: $bestProvider")
                bestProvider
            } else {
                android.util.Log.d(TAG, "Auto-select enabled but no data; using manual preference")
                prefManager.llmProviderPreference.get()
            }
        } else {
            prefManager.llmProviderPreference.get()
        }

        // Order providers: Preferred/Auto-selected first, then others as fallback
        val primary = providers[preferredProviderId] ?: googleProvider
        val fallbacks = providers.values.filter { it.name != primary.name }
        val allProviders = listOf(primary) + fallbacks

        for (provider in allProviders) {
            // Check circuit breaker first
            if (!circuitBreaker.isAvailable(provider.name)) {
                android.util.Log.d(TAG, "Skipping ${provider.name} for ${appInfo.packageName} - circuit breaker OPEN")
                continue
            }

            try {
                // Check if provider is available (original check)
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
                    availableTabs = tabNames,
                )

                // Record success with circuit breaker
                circuitBreaker.recordSuccess(provider.name)

                val calibratedConfidence = ConfidenceCalibrator.calibrate(
                    result.confidence,
                    provider.name,
                )

                // Only accept if calibrated confidence is above threshold
                if (calibratedConfidence < MIN_CONFIDENCE) {
                    android.util.Log.d(
                        TAG,
                        "${provider.name} calibrated confidence too low for ${appInfo.packageName}: $calibratedConfidence (original: ${result.confidence})",
                    )
                    continue
                }

                // Save to database
                val appTab = AppTab(
                    packageName = appInfo.packageName,
                    tabName = result.tabName,
                    confidence = calibratedConfidence, // Use calibrated confidence
                    source = AppTab.SOURCE_LLM,
                    isUserOverride = false,
                    reasoning = result.reasoning,
                    provider = provider.name,
                    model = provider.getCurrentModel()?.id,
                )

                categoryDao.insertAppCategory(appTab)

                android.util.Log.d(
                    TAG,
                    "${provider.name} categorized ${appInfo.packageName} as ${result.tabName} " +
                        "(confidence: ${result.confidence}, reason: ${result.reasoning})",
                )

                return true
            } catch (e: LLMException) {
                android.util.Log.e(TAG, "${provider.name} error for ${appInfo.packageName}: ${e.message}")
                // Record failure with circuit breaker
                circuitBreaker.recordFailure(provider.name, e)
                // Try next provider
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected error with ${provider.name}", e)
                // Record failure with circuit breaker
                circuitBreaker.recordFailure(provider.name, e)
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
        if (!checkConstraints()) return 0

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

        val tabNames = customCategories.map { it.name }

        // Get user's preferred provider (or auto-selected best provider)
        val prefManager = PreferenceManager.getInstance(context)
        val preferredProviderId = if (adaptiveSelector.isAutoSelectEnabled()) {
            val bestProvider = adaptiveSelector.getBestProvider()
            if (bestProvider != null) {
                android.util.Log.d(TAG, "Batch: Auto-selected provider: $bestProvider")
                bestProvider
            } else {
                android.util.Log.d(TAG, "Batch: Auto-select enabled but no data; using manual preference")
                prefManager.llmProviderPreference.get()
            }
        } else {
            prefManager.llmProviderPreference.get()
        }

        // Order providers: Preferred/Auto-selected first, then others as fallback
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
                    categories = tabNames,
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
                            // Check circuit breaker first
                            if (!circuitBreaker.isAvailable(provider.name)) {
                                android.util.Log.d(TAG, "Batch $batchIndex: Skipping ${provider.name} (circuit breaker OPEN)")
                                continue
                            }
                            if (!provider.isAvailable()) { // Original check
                                android.util.Log.d(
                                    TAG,
                                    "Batch $batchIndex: Skipping ${provider.name} (not available)",
                                )
                                continue
                            }

                            // Retry with exponential backoff
                            val batchStartTime = System.currentTimeMillis()
                            val apiResults = try {
                                LLMUtils.retryWithBackoff(
                                    maxRetries = MAX_RETRIES,
                                    initialDelayMs = INITIAL_RETRY_DELAY_MS,
                                    onRetry = { attempt, exception, nextDelayMs ->
                                        android.util.Log.w(
                                            TAG,
                                            "Attempt $attempt/$MAX_RETRIES failed with ${provider.name}, " +
                                                "retrying in ${nextDelayMs}ms: ${exception.message}",
                                        )
                                    },
                                ) {
                                    android.util.Log.d(
                                        TAG,
                                        "Batch $batchIndex/$totalBatches: Categorizing ${batch.size} apps with ${provider.name}",
                                    )
                                    try {
                                        val result = provider.categorizeAppBatch(batchInfo, tabNames)
                                        circuitBreaker.recordSuccess(provider.name)
                                        result
                                    } catch (e: Exception) {
                                        circuitBreaker.recordFailure(provider.name, e)
                                        throw e
                                    }
                                }
                            } catch (e: Exception) {
                                null
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
                                return@async Triple(apiResults, provider.name, provider.getCurrentModel()?.id)
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

                        return@async Triple(null, null, null)
                    }
                }.awaitAll()
            }

            // Process results from parallel batches
            results.forEach { (apiResults, providerName, modelId) ->
                totalApiCalls++

                if (apiResults != null) {
                    apiResults.forEach { (packageName, result) ->
                        val calibratedConfidence = ConfidenceCalibrator.calibrate(
                            result.confidence,
                            providerName!!, // Assert non-null here
                        )
                        if (calibratedConfidence >= MIN_CONFIDENCE) {
                            val appTab = AppTab(
                                packageName = packageName,
                                tabName = result.tabName,
                                confidence = calibratedConfidence,
                                source = AppTab.SOURCE_LLM,
                                isUserOverride = false,
                                reasoning = result.reasoning,
                                provider = providerName,
                                model = modelId,
                            )
                            categoryDao.insertAppCategory(appTab)
                            categorizedCount++

                            android.util.Log.d(
                                TAG,
                                "Saved: $packageName → ${result.tabName} ($calibratedConfidence) (original: ${result.confidence})",
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

            // Rate limiting between chunks (if enabled)
            val prefManager = PreferenceManager.getInstance(context)
            if (prefManager.autoCatEnableRateLimiting.get() && batchChunk.size == PARALLEL_BATCH_LIMIT) {
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
     * Categorizes apps sequentially (fallback method).
     */
    private suspend fun categorizeBatchSequential(apps: List<AppInfo>): Int {
        var categorizedCount = 0

        // Check preference for rate limiting
        val prefManager = PreferenceManager.getInstance(context)
        val preferredProviderId = prefManager.llmProviderPreference.get()
        val primaryProvider = providers[preferredProviderId] ?: googleProvider
        // If Google AI is preferred (or default), use the slow rate limit.
        val isGooglePreferred = preferredProviderId == "google_ai" || preferredProviderId.isEmpty()

        for (app in apps) {
            // Check circuit breaker for the primary provider before processing the app
            if (!circuitBreaker.isAvailable(primaryProvider.name)) {
                android.util.Log.d(TAG, "Skipping app ${app.packageName} - primary provider ${primaryProvider.name} is OPEN")
                continue // Skip this app if primary provider is down
            }

            try {
                if (categorize(app)) { // This call internally uses the circuit breaker already
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
                android.util.Log.e(TAG, "Error categorizing ${app.packageName} in sequential batch", e)
                // The circuit breaker would have already recorded a failure inside the categorize(app) call
                // So no need to call circuitBreaker.recordFailure here again for the provider.
                // Just continue with next app.
            }
        }

        return categorizedCount
    }

    /**
     * Resets all circuit breakers, allowing providers to be re-attempted.
     */
    fun resetCircuitBreakers() {
        circuitBreaker.resetAll()
    }

    companion object {
        private const val TAG = "LLMCategorizer"

        // Minimum confidence to accept LLM categorization
        private const val MIN_CONFIDENCE = 0.7f

        // Delay between API calls to respect rate limits (1 second between chunks)
        // Most LLM APIs allow 60+ requests/min, so 1s is safe for parallel processing
        private const val RATE_LIMIT_DELAY_MS = 1000L

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L // 1 second, doubles each retry

        // Parallel processing configuration
        // Process 4 batches concurrently for faster throughput
        private const val PARALLEL_BATCH_LIMIT = 4
    }
}
