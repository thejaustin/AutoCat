package app.lawnchair.categorization.llm

import kotlin.math.max
import kotlin.math.min

object ConfidenceCalibrator {
    private val calibrationFactors = mapOf(
        "claude" to 0.95f, // Claude overconfident
        "google_ai" to 1.05f, // Gemini underconfident
        "openai" to 1.00f, // GPT well-calibrated
        "perplexity" to 0.98f, // Perplexity slightly overconfident
    )

    fun calibrate(confidence: Float, provider: String): Float {
        val factor = calibrationFactors[provider] ?: 1.0f
        return (confidence * factor).coerceIn(0f, 1f)
    }
}
