package app.lawnchair.categorization

import android.content.Context
import android.util.Log
import app.lawnchair.categorization.stages.LLMCategorizer
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
    private val tabDao by lazy { database.tabDao() }
    private val llmCategorizer by lazy { LLMCategorizer(context, tabDao) }
    private val packageManager = context.packageManager

    private data class TabInfo(val tabName: String, val folderName: String?)

    // In-memory cache of app tabs (packageName -> TabInfo)
    // Uses AtomicReference for thread-safe swaps and immutable map for consistent reads
    private val tabCache = AtomicReference<Map<String, TabInfo>>(emptyMap())

    // Cache for folder icons (TabName|FolderName -> IconPath)
    private val folderIcons = ConcurrentHashMap<String, String>()
    private val iconsFile by lazy { java.io.File(context.filesDir, "folder_icons.json") }

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
                    loadFolderIcons()
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
                val appTabs = tabDao.getAllAppTabs()
                val newCache = appTabs.associate { appTab ->
                    appTab.packageName to TabInfo(appTab.tabName, appTab.folderName)
                }
                tabCache.set(newCache)
                cacheInitialized = true
                Log.d(TAG, "Cache initialized with ${newCache.size} categorized apps")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize tab cache", e)
            }
        }
    }

    private fun loadFolderIcons() {
        scope.launch {
            try {
                if (iconsFile.exists()) {
                    val jsonStr = iconsFile.readText()
                    val json = org.json.JSONObject(jsonStr)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        folderIcons[key] = json.getString(key)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load folder icons", e)
            }
        }
    }

    fun saveFolderIcon(tabName: String, folderName: String, iconPath: String) {
        val key = "$tabName|$folderName"
        folderIcons[key] = iconPath

        scope.launch {
            try {
                val json = org.json.JSONObject()
                folderIcons.forEach { (k, v) ->
                    json.put(k, v)
                }
                iconsFile.writeText(json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save folder icons", e)
            }
        }
    }

    fun getFolderIcon(tabName: String, folderName: String): String? {
        return folderIcons["$tabName|$folderName"]
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
     * @param folderName Folder name, or null
     */
    fun updateCacheForApp(packageName: String, tabName: String?, folderName: String? = null) {
        val currentCache = tabCache.get()
        val newCache = currentCache.toMutableMap()

        if (tabName != null) {
            newCache[packageName] = TabInfo(tabName, folderName)
            Log.d(TAG, "Cache updated: $packageName -> $tabName / $folderName")
        } else {
            newCache.remove(packageName)
            Log.d(TAG, "Cache entry removed: $packageName")
        }

        tabCache.set(newCache)
    }

    /**
     * Categorizes apps based on AutoCat database entries.
     * Uses in-memory cache for fast lookups, avoiding database queries.
     *
     * @param appList List of all apps to categorize
     * @return Map of TabName -> (FolderName -> List<AppInfo>)
     *         FolderName key is "" (empty string) if no folder exists.
     */
    fun categorizeApps(appList: List<app.lawnchair.data.apps.AppInfo?>?): Map<String, Map<String, List<AppInfo>>> {
        ensureInitialized()
        if (appList.isNullOrEmpty()) return emptyMap()

        val validApps = appList.filterNotNull()
        // Map<TabName, MutableMap<FolderName, MutableList<AppInfo>>>
        val appsByTab = mutableMapOf<String, MutableMap<String, MutableList<AppInfo>>>()
        val unassignedApps = mutableListOf<AppInfo>()

        // Use cached tabs (fast in-memory lookup)
        val cache = tabCache.get()
        validApps.forEach { app ->
            val tabInfo = app.packageName.let { cache[it] }

            if (tabInfo != null) {
                val subMap = appsByTab.getOrPut(tabInfo.tabName) { mutableMapOf() }
                val folderKey = tabInfo.folderName ?: ""
                subMap.getOrPut(folderKey) { mutableListOf() }.add(app)
            } else {
                unassignedApps.add(app)
            }
        }

        // Add unassigned apps to "Other" tab if any exist
        if (unassignedApps.isNotEmpty()) {
            val otherMap = appsByTab.getOrPut("Other") { mutableMapOf() }
            otherMap.getOrPut("") { mutableListOf() }.addAll(unassignedApps)
        }

        // Sort tabs alphabetically, and sub-folders alphabetically
        return appsByTab.toSortedMap().mapValues { entry ->
            entry.value.toSortedMap()
        }
    }

    /**
     * Gets the color for a tab from the database.
     *
     * @param tabName Name of the tab
     * @return Hex color string (e.g., "#4CAF50") or null if not found
     */
    suspend fun getTabColor(tabName: String): String? {
        return withContext(Dispatchers.IO) {
            tabDao.getCustomTabByName(tabName)?.colorHex
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
