package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.stages.BuiltInCategorizer
import app.lawnchair.categorization.stages.LLMCategorizer
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.category.CategoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Represents the current progress of categorization.
 *
 * @property isRunning Whether categorization is currently in progress
 * @property currentStage The current categorization stage (e.g., "Built-in", "LLM", "Complete")
 * @property processedCount Number of apps processed so far
 * @property totalCount Total number of apps to process
 * @property currentAppName Name of the app currently being processed (optional)
 */
data class CategorizationProgress(
    val isRunning: Boolean = false,
    val currentStage: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val currentAppName: String? = null,
) {
    val progressPercentage: Float
        get() = if (totalCount > 0) (processedCount.toFloat() / totalCount.toFloat()) else 0f
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

    private val database = CategoryDatabase.getInstance(context)
    private val categoryDao = database.categoryDao()
    private val metadataProvider = AppMetadataProvider(context)
    private val builtInCategorizer = BuiltInCategorizer(categoryDao)
    private val llmCategorizer = LLMCategorizer(context, categoryDao)
    private val appProvider = AutoCatAppProvider.getInstance(context)

    // Progress tracking
    private val _progress = MutableStateFlow(CategorizationProgress())
    val progress: StateFlow<CategorizationProgress> = _progress.asStateFlow()

    /**
     * Initializes categorization system on first run.
     *
     * - Ensures default categories exist
     * - Categorizes all installed apps using multi-stage pipeline:
     *   1. Built-in categorizer (Android system categories)
     *   2. LLM categorizer (for apps without built-in categories)
     *
     * Safe to call multiple times (idempotent).
     */
    suspend fun initializeCategorization() = withContext(Dispatchers.IO) {
        try {
            // Ensure default categories are initialized
            categoryDao.initializeDefaultCategoriesIfNeeded()

            // Get all installed apps
            val apps = metadataProvider.getInstalledApps()

            // Stage 1: Built-in categorizer
            val builtInCount = builtInCategorizer.categorizeBatch(apps)

            android.util.Log.d(
                TAG,
                "Stage 1 (Built-in) complete: $builtInCount/${apps.size} apps categorized",
            )

            // Stage 2: LLM categorizer for remaining apps
            val uncategorizedApps = apps.filter { app ->
                categoryDao.getAppCategory(app.packageName) == null
            }

            if (uncategorizedApps.isNotEmpty()) {
                android.util.Log.d(
                    TAG,
                    "Starting Stage 2 (LLM) for ${uncategorizedApps.size} uncategorized apps",
                )

                val llmCount = llmCategorizer.categorizeBatch(uncategorizedApps)

                android.util.Log.d(
                    TAG,
                    "Stage 2 (LLM) complete: $llmCount/${uncategorizedApps.size} apps categorized",
                )
            }

            val totalCategorized = builtInCount + (uncategorizedApps.size - uncategorizedApps.size)
            android.util.Log.d(
                TAG,
                "All stages complete: ${categoryDao.getAllAppCategories().size}/${apps.size} apps categorized",
            )

            // Refresh cache after categorization
            appProvider.refreshCache()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during categorization", e)
        }
    }

    /**
     * Re-categorizes all apps (useful after installing/updating apps).
     *
     * Only re-categorizes apps that don't have user overrides.
     * Emits progress updates via the progress StateFlow.
     */
    suspend fun recategorizeAll() = withContext(Dispatchers.IO) {
        try {
            _progress.value = CategorizationProgress(
                isRunning = true,
                currentStage = "Preparing",
                processedCount = 0,
                totalCount = 0,
            )

            // Delete non-user-override categories
            categoryDao.deleteNonUserOverrides()

            // Get all installed apps
            val apps = metadataProvider.getInstalledApps()

            _progress.value = CategorizationProgress(
                isRunning = true,
                currentStage = "Built-in",
                processedCount = 0,
                totalCount = apps.size,
            )

            // Stage 1: Built-in categorizer (fast, batch operation)
            val builtInCount = builtInCategorizer.categorizeBatch(apps)

            android.util.Log.d(
                TAG,
                "Stage 1 (Built-in) complete: $builtInCount/${apps.size} apps categorized",
            )

            // Get uncategorized apps for LLM stage
            val uncategorizedApps = apps.filter { app ->
                categoryDao.getAppCategory(app.packageName) == null
            }

            if (uncategorizedApps.isNotEmpty()) {
                _progress.value = CategorizationProgress(
                    isRunning = true,
                    currentStage = "LLM",
                    processedCount = 0,
                    totalCount = uncategorizedApps.size,
                )

                android.util.Log.d(
                    TAG,
                    "Starting Stage 2 (LLM) for ${uncategorizedApps.size} uncategorized apps",
                )

                // Stage 2: LLM categorizer with progress tracking
                var llmProcessed = 0
                for (app in uncategorizedApps) {
                    _progress.value = CategorizationProgress(
                        isRunning = true,
                        currentStage = "LLM",
                        processedCount = llmProcessed,
                        totalCount = uncategorizedApps.size,
                        currentAppName = app.label,
                    )

                    llmCategorizer.categorize(app)
                    llmProcessed++

                    // Small delay for rate limiting (handled in LLMCategorizer)
                    kotlinx.coroutines.delay(100)
                }

                android.util.Log.d(
                    TAG,
                    "Stage 2 (LLM) complete: $llmProcessed/${uncategorizedApps.size} apps processed",
                )
            }

            android.util.Log.d(
                TAG,
                "All stages complete: ${categoryDao.getAllAppCategories().size}/${apps.size} apps categorized",
            )

            // Refresh cache after categorization
            appProvider.refreshCache()

            // Mark as complete
            _progress.value = CategorizationProgress(
                isRunning = false,
                currentStage = "Complete",
                processedCount = apps.size,
                totalCount = apps.size,
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during re-categorization", e)
            _progress.value = CategorizationProgress(
                isRunning = false,
                currentStage = "Error",
                processedCount = 0,
                totalCount = 0,
            )
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
                android.util.Log.d(TAG, "Categorized new app (built-in): $packageName")
                // Update cache with the new category
                val appCategory = categoryDao.getAppCategory(packageName)
                appProvider.updateCacheForApp(packageName, appCategory?.category)
                return@withContext
            }

            // Stage 2: Try LLM categorizer
            categorized = llmCategorizer.categorize(appInfo)

            if (categorized) {
                android.util.Log.d(TAG, "Categorized new app (LLM): $packageName")
                // Update cache with the new category
                val appCategory = categoryDao.getAppCategory(packageName)
                appProvider.updateCacheForApp(packageName, appCategory?.category)
            } else {
                android.util.Log.d(TAG, "Could not categorize new app: $packageName")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error categorizing app: $packageName", e)
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
