package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.stages.BuiltInCategorizer
import app.lawnchair.categorization.stages.LLMCategorizer
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDatabase
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Represents the current progress of categorization.
 *
 * @property isRunning Whether categorization is currently in progress
 * @property currentStage The current categorization stage (e.g., "Built-in", "LLM", "Complete")
 * @property processedCount Number of apps processed so far
 * @property totalCount Total number of apps to process
 * @property currentAppName Name of the app currently being processed (optional)
 * @property currentBatch Current batch being processed (for batch mode)
 * @property totalBatches Total number of batches (for batch mode)
 * @property batchSize Size of each batch (for batch mode)
 * @property currentProvider Name of the LLM provider currently being used
 * @property estimatedTimeMs Estimated time remaining in milliseconds
 */
data class CategorizationProgress(
    val isRunning: Boolean = false,
    val currentStage: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentAppName: String? = null,
    val currentBatch: Int = 0,
    val totalBatches: Int = 0,
    val batchSize: Int = 0,
    val currentProvider: String? = null,
    val estimatedTimeMs: Long = 0,
) {
    val progressPercentage: Float
        get() = if (totalCount > 0) (processedCount.toFloat() / totalCount.toFloat()) else 0f

    val batchProgressText: String?
        get() = if (totalBatches > 0) "Batch $currentBatch/$totalBatches" else null
}

/**
 * Manages the app categorization pipeline.
 *
 * Coordinates categorization stages, database initialization, and
 * provides a clean interface for triggering categorization.
 *
 * Currently implements Stage 1 (Built-in categorizer).
 * Future: Rule-based, ML, and user override stages.
 */
class CategorizationManager(private val context: Context) {

    private val database by lazy { TabDatabase.getInstance(context) }
    private val categoryDao by lazy { database.tabDao() }
    private val metadataProvider by lazy { AppMetadataProvider(context) }
    private val builtInCategorizer by lazy { BuiltInCategorizer(categoryDao) }
    private val llmCategorizer by lazy { LLMCategorizer(context, categoryDao) }
    private val appProvider by lazy { AutoCatAppProvider.getInstance(context) }
    private val folderSyncService by lazy { CategoryFolderSyncService(context) }

    // Guard against concurrent categorization runs
    private val isCategorizationRunning = AtomicBoolean(false)

    // Progress tracking
    private val _progress = MutableStateFlow(CategorizationProgress())
    val progress: StateFlow<CategorizationProgress> = _progress.asStateFlow()

