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
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * LLM provider that runs inference on-device via MediaPipe Tasks GenAI.
 *
 * Supports two backends:
 * - MediaPipe model file (.bin): placed in context.filesDir/local_models/
 * - AICore (Gemini Nano): when requiresAiCore=true, MediaPipe delegates automatically
 *
 * Thread-safe via [inferenceMutex] — MediaPipe LlmInference must be called sequentially.
 */
class MediaPipeLLMProvider(
    private val context: Context,
    private val modelPath: String,
    private val requiresAiCore: Boolean = false,
) : LLMProvider {

    override val name: String = if (requiresAiCore) "Gemini Nano (AICore)" else "Local AI (MediaPipe)"

    override val requiresApiKey: Boolean = false

    private var inference: LlmInference? = null

    /** Guards all LlmInference access — MediaPipe is not thread-safe. */
    private val inferenceMutex = Mutex()

    private val learner by lazy {
        val dao = TabDatabase.getInstance(context).tabDao()
        UserCorrectionLearner.getInstance(context, dao)
    }

    private suspend fun getOrCreateInference(): LlmInference {
        inference?.let { return it }
        val numThreads = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        val options = LlmInference.LlmInferenceOptions.builder()
            .apply {
                if (requiresAiCore) {
                    setModelPath("")
                } else {
                    setModelPath(modelPath)
                }
                setMaxTokens(256)
                setNumThreads(numThreads)
            }
            .build()
        return LlmInference.createFromOptions(context, options).also { inference = it }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        when {
            requiresAiCore -> DeviceCapabilityChecker.probeAiCoreAvailable()
            else -> File(modelPath).exists()
        }
    }

    override suspend fun getCurrentModel(): ModelInfo? {
        if (!isAvailable()) return null
        return ModelInfo(
            id = if (requiresAiCore) "aicore_gemini_nano" else File(modelPath).name,
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
            inferenceMutex.withLock {
                val engine = getOrCreateInference()
                engine.generateResponse("Reply with: OK")
            }
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
        hints: String,
    ): CategorizationResult = withContext(Dispatchers.IO) {
        try {
            val effectiveHints = hints.ifBlank { learner.generateLLMHintText() }
            val prompt = buildPrompt(appName, appPackage, appDescription, availableTabs, effectiveHints)
            val raw = inferenceMutex.withLock {
                getOrCreateInference().generateResponse(prompt)
            }
            parseResponse(raw, availableTabs)
        } catch (e: Exception) {
            LLMLogger.logError(name, "CATEGORIZE_APP", e, mapOf("package" to appPackage))
            throw LLMException("MediaPipe categorization failed: ${e.message}", e)
        }
    }

    override suspend fun categorizeAppBatch(
        apps: List<AppBatchInfo>,
        availableTabs: List<String>,
        hints: String,
    ): Map<String, CategorizationResult> = withContext(Dispatchers.IO) {
        val effectiveHints = hints.ifBlank { learner.generateLLMHintText() }
        val results = mutableMapOf<String, CategorizationResult>()
        apps.forEach { app ->
            try {
                results[app.packageName] = categorizeApp(
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
            val raw = inferenceMutex.withLock {
                getOrCreateInference().generateResponse(prompt)
            }
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
            throw LLMException("MediaPipe suggestion failed: ${e.message}", e)
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
            val raw = inferenceMutex.withLock {
                getOrCreateInference().generateResponse(prompt)
            }
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

    fun release() {
        inference?.close()
        inference = null
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
        private const val TAG = "MediaPipeLLMProvider"
    }
}
