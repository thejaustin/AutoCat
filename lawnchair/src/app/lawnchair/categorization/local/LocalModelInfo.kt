package app.lawnchair.categorization.local

data class LocalModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val type: LocalModelType,
    val sizeMb: Int,
    val speedTier: LocalSpeedTier,
    val qualityTier: LocalQualityTier,
    val minRamMb: Long,
    val downloadUrl: String?,
    val sha256: String?,
    val minSdkVersion: Int = 26,
    val requiresArm64: Boolean = true,
    val recommendationBadge: String? = null,
) {
    /** File name the model is stored under once downloaded. */
    val fileName: String? get() = downloadUrl?.substringAfterLast('/')
}

enum class LocalModelType { LITERT_LM, ENDPOINT, CUSTOM_FILE }

enum class LocalSpeedTier { VERY_FAST, FAST, MEDIUM, SLOW }

enum class LocalQualityTier { BASIC, GOOD, HIGH }
