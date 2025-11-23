package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.data.category.CategoryDatabase
import com.android.launcher3.model.data.AppInfo
import kotlinx.coroutines.runBlocking

/**
 * Provides categorized app lists from AutoCat database.
 *
 * This class bridges the AutoCat categorization system with the app drawer UI,
 * grouping apps by their database-assigned categories.
 */
class AutoCatAppProvider(private val context: Context) {

    private val database = CategoryDatabase.getInstance(context)
    private val categoryDao = database.categoryDao()

    /**
     * Categorizes apps based on AutoCat database entries.
     *
     * @param appList List of all apps to categorize
     * @return Map of category name to list of apps in that category
     */
    fun categorizeApps(appList: List<AppInfo?>?): Map<String, List<AppInfo>> {
        if (appList.isNullOrEmpty()) return emptyMap()

        val validApps = appList.filterNotNull()
        val categorizedApps = mutableMapOf<String, MutableList<AppInfo>>()
        val uncategorizedApps = mutableListOf<AppInfo>()

        // Query database for all app categories
        val appCategories = runBlocking {
            categoryDao.getAllAppCategories()
        }.associateBy { it.packageName }

        // Group apps by their database category
        validApps.forEach { app ->
            val packageName = app.componentName?.packageName
            val category = packageName?.let { appCategories[it]?.category }

            if (category != null) {
                categorizedApps.getOrPut(category) { mutableListOf() }.add(app)
            } else {
                uncategorizedApps.add(app)
            }
        }

        // Add uncategorized apps to "Other" category if any exist
        if (uncategorizedApps.isNotEmpty()) {
            categorizedApps["Other"] = uncategorizedApps
        }

        // Sort categories alphabetically
        return categorizedApps.toSortedMap()
    }

    /**
     * Gets the color for a category from the database.
     *
     * @param categoryName Name of the category
     * @return Hex color string (e.g., "#4CAF50") or null if not found
     */
    fun getCategoryColor(categoryName: String): String? {
        return runBlocking {
            categoryDao.getCustomCategoryByName(categoryName)?.colorHex
        }
    }

    companion object {
        @Volatile
        private var instance: AutoCatAppProvider? = null

        /**
         * Gets singleton instance of AutoCatAppProvider.
         */
        fun getInstance(context: Context): AutoCatAppProvider {
            return instance ?: synchronized(this) {
                instance ?: AutoCatAppProvider(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
