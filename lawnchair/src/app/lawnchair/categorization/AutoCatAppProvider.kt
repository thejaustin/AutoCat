package app.lawnchair.categorization

import android.content.Context
import android.util.Log
import app.lawnchair.data.category.CategoryDatabase
import com.android.launcher3.model.data.AppInfo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Provides categorized app lists from AutoCat database.
 *
 * This class bridges the AutoCat categorization system with the app drawer UI,
 * grouping apps by their database-assigned categories.
 *
 * Uses an in-memory cache to avoid blocking database queries on every app drawer open.
 */
class AutoCatAppProvider(private val context: Context) {

    private val database = CategoryDatabase.getInstance(context)
    private val categoryDao = database.categoryDao()

    // In-memory cache of app categories (packageName -> categoryName)
    private val categoryCache = ConcurrentHashMap<String, String>()

    // Coroutine scope for async cache updates
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track if cache has been initialized
    @Volatile
    private var cacheInitialized = false

    init {
        // Initialize cache asynchronously on startup
        initializeCache()
    }

    /**
     * Initializes the in-memory cache from the database.
     * Called on provider creation and when categories are updated.
     */
    private fun initializeCache() {
        scope.launch {
            try {
                val appCategories = categoryDao.getAllAppCategories()
                categoryCache.clear()
                appCategories.forEach { appCategory ->
                    categoryCache[appCategory.packageName] = appCategory.category
                }
                cacheInitialized = true
                Log.d(TAG, "Cache initialized with ${categoryCache.size} categorized apps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize category cache", e)
            }
        }
    }

    /**
     * Invalidates and reloads the cache.
     * Should be called after categorization changes, new app installations, etc.
     */
    fun refreshCache() {
        Log.d(TAG, "Cache refresh requested")
        initializeCache()
    }

    /**
     * Updates cache for a single app without full reload.
     *
     * @param packageName Package name of the app
     * @param category Category name, or null to remove from cache
     */
    fun updateCacheForApp(packageName: String, category: String?) {
        if (category != null) {
            categoryCache[packageName] = category
            Log.d(TAG, "Cache updated: $packageName -> $category")
        } else {
            categoryCache.remove(packageName)
            Log.d(TAG, "Cache entry removed: $packageName")
        }
    }

    /**
     * Categorizes apps based on AutoCat database entries.
     * Uses in-memory cache for fast lookups, avoiding database queries.
     *
     * @param appList List of all apps to categorize
     * @return Map of category name to list of apps in that category
     */
    fun categorizeApps(appList: List<AppInfo?>?): Map<String, List<AppInfo>> {
        if (appList.isNullOrEmpty()) return emptyMap()

        val validApps = appList.filterNotNull()
        val categorizedApps = mutableMapOf<String, MutableList<AppInfo>>()
        val uncategorizedApps = mutableListOf<AppInfo>()

        // Use cached categories (fast in-memory lookup)
        validApps.forEach { app ->
            val packageName = app.componentName?.packageName
            val category = packageName?.let { categoryCache[it] }

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
        private const val TAG = "AutoCatAppProvider"

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
