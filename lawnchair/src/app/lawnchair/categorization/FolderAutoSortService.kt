package app.lawnchair.categorization

import android.content.Context
import android.content.pm.LauncherApps
import app.lawnchair.categorization.llm.AppBatchInfo
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.SuggestedFolder
import app.lawnchair.data.Converters
import app.lawnchair.data.folder.FolderInfoEntity
import app.lawnchair.data.folder.FolderItemEntity
import app.lawnchair.data.folder.service.FolderDao
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.AppFilter
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.pm.UserCache
import com.android.launcher3.util.ComponentKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for automatically sorting apps with tab assignments into folders.
 *
 * This is designed for apps that have tab metadata (e.g., from Smart Launcher import)
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

    private val tabDatabase by lazy { TabDatabase.getInstance(context) }
    private val tabDao by lazy { tabDatabase.categoryDao() }
    private val folderService by lazy { FolderService.INSTANCE.get(context) }
    private val launcherApps by lazy { context.getSystemService(LauncherApps::class.java) }
    private val userCache by lazy { UserCache.INSTANCE.get(context) }
    private val appFilter by lazy { AppFilter(context) }
    private val converters = Converters()

    // LLM providers for folder suggestions
    private val googleProvider by lazy { GoogleAIProvider(context) }
    private val providers by lazy {
        mapOf(
            "google_ai" to googleProvider,
        )
    }

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
                folder.getContents().forEach { item ->
                    if (item is AppInfo) {
                        val componentKey = converters.fromComponentKey(item.toComponentKey())
                        if (componentKey != null) {
                            appsInFolders.add(componentKey)
                        }
                    }
                }
            }

            // Build map of all installed apps
            val allInstalledApps = launcherApps?.let { service ->
                userCache.userProfiles.flatMap { userHandle ->
                    service.getActivityList(null, userHandle)
                        .filter { appFilter.shouldShowApp(it.componentName) }
                        .map { AppInfo(context, it, userHandle) }
                }
            } ?: emptyList()

            val appsByPackage = allInstalledApps
                .groupBy { it.componentName?.packageName }
                .filterKeys { it != null }

            // Group unsorted apps by tab
            val unsortedByTab = mutableMapOf<String, MutableList<AppInfo>>()

            appsWithTabs.forEach { appTab ->
                // Get AppInfo for this package
                val appsForPackage = appsByPackage[appTab.packageName] ?: emptyList()

                appsForPackage.forEach { appInfo ->
                    val componentKey = converters.fromComponentKey(appInfo.toComponentKey())

                    // Check if app is NOT in any folder
                    if (componentKey != null && !appsInFolders.contains(componentKey)) {
                        val tabName = appTab.tabName
                        unsortedByTab.getOrPut(tabName) { mutableListOf() }.add(appInfo)
                    }
                }
            }

            android.util.Log.d(TAG, "Found ${unsortedByTab.values.sumOf { it.size }} unsorted apps across ${unsortedByTab.size} tab names")

            unsortedByTab
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error detecting unsorted apps", e)
            emptyMap()
        }
    }

    /**
     * Suggests folder groupings for apps within a tab using LLM.
     *
     * @param tabName The tab name
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
            val appBatch = apps.mapNotNull { app ->
                app.componentName?.let { componentName ->
                    AppBatchInfo(
                        packageName = componentName.packageName,
                        appName = app.title.toString(),
                        appDescription = null, // We don't have descriptions for launcher apps yet
                    )
                }
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
                android.util.Log.d(TAG, "Processing ${apps.size} unsorted apps in '$tabName' tab")

                val suggestions = suggestFolderGroupings(tabName, apps)

                android.util.Log.d(TAG, "Got ${suggestions.size} folder suggestions for '$tabName' tab")

                suggestions.forEach { suggestion ->
                    try {
                        // Convert package names to AppInfo objects
                        val appInfos = getAppInfosFromPackageNames(suggestion.packageNames, apps)

                        if (appInfos.size >= 2) {
                            // Only create folder if we have at least 2 apps
                            createFolder(suggestion.name, appInfos)

                            totalFoldersCreated++
                            totalAppsSorted += appInfos.size

                            android.util.Log.d(
                                TAG,
                                "Created folder '${suggestion.name}' with ${appInfos.size} apps",
                            )
                        } else {
                            android.util.Log.w(
                                TAG,
                                "Skipping folder '${suggestion.name}' - only ${appInfos.size} apps found",
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error creating folder '${suggestion.name}'", e)
                    }
                }
            }
        }

        android.util.Log.d(
            TAG,
            "Auto-sort complete: $totalFoldersCreated folders created, $totalAppsSorted apps sorted",
        )

        AutoSortResult(
            foldersCreated = totalFoldersCreated,
            appsSorted = totalAppsSorted,
            tabsProcessed = unsortedApps.size,
        )
    }

    /**
     * Gets AppInfo objects from package names.
     */
    private fun getAppInfosFromPackageNames(
        packageNames: List<String>,
        availableApps: List<AppInfo>,
    ): List<AppInfo> {
        val appsByPackage = availableApps
            .filter { it.componentName != null }
            .groupBy { it.componentName!!.packageName }
        return packageNames.flatMap { appsByPackage[it] ?: emptyList() }
    }

    /**
     * Creates a folder with the given apps.
     */
    private suspend fun createFolder(
        folderName: String,
        apps: List<AppInfo>,
    ) {
        // Use 0 for the folder ID - Room will auto-generate a new ID
        folderService.updateFolderWithItems(
            folderInfoId = 0,
            title = folderName,
            appInfos = apps,
            icon = null,
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
