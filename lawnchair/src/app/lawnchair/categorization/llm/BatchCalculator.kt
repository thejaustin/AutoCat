package app.lawnchair.categorization.llm

/**
 * Calculates optimal batch sizes for LLM categorization requests.
 *
 * Takes into account model context windows, prompt overhead,
 * and category list sizes to maximize batching efficiency.
 */
object BatchCalculator {

    // Estimated tokens per app entry in batch prompt
    private const val TOKENS_PER_APP = 50

    // Base prompt overhead (instructions, formatting, etc.)
    private const val PROMPT_OVERHEAD = 200

    // Safety margin to avoid hitting context limits
    private const val SAFETY_MARGIN = 0.3f // Reserve 30% for response

    data class BatchConfig(
        val batchSize: Int,
        val estimatedTokens: Int,
        val estimatedBatches: Int,
        val estimatedTime: Long, // milliseconds
    )

    /**
     * Calculates optimal batch size based on model capabilities
     */
    fun calculateOptimalBatchSize(
        modelInfo: ModelInfo,
        totalApps: Int,
        categories: List<String>,
        rateLimitDelayMs: Long = 4000,
    ): BatchConfig {
        val contextWindow = modelInfo.contextWindow

        // Estimate category overhead (roughly 4 chars per token)
        val categoryText = categories.joinToString("\n") { "- $it" }
        val categoryTokens = categoryText.length / 4

        // Calculate available tokens for app entries
        val availablePromptTokens = (contextWindow * (1 - SAFETY_MARGIN)).toInt()
        val tokensForApps = availablePromptTokens - PROMPT_OVERHEAD - categoryTokens

        // Calculate max apps per batch
        val maxAppsPerBatch = (tokensForApps / TOKENS_PER_APP).coerceAtLeast(1)

        // Apply reasonable limits based on model tier
        val batchSize = when {
            // Large context models can handle bigger batches (e.g., 1M+ tokens)
            contextWindow >= 500000 -> maxAppsPerBatch.coerceIn(50, 150)

            // Medium-large context models (100k - 200k)
            contextWindow >= 100000 -> maxAppsPerBatch.coerceIn(20, 80)

            // Medium context models
            contextWindow >= 30000 -> maxAppsPerBatch.coerceIn(15, 40)

            // Small context models
            else -> maxAppsPerBatch.coerceIn(10, 20)
        }

        val batches = (totalApps + batchSize - 1) / batchSize
        val estimatedTokens = PROMPT_OVERHEAD + categoryTokens + (batchSize * TOKENS_PER_APP)
        val estimatedTime = batches * rateLimitDelayMs

        return BatchConfig(
            batchSize = batchSize,
            estimatedTokens = estimatedTokens,
            estimatedBatches = batches,
            estimatedTime = estimatedTime,
        )
    }

    /**
     * Estimates time savings from batching vs sequential
     */
    fun estimateTimeSavings(
        batchConfig: BatchConfig,
        totalApps: Int,
        rateLimitDelayMs: Long,
    ): TimeSavings {
        val sequentialTime = totalApps * rateLimitDelayMs
        val batchTime = batchConfig.estimatedTime

        return TimeSavings(
            sequentialTimeMs = sequentialTime,
            batchTimeMs = batchTime,
            savingsMs = sequentialTime - batchTime,
            speedupFactor = sequentialTime.toFloat() / batchTime.toFloat(),
        )
    }

    data class TimeSavings(
        val sequentialTimeMs: Long,
        val batchTimeMs: Long,
        val savingsMs: Long,
        val speedupFactor: Float,
    ) {
        fun toHumanReadable(): String {
            val seqMin = sequentialTimeMs / 60000
            val batchSec = batchTimeMs / 1000
            val savingsMin = savingsMs / 60000

            return "Sequential: ${seqMin}min → Batch: ${batchSec}s (${speedupFactor.toInt()}x faster, saves ${savingsMin}min)"
        }
    }

    /**
     * Estimates token savings from batching
     */
    fun estimateTokenSavings(
        batchConfig: BatchConfig,
        totalApps: Int,
    ): TokenSavings {
        // Sequential: each app gets full prompt overhead
        val sequentialTokens = totalApps * (PROMPT_OVERHEAD + TOKENS_PER_APP)

        // Batch: prompt overhead shared across batch
        val batchTokens = batchConfig.estimatedBatches * batchConfig.estimatedTokens

        return TokenSavings(
            sequentialTokens = sequentialTokens,
            batchTokens = batchTokens,
            savingsTokens = sequentialTokens - batchTokens,
            efficiencyRatio = sequentialTokens.toFloat() / batchTokens.toFloat(),
        )
    }

    data class TokenSavings(
        val sequentialTokens: Int,
        val batchTokens: Int,
        val savingsTokens: Int,
        val efficiencyRatio: Float,
    ) {
        fun toHumanReadable(): String {
            return "Sequential: $sequentialTokens tokens → Batch: $batchTokens tokens (${efficiencyRatio.toInt()}x more efficient)"
        }
    }

    /**
     * Validates if a batch size is safe for a model
     */
    fun validateBatchSize(
        modelInfo: ModelInfo,
        batchSize: Int,
        categories: List<String>,
    ): ValidationResult {
        val categoryTokens = categories.joinToString("\n").length / 4
        val requiredTokens = PROMPT_OVERHEAD + categoryTokens + (batchSize * TOKENS_PER_APP)
        val availableTokens = (modelInfo.contextWindow * (1 - SAFETY_MARGIN)).toInt()

        return if (requiredTokens <= availableTokens) {
            ValidationResult.Valid
        } else {
            val maxSafe = (availableTokens - PROMPT_OVERHEAD - categoryTokens) / TOKENS_PER_APP
            ValidationResult.TooLarge(
                requestedSize = batchSize,
                maxSafeSize = maxSafe,
                requiredTokens = requiredTokens,
                availableTokens = availableTokens,
            )
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class TooLarge(
            val requestedSize: Int,
            val maxSafeSize: Int,
            val requiredTokens: Int,
            val availableTokens: Int,
        ) : ValidationResult()
    }
}
