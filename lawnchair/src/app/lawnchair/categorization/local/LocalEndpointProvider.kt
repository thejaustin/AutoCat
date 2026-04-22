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
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * LLM provider for local OpenAI-compatible REST servers (Ollama, LM Studio, koboldcpp).
 *
 * Speaks the /v1/chat/completions endpoint. Ollama also exposes this at the same path.
 *
 * [preferredModelId] pins the model name; blank means "use first available from /v1/models".
 *
 * Batch requests run up to [PARALLEL_BATCH_LIMIT] concurrent coroutines since the
 * server (not this client) manages threading.
 */
class LocalEndpointProvider(
    private val context: Context,
    private val baseUrl: String,
    private val preferredModelId: String = "",
) : LLMProvider {

    override val name: String = "Local Server"

    override val requiresApiKey: Boolean = false

    private val learner by lazy {
        val dao = TabDatabase.getInstance(context).tabDao()
        UserCorrectionLearner.getInstance(context, dao)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Cached ping result — re-checked every 30 s
    private var lastPingMs: Long = 0L
    private var lastPingResult: Boolean = false

    // Cached model id — refreshed when null
    private var resolvedModelId: String? = null

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastPingMs < 30_000L) return@withContext lastPingResult
        val reachable = DeviceCapabilityChecker.isLocalServerReachable(baseUrl)
        lastPingMs = now
        lastPingResult = reachable
        reachable
    }

    override suspend fun getCurrentModel(): ModelInfo? = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext null
        val modelId = resolveModelId() ?: return@withContext null
        ModelInfo(
            id = modelId,
            displayName = modelId,
            provider = "local_endpoint",
            costTier = ModelInfo.CostTier.FREE,
            contextWindow = 4096,
            speedTier = ModelInfo.SpeedTier.FAST,
            qualityTier = ModelInfo.QualityTier.HIGH,
        )
    }

    override suspend fun testConnection(): TestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        return@withContext try {
            val modelId = resolveModelId()
                ?: return@withContext TestResult(false, "No models found at $baseUrl", latencyMs = System.currentTimeMillis() - start)
            val latency = System.currentTimeMillis() - start
            TestResult(success = true, message = "Connected", latencyMs = latency, modelVersion = modelId)
        } catch (e: Exception) {
            TestResult(false, "Failed: ${e.message}", latencyMs = System.currentTimeMillis() - start, error = e)
        }
    }

    override suspend fun categorizeApp(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableTabs: List<String>,
        hints: String,
    ): CategorizationResult = withContext(Dispatchers.IO) {
        val modelId = resolveModelId() ?: throw LLMException("No model available at $baseUrl")
        val effectiveHints = hints.ifBlank { learner.generateLLMHintText() }
        val (system, user) = buildMessages(appName, appPackage, appDescription, availableTabs, effectiveHints)
        val raw = callChatCompletions(modelId, system, user)
        parseResponse(raw, availableTabs)
    }

    override suspend fun categorizeAppBatch(
        apps: List<AppBatchInfo>,
        availableTabs: List<String>,
        hints: String,
    ): Map<String, CategorizationResult> = coroutineScope {
        val effectiveHints = hints.ifBlank { withContext(Dispatchers.IO) { learner.generateLLMHintText() } }
        apps.chunked(PARALLEL_BATCH_LIMIT).flatMap { chunk ->
            chunk.map { app ->
                async(Dispatchers.IO) {
                    try {
                        app.packageName to categorizeApp(
                            appName = app.appName,
                            appPackage = app.packageName,
                            appDescription = app.appDescription,
                            availableTabs = availableTabs,
                            hints = effectiveHints,
                        )
                    } catch (e: Exception) {
                        LLMLogger.logError(name, "CATEGORIZE_BATCH", e, mapOf("package" to app.packageName))
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }.toMap()
    }

    override suspend fun suggestCategories(
        installedApps: List<String>,
        existingTabs: List<String>,
        maxSuggestions: Int,
    ): List<SuggestedCategory> = withContext(Dispatchers.IO) {
        val modelId = resolveModelId() ?: throw LLMException("No model available at $baseUrl")
        val appsSample = installedApps.take(40).joinToString(", ")
        val existing = existingTabs.joinToString(", ")
        val system = "You are a category naming assistant. Reply only with comma-separated category names, nothing else."
        val user = "Android apps: $appsSample\nExisting categories: $existing\n" +
            "Suggest $maxSuggestions new category names:"
        val raw = callChatCompletions(modelId, system, user)
        raw.split(",").mapIndexedNotNull { i, s ->
            val name = s.trim().take(30)
            if (name.isBlank()) null else SuggestedCategory(name, "", emptyList(), 0.65f - (i * 0.05f))
        }.take(maxSuggestions)
    }

    override suspend fun suggestFolders(
        tabName: String,
        apps: List<AppBatchInfo>,
    ): List<SuggestedFolder> = withContext(Dispatchers.IO) {
        if (apps.isEmpty()) return@withContext emptyList()
        val modelId = resolveModelId() ?: return@withContext emptyList()
        try {
            val appList = apps.take(20).joinToString(", ") { it.appName }
            val system = "You are a folder naming assistant. Reply only with folder names separated by commas, nothing else."
            val user = "Apps in '$tabName': $appList\nSuggest 2-3 folder names to organize them into subgroups:"
            val raw = callChatCompletions(modelId, system, user)
            val folderNames = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
            folderNames.map { folderName ->
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

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Returns the model ID to use: [preferredModelId] if set, otherwise the first
     * model from /v1/models. Result is cached for the lifetime of this instance.
     */
    private fun resolveModelId(): String? {
        if (preferredModelId.isNotBlank()) return preferredModelId
        resolvedModelId?.let { return it }
        return fetchFirstModelId().also { resolvedModelId = it }
    }

    private fun fetchFirstModelId(): String? {
        return try {
            val url = baseUrl.trimEnd('/') + "/v1/models"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                json.optJSONArray("data")?.optJSONObject(0)?.optString("id")
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun callChatCompletions(modelId: String, systemMessage: String, userMessage: String): String {
        val url = baseUrl.trimEnd('/') + "/v1/chat/completions"
        val messages = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", systemMessage)
                },
            )
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                },
            )
        }
        val body = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("max_tokens", 32)
            put("temperature", 0.0)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder().url(url).post(body).build()
        val responseText = httpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw LLMException("HTTP ${resp.code} from local endpoint")
            resp.body?.string() ?: throw LLMException("Empty response from local endpoint")
        }

        val json = JSONObject(responseText)
        return json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?: throw LLMException("Unexpected response format from local endpoint")
    }

    private fun buildMessages(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableTabs: List<String>,
        hints: String,
    ): Pair<String, String> {
        val categories = availableTabs.joinToString(", ")
        val system = "You are an Android app categorizer. " +
            "Given an app, reply with exactly one of the provided category names and nothing else."
        val desc = if (appDescription.isNullOrBlank()) "" else "\nDescription: $appDescription"
        val hintsLine = if (hints.isBlank()) "" else "\nContext: $hints"
        val user = "App: $appName ($appPackage)$desc\nCategories: $categories$hintsLine"
        return system to user
    }

    private fun parseResponse(raw: String, availableTabs: List<String>): CategorizationResult {
        val cleaned = raw.trim().lines().firstOrNull()?.trim() ?: ""
        val matched = availableTabs.firstOrNull { it.equals(cleaned, ignoreCase = true) }
            ?: availableTabs.firstOrNull { cleaned.startsWith(it, ignoreCase = true) }
            ?: availableTabs.firstOrNull { cleaned.contains(it, ignoreCase = true) }
        return CategorizationResult(
            tabName = matched ?: availableTabs.firstOrNull() ?: "Other",
            confidence = if (matched != null) 0.80f else 0.3f,
            reasoning = "Local endpoint inference",
        )
    }

    companion object {
        private const val PARALLEL_BATCH_LIMIT = 4
    }
}
