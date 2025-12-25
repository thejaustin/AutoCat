package app.lawnchair.data.tab

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.lawnchair.data.tab.entities.ModelAccuracy
import app.lawnchair.data.tab.entities.ModelAccuracyStats
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for model accuracy tracking operations.
 *
 * Provides methods to record and analyze LLM model prediction accuracy
 * based on user acceptance or correction of categorizations.
 */
@Dao
interface AccuracyDao {

    /**
     * Records a prediction result (correct or incorrect).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accuracy: ModelAccuracy)

    /**
     * Records multiple prediction results in a batch.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accuracies: List<ModelAccuracy>)

    /**
     * Gets accuracy statistics grouped by provider and model.
     *
     * @param since Only include predictions made after this timestamp (milliseconds)
     * @return List of accuracy stats sorted by accuracy descending
     */
    @Query(
        """
        SELECT
            provider,
            model,
            COUNT(*) as total,
            SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) as correct,
            (SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as accuracy
        FROM model_accuracy
        WHERE timestamp >= :since
        GROUP BY provider, model
        ORDER BY accuracy DESC
        """,
    )
    suspend fun getAccuracyByModel(since: Long): List<ModelAccuracyStats>

    /**
     * Observes accuracy statistics with reactive updates.
     *
     * @param since Only include predictions made after this timestamp (milliseconds)
     * @return Flow that emits updated stats whenever accuracy data changes
     */
    @Query(
        """
        SELECT
            provider,
            model,
            COUNT(*) as total,
            SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) as correct,
            (SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as accuracy
        FROM model_accuracy
        WHERE timestamp >= :since
        GROUP BY provider, model
        ORDER BY accuracy DESC
        """,
    )
    fun observeAccuracyByModel(since: Long): Flow<List<ModelAccuracyStats>>

    /**
     * Gets accuracy statistics for a specific provider.
     *
     * @param provider The provider name (e.g., "google_ai", "claude")
     * @param since Only include predictions made after this timestamp
     * @return List of accuracy stats for the provider's models
     */
    @Query(
        """
        SELECT
            provider,
            model,
            COUNT(*) as total,
            SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) as correct,
            (SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as accuracy
        FROM model_accuracy
        WHERE provider = :provider AND timestamp >= :since
        GROUP BY provider, model
        ORDER BY accuracy DESC
        """,
    )
    suspend fun getAccuracyByProvider(provider: String, since: Long): List<ModelAccuracyStats>

    /**
     * Gets the total number of accuracy records.
     */
    @Query("SELECT COUNT(*) FROM model_accuracy")
    suspend fun getTotalRecords(): Int

    /**
     * Gets the number of records for a specific provider/model combination.
     *
     * @param provider The provider name
     * @param model The model name
     * @return Number of recorded predictions for this model
     */
    @Query("SELECT COUNT(*) FROM model_accuracy WHERE provider = :provider AND model = :model")
    suspend fun getRecordCount(provider: String, model: String): Int

    /**
     * Deletes accuracy records older than the specified timestamp.
     * Useful for cleaning up old data or limiting database size.
     *
     * @param before Delete records with timestamp older than this (milliseconds)
     * @return Number of records deleted
     */
    @Query("DELETE FROM model_accuracy WHERE timestamp < :before")
    suspend fun deleteOldRecords(before: Long): Int

    /**
     * Deletes all accuracy records (use with caution).
     */
    @Query("DELETE FROM model_accuracy")
    suspend fun deleteAll()

    /**
     * Gets the most accurate model based on recent predictions.
     *
     * @param minSamples Minimum number of predictions required to be considered
     * @param since Only include predictions made after this timestamp
     * @return The best performing model, or null if no model meets criteria
     */
    @Query(
        """
        SELECT
            provider,
            model,
            COUNT(*) as total,
            SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) as correct,
            (SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as accuracy
        FROM model_accuracy
        WHERE timestamp >= :since
        GROUP BY provider, model
        HAVING COUNT(*) >= :minSamples
        ORDER BY accuracy DESC, total DESC
        LIMIT 1
        """,
    )
    suspend fun getBestModel(minSamples: Int = 10, since: Long): ModelAccuracyStats?

    /**
     * Gets accuracy statistics for a specific category.
     * Helps identify which models perform best for certain types of apps.
     *
     * @param category The category name (e.g., "Games", "Social")
     * @param since Only include predictions made after this timestamp
     * @return List of accuracy stats for this category
     */
    @Query(
        """
        SELECT
            provider,
            model,
            COUNT(*) as total,
            SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) as correct,
            (SUM(CASE WHEN was_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) as accuracy
        FROM model_accuracy
        WHERE category = :category AND timestamp >= :since
        GROUP BY provider, model
        ORDER BY accuracy DESC
        """,
    )
    suspend fun getAccuracyByCategory(category: String, since: Long): List<ModelAccuracyStats>
}
