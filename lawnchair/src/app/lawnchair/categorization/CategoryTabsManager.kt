package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.data.category.entities.CustomCategory
import app.lawnchair.preferences.PreferenceManager
import kotlinx.coroutines.runBlocking

/**
 * Manages dynamic category tabs for the app drawer.
 *
 * Queries the database for visible categories and generates tab configurations
 * based on user preferences.
 */
class CategoryTabsManager(private val context: Context) {

    private val database = CategoryDatabase.getInstance(context)
    private val categoryDao = database.categoryDao()
    private val prefs = PreferenceManager.getInstance(context)

    /**
     * Represents a tab in the app drawer.
     *
     * @property id Unique identifier for the tab
     * @property name Display name shown on the tab
     * @property category Category name for filtering apps (null for special tabs like "Other")
     * @property colorHex Hex color code for the tab (e.g., "#4CAF50")
     * @property isWorkTab True if this is the work profile tab
     * @property isOtherTab True if this is the uncategorized apps tab
     */
    data class TabInfo(
        val id: String,
        val name: String,
        val category: String?,
        val colorHex: String?,
        val isWorkTab: Boolean = false,
        val isOtherTab: Boolean = false,
    )

    /**
     * Gets the list of tabs to display in the app drawer.
     *
     * Returns category tabs from the database, plus "Other" tab for uncategorized apps,
     * and optionally the "Work" tab based on preferences.
     *
     * @param hasWorkApps Whether the device has work profile apps
     * @return List of tabs in display order
     */
    fun getTabs(hasWorkApps: Boolean): List<TabInfo> {
        val tabs = mutableListOf<TabInfo>()

        // Get visible categories from database (already sorted by sortOrder)
        val categories = runBlocking {
            categoryDao.getVisibleCustomCategories()
        }

        android.util.Log.d(TAG, "getTabs: Found ${categories.size} visible categories")
        categories.forEach { category ->
            android.util.Log.d(TAG, "  - ${category.name} (id: ${category.id}, visible: ${category.isVisible}, sortOrder: ${category.sortOrder})")
        }

        // Add a tab for each visible category
        categories.forEach { category ->
            tabs.add(
                TabInfo(
                    id = "category_${category.id}",
                    name = category.name,
                    category = category.name,
                    colorHex = category.colorHex,
                ),
            )
        }

        // Add "Other" tab for uncategorized apps
        tabs.add(
            TabInfo(
                id = "other",
                name = "Other",
                category = null,
                colorHex = "#9E9E9E", // Gray color
                isOtherTab = true,
            ),
        )

        // Add "Work" tab if enabled and work apps exist
        if (hasWorkApps && prefs.showWorkTab.get()) {
            tabs.add(
                TabInfo(
                    id = "work",
                    name = "Work",
                    category = null,
                    colorHex = "#607D8B", // Blue-gray color
                    isWorkTab = true,
                ),
            )
        }

        android.util.Log.d(TAG, "getTabs: Returning ${tabs.size} total tabs")
        return tabs
    }

    /**
     * Gets the default tab index to show on app drawer open.
     *
     * @param tabs List of available tabs
     * @return Index of the tab to show (defaults to first tab)
     */
    fun getDefaultTabIndex(tabs: List<TabInfo>): Int {
        // Show first category tab by default
        return 0
    }

    companion object {
        private const val TAG = "CategoryTabsManager"

        @Volatile
        private var instance: CategoryTabsManager? = null

        /**
         * Gets singleton instance of CategoryTabsManager.
         */
        fun getInstance(context: Context): CategoryTabsManager {
            return instance ?: synchronized(this) {
                instance ?: CategoryTabsManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
