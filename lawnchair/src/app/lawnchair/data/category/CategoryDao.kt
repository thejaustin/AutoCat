package app.lawnchair.data.category

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.lawnchair.data.category.entities.AppCategory
import app.lawnchair.data.category.entities.CustomCategory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app categorization operations.
 *
 * Provides methods to manage app categories and custom category definitions.
 * Uses Flow for reactive data observation and supports batch operations.
 */
@Dao
interface CategoryDao {

    // ==================== AppCategory Operations ====================

    /**
     * Inserts or updates an app's category information.
     * On conflict (same package name), replaces the existing entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppCategory(appCategory: AppCategory)

    /**
     * Inserts or updates multiple app categories in a single transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppCategories(appCategories: List<AppCategory>)

    /**
     * Updates an existing app category.
     */
    @Update
    suspend fun updateAppCategory(appCategory: AppCategory)

    /**
     * Deletes an app's category information.
     */
    @Delete
    suspend fun deleteAppCategory(appCategory: AppCategory)

    /**
     * Gets the category for a specific app by package name.
     */
    @Query("SELECT * FROM app_categories WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppCategory(packageName: String): AppCategory?

    /**
     * Observes the category for a specific app.
     * Returns Flow that emits whenever the category changes.
     */
    @Query("SELECT * FROM app_categories WHERE package_name = :packageName LIMIT 1")
    fun observeAppCategory(packageName: String): Flow<AppCategory?>

    /**
     * Gets all categorized apps.
     */
    @Query("SELECT * FROM app_categories ORDER BY category, package_name")
    suspend fun getAllAppCategories(): List<AppCategory>

    /**
     * Observes all categorized apps with reactive updates.
     */
    @Query("SELECT * FROM app_categories ORDER BY category, package_name")
    fun observeAllAppCategories(): Flow<List<AppCategory>>

    /**
     * Gets all apps in a specific category.
     */
    @Query("SELECT * FROM app_categories WHERE category = :categoryName ORDER BY package_name")
    suspend fun getAppsByCategory(categoryName: String): List<AppCategory>

    /**
     * Observes all apps in a specific category.
     */
    @Query("SELECT * FROM app_categories WHERE category = :categoryName ORDER BY package_name")
    fun observeAppsByCategory(categoryName: String): Flow<List<AppCategory>>

    /**
     * Gets all apps that were categorized by a specific source.
     */
    @Query("SELECT * FROM app_categories WHERE source = :source ORDER BY category, package_name")
    suspend fun getAppsBySource(source: String): List<AppCategory>

    /**
     * Gets all apps with user overrides (manually categorized).
     */
    @Query("SELECT * FROM app_categories WHERE is_user_override = 1 ORDER BY category, package_name")
    suspend fun getUserOverriddenApps(): List<AppCategory>

    /**
     * Gets count of apps in each category.
     */
    @Query("SELECT category, COUNT(*) as count FROM app_categories GROUP BY category")
    suspend fun getCategoryCounts(): Map<String, Int>

    /**
     * Deletes all app categories that are not user overrides.
     * Useful for re-categorizing apps when rules or models change.
     */
    @Query("DELETE FROM app_categories WHERE is_user_override = 0")
    suspend fun deleteNonUserOverrides()

    /**
     * Deletes categories for apps that no longer exist (cleanup operation).
     */
    @Query("DELETE FROM app_categories WHERE package_name IN (:packageNames)")
    suspend fun deleteAppsByPackageNames(packageNames: List<String>)

    // ==================== CustomCategory Operations ====================

    /**
     * Inserts a new custom category.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(customCategory: CustomCategory): Long

    /**
     * Inserts multiple custom categories.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategories(customCategories: List<CustomCategory>)

    /**
     * Updates an existing custom category.
     */
    @Update
    suspend fun updateCustomCategory(customCategory: CustomCategory)

    /**
     * Deletes a custom category.
     */
    @Delete
    suspend fun deleteCustomCategory(customCategory: CustomCategory)

    /**
     * Gets all custom categories ordered by sort order.
     */
    @Query("SELECT * FROM custom_categories ORDER BY sort_order")
    suspend fun getAllCustomCategories(): List<CustomCategory>

    /**
     * Observes all custom categories with reactive updates.
     */
    @Query("SELECT * FROM custom_categories ORDER BY sort_order")
    fun observeAllCustomCategories(): Flow<List<CustomCategory>>

    /**
     * Gets only visible custom categories.
     */
    @Query("SELECT * FROM custom_categories WHERE is_visible = 1 ORDER BY sort_order")
    suspend fun getVisibleCustomCategories(): List<CustomCategory>

    /**
     * Observes only visible custom categories.
     */
    @Query("SELECT * FROM custom_categories WHERE is_visible = 1 ORDER BY sort_order")
    fun observeVisibleCustomCategories(): Flow<List<CustomCategory>>

    /**
     * Gets a custom category by name.
     */
    @Query("SELECT * FROM custom_categories WHERE name = :name LIMIT 1")
    suspend fun getCustomCategoryByName(name: String): CustomCategory?

    /**
     * Gets a custom category by ID.
     */
    @Query("SELECT * FROM custom_categories WHERE id = :id LIMIT 1")
    suspend fun getCustomCategoryById(id: Int): CustomCategory?

    /**
     * Updates the visibility of a custom category.
     */
    @Query("UPDATE custom_categories SET is_visible = :isVisible WHERE id = :id")
    suspend fun updateCustomCategoryVisibility(id: Int, isVisible: Boolean)

    /**
     * Updates the sort order for a custom category.
     */
    @Query("UPDATE custom_categories SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateCustomCategorySortOrder(id: Int, sortOrder: Int)

    /**
     * Deletes all custom categories (useful for reset operations).
     */
    @Query("DELETE FROM custom_categories")
    suspend fun deleteAllCustomCategories()

    // ==================== Transactional Operations ====================

    /**
     * Initializes default categories if the table is empty.
     * This ensures users always have standard categories available.
     */
    @Transaction
    suspend fun initializeDefaultCategoriesIfNeeded() {
        val existingCategories = getAllCustomCategories()
        if (existingCategories.isEmpty()) {
            insertCustomCategories(CustomCategory.getDefaultCategories())
        }
    }
}
