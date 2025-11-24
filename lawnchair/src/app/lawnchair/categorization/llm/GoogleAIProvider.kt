package app.lawnchair.categorization.llm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google AI (Gemini) LLM provider implementation.
 *
 * Uses the Gemini API for app categorization. Supports both:
 * - Free tier (using default API key with rate limits)
 * - User-provided API keys for higher limits
 *
 * API Docs: https://ai.google.dev/api/rest
 */
class GoogleAIProvider(
    private val context: Context,
    private val apiKey: String? = null,
) : LLMProvider {

    override val name: String = "Google AI (Gemini)"

    override val requiresApiKey: Boolean = false // Has free tier

    private val effectiveApiKey: String
        get() = apiKey ?: DEFAULT_API_KEY

    override suspend fun isAvailable(): Boolean {
        // Google AI free tier is always available
        return true
    }

    override suspend fun categorizeApp(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableCategories: List<String>,
    ): CategorizationResult = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(appName, appPackage, appDescription, availableCategories)
            val response = callGeminiAPI(prompt)
            parseResponse(response, availableCategories)
        } catch (e: Exception) {
            throw LLMException("Google AI categorization failed: ${e.message}", e)
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

    private fun callGeminiAPI(prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$effectiveApiKey")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2) // Lower temperature for more consistent categorization
                    put("maxOutputTokens", 200)
                })
            }

            connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw LLMException("Gemini API error: $responseCode - $errorBody")
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
            val candidates = response.getJSONArray("candidates")

            if (candidates.length() == 0) {
                throw LLMException("No response candidates from Gemini")
            }

            val content = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
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
            throw LLMException("Failed to parse Gemini response: ${e.message}", e)
        }
    }

    companion object {
        // TODO: Replace with actual free-tier API key or remove if user must provide
        private const val DEFAULT_API_KEY = "YOUR_GOOGLE_AI_API_KEY"
    }
}
