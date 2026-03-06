package app.lawnchair.categorization.local

object LocalModelRegistry {

    val ALL_MODELS = listOf(
        LocalModelInfo(
            id = "aicore_gemini_nano",
            displayName = "Gemini Nano (On-device)",
            description = "Google's on-device model via AICore. Requires Pixel 8+ or Galaxy S24+.",
            type = LocalModelType.AICORE,
            sizeMb = 0,
            speedTier = LocalSpeedTier.VERY_FAST,
            qualityTier = LocalQualityTier.HIGH,
            minRamMb = 4_000L,
            downloadUrl = null,
            sha256 = null,
            requiresAiCore = true,
            minSdkVersion = 35,
            requiresArm64 = true,
        ),
        LocalModelInfo(
            id = "gemma_2b_int4",
            displayName = "Gemma 2B INT4",
            description = "Google's Gemma 2B quantized to INT4 via MediaPipe. Best balance of speed and quality.",
            type = LocalModelType.MEDIAPIPE,
            sizeMb = 1_400,
            speedTier = LocalSpeedTier.FAST,
            qualityTier = LocalQualityTier.HIGH,
            minRamMb = DeviceCapabilityChecker.RAM_MB_GEMMA_2B,
            downloadUrl = "https://huggingface.co/litert-community/Gemma2-2B-IT/resolve/main/gemma2-2b-it-gpu-int4.bin",
            sha256 = null,
            requiresAiCore = false,
            minSdkVersion = 29,
            requiresArm64 = true,
            recommendationBadge = "Best for S22 Ultra",
        ),
        LocalModelInfo(
            id = "tinyllama_1b_int4",
            displayName = "TinyLlama 1.1B INT4",
            description = "Lightweight model for budget devices. Fast but lower quality than Gemma.",
            type = LocalModelType.MEDIAPIPE,
            sizeMb = 700,
            speedTier = LocalSpeedTier.VERY_FAST,
            qualityTier = LocalQualityTier.BASIC,
            minRamMb = DeviceCapabilityChecker.RAM_MB_TINYLLAMA,
            downloadUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/tinyllama-1.1b-chat-v1.0-gpu-int4.bin",
            sha256 = null,
            requiresAiCore = false,
            minSdkVersion = 26,
            requiresArm64 = true,
        ),
        LocalModelInfo(
            id = "phi2_int4",
            displayName = "Phi-2 2.7B INT4",
            description = "Microsoft's Phi-2 model. Higher quality for devices with enough RAM.",
            type = LocalModelType.MEDIAPIPE,
            sizeMb = 1_500,
            speedTier = LocalSpeedTier.MEDIUM,
            qualityTier = LocalQualityTier.HIGH,
            minRamMb = 6_000L,
            downloadUrl = "https://huggingface.co/litert-community/phi-2/resolve/main/phi-2-gpu-int4.bin",
            sha256 = null,
            requiresAiCore = false,
            minSdkVersion = 29,
            requiresArm64 = true,
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
            requiresAiCore = false,
            minSdkVersion = 26,
            requiresArm64 = false,
        ),
        LocalModelInfo(
            id = "custom_file",
            displayName = "Custom model file",
            description = "Use a MediaPipe .bin model file you provided manually.",
            type = LocalModelType.CUSTOM_FILE,
            sizeMb = 0,
            speedTier = LocalSpeedTier.MEDIUM,
            qualityTier = LocalQualityTier.GOOD,
            minRamMb = 0L,
            downloadUrl = null,
            sha256 = null,
            requiresAiCore = false,
            minSdkVersion = 26,
            requiresArm64 = false,
        ),
    )

    fun getCompatibleModels(caps: DeviceCapabilities): List<LocalModelInfo> {
        return ALL_MODELS.filter { model ->
            when {
                model.requiresAiCore && !caps.isAiCoreAvailable -> false
                model.minSdkVersion > caps.androidVersion -> false
                model.requiresArm64 && !caps.supportedAbis.contains("arm64-v8a") -> false
                model.minRamMb > 0 && caps.totalRamMb < model.minRamMb -> false
                else -> true
            }
        }
    }

    fun getRecommendedModel(caps: DeviceCapabilities): LocalModelInfo? {
        val compatible = getCompatibleModels(caps)

        // Prefer AICore if available
        compatible.firstOrNull { it.type == LocalModelType.AICORE }?.let { return it }

        // Prefer highest quality MediaPipe model that fits in RAM
        return compatible
            .filter { it.type == LocalModelType.MEDIAPIPE }
            .sortedWith(
                compareByDescending<LocalModelInfo> { it.qualityTier.ordinal }
                    .thenBy { it.minRamMb },
            )
            .firstOrNull()
    }
}
