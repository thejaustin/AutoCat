package app.lawnchair.categorization.llm.test

import app.lawnchair.categorization.llm.BatchCalculator
import app.lawnchair.categorization.llm.ModelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchCalculatorTest {

    private val smallModel = ModelInfo(
        id = "small-model",
        displayName = "Small Model",
        provider = "test",
        isAvailable = true,
        costTier = "low",
        contextWindow = 8000,
        speedTier = "fast",
        qualityTier = "standard",
        recommendedFor = emptyList(),
    )

    private val mediumModel = ModelInfo(
        id = "medium-model",
        displayName = "Medium Model",
        provider = "test",
        isAvailable = true,
        costTier = "medium",
        contextWindow = 32000,
        speedTier = "medium",
        qualityTier = "high",
        recommendedFor = emptyList(),
    )

    private val largeModel = ModelInfo(
        id = "large-model",
        displayName = "Large Model",
        provider = "test",
        isAvailable = true,
        costTier = "high",
        contextWindow = 128000,
        speedTier = "slow",
        qualityTier = "premium",
        recommendedFor = emptyList(),
    )

    private val sampleCategories = listOf("Games", "Productivity", "Social", "Utilities")

    @Test
    fun `calculateOptimalBatchSize scales with context window`() {
        val totalApps = 100

        val smallConfig = BatchCalculator.calculateOptimalBatchSize(smallModel, totalApps, sampleCategories)
        val mediumConfig = BatchCalculator.calculateOptimalBatchSize(mediumModel, totalApps, sampleCategories)
        val largeConfig = BatchCalculator.calculateOptimalBatchSize(largeModel, totalApps, sampleCategories)

        // Small context should result in smaller batches
        assertTrue("Small batch size should be <= 20", smallConfig.batchSize <= 20)

        // Medium context should allow larger batches
        assertTrue("Medium batch size should be >= 15", mediumConfig.batchSize >= 15)
        assertTrue("Medium batch size should be <= 30", mediumConfig.batchSize <= 30)

        // Large context should allow even larger batches
        assertTrue("Large batch size should be >= 20", largeConfig.batchSize >= 20)
        assertTrue("Large batch size should be <= 50", largeConfig.batchSize <= 50)
    }

    @Test
    fun `calculateOptimalBatchSize calculates estimated time correctly`() {
        val totalApps = 100
        val rateLimitMs = 1000L
        val config = BatchCalculator.calculateOptimalBatchSize(
            mediumModel,
            totalApps,
            sampleCategories,
            rateLimitDelayMs = rateLimitMs,
        )

        val expectedBatches = (totalApps + config.batchSize - 1) / config.batchSize
        assertEquals(expectedBatches, config.estimatedBatches)
        assertEquals(expectedBatches * rateLimitMs, config.estimatedTime)
    }

    @Test
    fun `estimateTimeSavings shows speedup`() {
        val config = BatchCalculator.BatchConfig(20, 1000, 5, 5000)
        val savings = BatchCalculator.estimateTimeSavings(config, 100, 1000)

        assertEquals(100000L, savings.sequentialTimeMs) // 100 apps * 1000ms
        assertEquals(5000L, savings.batchTimeMs)
        assertEquals(95000L, savings.savingsMs)
        assertEquals(20.0f, savings.speedupFactor, 0.1f)
    }

    @Test
    fun `estimateTokenSavings shows efficiency`() {
        // Sequential: 100 * (200 overhead + 50 per app) = 25000 tokens
        // Batch: 5 batches * 1000 tokens = 5000 tokens
        val config = BatchCalculator.BatchConfig(20, 1000, 5, 5000)
        val savings = BatchCalculator.estimateTokenSavings(config, 100)

        assertTrue(savings.sequentialTokens > savings.batchTokens)
        assertEquals(5.0f, savings.efficiencyRatio, 0.1f)
    }

    @Test
    fun `validateBatchSize rejects overly large batches`() {
        // Create a model with very small context
        val tinyModel = smallModel.copy(contextWindow = 500)

        // Try to fit 100 apps (needs ~5000 tokens)
        val result = BatchCalculator.validateBatchSize(tinyModel, 100, sampleCategories)

        assertTrue(result is BatchCalculator.ValidationResult.TooLarge)
    }

    @Test
    fun `validateBatchSize accepts safe batches`() {
        // Try to fit 10 apps (needs ~700 tokens) into 8000 context
        val result = BatchCalculator.validateBatchSize(smallModel, 10, sampleCategories)

        assertTrue(result is BatchCalculator.ValidationResult.Valid)
    }
}
