package app.lawnchair.categorization

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import app.lawnchair.categorization.stages.LLMCategorizer
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.tab.TabDatabase
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Provides categorized app lists from AutoCat database.
 *
 * This class bridges the AutoCat categorization system with the app drawer UI,
 * grouping apps by their database-assigned tabs.
 *
 * Uses an in-memory cache to avoid blocking database queries on every app drawer open.
 */
class AutoCatAppProvider(private val context: Context) {

    private val database = TabDatabase.getInstance(context)
    private val categoryDao = database.categoryDao()
    private val llmCategorizer by lazy { LLMCategorizer(context, categoryDao) }
    private val packageManager = context.packageManager

    private data class CategoryInfo(val tabName: String, val subCategory: String?)

    // In-memory cache of app tabs (packageName -> CategoryInfo)
    private val categoryCache = ConcurrentHashMap<String, CategoryInfo>()

    // Cache for subcategory icons (TabName|SubCategory -> IconPath)
    private val subCategoryIcons = ConcurrentHashMap<String, String>()
    private val iconsFile by lazy { java.io.File(context.filesDir, "subcategory_icons.json") }

    // Coroutine scope for async cache updates
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track if cache has been initialized
    @Volatile
    private var cacheInitialized = false

    @Volatile
    private var initializationStarted = false

    /**
     * Ensures cache is initialized. Safe to call multiple times.
     * Initialization happens lazily on first access.
     */
    private fun ensureInitialized() {
        if (!initializationStarted) {
            synchronized(this) {
                if (!initializationStarted) {
                    initializationStarted = true
                    initializeCache()
                    loadSubCategoryIcons()
                }
            }
        }
    }

    /**
     * Initializes the in-memory cache from the database.
     * Called on provider creation and when tabs are updated.
     */
    private fun initializeCache() {
        scope.launch {
            try {
                val appCategories = categoryDao.getAllAppCategories()
                categoryCache.clear()
                appCategories.forEach { appCategory ->
                    categoryCache[appCategory.packageName] = CategoryInfo(appCategory.tabName, appCategory.subCategory)
                }
                cacheInitialized = true
                Log.d(TAG, "Cache initialized with ${categoryCache.size} categorized apps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize tab cache", e)
            }
        }
    }

    private fun loadSubCategoryIcons() {
        scope.launch {
            try {
                if (iconsFile.exists()) {
                    val jsonStr = iconsFile.readText()
                    val json = org.json.JSONObject(jsonStr)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        subCategoryIcons[key] = json.getString(key)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load subcategory icons", e)
            }
        }
    }

    fun saveSubCategoryIcon(tabName: String, subCategory: String, iconPath: String) {
        val key = "$tabName|$subCategory"
        subCategoryIcons[key] = iconPath

        scope.launch {
            try {
                val json = org.json.JSONObject()
                subCategoryIcons.forEach { (k, v) ->
                    json.put(k, v)
                }
                iconsFile.writeText(json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save subcategory icons", e)
            }
        }
    }

    fun getSubCategoryIcon(tabName: String, subCategory: String): String? {
        return subCategoryIcons["$tabName|$subCategory"]
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
     * @param tabName Tab name, or null to remove from cache
     * @param subCategory Subcategory name, or null
     */
    fun updateCacheForApp(packageName: String, tabName: String?, subCategory: String? = null) {
        if (tabName != null) {
            categoryCache[packageName] = CategoryInfo(tabName, subCategory)
            Log.d(TAG, "Cache updated: $packageName -> $tabName / $subCategory")
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
     * @return Map of TabName -> (SubCategory -> List<AppInfo>)
     *         SubCategory key is "" (empty string) if no subcategory exists.
     */
    fun categorizeApps(appList: List<app.lawnchair.data.apps.AppInfo?>?): Map<String, Map<String, List<AppInfo>>> {
        ensureInitialized()
        if (appList.isNullOrEmpty()) return emptyMap()

        val validApps = appList.filterNotNull()
        // Map<TabName, MutableMap<SubCategory, MutableList<AppInfo>>>
        val categorizedApps = mutableMapOf<String, MutableMap<String, MutableList<AppInfo>>>()
        val uncategorizedApps = mutableListOf<AppInfo>()

        // Use cached categories (fast in-memory lookup)
        validApps.forEach { app ->
            val catInfo = app.packageName?.let { categoryCache[it] }

            if (catInfo != null) {
                val subMap = categorizedApps.getOrPut(catInfo.tabName) { mutableMapOf() }
                val subCatKey = catInfo.subCategory ?: ""
                subMap.getOrPut(subCatKey) { mutableListOf() }.add(app)
            } else {
                uncategorizedApps.add(app)
            }
        }

        // Add uncategorized apps to "Other" tab if any exist
        if (uncategorizedApps.isNotEmpty()) {
            val otherMap = categorizedApps.getOrPut("Other") { mutableMapOf() }
            otherMap.getOrPut("") { mutableListOf() }.addAll(uncategorizedApps)
        }

        // Sort tabs alphabetically, and sub-folders alphabetically
        return categorizedApps.toSortedMap().mapValues { entry ->
            entry.value.toSortedMap()
        }
    }

    /**
     * Gets the color for a category from the database.
     *
     * @param categoryName Name of the category
     * @return Hex color string (e.g., "#4CAF50") or null if not found
     */
    suspend fun getTabColor(tabName: String): String? {
        return withContext(Dispatchers.IO) {
            categoryDao.getCustomCategoryByName(tabName)?.colorHex
        }
    }

    /**
     * Categorizes a single new app using the LLM categorizer and updates the cache.
     *
     * @param packageName The package name of the app to categorize.
     */
    suspend fun categorizeNewApp(packageName: String) {
        withContext(Dispatchers.IO) {
            try {
                // Fetch AppInfo for the given package name
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                val appInfo = AppInfo(
                    packageName = packageName,
                    label = packageManager.getApplicationLabel(applicationInfo).toString(),
                    category = applicationInfo.category.takeIf { it != -1 }, // -1 means undefined
                    installedTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime,
                    description = null,
                )

                // Categorize using LLMCategorizer
                llmCategorizer.categorize(appInfo)
                refreshCache()
                Log.d(TAG, "Successfully categorized and cached new app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to categorize new app: $packageName", e)
            }
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
