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
 * Perplexity LLM provider implementation.
 *
 * Uses the Perplexity API for app categorization.
 * API Docs: https://docs.perplexity.ai/
 */
class PerplexityProvider(
    private val context: Context,
    private val apiKey: String? = null,
) : LLMProvider {

    override val name: String = "Perplexity"

    override val requiresApiKey: Boolean = true

    private val effectiveApiKey: String
        get() {
            val userKey = apiKey ?: PreferenceManager.getInstance(context).llmPerplexityKey.get()
            return userKey.ifEmpty { "" }
        }

    override suspend fun isAvailable(): Boolean {
        return effectiveApiKey.isNotEmpty()
    }

    override suspend fun categorizeApp(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableCategories: List<String>,
    ): CategorizationResult = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(appName, appPackage, appDescription, availableCategories)
            val response = callPerplexityAPI(prompt)
            parseResponse(response, availableCategories)
        } catch (e: Exception) {
            throw LLMException("Perplexity categorization failed: ${e.message}", e)
        }
    }

    override suspend fun suggestCategories(
        installedApps: List<String>,
        existingCategories: List<String>,
        maxSuggestions: Int,
    ): List<SuggestedCategory> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildSuggestionPrompt(installedApps, existingCategories, maxSuggestions)
            val response = callPerplexityAPI(prompt)
            parseSuggestionResponse(response)
        } catch (e: Exception) {
            throw LLMException("Perplexity category suggestion failed: ${e.message}", e)
        }
    }

    private fun buildPrompt(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableCategories: List<String>,
    ): String {
        val descriptionText = appDescription?.let { "\nDescription: $it" } ?: ""

        return """
You are an expert at categorizing Android apps. Given an app's information, choose the BEST matching category from the provided list.

App Name: $appName
Package: $appPackage$descriptionText

Available Categories:
${availableCategories.joinToString("\n") { "- $it" }}

Instructions:
1. Analyze the app's name, package, and description
2. Choose the MOST appropriate category from the list above
3. Provide a confidence score (0.0 to 1.0)
4. Give a brief 1-sentence reason for your choice

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

    private fun callPerplexityAPI(prompt: String): String {
        val url = URL("https://api.perplexity.ai/chat/completions")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $effectiveApiKey")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", "llama-3.1-sonar-small-128k-online")
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
                put("temperature", 0.2)
                put("max_tokens", 200)
            }

            connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw LLMException("Perplexity API error: $responseCode - $errorBody")
            }

            return connection.inputStream.bufferedReader().readText()
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
            val content = response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

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
            throw LLMException("Failed to parse Perplexity response: ${e.message}", e)
        }
    }

    private fun parseSuggestionResponse(responseJson: String): List<SuggestedCategory> {
        try {
            val response = JSONObject(responseJson)
            val content = response.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

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
            throw LLMException("Failed to parse Perplexity suggestion response: ${e.message}", e)
        }
    }
}
