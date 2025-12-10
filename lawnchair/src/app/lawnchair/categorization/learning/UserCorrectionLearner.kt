package app.lawnchair.categorization.learning

import android.content.Context
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.AppTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Learns from user corrections to improve future categorization.
 *
 * Analyzes patterns in user-overridden categorizations to:
 * - Identify common miscategorizations
 * - Extract tab preferences for package patterns
 * - Generate hints for LLM providers
 *
 * The learner builds a knowledge base from user corrections that can be
 * used to bias future categorization decisions.
 */
class UserCorrectionLearner(
    private val context: Context,
    private val categoryDao: TabDao,
) {

    /**
     * Analyzes user corrections and returns categorization hints.
     *
     * Returns a map of package patterns to preferred categories based on
     * user corrections. These hints can be added to LLM prompts to improve
     * accuracy.
     *
     * @return Map of package pattern -> tab preference with confidence
     */
    suspend fun getCategoryHints(): Map<String, CategoryHint> = withContext(Dispatchers.IO) {
        val userOverrides = categoryDao.getUserOverriddenApps()

        if (userOverrides.isEmpty()) {
            return@withContext emptyMap()
        }

        // Group by package prefix (e.g., "com.google.*" -> category patterns)
        val packagePrefixPatterns = mutableMapOf<String, MutableList<String>>()

        userOverrides.forEach { override ->
            val packagePrefix = extractPackagePrefix(override.packageName)
            packagePrefixPatterns
                .getOrPut(packagePrefix) { mutableListOf() }
                .add(override.tabName)
        }

        // Build hints from patterns with multiple examples
        val hints = mutableMapOf<String, CategoryHint>()

        packagePrefixPatterns.forEach { (prefix, categories) ->
            if (categories.size >= MIN_SAMPLES_FOR_HINT) {
                // Find most common category for this prefix
                val categoryFrequency = categories.groupingBy { it }.eachCount()
                val mostCommon = categoryFrequency.maxByOrNull { it.value }

                if (mostCommon != null) {
                    val confidence = mostCommon.value.toFloat() / categories.size
                    if (confidence >= MIN_CONFIDENCE_FOR_HINT) {
                        hints[prefix] = CategoryHint(
                            pattern = "$prefix.*",
                            tabName = mostCommon.key,
                            confidence = confidence,
                            sampleCount = categories.size,
                        )
                    }
                }
            }
        }

        hints
    }

    /**
     * Generates an LLM prompt supplement with learned patterns.
     *
     * Converts user correction patterns into natural language hints that can be
     * appended to LLM categorization prompts.
     *
     * Example output:
     * "Based on previous corrections:
     *  - Apps from com.google tend to be in Productivity (90% confidence)
     *  - Apps from com.facebook tend to be in Social (85% confidence)"
     */
    suspend fun generateLLMHintText(): String = withContext(Dispatchers.IO) {
        val hints = getCategoryHints()

        if (hints.isEmpty()) {
            return@withContext ""
        }

        val hintLines = hints.values
            .sortedByDescending { it.confidence }
            .take(MAX_HINTS_IN_PROMPT)
            .map { hint ->
                val confidencePercent = (hint.confidence * 100).toInt()
                "- Apps from ${hint.pattern.removeSuffix(".*")} tend to be in " +
                    "${hint.tabName} ($confidencePercent% confidence, ${hint.sampleCount} samples)"
            }

        if (hintLines.isEmpty()) {
            ""
        } else {
            "\n\nBased on previous user corrections:\n" + hintLines.joinToString("\n")
        }
    }

    /**
     * Checks if a specific app should be biased toward a category based on learned patterns.
     *
     * @param packageName The app package to check
     * @return CategoryHint if a strong pattern exists, null otherwise
     */
    suspend fun getHintForPackage(packageName: String): CategoryHint? = withContext(Dispatchers.IO) {
        val hints = getCategoryHints()
        val prefix = extractPackagePrefix(packageName)

        hints[prefix]?.takeIf { it.confidence >= MIN_CONFIDENCE_FOR_OVERRIDE }
    }

    /**
     * Analyzes categorization accuracy by comparing LLM results with user overrides.
     *
     * @return Statistics about categorization quality
     */
    suspend fun analyzeAccuracy(): AccuracyStats = withContext(Dispatchers.IO) {
        val userOverrides = categoryDao.getUserOverriddenApps()
        val allCategorizations = categoryDao.getAllAppCategories()

        val totalCategorizations = allCategorizations.size
        val totalOverrides = userOverrides.size
        val overrideRate = if (totalCategorizations > 0) {
            (totalOverrides.toFloat() / totalCategorizations) * 100
        } else {
            0f
        }

        // Group overrides by original source to see which stage needs improvement
        val overridesBySource = userOverrides.groupBy { override ->
            // Try to infer original source before user override
            // In practice, we'd need to store override history
            "llm" // Simplified for now
        }

        AccuracyStats(
            totalCategorizations = totalCategorizations,
            userOverrides = totalOverrides,
            overrideRate = overrideRate,
            overridesBySource = overridesBySource.mapValues { it.value.size },
        )
    }

    /**
     * Extracts a meaningful package prefix for pattern matching.
     *
     * Examples:
     * - "com.google.android.apps.photos" -> "com.google"
     * - "org.mozilla.firefox" -> "org.mozilla"
     * - "simple.app.name" -> "simple.app"
     */
    private fun extractPackagePrefix(packageName: String): String {
        val parts = packageName.split(".")
        return when {
            // Take first two parts for common prefixes (com.google, org.mozilla)
            parts.size >= 2 && parts[0] in listOf("com", "org", "net", "io") ->
                "${parts[0]}.${parts[1]}"

            // Take first part for others
            parts.isNotEmpty() -> parts[0]

            else -> packageName
        }
    }

    companion object {
        private const val TAG = "UserCorrectionLearner"

        // Minimum number of samples needed to establish a pattern
        private const val MIN_SAMPLES_FOR_HINT = 2

        // Minimum confidence to include hint in LLM prompt
        private const val MIN_CONFIDENCE_FOR_HINT = 0.6f

        // Minimum confidence to auto-apply hint without LLM
        private const val MIN_CONFIDENCE_FOR_OVERRIDE = 0.85f

        // Maximum hints to include in LLM prompt (to avoid prompt bloat)
        private const val MAX_HINTS_IN_PROMPT = 5

        @Volatile
        private var instance: UserCorrectionLearner? = null

        fun getInstance(context: Context, categoryDao: TabDao): UserCorrectionLearner {
            return instance ?: synchronized(this) {
                instance ?: UserCorrectionLearner(context, categoryDao).also { instance = it }
            }
        }
    }
}

/**
 * Represents a learned pattern from user corrections.
 *
 * @property pattern Package pattern (e.g., "com.google.*")
 * @property category Preferred category for this pattern
 * @property confidence Confidence in this pattern (0.0 - 1.0)
 * @property sampleCount Number of user corrections supporting this pattern
 */
data class CategoryHint(
    val pattern: String,
    val tabName: String,
    val confidence: Float,
    val sampleCount: Int,
)

/**
 * Statistics about categorization accuracy and user corrections.
 *
 * @property totalCategorizations Total number of categorized apps
 * @property userOverrides Number of apps with user corrections
 * @property overrideRate Percentage of apps that were corrected (0-100)
 * @property overridesBySource Breakdown of overrides by original categorization source
 */
data class AccuracyStats(
    val totalCategorizations: Int,
    val userOverrides: Int,
    val overrideRate: Float,
    val overridesBySource: Map<String, Int>,
)
