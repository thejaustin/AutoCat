package app.lawnchair.data.tab.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the accuracy of LLM model predictions based on user behavior.
 *
 * This entity records whether a model's categorization was accepted or corrected
 * by the user, enabling data-driven model selection and performance comparison.
 *
 * @property id Auto-generated primary key
 * @property provider The LLM provider (e.g., "google_ai", "claude", "openai")
 * @property model The specific model used (e.g., "gemini-2.0-flash-exp", "claude-3-5-haiku")
 * @property category The category that was predicted
 * @property wasCorrect True if user kept the prediction, false if they overrode it
 * @property confidence The model's confidence score (0.0 - 1.0)
 * @property timestamp When this prediction was made/validated (milliseconds since epoch)
 * @property packageName The app package this prediction was for
 */
@Entity(tableName = "model_accuracy")
data class ModelAccuracy(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "provider")
    val provider: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "tab_name")
    val tabName: String,

    @ColumnInfo(name = "was_correct")
    val wasCorrect: Boolean,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "package_name")
    val packageName: String,
)

/**
 * Data class for aggregated accuracy statistics by model.
 *
 * @property provider The LLM provider
 * @property model The specific model
 * @property total Total number of predictions tracked
 * @property correct Number of predictions that were correct (user kept them)
 * @property accuracy Accuracy percentage (0.0 - 100.0)
 */
data class ModelAccuracyStats(
    val provider: String,
    val model: String,
    val total: Int,
    val correct: Int,
    val accuracy: Float,
)
