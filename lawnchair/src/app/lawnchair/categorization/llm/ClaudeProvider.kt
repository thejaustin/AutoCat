package app.lawnchair.categorization.llm

import android.content.Context
import app.lawnchair.preferences.PreferenceManager
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anthropic Claude LLM provider implementation.
 *
 * Uses the Claude API for app categorization.
 * API Docs: https://docs.anthropic.com/claude/reference/
 */
class ClaudeProvider(
    private val context: Context,
    private val apiKey: String? = null,
) : LLMProvider {

    override val name: String = "Claude (Anthropic)"

    override val requiresApiKey: Boolean = true

    override suspend fun getCurrentModel(): ModelInfo? {
        return if (isAvailable()) {
            ModelRegistry.getModelById("claude", effectiveModel)
        } else {
            null
        }
    }

    private val effectiveApiKey: String
        get() {
            val userKey = apiKey ?: PreferenceManager.getInstance(context).llmClaudeKey.get()
            val finalKey = userKey.ifEmpty { "" }
            android.util.Log.d(TAG, "Claude API key status: ${if (finalKey.isEmpty()) "NOT SET" else "SET (length: ${finalKey.length})"}")
            return finalKey
        }

    override suspend fun isAvailable(): Boolean {
        val available = effectiveApiKey.isNotEmpty()
        android.util.Log.d(TAG, "Claude provider available: $available")
        return available
    }

    override suspend fun testConnection(): TestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            LLMLogger.logInfo(
                provider = name,
                operation = "TEST_CONNECTION",
                message = "Testing connection to Claude",
                details = mapOf("model" to effectiveModel),
            )

            // Make a minimal API call to test connectivity
            val testPrompt = "Respond with 'OK'"
            val response = callClaudeAPIWithFallback(testPrompt)

            val latency = System.currentTimeMillis() - startTime

            // Parse to validate response format
            val jsonResponse = JSONObject(response)
            val content = jsonResponse.optJSONArray("content")

            if (content != null && content.length() > 0) {
                LLMLogger.logInfo(
                    provider = name,
                    operation = "TEST_CONNECTION",
                    message = "Connection test successful",
                    details = mapOf(
                        "latencyMs" to latency,
                        "model" to effectiveModel,
                    ),
                )

                TestResult(
                    success = true,
                    message = "Successfully connected to Claude",
                    latencyMs = latency,
                    modelVersion = effectiveModel,
                )
            } else {
                val errorMsg = "Invalid response format from Claude"
                LLMLogger.logWarning(
                    provider = name,
                    operation = "TEST_CONNECTION",
                    message = errorMsg,
                )

                TestResult(
                    success = false,
                    message = errorMsg,
                    latencyMs = latency,
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime

            LLMLogger.logError(
                provider = name,
                operation = "TEST_CONNECTION",
                error = e,
                context = mapOf("latencyMs" to latency),
            )

            TestResult(
                success = false,
                message = "Connection test failed: ${e.message}",
                latencyMs = latency,
                error = e,
            )
        }
    }

    /**
     * Gets the effective model to use, with fallback handling
     */
    private val effectiveModel: String
        get() {
            // Try to get user's preferred model from preferences
            val prefs = PreferenceManager.getInstance(context)
            val preferredModel = try {
                prefs.llmClaudeModel.get()
            } catch (e: Exception) {
                // Preference might not exist yet
                android.util.Log.w(TAG, "Could not read llmClaudeModel preference: ${e.message}")
                null
            }

            // Check if preferred model is available
            val model = if (!preferredModel.isNullOrEmpty() &&
                ModelRegistry.isModelAvailable("claude", preferredModel)
            ) {
                preferredModel
            } else {
                // Fall back to registry's recommended model
                val fallback = ModelRegistry.getFallbackModel("claude", preferredModel ?: "")
                fallback?.id ?: DEFAULT_MODEL
            }

            android.util.Log.d(TAG, "Using model: $model")
            return model
        }

    companion object {
        private const val TAG = "ClaudeProvider"
        private const val DEFAULT_MODEL = "claude-3-5-haiku-20241022"
    }

    override suspend fun categorizeApp(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableCategories: List<String>,
    ): CategorizationResult = withContext(Dispatchers.IO) {
        try {
            LLMLogger.logDebug(
                provider = name,
                operation = "CATEGORIZE_APP",
                message = "Categorizing app: $appName",
                details = mapOf(
                    "package" to appPackage,
                    "model" to effectiveModel,
                ),
            )

            val prompt = buildPrompt(appName, appPackage, appDescription, availableCategories)
            val response = callClaudeAPIWithFallback(prompt)
            val result = parseResponse(response, availableCategories)

            LLMLogger.logInfo(
                provider = name,
                operation = "CATEGORIZE_APP",
                message = "Successfully categorized: $appName -> ${result.category}",
                details = mapOf(
                    "package" to appPackage,
                    "category" to result.category,
                    "confidence" to result.confidence,
                ),
            )

            result
        } catch (e: Exception) {
            LLMLogger.logError(
                provider = name,
                operation = "CATEGORIZE_APP",
                error = e,
                context = mapOf(
                    "package" to appPackage,
                    "appName" to appName,
                ),
            )
            throw LLMException("Claude categorization failed: ${e.message}", e)
        }
    }

    override suspend fun categorizeAppBatch(
        apps: List<AppBatchInfo>,
        availableCategories: List<String>,
    ): Map<String, CategorizationResult> = withContext(Dispatchers.IO) {
        try {
            LLMLogger.logDebug(
                provider = name,
                operation = "CATEGORIZE_BATCH",
                message = "Categorizing ${apps.size} apps in batch",
                details = mapOf(
                    "batchSize" to apps.size,
                    "model" to effectiveModel,
                ),
            )

            val prompt = buildBatchPrompt(apps, availableCategories)
            val response = callClaudeAPIWithFallback(prompt)
            val results = parseBatchResponse(response, apps, availableCategories)

            LLMLogger.logInfo(
                provider = name,
                operation = "CATEGORIZE_BATCH",
                message = "Successfully categorized ${results.size} apps in batch",
                details = mapOf(
                    "requestedApps" to apps.size,
                    "successfulApps" to results.size,
                ),
            )

            results
        } catch (e: Exception) {
            LLMLogger.logWarning(
                provider = name,
                operation = "CATEGORIZE_BATCH",
                message = "Batch categorization failed, falling back to sequential",
                details = mapOf(
                    "error" to (e.message ?: "Unknown error"),
                    "batchSize" to apps.size,
                ),
            )

            // Fallback to sequential categorization
            val results = mutableMapOf<String, CategorizationResult>()
            apps.forEach { app ->
                try {
                    val result = categorizeApp(
                        appName = app.appName,
                        appPackage = app.packageName,
                        appDescription = app.appDescription,
                        availableCategories = availableCategories,
                    )
                    results[app.packageName] = result
                } catch (e: Exception) {
                    LLMLogger.logError(
                        provider = name,
                        operation = "CATEGORIZE_BATCH_FALLBACK",
                        error = e,
                        context = mapOf(
                            "package" to app.packageName,
                            "appName" to app.appName,
                        ),
                    )
                }
            }
            results
        }
    }

    override suspend fun suggestCategories(
        installedApps: List<String>,
        existingCategories: List<String>,
        maxSuggestions: Int,
    ): List<SuggestedCategory> = withContext(Dispatchers.IO) {
        try {
            LLMLogger.logDebug(
                provider = name,
                operation = "SUGGEST_CATEGORIES",
                message = "Suggesting categories for ${installedApps.size} apps",
                details = mapOf(
                    "appsCount" to installedApps.size,
                    "maxSuggestions" to maxSuggestions,
                    "model" to effectiveModel,
                ),
            )

            val prompt = buildSuggestionPrompt(installedApps, existingCategories, maxSuggestions)
            val response = callClaudeAPIWithFallback(prompt)
            val suggestions = parseSuggestionResponse(response)

            LLMLogger.logInfo(
                provider = name,
                operation = "SUGGEST_CATEGORIES",
                message = "Successfully generated ${suggestions.size} category suggestions",
                details = mapOf(
                    "suggestionsCount" to suggestions.size,
                ),
            )

            suggestions
        } catch (e: Exception) {
            LLMLogger.logError(
                provider = name,
                operation = "SUGGEST_CATEGORIES",
                error = e,
                context = mapOf(
                    "appsCount" to installedApps.size,
                ),
            )
            throw LLMException("Claude category suggestion failed: ${e.message}", e)
        }
    }

    /**
     * Calls Claude API with automatic model fallback.
     * Tries the preferred model first, then falls back to other available models if it fails.
     */
    private fun callClaudeAPIWithFallback(prompt: String): String {
        // Get list of available models for this provider
        val availableModels = ModelRegistry.getAvailableModels("claude")

        // Start with the effective (preferred) model
        val modelsToTry = listOf(effectiveModel) +
            availableModels.map { it.id }.filter { it != effectiveModel }

        var lastException: Exception? = null

        for ((index, model) in modelsToTry.withIndex()) {
            try {
                android.util.Log.d(TAG, "Attempting API call with model: $model (attempt ${index + 1}/${modelsToTry.size})")

                val response = callClaudeAPI(prompt, model)

                // If we succeeded with a non-preferred model, log it
                if (model != effectiveModel) {
                    LLMLogger.logWarning(
                        provider = name,
                        operation = "MODEL_FALLBACK",
                        message = "Successfully used fallback model: $model",
                        details = mapOf(
                            "preferredModel" to effectiveModel,
                            "fallbackModel" to model,
                            "attemptNumber" to (index + 1),
                        ),
                    )
                }

                return response
            } catch (e: Exception) {
                lastException = e

                // Check if error is model-specific (not found, deprecated, etc.)
                val errorMessage = e.message ?: ""
                val isModelSpecificError = errorMessage.contains("model", ignoreCase = true) ||
                    errorMessage.contains("not found", ignoreCase = true) ||
                    errorMessage.contains("deprecated", ignoreCase = true) ||
                    errorMessage.contains("unavailable", ignoreCase = true) ||
                    errorMessage.contains("invalid_model", ignoreCase = true)

                if (isModelSpecificError && index < modelsToTry.size - 1) {
                    LLMLogger.logWarning(
                        provider = name,
                        operation = "MODEL_FALLBACK",
                        message = "Model $model failed, trying next model",
                        details = mapOf(
                            "failedModel" to model,
                            "error" to errorMessage,
                            "nextModel" to modelsToTry[index + 1],
                        ),
                    )
                    continue
                } else if (!isModelSpecificError) {
                    // Non-model errors (auth, network, etc.) should fail immediately
                    throw e
                }
            }
        }

        // All models failed
        LLMLogger.logError(
            provider = name,
            operation = "MODEL_FALLBACK",
            error = lastException ?: Exception("All models failed"),
            context = mapOf(
                "triedModels" to modelsToTry.joinToString(),
                "totalAttempts" to modelsToTry.size,
            ),
        )

        throw LLMException(
            "All available Claude models failed. Last error: ${lastException?.message}",
            lastException,
        )
    }

    /**
     * Sanitizes user input to prevent prompt injection attacks.
     * Removes control characters, limits length, and escapes special characters.
     */
    private fun sanitizeInput(text: String): String {
        return text
            .replace("\"", "\\\"") // Escape quotes
            .replace("\n", " ") // Remove newlines
            .replace("\r", " ") // Remove carriage returns
            .replace("\t", " ") // Remove tabs
            .take(200) // Limit length to prevent token overflow
            .trim()
    }

    private fun buildPrompt(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableCategories: List<String>,
    ): String {
        // Sanitize all user-controlled inputs
        val safeAppName = sanitizeInput(appName)
        val safeAppPackage = sanitizeInput(appPackage)
        val safeDescription = appDescription?.let { sanitizeInput(it) }

        val descriptionText = safeDescription?.let { "\nDescription: $it" } ?: ""

        return """
You are an expert at categorizing Android apps. Given an app's information, choose the BEST matching category from the provided list.

App Name: $safeAppName
Package: $safeAppPackage$descriptionText

Available Categories:
${availableCategories.joinToString("\n") { "- $it" }}

Instructions:
1. Analyze the app's name, package, and description carefully
2. Choose the MOST SPECIFIC and appropriate category from the list above
3. Prefer narrower, more specific categories over broad parent categories
   - Example: "Notes" is better than "Productivity" for note-taking apps
   - Example: "Finance" is better than "Utilities" for banking apps
4. Provide a confidence score (0.0 to 1.0)
5. Give a brief 1-sentence reason for your choice

Respond ONLY in this JSON format:
{
  "category": "category name",
  "confidence": 0.85,
  "reasoning": "brief explanation"
}
        """.trimIndent()
    }

    private fun buildSuggestionPrompt(
        installedApps: List<String>,
        existingCategories: List<String>,
        maxSuggestions: Int,
    ): String {
        val appSample = installedApps.take(50).joinToString("\n") { "- $it" }
        val existingText = if (existingCategories.isNotEmpty()) {
            "\n\nExisting Categories (do NOT suggest these):\n${existingCategories.joinToString("\n") { "- $it" }}"
        } else {
            ""
        }

        return """
You are an expert at organizing Android apps. Analyze this list of installed apps and suggest useful custom categories that would help organize them.

Installed Apps:
$appSample

Instructions:
1. Analyze the types of apps installed
2. Suggest $maxSuggestions useful category names that would help organize these apps
3. Categories should be specific and meaningful (e.g., "Finance", "Travel", "Education")
4. Each category should have at least 2-3 apps that would fit
5. Provide a brief description and example apps for each category
6. DO NOT suggest generic categories like "Other" or "Miscellaneous"$existingText

Respond ONLY in this JSON format:
{
  "suggestions": [
    {
      "name": "Category Name",
      "description": "Brief description of what belongs here",
      "exampleApps": ["App 1", "App 2", "App 3"],
      "confidence": 0.85
    }
  ]
}
        """.trimIndent()
    }

    private fun buildBatchPrompt(
        apps: List<AppBatchInfo>,
        availableCategories: List<String>,
    ): String {
        val appsText = apps.joinToString("\n") { app ->
            // Sanitize all app inputs to prevent injection
            val safeName = sanitizeInput(app.appName)
            val safePackage = sanitizeInput(app.packageName)
            val safeDesc = app.appDescription?.let { sanitizeInput(it) }
            val desc = safeDesc?.let { " | Description: $it" } ?: ""
            "- $safeName ($safePackage)$desc"
        }

        return """
You are an expert at categorizing Android apps. Given a list of apps, categorize each one by choosing the BEST matching category from the provided list.

Apps to categorize:
$appsText

Available Categories:
${availableCategories.joinToString("\n") { "- $it" }}

Instructions:
1. Analyze each app's name, package, and description carefully
2. Choose the MOST SPECIFIC and appropriate category from the list above for each app
3. Prefer narrower, more specific categories over broad parent categories
   - Example: "Notes" is better than "Productivity" for note-taking apps
   - Example: "Finance" is better than "Utilities" for banking apps
   - Example: "Shopping" is better than "Lifestyle" for shopping apps
4. Provide a confidence score (0.0 to 1.0) for each
5. Give a brief 1-sentence reason for each choice

Respond ONLY in this JSON format:
{
  "results": {
    "com.example.package1": {
      "category": "category name",
      "confidence": 0.85,
      "reasoning": "brief explanation"
    },
    "com.example.package2": {
      "category": "category name",
      "confidence": 0.90,
      "reasoning": "brief explanation"
    }
  }
}
        """.trimIndent()
    }

    private fun callClaudeAPI(prompt: String, model: String = effectiveModel): String {
        val startTime = System.currentTimeMillis()
        val endpoint = "https://api.anthropic.com/v1/messages"
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 30000 // 30 seconds
            connection.readTimeout = 60000 // 60 seconds
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", effectiveApiKey)
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", model)
                put("max_tokens", 1024)
                put(
                    "messages",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", prompt)
                            },
                        )
                    },
                )
            }

            LLMLogger.logRequest(
                provider = name,
                endpoint = endpoint,
                requestBody = requestBody.toString(),
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "anthropic-version" to "2023-06-01",
                ),
            )

            connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

            val responseCode = connection.responseCode
            val durationMs = System.currentTimeMillis() - startTime

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"

                LLMLogger.logResponse(
                    provider = name,
                    statusCode = responseCode,
                    responseBody = errorBody,
                    durationMs = durationMs,
                )

                throw LLMException("Claude API error: $responseCode - $errorBody")
            }

            val responseBody = connection.inputStream.bufferedReader().readText()

            LLMLogger.logResponse(
                provider = name,
                statusCode = responseCode,
                responseBody = responseBody,
                durationMs = durationMs,
            )

            return responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(
        responseJson: String,
        availableCategories: List<String>,
    ): CategorizationResult {
        try {
            val response = JSONObject(responseJson)
            val content = response.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")

            // Extract JSON from markdown code blocks if present
            val jsonText = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val result = JSONObject(jsonText)
            val category = result.getString("category")
            val confidence = result.getDouble("confidence").toFloat()
            val reasoning = result.optString("reasoning", null)

            // Validate category is in available list
            if (!availableCategories.contains(category)) {
                throw LLMException("LLM suggested invalid category: $category")
            }

            return CategorizationResult(
                category = category,
                confidence = confidence.coerceIn(0f, 1f),
                reasoning = reasoning,
            )
        } catch (e: Exception) {
            throw LLMException("Failed to parse Claude response: ${e.message}", e)
        }
    }

    private fun parseSuggestionResponse(responseJson: String): List<SuggestedCategory> {
        try {
            val response = JSONObject(responseJson)
            val content = response.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")

            // Extract JSON from markdown code blocks if present
            val jsonText = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val result = JSONObject(jsonText)
            val suggestions = result.getJSONArray("suggestions")

            return (0 until suggestions.length()).map { i ->
                val suggestion = suggestions.getJSONObject(i)
                val exampleApps = suggestion.getJSONArray("exampleApps")
                val examples = (0 until exampleApps.length()).map { j ->
                    exampleApps.getString(j)
                }

                SuggestedCategory(
                    name = suggestion.getString("name"),
                    description = suggestion.getString("description"),
                    exampleApps = examples,
                    confidence = suggestion.getDouble("confidence").toFloat().coerceIn(0f, 1f),
                )
            }
        } catch (e: Exception) {
            throw LLMException("Failed to parse Claude suggestion response: ${e.message}", e)
        }
    }

    private fun parseBatchResponse(
        responseJson: String,
        apps: List<AppBatchInfo>,
        availableCategories: List<String>,
    ): Map<String, CategorizationResult> {
        try {
            val response = JSONObject(responseJson)
            val content = response.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")

            // Extract JSON from markdown code blocks if present
            val jsonText = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val result = JSONObject(jsonText)
            val resultsObj = result.getJSONObject("results")

            val categorizations = mutableMapOf<String, CategorizationResult>()

            // Iterate through each package in the results
            resultsObj.keys().forEach { packageName ->
                val appResult = resultsObj.getJSONObject(packageName)
                val category = appResult.getString("category")
                val confidence = appResult.getDouble("confidence").toFloat()
                val reasoning = appResult.optString("reasoning", null)

                // Validate category is in available list
                if (availableCategories.contains(category)) {
                    categorizations[packageName] = CategorizationResult(
                        category = category,
                        confidence = confidence.coerceIn(0f, 1f),
                        reasoning = reasoning,
                    )
                } else {
                    LLMLogger.logWarning(
                        provider = name,
                        operation = "PARSE_BATCH_RESPONSE",
                        message = "Invalid category suggested for $packageName: $category",
                    )
                }
            }

            return categorizations
        } catch (e: Exception) {
            throw LLMException("Failed to parse Claude batch response: ${e.message}", e)
        }
    }
}
