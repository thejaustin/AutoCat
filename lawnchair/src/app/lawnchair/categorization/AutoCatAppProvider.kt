package app.lawnchair.categorization

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import app.lawnchair.categorization.stages.LLMCategorizer
import app.lawnchair.categorization.stages.MLCategorizer
import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.tab.TabDatabase
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    private val database by lazy { TabDatabase.getInstance(context) }
    private val categoryDao by lazy { database.tabDao() }
    private val llmCategorizer by lazy { LLMCategorizer(context, categoryDao) }
    private val mlCategorizer by lazy { MLCategorizer(context, categoryDao) }
    private val packageManager = context.packageManager

    private data class CategoryInfo(val tabName: String, val subCategory: String?)

    // In-memory cache of app tabs (packageName -> CategoryInfo)
    // Uses AtomicReference for thread-safe swaps and immutable map for consistent reads
    private val categoryCache = AtomicReference<Map<String, CategoryInfo>>(emptyMap())

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
                val appCategories = categoryDao.getAllAppTabs()
                val newCache = appCategories.associate { appCategory ->
                    appCategory.packageName to CategoryInfo(appCategory.tabName, appCategory.subCategory)
                }
                categoryCache.set(newCache)
                cacheInitialized = true
                Log.d(TAG, "Cache initialized with ${newCache.size} categorized apps")
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
        val currentCache = categoryCache.get()
        val newCache = currentCache.toMutableMap()

        if (tabName != null) {
            newCache[packageName] = CategoryInfo(tabName, subCategory)
            Log.d(TAG, "Cache updated: $packageName -> $tabName / $subCategory")
        } else {
            newCache.remove(packageName)
            Log.d(TAG, "Cache entry removed: $packageName")
        }

        categoryCache.set(newCache)
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
        val vaultApps = mutableListOf<AppInfo>()

        // Use cached categories (fast in-memory lookup)
        val cache = categoryCache.get()
        validApps.forEach { app ->
            val wrapper = com.android.launcher3.util.ApplicationInfoWrapper(context, app.packageName, android.os.Process.myUserHandle())
            val isArchived = wrapper.isArchived()
            val isEnabled = wrapper.isEnabled()

            if (isArchived || !isEnabled) {
                vaultApps.add(app)
            } else {
                val catInfo = app.packageName.let { cache[it] }

                if (catInfo != null) {
                    val subMap = categorizedApps.getOrPut(catInfo.tabName) { mutableMapOf() }
                    val subCatKey = catInfo.subCategory ?: ""
                    subMap.getOrPut(subCatKey) { mutableListOf() }.add(app)
                } else {
                    uncategorizedApps.add(app)
                }
            }
        }

        // Add uncategorized apps to "Other" tab if any exist
        if (uncategorizedApps.isNotEmpty()) {
            val otherMap = categorizedApps.getOrPut(CategorizationConstants.UNCATEGORIZED_TAB) { mutableMapOf() }
            otherMap.getOrPut("") { mutableListOf() }.addAll(uncategorizedApps)
        }

        // Add archived/frozen apps to Vault tab
        if (vaultApps.isNotEmpty()) {
            val vaultMap = categorizedApps.getOrPut(AppTabsController.TAB_VAULT) { mutableMapOf() }
            vaultMap.getOrPut("") { mutableListOf() }.addAll(vaultApps)
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
            categoryDao.getCustomTabByName(tabName)?.colorHex
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

                // Stage 1: LLM categorizer
                val llmSuccess = llmCategorizer.categorize(appInfo)

                // Stage 2: On-device ML categorizer (fallback when LLM fails or no key)
                if (!llmSuccess) {
                    mlCategorizer.categorize(appInfo)
                }

                refreshCache()
                Log.d(TAG, "Successfully categorized and cached new app: $packageName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to categorize new app: $packageName", e)
            }
        }
    }

    /**
     * Cancels all running coroutines and cleans up resources.
     * Should be called when the provider is no longer needed.
     */
    fun cleanup() {
        scope.cancel()
        Log.d(TAG, "AutoCatAppProvider cleaned up, coroutine scope cancelled")
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

        /**
         * Cleans up the singleton instance and cancels all coroutines.
         * Call this when the application is terminating to prevent memory leaks.
         */
        fun cleanup() {
            instance?.cleanup()
            instance = null
        }
    }
}
