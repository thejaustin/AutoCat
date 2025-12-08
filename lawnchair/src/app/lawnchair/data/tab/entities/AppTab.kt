package app.lawnchair.data.tab.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the tab assignment information for an installed app.
 *
 * This entity stores how an app has been assigned to a tab through the auto-categorization
 * pipeline (built-in, rule-based, LLM) or via user override.
 *
 * @property packageName The unique package identifier for the app
 * @property category The tab name (e.g., "Games", "Social", "Tools") - stored as "category" for DB compatibility
 * @property confidence Confidence score from categorization (0.0 - 1.0)
 * @property source The categorization source: "built-in", "rule", "ml", "llm", or "user"
 * @property isUserOverride If true, prevents automatic re-categorization
 * @property lastUpdated Timestamp of last categorization update (milliseconds since epoch)
 * @property reasoning LLM explanation for why the app was categorized (null for non-LLM sources)
 */
@Entity(tableName = "app_categories")
data class AppTab(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "sub_category")
    val subCategory: String? = null,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "is_user_override")
    val isUserOverride: Boolean = false,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "reasoning")
    val reasoning: String? = null,
) {
    companion object {
        const val SOURCE_BUILT_IN = "built-in"
        const val SOURCE_RULE = "rule"
        const val SOURCE_ML = "ml"
        const val SOURCE_USER = "user"
        const val SOURCE_LLM = "llm"

        // Confidence thresholds
        const val CONFIDENCE_BUILT_IN = 0.95f
        const val CONFIDENCE_RULE_HIGH = 0.80f
        const val CONFIDENCE_RULE_MEDIUM = 0.70f
        const val CONFIDENCE_ML_THRESHOLD = 0.60f
    }

    /**
     * Returns true if this categorization should be considered reliable.
     */
    fun isReliable(): Boolean = confidence >= CONFIDENCE_RULE_MEDIUM || isUserOverride
}
