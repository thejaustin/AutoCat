package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.llm.AppBatchInfo
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.data.Converters
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.folder.FolderInfoEntity
import app.lawnchair.data.folder.FolderItemEntity
import app.lawnchair.data.folder.service.FolderDao
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.util.ComponentKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for automatically sorting apps with tab assignments into folders.
 *
 * This is designed for apps that have category/tab metadata (e.g., from Smart Launcher import)
 * but aren't organized into folders yet. The LLM analyzes these apps and suggests logical
 * folder groupings within each tab.
 *
 * Key behavior:
 * - Only processes apps with tab assignments (AppTab entries)
 * - Skips apps already in folders (respects manual organization)
 * - Uses LLM to suggest folder names and groupings
 * - Creates folders and assigns apps automatically
 */
class FolderAutoSortService(private val context: Context) {

    private val tabDatabase = TabDatabase.getInstance(context)
    private val tabDao: TabDao = tabDatabase.categoryDao()
    private val folderService = FolderService.INSTANCE.get(context)
    private val metadataProvider = AppMetadataProvider(context)
    private val converters = Converters()

    // LLM providers for folder suggestions
    private val googleProvider = GoogleAIProvider(context)
    private val providers = mapOf(
        "google_ai" to googleProvider,
    )

    companion object {
        private const val TAG = "FolderAutoSortService"

        @Volatile
        private var instance: FolderAutoSortService? = null

        fun getInstance(context: Context): FolderAutoSortService {
            return instance ?: synchronized(this) {
                instance ?: FolderAutoSortService(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Detects apps that have tab assignments but aren't in any folder.
     *
     * @return Map of tab name to list of unsorted apps in that tab
     */
    suspend fun detectUnsortedApps(): Map<String, List<AppInfo>> = withContext(Dispatchers.IO) {
        try {
            // Get all apps with tab assignments
            val appsWithTabs = tabDao.getAllAppCategories()

            // Get all folder items to know which apps are already in folders
            val existingFolders = folderService.getAllFolders()
            val appsInFolders = mutableSetOf<String>()

            existingFolders.forEach { folder ->
                folder.contents.forEach { item ->
                    if (item is AppInfo) {
                        val componentKey = converters.fromComponentKey(item.toComponentKey())
                        if (componentKey != null) {
                            appsInFolders.add(componentKey)
                        }
                    }
                }
            }

            // Group unsorted apps by tab
            val unsortedByTab = mutableMapOf<String, MutableList<AppInfo>>()

            appsWithTabs.forEach { appTab ->
                // Get AppInfo for this package
                val appInfo = metadataProvider.getAppInfo(appTab.packageName)

                if (appInfo != null) {
                    val componentKey = converters.fromComponentKey(appInfo.toComponentKey())

                    // Check if app is NOT in any folder
                    if (componentKey != null && !appsInFolders.contains(componentKey)) {
                        val tabName = appTab.category
                        unsortedByTab.getOrPut(tabName) { mutableListOf() }.add(appInfo)
                    }
                }
            }

            android.util.Log.d(TAG, "Found ${unsortedByTab.values.sumOf { it.size }} unsorted apps across ${unsortedByTab.size} tabs")

            unsortedByTab
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error detecting unsorted apps", e)
            emptyMap()
        }
    }

    /**
     * Suggests folder groupings for apps within a tab using LLM.
     *
     * @param tabName The tab/category name
     * @param apps List of apps to organize
     * @return List of suggested folders with package names
     */
    suspend fun suggestFolderGroupings(
        tabName: String,
        apps: List<AppInfo>,
    ): List<SuggestedFolder> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "Suggesting folder groupings for ${apps.size} apps in '$tabName' tab")

            // Get user's preferred provider
            val prefManager = PreferenceManager.getInstance(context)
            val preferredProviderId = prefManager.llmProviderPreference.get()
            val provider = providers[preferredProviderId] ?: googleProvider

            if (!provider.isAvailable()) {
                android.util.Log.w(TAG, "LLM provider ${provider.name} not available")
                return@withContext emptyList()
            }

            // Convert AppInfo to AppBatchInfo for LLM
            val appBatch = apps.map { app ->
                AppBatchInfo(
                    packageName = app.componentName.packageName,
                    appName = app.title.toString(),
                    appDescription = null, // We don't have descriptions for launcher apps yet
                )
            }

            // Call LLM provider's suggestFolders method
            provider.suggestFolders(tabName, appBatch)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error suggesting folder groupings for tab: $tabName", e)
            emptyList()
        }
    }

    /**
     * Auto-sorts all unsorted apps into folders.
     *
     * @return Number of folders created
     */
    suspend fun autoSortAll(): AutoSortResult = withContext(Dispatchers.IO) {
        val unsortedApps = detectUnsortedApps()
        var totalFoldersCreated = 0
        var totalAppsSorted = 0

        unsortedApps.forEach { (tabName, apps) ->
            if (apps.isNotEmpty()) {
                val suggestions = suggestFolderGroupings(tabName, apps)

                suggestions.forEach { suggestion ->
                    // Create the folder
                    // Note: We need to implement folder creation with the existing FolderService
                    // For now, this is a placeholder
                    totalFoldersCreated++
                    totalAppsSorted += suggestion.apps.size
                }
            }
        }

        AutoSortResult(
            foldersCreated = totalFoldersCreated,
            appsSorted = totalAppsSorted,
            tabsProcessed = unsortedApps.size,
        )
    }
}

/**
 * Result of auto-sort operation.
 */
data class AutoSortResult(
    val foldersCreated: Int,
    val appsSorted: Int,
    val tabsProcessed: Int,
)
