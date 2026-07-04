package app.lawnchair.categorization.llm

/**
 * Configuration for LLM models across different providers.
 *
 * Defines model capabilities, availability, and recommendations
 * for different categorization tasks.
 */

data class ModelInfo(
    val id: String, // API identifier (e.g., "gemini-2.0-flash-exp")
    val displayName: String, // UI name (e.g., "Gemini 2.0 Flash")
    val provider: String, // Provider ID: "google_ai", "claude", "openai", "perplexity"
    val isAvailable: Boolean = true, // False if deprecated
    val costTier: CostTier, // Cost classification
    val contextWindow: Int, // Maximum tokens
    val speedTier: SpeedTier, // Relative speed
    val qualityTier: QualityTier, // Relative quality
    val recommendedFor: List<String> = listOf("categorization"), // Use cases
) {
    enum class CostTier {
        FREE,
        LOW,
        MEDIUM,
        HIGH,
    }

    enum class SpeedTier {
        SLOW,
        MEDIUM,
        FAST,
        VERY_FAST,
    }

    enum class QualityTier {
        STANDARD,
        HIGH,
        PREMIUM,
    }
}

/**
 * Central registry of all supported models across providers
 */
object ModelRegistry {

    // Google AI (Gemini) Models
    val GOOGLE_AI_MODELS = listOf(
        ModelInfo(
            id = "gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash (Recommended)",
            provider = "google_ai",
            isAvailable = true,
            costTier = ModelInfo.CostTier.FREE,
            contextWindow = 1048576, // 1M tokens
            speedTier = ModelInfo.SpeedTier.VERY_FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
            recommendedFor = listOf("categorization", "suggestions", "batch"),
        ),
        ModelInfo(
            id = "gemini-2.0-pro-exp",
            displayName = "Gemini 2.0 Pro",
            provider = "google_ai",
            isAvailable = true,
            costTier = ModelInfo.CostTier.LOW,
            contextWindow = 2097152, // 2M tokens
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.PREMIUM,
            recommendedFor = listOf("suggestions"),
        ),
        ModelInfo(
            id = "gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash (Deprecated)",
            provider = "google_ai",
            isAvailable = false,
            costTier = ModelInfo.CostTier.FREE,
            contextWindow = 1048576,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.STANDARD,
            recommendedFor = emptyList(),
        ),
    )

    // Anthropic Claude Models
    val CLAUDE_MODELS = listOf(
        ModelInfo(
            id = "claude-3-5-haiku-20241022",
            displayName = "Claude 3.5 Haiku (Fast)",
            provider = "claude",
            isAvailable = true,
            costTier = ModelInfo.CostTier.LOW,
            contextWindow = 200000,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
            recommendedFor = listOf("categorization", "batch"),
        ),
        ModelInfo(
            id = "claude-3-7-sonnet-20250219",
            displayName = "Claude 3.7 Sonnet (Best Quality)",
            provider = "claude",
            isAvailable = true,
            costTier = ModelInfo.CostTier.MEDIUM,
            contextWindow = 200000,
            speedTier = ModelInfo.SpeedTier.MEDIUM,
            qualityTier = ModelInfo.QualityTier.PREMIUM,
            recommendedFor = listOf("suggestions"),
        ),
    )

    // OpenAI Models
    val OPENAI_MODELS = listOf(
        ModelInfo(
            id = "gpt-4o-mini",
            displayName = "GPT-4o Mini (Recommended)",
            provider = "openai",
            isAvailable = true,
            costTier = ModelInfo.CostTier.LOW,
            contextWindow = 128000,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
            recommendedFor = listOf("categorization", "suggestions", "batch"),
        ),
        ModelInfo(
            id = "gpt-4o",
            displayName = "GPT-4o (Best Quality)",
            provider = "openai",
            isAvailable = true,
            costTier = ModelInfo.CostTier.HIGH,
            contextWindow = 128000,
            speedTier = ModelInfo.SpeedTier.MEDIUM,
            qualityTier = ModelInfo.QualityTier.PREMIUM,
            recommendedFor = listOf("suggestions"),
        ),
        ModelInfo(
            id = "o3-mini",
            displayName = "o3-mini (Reasoning)",
            provider = "openai",
            isAvailable = true,
            costTier = ModelInfo.CostTier.MEDIUM,
            contextWindow = 200000,
            speedTier = ModelInfo.SpeedTier.MEDIUM,
            qualityTier = ModelInfo.QualityTier.PREMIUM,
            recommendedFor = listOf("suggestions"),
        ),
    )

    // Perplexity Models (updated to current API - 2026)
    val PERPLEXITY_MODELS = listOf(
        ModelInfo(
            id = "sonar",
            displayName = "Sonar (Fast & Affordable)",
            provider = "perplexity",
            isAvailable = true,
            costTier = ModelInfo.CostTier.LOW,
            contextWindow = 128000,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.STANDARD,
            recommendedFor = listOf("categorization"),
        ),
        ModelInfo(
            id = "sonar-pro",
            displayName = "Sonar Pro (Advanced)",
            provider = "perplexity",
            isAvailable = true,
            costTier = ModelInfo.CostTier.MEDIUM,
            contextWindow = 200000,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
            recommendedFor = listOf("categorization", "suggestions", "batch"),
        ),
        ModelInfo(
            id = "sonar-reasoning",
            displayName = "Sonar Reasoning (Problem Solving)",
            provider = "perplexity",
            isAvailable = true,
            costTier = ModelInfo.CostTier.MEDIUM,
            contextWindow = 128000,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
            recommendedFor = listOf("suggestions"),
        ),
    )

