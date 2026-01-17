package app.lawnchair.categorization.learning

import android.content.Context
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.AppTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Learns from user corrections to improve future tab assignments.
 *
 * Analyzes patterns in user-overridden assignments to:
 * - Identify common misassignments
 * - Extract tab preferences for package patterns
 * - Generate hints for LLM providers
 *
 * The learner builds a knowledge base from user corrections that can be
 * used to bias future tab assignment decisions.
 */
class UserCorrectionLearner(
    private val context: Context,
    private val tabDao: TabDao,
) {

    /**
     * Analyzes user corrections and returns tab assignment hints.
     *
     * Returns a map of package patterns to preferred tabs based on
     * user corrections. These hints can be added to LLM prompts to improve
     * accuracy.
     *
     * @return Map of package pattern -> tab preference with confidence
     */
    suspend fun getTabHints(): Map<String, TabHint> = withContext(Dispatchers.IO) {
        val userOverrides = tabDao.getUserOverriddenApps()

        if (userOverrides.isEmpty()) {
            return@withContext emptyMap()
        }

        // Group by package prefix (e.g., "com.google.*" -> tab patterns)
        val packagePrefixPatterns = mutableMapOf<String, MutableList<String>>()

        userOverrides.forEach { override ->
            val packagePrefix = extractPackagePrefix(override.packageName)
            packagePrefixPatterns
                .getOrPut(packagePrefix) { mutableListOf() }
                .add(override.tabName)
        }

        // Build hints from patterns with multiple examples
        val hints = mutableMapOf<String, TabHint>()

        packagePrefixPatterns.forEach { (prefix, tabs) ->
            if (tabs.size >= MIN_SAMPLES_FOR_HINT) {
                // Find most common tab for this prefix
                val tabFrequency = tabs.groupingBy { it }.eachCount()
                val mostCommon = tabFrequency.maxByOrNull { it.value }

                if (mostCommon != null) {
                    val confidence = mostCommon.value.toFloat() / tabs.size
                    if (confidence >= MIN_CONFIDENCE_FOR_HINT) {
                        hints[prefix] = TabHint(
                            pattern = "$prefix.*",
                            tabName = mostCommon.key,
                            confidence = confidence,
                            sampleCount = tabs.size,
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
     * appended to LLM tab assignment prompts.
     *
     * Example output:
     * "Based on previous corrections:
     *  - Apps from com.google tend to be in Productivity (90% confidence)
     *  - Apps from com.facebook tend to be in Social (85% confidence)"
     */
    suspend fun generateLLMHintText(): String = withContext(Dispatchers.IO) {
        val hints = getTabHints()

        if (hints.isEmpty()) {
            return@withContext ""
        }

        val hintLines = hints.values
            .sortedByDescending { it.confidence }
            .take(MAX_HINTS_IN_PROMPT)
            .map {
                val confidencePercent = (it.confidence * 100).toInt()
                "- Apps from ${it.pattern.removeSuffix(".*")} tend to be in " +
                    "${it.tabName} ($confidencePercent% confidence, ${it.sampleCount} samples)"
            }

        if (hintLines.isEmpty()) {
            ""
        } else {
            "\n\nBased on previous user corrections:\n" + hintLines.joinToString("\n")
        }
    }

    /**
     * Checks if a specific app should be biased toward a tab based on learned patterns.
     *
     * @param packageName The app package to check
     * @return TabHint if a strong pattern exists, null otherwise
     */
    suspend fun getHintForPackage(packageName: String): TabHint? = withContext(Dispatchers.IO) {
        val hints = getTabHints()
        val prefix = extractPackagePrefix(packageName)

        hints[prefix]?.takeIf { it.confidence >= MIN_CONFIDENCE_FOR_OVERRIDE }
    }

    /**
     * Analyzes tab assignment accuracy by comparing LLM results with user overrides.
     *
     * @return Statistics about assignment quality
     */
    suspend fun analyzeAccuracy(): TabAccuracyStats = withContext(Dispatchers.IO) {
        val userOverrides = tabDao.getUserOverriddenApps()
        val allAssignments = tabDao.getAllAppTabs()

        val totalAssignments = allAssignments.size
        val totalOverrides = userOverrides.size
        val overrideRate = if (totalAssignments > 0) {
            (totalOverrides.toFloat() / totalAssignments) * 100
        } else {
            0f
        }

        // Group overrides by original source to see which stage needs improvement
        val overridesBySource = userOverrides.groupBy {
            // Try to infer original source before user override
            // In practice, we'd need to store override history
            "llm" // Simplified for now
        }

        TabAccuracyStats(
            totalAssignments = totalAssignments,
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

        fun getInstance(context: Context, tabDao: TabDao): UserCorrectionLearner {
            return instance ?: synchronized(this) {
                instance ?: UserCorrectionLearner(context, tabDao).also { instance = it }
            }
        }
    }
}

/**
 * Represents a learned pattern from user corrections.
 *
 * @property pattern Package pattern (e.g., "com.google.*")
 * @property tabName Preferred tab for this pattern
 * @property confidence Confidence in this pattern (0.0 - 1.0)
 * @property sampleCount Number of user corrections supporting this pattern
 */
data class TabHint(
    val pattern: String,
    val tabName: String,
    val confidence: Float,
    val sampleCount: Int,
)

/**
 * Statistics about tab assignment accuracy and user corrections.
 *
 * @property totalAssignments Total number of assigned apps
 * @property userOverrides Number of apps with user corrections
 * @property overrideRate Percentage of apps that were corrected (0-100)
 * @property overridesBySource Breakdown of overrides by original assignment source
 */
data class TabAccuracyStats(
    val totalAssignments: Int,
    val userOverrides: Int,
    val overrideRate: Float,
    val overridesBySource: Map<String, Int>,
)
