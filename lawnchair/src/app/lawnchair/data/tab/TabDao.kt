package app.lawnchair.data.tab

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app tab assignment operations.
 *
 * Provides methods to manage app tab assignments and custom tab definitions.
 * Uses Flow for reactive data observation and supports batch operations.
 */
@Dao
interface TabDao {

    // ==================== AppTab Operations ====================

    /**
     * Inserts or updates an app's tab assignment information.
     * On conflict (same package name), replaces the existing entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppCategory(appCategory: AppTab)

    /**
     * Inserts or updates multiple app tab assignments in a single transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppCategories(appCategories: List<AppTab>)

    /**
     * Updates an existing app tab assignment.
     */
    @Update
    suspend fun updateAppCategory(appCategory: AppTab)

    /**
     * Deletes an app's tab assignment information.
     */
    @Delete
    suspend fun deleteAppCategory(appCategory: AppTab)

    /**
     * Gets the tab assignment for a specific app by package name.
     */
    @Query("SELECT * FROM app_categories WHERE package_name = :packageName LIMIT 1")
    suspend fun getAppCategory(packageName: String): AppTab?

    /**
     * Observes the tab assignment for a specific app.
     * Returns Flow that emits whenever the tab assignment changes.
     */
    @Query("SELECT * FROM app_categories WHERE package_name = :packageName LIMIT 1")
    fun observeAppCategory(packageName: String): Flow<AppTab?>

    /**
     * Gets all apps with tab assignments.
     */
    @Query("SELECT * FROM app_categories ORDER BY tab_name, package_name")
    suspend fun getAllAppCategories(): List<AppTab>

    /**
     * Observes all apps with tab assignments with reactive updates.
     */
    @Query("SELECT * FROM app_categories ORDER BY tab_name, package_name")
    fun observeAllAppCategories(): Flow<List<AppTab>>

    /**
     * Gets all apps in a specific tab.
     */
    @Query("SELECT * FROM app_categories WHERE tab_name = :tabName ORDER BY package_name")
    suspend fun getAppsByTab(tabName: String): List<AppTab>

    /**
     * Observes all apps in a specific tab.
     */
    @Query("SELECT * FROM app_categories WHERE tab_name = :tabName ORDER BY package_name")
    fun observeAppsByTab(tabName: String): Flow<List<AppTab>>

    /**
     * Gets all apps that were categorized by a specific source.
     */
    @Query("SELECT * FROM app_categories WHERE source = :source ORDER BY tab_name, package_name")
    suspend fun getAppsBySource(source: String): List<AppTab>

    /**
     * Gets all apps with user overrides (manually categorized).
     */
    @Query("SELECT * FROM app_categories WHERE is_user_override = 1 ORDER BY tab_name, package_name")
    suspend fun getUserOverriddenApps(): List<AppTab>

    /**
     * Gets count of apps in each tab.
     */
    @Query("SELECT tab_name as category, COUNT(*) as count FROM app_categories GROUP BY tab_name")
    suspend fun getTabCounts(): List<TabCount>

    /**
     * Deletes all app tab assignments that are not user overrides.
     * Useful for re-categorizing apps when rules or models change.
     */
    @Query("DELETE FROM app_categories WHERE is_user_override = 0")
    suspend fun deleteNonUserOverrides()

    /**
     * Deletes tab assignments for apps that no longer exist (cleanup operation).
     */
    @Query("DELETE FROM app_categories WHERE package_name IN (:packageNames)")
    suspend fun deleteAppsByPackageNames(packageNames: List<String>)

    /**
     * Updates the tab name for all apps in a specific tab.
     * Used when renaming a custom tab.
     */
    @Query("UPDATE app_categories SET tab_name = :newTabName WHERE tab_name = :oldTabName")
    suspend fun updateAppTabName(oldTabName: String, newTabName: String)

    /**
     * Resets the tab to 'Other' for all apps in a specific tab.
     * Used when deleting a custom tab.
     */
    @Query("UPDATE app_categories SET tab_name = 'Other' WHERE tab_name = :tabName")
    suspend fun resetAppTabsForDeletedTab(tabName: String)

    // ==================== CustomTab Operations ====================

    /**
     * Inserts a new custom tab.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(customCategory: CustomTab): Long

    /**
     * Inserts multiple custom tabs.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategories(customCategories: List<CustomTab>)

    /**
     * Updates an existing custom tab.
     */
    @Update
    suspend fun updateCustomCategory(customCategory: CustomTab)

    /**
     * Deletes a custom tab.
     */
    @Delete
    suspend fun deleteCustomCategory(customCategory: CustomTab)

    /**
     * Gets all custom tabs ordered by sort order.
     */
    @Query("SELECT * FROM custom_categories ORDER BY sort_order")
    suspend fun getAllCustomCategories(): List<CustomTab>

    /**
     * Observes all custom tabs with reactive updates.
     */
    @Query("SELECT * FROM custom_categories ORDER BY sort_order")
    fun observeAllCustomCategories(): Flow<List<CustomTab>>

    /**
     * Gets only visible custom tabs.
     */
    @Query("SELECT * FROM custom_categories WHERE is_visible = 1 ORDER BY sort_order")
    suspend fun getVisibleCustomCategories(): List<CustomTab>

    /**
     * Observes only visible custom tabs.
     */
    @Query("SELECT * FROM custom_categories WHERE is_visible = 1 ORDER BY sort_order")
    fun observeVisibleCustomCategories(): Flow<List<CustomTab>>

    /**
     * Gets a custom tab by name.
     */
    @Query("SELECT * FROM custom_categories WHERE name = :name LIMIT 1")
    suspend fun getCustomCategoryByName(name: String): CustomTab?

    /**
     * Gets a custom tab by ID.
     */
    @Query("SELECT * FROM custom_categories WHERE id = :id LIMIT 1")
    suspend fun getCustomCategoryById(id: Int): CustomTab?

    /**
     * Updates the visibility of a custom tab.
     */
    @Query("UPDATE custom_categories SET is_visible = :isVisible WHERE id = :id")
    suspend fun updateCustomCategoryVisibility(id: Int, isVisible: Boolean)

    /**
     * Updates the sort order for a custom tab.
     */
    @Query("UPDATE custom_categories SET sort_order = :sortOrder WHERE id = :id")
    suspend fun updateCustomCategorySortOrder(id: Int, sortOrder: Int)

    /**
     * Deletes all custom tabs (useful for reset operations).
     */
    @Query("DELETE FROM custom_categories")
    suspend fun deleteAllCustomCategories()

    // ==================== Transactional Operations ====================

    /**
     * Initializes default tabs if the table is empty.
     * This ensures users always have standard tabs available.
     */
    @Transaction
    suspend fun initializeDefaultCategoriesIfNeeded() {
        val existingCategories = getAllCustomCategories()
        if (existingCategories.isEmpty()) {
            insertCustomCategories(CustomTab.getDefaultTabs())
        }
    }
}

/**
 * Data class for tab count query results.
 *
 * @property category The tab name (stored as "category" for DB compatibility)
 * @property count The number of apps in this tab
 */
data class TabCount(
    val category: String,
    val count: Int,
)