    private fun getAutoModel(provider: String): ModelInfo {
        return ModelInfo(
            id = "auto",
            displayName = "Auto (Recommended)",
            provider = provider,
            isAvailable = true,
            costTier = ModelInfo.CostTier.FREE,
            contextWindow = 0,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
            recommendedFor = listOf("categorization", "suggestions", "batch"),
        )
    }

    /**
     * Gets all available models for a provider
     */
    fun getAvailableModels(provider: String): List<ModelInfo> {
        val models = when (provider) {
            "google_ai" -> GOOGLE_AI_MODELS.filter { it.isAvailable }
            "claude" -> CLAUDE_MODELS.filter { it.isAvailable }
            "openai" -> OPENAI_MODELS.filter { it.isAvailable }
            "perplexity" -> PERPLEXITY_MODELS.filter { it.isAvailable }
            else -> emptyList()
        }
        return listOf(getAutoModel(provider)) + models
    }

    /**
     * Gets all models (including deprecated) for a provider
     */
    fun getAllModels(provider: String): List<ModelInfo> {
        return when (provider) {
            "google_ai" -> GOOGLE_AI_MODELS
            "claude" -> CLAUDE_MODELS
            "openai" -> OPENAI_MODELS
            "perplexity" -> PERPLEXITY_MODELS
            else -> emptyList()
        }
    }

    /**
     * Gets the default recommended model for a provider and operation
     */
    fun getDefaultModel(provider: String, operation: String = "categorization"): ModelInfo? {
        return getAvailableModels(provider)
            .filter { it.recommendedFor.contains(operation) }
            .sortedWith(
                compareBy<ModelInfo> { it.costTier.ordinal }
                    .thenBy { it.speedTier.ordinal }
                    .thenByDescending { it.qualityTier.ordinal },
            )
            .firstOrNull()
    }

    /**
     * Gets model info by ID, or null if not found
     */
    fun getModelById(provider: String, modelId: String): ModelInfo? {
        return getAllModels(provider).find { it.id == modelId }
    }

    /**
     * Checks if a model is available
     */
    fun isModelAvailable(provider: String, modelId: String): Boolean {
        return getModelById(provider, modelId)?.isAvailable == true
    }

    /**
     * Gets a fallback model if the specified one is unavailable
     */
    fun getFallbackModel(provider: String, unavailableModelId: String): ModelInfo? {
        val model = getModelById(provider, unavailableModelId)

        // If model not found or available, return default
        if (model == null || model.isAvailable) {
            return getDefaultModel(provider)
        }

        // Try to find a similar model (same tier)
        val similar = getAvailableModels(provider)
            .filter { it.speedTier == model.speedTier && it.costTier == model.costTier }
            .firstOrNull()

        return similar ?: getDefaultModel(provider)
    }
}

/**
 * Model selection helper for choosing the best model for a task
 */
object ModelSelector {

    /**
     * Selects the best model for an operation based on criteria
     */
    fun selectBestModel(
        provider: String,
        operation: String = "categorization",
        batchSize: Int? = null,
        preferSpeed: Boolean = true,
        maxCostTier: ModelInfo.CostTier = ModelInfo.CostTier.MEDIUM,
    ): ModelInfo? {
        val availableModels = ModelRegistry.getAvailableModels(provider)
            .filter { it.costTier.ordinal <= maxCostTier.ordinal }

        if (availableModels.isEmpty()) return null

        // Filter by recommended operation
        val suitable = availableModels.filter { it.recommendedFor.contains(operation) }
        if (suitable.isEmpty()) return availableModels.firstOrNull()

        // For batch operations, prefer larger context windows
        if (operation == "batch" && batchSize != null) {
            return suitable
                .filter { it.contextWindow >= estimateRequiredContext(batchSize) }
                .sortedByDescending { it.contextWindow }
                .firstOrNull() ?: suitable.maxByOrNull { it.contextWindow }
        }

        // Otherwise, prefer speed vs quality based on flag
        return if (preferSpeed) {
            suitable.sortedBy { it.speedTier.ordinal }.first()
        } else {
            suitable.sortedByDescending { it.qualityTier.ordinal }.first()
        }
    }

    /**
     * Estimates required context window for a batch size
     */
    private fun estimateRequiredContext(batchSize: Int): Int {
        // Rough estimate: 50 tokens per app + 200 overhead
        return (batchSize * 50) + 200
    }

    /**
     * Gets model description for UI display
     */
    fun getModelDescription(model: ModelInfo): String {
        val parts = mutableListOf<String>()

        parts.add(
            when (model.costTier) {
                ModelInfo.CostTier.FREE -> "Free"
                ModelInfo.CostTier.LOW -> "Low cost"
                ModelInfo.CostTier.MEDIUM -> "Medium cost"
                ModelInfo.CostTier.HIGH -> "High cost"
            },
        )

        parts.add(
            when (model.speedTier) {
                ModelInfo.SpeedTier.VERY_FAST -> "Very fast"
                ModelInfo.SpeedTier.FAST -> "Fast"
                ModelInfo.SpeedTier.MEDIUM -> "Medium speed"
                ModelInfo.SpeedTier.SLOW -> "Slow"
            },
        )

        parts.add(
            when (model.qualityTier) {
                ModelInfo.QualityTier.PREMIUM -> "Premium quality"
                ModelInfo.QualityTier.HIGH -> "High quality"
                ModelInfo.QualityTier.STANDARD -> "Standard quality"
            },
        )

        parts.add("${model.contextWindow / 1000}K context")

        return parts.joinToString(", ")
    }
}
