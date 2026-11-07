package app.lawnchair.categorization.local

import android.content.Context
import app.lawnchair.categorization.learning.UserCorrectionLearner
import app.lawnchair.categorization.llm.AppBatchInfo
import app.lawnchair.categorization.llm.CategorizationResult
import app.lawnchair.categorization.llm.LLMException
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.ModelInfo
import app.lawnchair.categorization.llm.SuggestedCategory
import app.lawnchair.categorization.llm.SuggestedFolder
import app.lawnchair.categorization.llm.TestResult
import app.lawnchair.data.tab.TabDatabase
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LLM provider that runs a LiteRT-LM model (.litertlm) on the device's CPU.
 *
 * The engine is loaded on first use and closed again after [IDLE_RELEASE_MS] without
 * requests, so a model doesn't stay in memory while the launcher is idle.
 * All engine access goes through [engineMutex]; the engine is not thread-safe.
 */
class LiteRtLmProvider(
    private val context: Context,
    val modelPath: String,
) : LLMProvider {

    override val name: String = "On-device AI"

    override val requiresApiKey: Boolean = false

    private var engine: Engine? = null
    private val engineMutex = Mutex()
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var releaseJob: Job? = null

    private val learner by lazy {
        val dao = TabDatabase.getInstance(context).categoryDao()
        UserCorrectionLearner.getInstance(context, dao)
    }

    /**
     * Runs [prompt] in a fresh conversation and returns the model's text reply.
     */
    private suspend fun generate(prompt: String): String = engineMutex.withLock {
        releaseJob?.cancel()
        try {
            val activeEngine = engine ?: loadEngine().also { engine = it }
            activeEngine.createConversation().use { conversation ->
                val reply = conversation.sendMessage(prompt)
                reply.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
                    .replace(THINKING_BLOCK, "")
                    .trim()
            }
        } finally {
            scheduleRelease()
        }
    }

    private fun loadEngine(): Engine {
        val config = EngineConfig(modelPath, Backend.CPU())
        return try {
            Engine(config).apply { initialize() }
        } catch (e: UnsatisfiedLinkError) {
            // The native library only ships for arm64-v8a
            throw LLMException("On-device AI isn't supported on this device", e)
        }
    }

    private fun scheduleRelease() {
        releaseJob = releaseScope.launch {
            delay(IDLE_RELEASE_MS)
            release()
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        File(modelPath).isFile
    }

    override suspend fun getCurrentModel(): ModelInfo? {
        if (!isAvailable()) return null
        return ModelInfo(
            id = File(modelPath).name,
            displayName = name,
            provider = "local",
            costTier = ModelInfo.CostTier.FREE,
            contextWindow = 4096,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
        )
    }

    override suspend fun testConnection(): TestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        return@withContext try {
            generate("Reply with: OK")
            val latency = System.currentTimeMillis() - start
            TestResult(success = true, message = "Model loaded and inference OK", latencyMs = latency, modelVersion = name)
        } catch (e: Exception) {
            TestResult(success = false, message = "Failed: ${e.message}", latencyMs = System.currentTimeMillis() - start, error = e)
        }
    }

    override suspend fun categorizeApp(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableTabs: List<String>,
    ): CategorizationResult = categorizeWithHints(appName, appPackage, appDescription, availableTabs, hints = "")

    private suspend fun categorizeWithHints(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableTabs: List<String>,
        hints: String,
    ): CategorizationResult = withContext(Dispatchers.IO) {
        try {
            val effectiveHints = hints.ifBlank { learner.generateLLMHintText() }
            val prompt = buildPrompt(appName, appPackage, appDescription, availableTabs, effectiveHints)
            parseResponse(generate(prompt), availableTabs)
        } catch (e: Exception) {
            LLMLogger.logError(name, "CATEGORIZE_APP", e, mapOf("package" to appPackage))
            throw LLMException("On-device categorization failed: ${e.message}", e)
        }
    }

    override suspend fun categorizeAppBatch(
        apps: List<AppBatchInfo>,
        availableTabs: List<String>,
    ): Map<String, CategorizationResult> = withContext(Dispatchers.IO) {
        val effectiveHints = learner.generateLLMHintText()
        val results = mutableMapOf<String, CategorizationResult>()
        apps.forEach { app ->
            try {
                results[app.packageName] = categorizeWithHints(
                    appName = app.appName,
                    appPackage = app.packageName,
                    appDescription = app.appDescription,
                    availableTabs = availableTabs,
                    hints = effectiveHints,
                )
            } catch (e: Exception) {
                LLMLogger.logError(name, "CATEGORIZE_BATCH", e, mapOf("package" to app.packageName))
            }
        }
        results
    }

    override suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        generate(prompt)
    }

    override suspend fun suggestCategories(
        installedApps: List<String>,
        existingTabs: List<String>,
        maxSuggestions: Int,
    ): List<SuggestedCategory> = withContext(Dispatchers.IO) {
        try {
            val appsSample = installedApps.take(30).joinToString(", ")
            val existing = existingTabs.joinToString(", ")
            val prompt = "Given these Android apps: $appsSample\n" +
                "Existing categories: $existing\n" +
                "Suggest $maxSuggestions new useful category names (comma-separated, no descriptions):"
            val raw = generate(prompt)
            raw.split(",").mapIndexedNotNull { i, name ->
                val trimmed = name.trim().take(30)
                if (trimmed.isBlank()) {
                    null
                } else {
                    SuggestedCategory(
                        name = trimmed,
                        description = "",
                        exampleApps = emptyList(),
                        confidence = 0.6f - (i * 0.05f),
                    )
                }
            }.take(maxSuggestions)
        } catch (e: Exception) {
            LLMLogger.logError(name, "SUGGEST_CATEGORIES", e)
            throw LLMException("On-device suggestion failed: ${e.message}", e)
        }
    }

    override suspend fun suggestFolders(
        tabName: String,
        apps: List<AppBatchInfo>,
    ): List<SuggestedFolder> = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) return@withContext emptyList()
        try {
            val appList = apps.take(20).joinToString(", ") { it.appName }
            val prompt = "These apps are all in the '$tabName' category: $appList\n" +
                "Suggest 2-3 folder names to organize them into subgroups.\n" +
                "Reply with only folder names separated by commas, nothing else."
            val raw = generate(prompt)
            val folderNames = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
            folderNames.map { folderName ->
                // Assign apps to folders by roughly even distribution (LLM gave names only)
                val chunk = apps.take(apps.size / folderNames.size.coerceAtLeast(1))
                SuggestedFolder(
                    name = folderName,
                    description = "Apps grouped under '$tabName'",
                    packageNames = chunk.map { it.packageName },
                    confidence = 0.65f,
                )
            }
        } catch (e: Exception) {
            LLMLogger.logError(name, "SUGGEST_FOLDERS", e, mapOf("tab" to tabName))
            emptyList()
        }
    }

    /** Closes the engine and frees the model's memory. It reloads on the next request. */
    suspend fun release() {
        engineMutex.withLock {
            engine?.close()
            engine = null
        }
    }

    private fun buildPrompt(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableTabs: List<String>,
        hints: String,
    ): String {
        val categories = availableTabs.joinToString(", ")
        val desc = if (appDescription.isNullOrBlank()) "" else "\nDescription: $appDescription"
        val hintsLine = if (hints.isBlank()) "" else "\nContext: $hints"
        return "Categorize this Android app into exactly one of the given categories.\n" +
            "App: $appName ($appPackage)$desc\n" +
            "Categories: $categories$hintsLine\n" +
            "Reply with only the category name, nothing else."
    }

    private fun parseResponse(raw: String, availableTabs: List<String>): CategorizationResult {
        val cleaned = raw.trim().lines().firstOrNull()?.trim() ?: ""
        // Exact match first, then prefix/contains fallback
        val matched = availableTabs.firstOrNull { it.equals(cleaned, ignoreCase = true) }
            ?: availableTabs.firstOrNull { cleaned.startsWith(it, ignoreCase = true) }
            ?: availableTabs.firstOrNull { cleaned.contains(it, ignoreCase = true) }
        return CategorizationResult(
            tabName = matched ?: availableTabs.firstOrNull() ?: "Other",
            confidence = if (matched != null) 0.75f else 0.3f,
            reasoning = "Local inference",
        )
    }

    companion object {
        private const val TAG = "LiteRtLmProvider"
        private const val IDLE_RELEASE_MS = 60_000L

        // Reasoning models such as Qwen3 may answer with a <think> block first
        private val THINKING_BLOCK = Regex("(?s)<think>.*?</think>")
    }
}