    /**
     * Initializes categorization system on first run.
     *
     * - Ensures default categories exist
     * - Categorizes all installed apps using multi-stage pipeline:
     *   1. LLM categorizer (for custom categories - higher priority)
     *   2. Built-in categorizer (Android system categories - fallback)
     *
     * Safe to call multiple times (idempotent).
     */
    suspend fun initializeCategorization() = withContext(Dispatchers.IO) {
        try {
            // Ensure default tabs are initialized
            categoryDao.initializeDefaultTabsIfNeeded()

            // Get all installed apps
            val apps = metadataProvider.getInstalledApps()

            // Fetch all categories in one query to avoid N+1 problem
            val allCategories = categoryDao.getAllAppTabs().associateBy { it.packageName }
            val uncategorizedApps = apps.filter { app ->
                !allCategories.containsKey(app.packageName)
            }

            android.util.Log.d(
                TAG,
                "Initialization: ${apps.size} total apps, ${uncategorizedApps.size} uncategorized",
            )

            // Skip if all apps are already categorized
            if (uncategorizedApps.isEmpty()) {
                android.util.Log.d(TAG, "All apps already categorized, skipping initialization")
                return@withContext
            }

            // Stage 1: LLM categorizer for custom categories (PRIORITY)
            // Only run if custom categories exist
            val customCategories = categoryDao.getVisibleCustomTabs()
            val llmCount = if (customCategories.isNotEmpty()) {
                llmCategorizer.categorizeBatch(uncategorizedApps)
            } else {
                android.util.Log.d(TAG, "No custom categories, skipping LLM categorization")
                0
            }

            android.util.Log.d(
                TAG,
                "Stage 1 (LLM) complete: $llmCount/${uncategorizedApps.size} apps categorized",
            )

            // Stage 2: Built-in categorizer for remaining apps (FALLBACK)
            // Re-fetch categories to see which apps are still uncategorized after LLM
            val categoriesAfterLLM = categoryDao.getAllAppTabs().associateBy { it.packageName }
            val stillUncategorized = uncategorizedApps.filter { app ->
                !categoriesAfterLLM.containsKey(app.packageName)
            }

            if (stillUncategorized.isNotEmpty()) {
                android.util.Log.d(
                    TAG,
                    "Starting Stage 2 (Built-in) for ${stillUncategorized.size} remaining uncategorized apps",
                )

                val builtInCount = builtInCategorizer.categorizeBatch(stillUncategorized)

                android.util.Log.d(
                    TAG,
                    "Stage 2 (Built-in) complete: $builtInCount/${stillUncategorized.size} apps categorized",
                )
            }

            android.util.Log.d(
                TAG,
                "All stages complete: ${categoryDao.getAllAppTabs().size}/${apps.size} apps categorized",
            )

            // Refresh cache after categorization
            appProvider.refreshCache()

            // Sync to folders if enabled
            if (folderSyncService.isSyncEnabled()) {
                android.util.Log.d(TAG, "Starting folder sync")

                val allCats = categoryDao.getAllAppTabs()
                val categorizations = allCats.associate { it.packageName to it.tabName }

                val syncResult = folderSyncService.syncCategoriesToFolders(categorizations)

                android.util.Log.d(TAG, "Folder sync complete: ${syncResult.message}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during categorization", e)
        }
    }

    /**
     * Re-categorizes all apps (useful after installing/updating apps).
     *
     * Only re-categorizes apps that don't have user overrides.
     * Emits progress updates via the progress StateFlow.
     *
     * Returns false if categorization is already running.
     */
    suspend fun recategorizeAll(): Boolean {
        // Prevent concurrent execution
        if (!isCategorizationRunning.compareAndSet(false, true)) {
            android.util.Log.w(TAG, "Categorization already running, ignoring request")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                _progress.update {
                    CategorizationProgress(
                        isRunning = true,
                        currentStage = "Preparing",
                        processedCount = 0,
                        totalCount = 0,
                    )
                }

                // Delete non-user-override categories
                categoryDao.deleteNonUserOverrides()

                // Get all installed apps
                val apps = metadataProvider.getInstalledApps()

                _progress.update {
                    CategorizationProgress(
                        isRunning = true,
                        currentStage = "LLM",
                        processedCount = 0,
                        totalCount = apps.size,
                    )
                }

                // Stage 1: LLM categorizer for custom categories (PRIORITY)
                android.util.Log.d(
                    TAG,
                    "Starting Stage 1 (LLM) for ${apps.size} apps",
                )

                val llmCount = llmCategorizer.categorizeBatch(apps) { progress ->
                    _progress.value = progress
                }

                android.util.Log.d(
                    TAG,
                    "Stage 1 (LLM) complete: $llmCount/${apps.size} apps categorized",
                )

                // Get uncategorized apps for built-in stage (FALLBACK)
                val allCategories = categoryDao.getAllAppTabs().associateBy { it.packageName }
                val uncategorizedApps = apps.filter { app ->
                    !allCategories.containsKey(app.packageName)
                }

                if (uncategorizedApps.isNotEmpty()) {
                    _progress.update {
                        CategorizationProgress(
                            isRunning = true,
                            currentStage = "Built-in",
                            processedCount = 0,
                            totalCount = uncategorizedApps.size,
                        )
                    }

                    android.util.Log.d(
                        TAG,
                        "Starting Stage 2 (Built-in) for ${uncategorizedApps.size} uncategorized apps",
                    )

                    val builtInCount = builtInCategorizer.categorizeBatch(uncategorizedApps)

                    android.util.Log.d(
                        TAG,
                        "Stage 2 (Built-in) complete: $builtInCount/${uncategorizedApps.size} apps categorized",
                    )
                }

                android.util.Log.d(
                    TAG,
                    "All stages complete: ${categoryDao.getAllAppTabs().size}/${apps.size} apps categorized",
                )

                // Refresh cache after categorization
                appProvider.refreshCache()

                // Sync to folders if enabled
                if (folderSyncService.isSyncEnabled()) {
                    _progress.update {
                        CategorizationProgress(
                            isRunning = true,
                            currentStage = "Syncing folders",
                            processedCount = apps.size,
                            totalCount = apps.size,
                        )
                    }

                    android.util.Log.d(TAG, "Starting folder sync")

                    val allCats = categoryDao.getAllAppTabs()
                    val categorizations = allCats.associate { it.packageName to it.tabName }

                    val syncResult = folderSyncService.syncCategoriesToFolders(categorizations)

                    android.util.Log.d(TAG, "Folder sync complete: ${syncResult.message}")
                }

                // Mark as complete
                _progress.update {
                    CategorizationProgress(
                        isRunning = false,
                        currentStage = "Complete",
                        processedCount = apps.size,
                        totalCount = apps.size,
                    )
                }
                true
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error during re-categorization", e)
                _progress.update {
                    CategorizationProgress(
                        isRunning = false,
                        currentStage = "Error: ${e.message}",
                        processedCount = 0,
                        totalCount = 0,
                    )
                }
                false
            } finally {
                isCategorizationRunning.set(false)
                // Ensure running state is cleared even if unexpected error
                _progress.update { it.copy(isRunning = false) }
            }
        }
    }

