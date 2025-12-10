package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.preferences.PreferenceManager
import kotlinx.coroutines.runBlocking

/**
 * Manages dynamic category tabs for the app drawer.
 *
 * Queries the database for visible categories and generates tab configurations
 * based on user preferences.
 */
class CategoryTabsManager(private val context: Context) {

    private val database = TabDatabase.getInstance(context)
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
        val tabName: String?,
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
        val tabs = runBlocking {
            categoryDao.getVisibleCustomCategories()
        }

        android.util.Log.d(TAG, "getTabs: Found ${tabs.size} visible tabs")
        tabs.forEach { tab ->
            android.util.Log.d(TAG, "  - ${tab.name} (id: ${tab.id}, visible: ${tab.isVisible}, sortOrder: ${tab.sortOrder})")
        }

        // Group categories: only create tabs for top-level categories
        val topLevelCategories = tabs
            .map { it.name.split(" > ").first() }
            .distinct()
            .mapNotNull { name -> tabs.find { it.name == name } ?: tabs.find { it.name.startsWith("$name >") }?.copy(name = name) }

        android.util.Log.d(TAG, "getTabs: Found ${topLevelCategories.size} top-level tabs from ${tabs.size} total")

        // Add a tab for each top-level category
        topLevelCategories.forEach { tab ->
            // Use the top-level name
            val name = tab.name.split(" > ").first()
            tabs.add(
                TabInfo(
                    id = "tab_${tab.id}", // Use ID of the representative tab
                    name = name,
                    tabName = name, // This will be used as a prefix match
                    colorHex = tab.colorHex,
                ),
            )
        }

        // Add "Other" tab for uncategorized apps
        tabs.add(
            TabInfo(
                id = "other",
                name = "Other",
                tabName = null,
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
                    tabName = null,
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
