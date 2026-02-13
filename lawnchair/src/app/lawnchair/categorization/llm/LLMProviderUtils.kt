package app.lawnchair.categorization.llm

import android.content.Context
import android.util.Log
import app.lawnchair.preferences.PreferenceManager

/**
 * Shared utility functions for LLM provider API key and model resolution.
 * Eliminates duplicated logic across GoogleAI, Claude, OpenAI, and Perplexity providers.
 */
object LLMProviderUtils {

    /**
     * Resolves the effective API key using priority order:
     * 1. Constructor-provided key
     * 2. User preference
     * 3. Environment variable
     *
     * @param constructorKey API key passed via constructor (may be null)
     * @param context Android context for accessing preferences
     * @param prefKeyGetter Function to get the preference value from PreferenceManager
     * @param envVarName Environment variable name to check
     * @param tag Log tag for the provider
     * @param providerName Display name for logging
     */
    fun resolveApiKey(
        constructorKey: String?,
        context: Context,
        prefKeyGetter: (PreferenceManager) -> String,
        envVarName: String,
        tag: String,
        providerName: String,
    ): String {
        val userKey = constructorKey ?: prefKeyGetter(PreferenceManager.getInstance(context))
        val envKey = System.getenv(envVarName) ?: ""
        val finalKey = when {
            !userKey.isNullOrEmpty() -> userKey
            envKey.isNotEmpty() -> envKey
            else -> ""
        }
        Log.d(
            tag,
            "$providerName API key status: ${
                if (finalKey.isEmpty()) {
                    "NOT SET"
                } else {
                    "SET (length: ${finalKey.length}, source: ${
                        when {
                            !userKey.isNullOrEmpty() -> "user pref"
                            envKey.isNotEmpty() -> "env var"
                            else -> "none"
                        }
                    })"
                }
            }",
        )
        return finalKey
    }

    /**
     * Resolves the effective model for a provider, with fallback handling.
     *
     * @param context Android context for accessing preferences
     * @param providerId ModelRegistry provider ID (e.g., "google_ai", "claude")
     * @param prefModelGetter Function to get the model preference from PreferenceManager
     * @param defaultModel Default model ID if no preference or fallback
     * @param tag Log tag for the provider
     */
    fun resolveModel(
        context: Context,
        providerId: String,
        prefModelGetter: (PreferenceManager) -> String,
        defaultModel: String,
        tag: String,
    ): String {
        val prefs = PreferenceManager.getInstance(context)
        val preferredModel = try {
            prefModelGetter(prefs)
        } catch (e: Exception) {
            Log.w(tag, "Could not read model preference: ${e.message}")
            null
        }

        val model = if (!preferredModel.isNullOrEmpty() &&
            ModelRegistry.isModelAvailable(providerId, preferredModel)
        ) {
            preferredModel
        } else {
            val fallback = ModelRegistry.getFallbackModel(providerId, preferredModel ?: "")
            fallback?.id ?: defaultModel
        }

        Log.d(tag, "Using model: $model")
        return model
    }

    /**
     * Generates a language-specific instruction for the prompt.
     */
    fun getLanguageInstruction(context: Context): String {
        val prefs = PreferenceManager.getInstance(context)
        var language = prefs.llmPromptLanguage.get()

        if (language == "System Default") {
            val locale = context.resources.configuration.locales[0]
            language = locale.displayLanguage
        }

        return if (language != "English") {
            "\nIMPORTANT: Respond in $language. Ensure category names match the list provided exactly."
        } else {
            ""
        }
    }
}
