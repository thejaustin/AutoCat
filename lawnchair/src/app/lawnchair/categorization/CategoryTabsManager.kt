package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.preferences.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages dynamic app tabs for the app drawer.
 *
 * Queries the database for visible tabs and generates tab configurations
 * based on user preferences.
 */
class CategoryTabsManager(private val context: Context) {

    private val database by lazy { TabDatabase.getInstance(context) }
    private val categoryDao by lazy { database.categoryDao() }
    private val prefs by lazy { PreferenceManager.getInstance(context) }

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
     * Returns app tabs from the database, plus "Other" tab for uncategorized apps,
     * and optionally the "Work" tab based on preferences.
     *
     * @param hasWorkApps Whether the device has work profile apps
     * @return List of tabs in display order
     */
    suspend fun getTabs(hasWorkApps: Boolean): List<TabInfo> {
        val tabs = mutableListOf<TabInfo>()

        val isZenModeActive = if (prefs.autoCatEnableZenMode.get()) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.currentInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }

        // Get visible custom tabs from database (already sorted by sortOrder)
        val customCategoryTabs = withContext(Dispatchers.IO) {
            categoryDao.getVisibleCustomCategories().filter {
                !isZenModeActive || !it.hideInZenMode
            }
        }

        android.util.Log.d(TAG, "getTabs: Found ${customCategoryTabs.size} visible custom tabs")
        customCategoryTabs.forEach { tab ->
            android.util.Log.d(TAG, "  - ${tab.name} (id: ${tab.id}, visible: ${tab.isVisible}, sortOrder: ${tab.sortOrder})")
        }

        // Group tabs: only create tabs for top-level tabs
        val topLevelTabs = customCategoryTabs
            .map { it.name.split(" > ").first() }
            .distinct()
            .mapNotNull { name -> customCategoryTabs.find { it.name == name } ?: customCategoryTabs.find { it.name.startsWith("$name >") }?.copy(name = name) }

        android.util.Log.d(TAG, "getTabs: Found ${topLevelTabs.size} top-level tabs from ${customCategoryTabs.size} total")

        // Add a tab for each top-level tab
        topLevelTabs.forEach { tab ->
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
        // Show first tab by default
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
