package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.llm.AppBatchInfo
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.LLMUtils
import app.lawnchair.categorization.local.DeviceCapabilityChecker
import app.lawnchair.categorization.local.LocalEndpointProvider
import app.lawnchair.categorization.local.LocalModelRegistry
import app.lawnchair.categorization.local.LocalModelType
import app.lawnchair.categorization.local.MediaPipeLLMProvider
import app.lawnchair.categorization.stages.BuiltInCategorizer
import app.lawnchair.categorization.stages.LLMCategorizer
import app.lawnchair.categorization.stages.MLCategorizer
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences.PreferenceManager
import io.sentry.Sentry
import java.io.File
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
    private val mlCategorizer by lazy { MLCategorizer(context, categoryDao) }
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
            val prefManager = PreferenceManager.getInstance(context)

            // Check device state constraints for LLM
            val isWifiRequired = prefManager.llmOnlyOnWifi.get()
            val isChargingRequired = prefManager.llmOnlyWhileCharging.get()
            val isWifiConnected = LLMUtils.isConnectedToWifi(context)
            val isCharging = LLMUtils.isCharging(context)

            val canRunLLM = (!isWifiRequired || isWifiConnected) && (!isChargingRequired || isCharging)

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

            // Stage 0: Local ML categorizer (FAST, OFFLINE INITIAL PASS)
            android.util.Log.d(TAG, "Starting Stage 0 (Local ML - Keyword/TFLite)")
            mlCategorizer.categorizeBatch(uncategorizedApps)

            // Stage 0.5: Local LLM providers (on-device inference / local server)
            val useLocalModel = prefManager.llmUseLocalModel.get()
            if (useLocalModel) {
                val categoriesAfterML = categoryDao.getAllAppTabs().associateBy { it.packageName }
                val uncategorizedForLocal = uncategorizedApps.filter { !categoriesAfterML.containsKey(it.packageName) }
                if (uncategorizedForLocal.isNotEmpty()) {
                    android.util.Log.d(TAG, "Starting Stage 0.5 (Local LLM) for ${uncategorizedForLocal.size} apps")
                    buildLocalProviders(prefManager).forEach { provider ->
                        val nowCategorized = categoryDao.getAllAppTabs().associateBy { it.packageName }
                        val stillUncategorized = uncategorizedForLocal.filter { !nowCategorized.containsKey(it.packageName) }
                        if (stillUncategorized.isNotEmpty()) {
                            runLocalProviderStage(provider, stillUncategorized)
                        }
                    }
                }
            }

            // Stage 1: LLM categorizer for custom categories (PRIORITY)
            // Only run if custom categories exist and constraints are met
            val categoriesAfterLocal = categoryDao.getAllAppTabs().associateBy { it.packageName }
            val uncategorizedForLLM = uncategorizedApps.filter { app ->
                !categoriesAfterLocal.containsKey(app.packageName)
            }

            val customCategories = categoryDao.getVisibleCustomTabs()
            val llmCount = if (customCategories.isNotEmpty() && canRunLLM && uncategorizedForLLM.isNotEmpty()) {
                llmCategorizer.categorizeBatch(uncategorizedForLLM)
            } else {
                if (customCategories.isNotEmpty() && !canRunLLM) {
                    android.util.Log.i(TAG, "LLM categorization postponed due to device state constraints (Wi-Fi/Charging)")
                }
                0
            }

            android.util.Log.d(
                TAG,
                "Stage 1 (LLM) complete: $llmCount/${uncategorizedForLLM.size} apps categorized",
            )

            // Stage 2: On-device ML categorizer (fallback for Stage 1, or main for non-Local users)
            val categoriesAfterLLM = categoryDao.getAllAppTabs().associateBy { it.packageName }
            val uncategorizedAfterLLM = uncategorizedApps.filter { app ->
                !categoriesAfterLLM.containsKey(app.packageName)
            }

            if (uncategorizedAfterLLM.isNotEmpty() && !useLocalModel) {
                android.util.Log.d(
                    TAG,
                    "Starting Stage 2 (ML) for ${uncategorizedAfterLLM.size} remaining apps",
                )

                val mlCount = mlCategorizer.categorizeBatch(uncategorizedAfterLLM)

                android.util.Log.d(
                    TAG,
                    "Stage 2 (ML) complete: $mlCount/${uncategorizedAfterLLM.size} apps categorized",
                )
            }

            // Stage 3: Built-in categorizer for remaining apps (FALLBACK)
            val categoriesAfterML = categoryDao.getAllAppTabs().associateBy { it.packageName }
            val stillUncategorized = uncategorizedApps.filter { app ->
                !categoriesAfterML.containsKey(app.packageName)
            }

            if (stillUncategorized.isNotEmpty()) {
                android.util.Log.d(
                    TAG,
                    "Starting Stage 3 (Built-in) for ${stillUncategorized.size} remaining uncategorized apps",
                )

                val builtInCount = builtInCategorizer.categorizeBatch(stillUncategorized)

                android.util.Log.d(
                    TAG,
                    "Stage 3 (Built-in) complete: $builtInCount/${stillUncategorized.size} apps categorized",
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
            if (Sentry.isEnabled()) {
                Sentry.captureException(e)
            }
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
                val prefManager = PreferenceManager.getInstance(context)

                // Check device state constraints for LLM
                val isWifiRequired = prefManager.llmOnlyOnWifi.get()
                val isChargingRequired = prefManager.llmOnlyWhileCharging.get()
                val isWifiConnected = LLMUtils.isConnectedToWifi(context)
                val isCharging = LLMUtils.isCharging(context)

                val canRunLLM = (!isWifiRequired || isWifiConnected) && (!isChargingRequired || isCharging)

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

                // Stage 0: Local ML (FAST, OFFLINE INITIAL PASS)
                _progress.update {
                    it.copy(currentStage = "Local ML", totalCount = apps.size)
                }
                android.util.Log.d(TAG, "Starting Stage 0 (Local ML - Keyword/TFLite)")
                mlCategorizer.categorizeBatch(apps)

                // Stage 0.5: Local LLM providers (on-device inference / local server)
                val useLocalModel = prefManager.llmUseLocalModel.get()
                if (useLocalModel) {
                    val categoriesAfterML = categoryDao.getAllAppTabs().associateBy { it.packageName }
                    val uncategorizedForLocal = apps.filter { !categoriesAfterML.containsKey(it.packageName) }
                    if (uncategorizedForLocal.isNotEmpty()) {
                        _progress.update {
                            it.copy(currentStage = "Local LLM", totalCount = apps.size, processedCount = apps.size - uncategorizedForLocal.size)
                        }
                        android.util.Log.d(TAG, "Starting Stage 0.5 (Local LLM) for ${uncategorizedForLocal.size} apps")
                        buildLocalProviders(prefManager).forEach { provider ->
                            val nowCategorized = categoryDao.getAllAppTabs().associateBy { it.packageName }
                            val stillUncategorized = uncategorizedForLocal.filter { !nowCategorized.containsKey(it.packageName) }
                            if (stillUncategorized.isNotEmpty()) {
                                runLocalProviderStage(provider, stillUncategorized)
                            }
                        }
                    }
                }

                // Stage 1: LLM categorizer for custom categories (PRIORITY)
                val categoriesAfterLocal = categoryDao.getAllAppTabs().associateBy { it.packageName }
                val uncategorizedForLLM = apps.filter { app ->
                    !categoriesAfterLocal.containsKey(app.packageName)
                }

                _progress.update {
                    CategorizationProgress(
                        isRunning = true,
                        currentStage = if (canRunLLM && uncategorizedForLLM.isNotEmpty()) "Cloud AI" else "Built-in",
                        processedCount = apps.size - uncategorizedForLLM.size,
                        totalCount = apps.size,
                    )
                }

                if (canRunLLM && uncategorizedForLLM.isNotEmpty()) {
                    android.util.Log.d(
                        TAG,
                        "Starting Stage 1 (LLM) for ${uncategorizedForLLM.size} apps",
                    )

                    llmCategorizer.categorizeBatch(uncategorizedForLLM) { progress ->
                        _progress.value = progress
                    }
                } else if (uncategorizedForLLM.isNotEmpty() && !canRunLLM) {
                    android.util.Log.i(TAG, "Cloud AI categorization postponed due to constraints")
                }

                // Stage 2: On-device ML categorizer (fallback)
                val categoriesAfterLLM = categoryDao.getAllAppTabs().associateBy { it.packageName }
                val uncategorizedAfterLLM = apps.filter { app ->
                    !categoriesAfterLLM.containsKey(app.packageName)
                }

                if (uncategorizedAfterLLM.isNotEmpty()) {
                    _progress.update {
                        CategorizationProgress(
                            isRunning = true,
                            currentStage = "On-device ML",
                            processedCount = apps.size - uncategorizedAfterLLM.size,
                            totalCount = apps.size,
                        )
                    }

                    android.util.Log.d(
                        TAG,
                        "Starting Stage 2 (ML) for ${uncategorizedAfterLLM.size} uncategorized apps",
                    )

                    mlCategorizer.categorizeBatch(uncategorizedAfterLLM)
                }

                // Stage 3: Built-in categorizer for remaining apps (FALLBACK)
                val categoriesAfterML = categoryDao.getAllAppTabs().associateBy { it.packageName }
                val stillUncategorized = apps.filter { app ->
                    !categoriesAfterML.containsKey(app.packageName)
                }

                if (stillUncategorized.isNotEmpty()) {
                    _progress.update {
                        CategorizationProgress(
                            isRunning = true,
                            currentStage = "Built-in",
                            processedCount = apps.size - stillUncategorized.size,
                            totalCount = apps.size,
                        )
                    }

                    android.util.Log.d(
                        TAG,
                        "Starting Stage 3 (Built-in) for ${stillUncategorized.size} uncategorized apps",
                    )

                    builtInCategorizer.categorizeBatch(stillUncategorized)
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
                if (Sentry.isEnabled()) {
                    Sentry.captureException(e)
                }
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
            if (Sentry.isEnabled()) {
                Sentry.captureException(e)
            }
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

    /**
     * Builds the ordered list of local LLM providers based on current preferences.
     *
     * Priority: local endpoint (if enabled + reachable) → MediaPipe/AICore model
     */
    private suspend fun buildLocalProviders(prefs: PreferenceManager): List<LLMProvider> {
        val providers = mutableListOf<LLMProvider>()

        val endpointUrl = prefs.localEndpointUrl.get()
        if (prefs.localEndpointEnabled.get() && endpointUrl.isNotBlank()) {
            providers += LocalEndpointProvider(context, endpointUrl, prefs.localEndpointModelId.get())
        }

        val modelPath = resolveLocalModelPath(prefs)
        if (modelPath != null) {
            val requiresAiCore = modelPath.isEmpty()
            providers += MediaPipeLLMProvider(context, modelPath, requiresAiCore)
        }

        return providers
    }

    /**
     * Resolves the path to the local MediaPipe model, or empty string for AICore.
     * Returns null if no usable model is configured.
     */
    private fun resolveLocalModelPath(prefs: PreferenceManager): String? {
        // 1. User-provided custom path
        val customPath = prefs.localCustomModelPath.get()
        if (customPath.isNotBlank() && File(customPath).exists()) return customPath

        // 2. Downloaded model matching the selected model id
        val selectedId = prefs.selectedLocalModelId.get()
        if (selectedId.isNotBlank()) {
            val model = LocalModelRegistry.ALL_MODELS.firstOrNull { it.id == selectedId }
            if (model != null) {
                when (model.type) {
                    LocalModelType.AICORE -> return ""

                    // empty path = AICore
                    LocalModelType.MEDIAPIPE -> {
                        val downloaded = File(context.filesDir, "local_models/${model.id}.bin")
                        if (downloaded.exists()) return downloaded.absolutePath
                    }

                    else -> Unit
                }
            }
        }

        // 3. Auto-select best compatible downloaded model
        val caps = DeviceCapabilityChecker.getCapabilities(context)
        val recommended = LocalModelRegistry.getRecommendedModel(caps) ?: return null
        return when (recommended.type) {
            LocalModelType.AICORE -> ""

            LocalModelType.MEDIAPIPE -> {
                val f = File(context.filesDir, "local_models/${recommended.id}.bin")
                if (f.exists()) f.absolutePath else null
            }

            else -> null
        }
    }

    /**
     * Runs a local LLM provider against a list of uncategorized apps.
     * Returns the number of apps successfully categorized.
     */
    private suspend fun runLocalProviderStage(
        provider: LLMProvider,
        apps: List<app.lawnchair.data.apps.AppInfo>,
    ): Int {
        return try {
            if (!provider.isAvailable()) return 0
            val tabs = categoryDao.getVisibleCustomTabs().map { it.name }
            if (tabs.isEmpty()) return 0
            val batchInfos = apps.map { AppBatchInfo(it.packageName, it.appName, null) }
            val results = provider.categorizeAppBatch(batchInfos, tabs)
            var count = 0
            results.forEach { (pkg, result) ->
                try {
                    categoryDao.insertAppTab(
                        app.lawnchair.data.tab.entities.AppTab(
                            packageName = pkg,
                            tabName = result.tabName,
                            source = app.lawnchair.data.tab.entities.AppTab.SOURCE_LLM,
                            confidence = result.confidence,
                        ),
                    )
                    count++
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Failed to insert local LLM result for $pkg", e)
                }
            }
            count
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Local LLM stage failed for provider ${provider.name}", e)
            if (Sentry.isEnabled()) Sentry.captureException(e)
            0
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
