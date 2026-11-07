package app.lawnchair.categorization.local

object LocalModelRegistry {

    val ALL_MODELS = listOf(
        LocalModelInfo(
            id = "qwen3_0_6b",
            displayName = "Qwen3 0.6B",
            description = "Small and fast. Good enough for sorting apps into tabs on most phones.",
            type = LocalModelType.LITERT_LM,
            sizeMb = 585,
            speedTier = LocalSpeedTier.VERY_FAST,
            qualityTier = LocalQualityTier.GOOD,
            minRamMb = 4_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm",
            sha256 = null,
        ),
        LocalModelInfo(
            id = "qwen2_5_1_5b",
            displayName = "Qwen2.5 1.5B",
            description = "More accurate than Qwen3 0.6B, at about two and a half times the size.",
            type = LocalModelType.LITERT_LM,
            sizeMb = 1_523,
            speedTier = LocalSpeedTier.FAST,
            qualityTier = LocalQualityTier.HIGH,
            minRamMb = 6_000L,
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
            sha256 = null,
        ),
        LocalModelInfo(
            id = "gemma4_e2b",
            displayName = "Gemma 4 E2B",
            description = "Google's Gemma 4 for phones. Best quality here, but a large download.",
            type = LocalModelType.LITERT_LM,
            sizeMb = 2_468,
            speedTier = LocalSpeedTier.MEDIUM,
            qualityTier = LocalQualityTier.HIGH,
            minRamMb = 8_000L,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
            sha256 = null,
        ),
        LocalModelInfo(
            id = "local_endpoint",
            displayName = "Local Server (Ollama / LM Studio)",
            description = "Connect to a local AI server running on your device or LAN.",
            type = LocalModelType.ENDPOINT,
            sizeMb = 0,
            speedTier = LocalSpeedTier.FAST,
            qualityTier = LocalQualityTier.HIGH,
            minRamMb = 0L,
            downloadUrl = null,
            sha256 = null,
            requiresArm64 = false,
        ),
        LocalModelInfo(
            id = "custom_file",
            displayName = "Custom model file",
            description = "Use a LiteRT-LM .litertlm model file you provided manually.",
            type = LocalModelType.CUSTOM_FILE,
            sizeMb = 0,
            speedTier = LocalSpeedTier.MEDIUM,
            qualityTier = LocalQualityTier.GOOD,
            minRamMb = 0L,
            downloadUrl = null,
            sha256 = null,
        ),
    )

    fun findById(id: String): LocalModelInfo? = ALL_MODELS.firstOrNull { it.id == id }

    fun getCompatibleModels(caps: DeviceCapabilities): List<LocalModelInfo> {
        return ALL_MODELS.filter { model ->
            when {
                model.minSdkVersion > caps.androidVersion -> false
                model.requiresArm64 && !caps.supportedAbis.contains("arm64-v8a") -> false
                model.minRamMb > 0 && caps.totalRamMb < model.minRamMb -> false
                else -> true
            }
        }
    }

    /** The smallest compatible downloadable model: categorizing apps doesn't need a big one. */
    fun getRecommendedModel(caps: DeviceCapabilities): LocalModelInfo? {
        return getCompatibleModels(caps)
            .filter { it.type == LocalModelType.LITERT_LM }
            .minByOrNull { it.sizeMb }
    }
}