    /**
     * Categorizes a single newly installed app.
     *
     * Uses multi-stage pipeline: built-in categorizer first, then LLM if needed.
     *
     * @param packageName The package name of the new app
     */
    suspend fun categorizeNewApp(packageName: String) = withContext(Dispatchers.IO) {
        try {
            val appInfo = metadataProvider.getAppInfo(packageName) ?: return@withContext

            // Stage 1: Try built-in categorizer
            var categorized = builtInCategorizer.categorize(appInfo)

            if (categorized) {
                val appTab = categoryDao.getAppTab(packageName)
                appProvider.updateCacheForApp(packageName, appTab?.tabName)
                return@withContext
            }

            // Stage 2: Try LLM categorizer
            categorized = llmCategorizer.categorize(appInfo)

            if (categorized) {
                android.util.Log.d(TAG, "Categorized new app (LLM): $packageName")
                val appTab = categoryDao.getAppTab(packageName)
                appProvider.updateCacheForApp(packageName, appTab?.tabName)
            } else {
                android.util.Log.d(TAG, "Could not categorize new app: $packageName")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error categorizing app: $packageName", e)
        }
    }

    /**
     * Resets all circuit breakers for LLM providers.
     */
    fun resetCircuitBreakers() {
        llmCategorizer.resetCircuitBreakers()
    }

    /**
     * Gets all package names assigned to a specific tab.
     *
     * @param tabName The name of the tab (null for "Other" tab)
     * @return List of package names
     */
    suspend fun getPackagesInTab(tabName: String?): List<String> = withContext(Dispatchers.IO) {
        if (tabName == null) {
            // "Other" tab: apps not in app_categories table but installed
            val categorizedPackages = categoryDao.getAllAppTabs().map { it.packageName }.toSet()
            metadataProvider.getInstalledApps()
                .map { it.packageName }
                .filter { it !in categorizedPackages }
        } else {
            categoryDao.getAppsByTab(tabName).map { it.packageName }
        }
    }

    companion object {
        private const val TAG = "CategorizationManager"

        @Volatile
        private var instance: CategorizationManager? = null

        /**
         * Gets singleton instance of CategorizationManager.
         */
        fun getInstance(context: Context): CategorizationManager {
            return instance ?: synchronized(this) {
                instance ?: CategorizationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
