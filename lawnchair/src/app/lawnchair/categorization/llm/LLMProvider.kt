package app.lawnchair.categorization.llm

/**
 * Represents the result of an LLM categorization request.
 *
 * @property category The suggested category name
 * @property confidence Confidence score (0.0 to 1.0)
 * @property reasoning Brief explanation of why this category was chosen
 */
data class CategorizationResult(
    val category: String,
    val confidence: Float,
    val reasoning: String?,
)

/**
 * Represents a suggested category from LLM analysis.
 *
 * @property name The suggested category name
 * @property description Brief description of what apps belong in this category
 * @property exampleApps Example apps that would fit this category
 * @property confidence How confident the LLM is in this suggestion (0.0 to 1.0)
 */
data class SuggestedCategory(
    val name: String,
    val description: String,
    val exampleApps: List<String>,
    val confidence: Float,
)

/**
 * Abstract interface for LLM providers used in app categorization.
 *
 * Implementations provide different LLM backends (Google AI, Claude, OpenAI, etc.)
 * with a unified interface for categorizing apps.
 */
interface LLMProvider {
    /**
     * The display name of this provider (e.g., "Google AI", "Claude", "OpenAI")
     */
    val name: String

    /**
     * Whether this provider requires an API key from the user.
     * Free providers (like Google AI free tier) return false.
     */
    val requiresApiKey: Boolean

    /**
     * Checks if the provider is properly configured and ready to use.
     * For providers requiring API keys, this validates the key is set.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Categorizes an app using this LLM provider.
     *
     * @param appName The name of the app
     * @param appPackage The package name (e.g., "com.example.app")
     * @param appDescription Optional description from Play Store or manifest
     * @param availableCategories List of custom categories to choose from
     * @return CategorizationResult with suggested category and confidence
     * @throws LLMException if categorization fails
     */
    suspend fun categorizeApp(
        appName: String,
        appPackage: String,
        appDescription: String?,
        availableCategories: List<String>,
    ): CategorizationResult

    /**
     * Analyzes installed apps and suggests useful categories beyond built-in ones.
     *
     * @param installedApps List of app names to analyze
     * @param existingCategories Categories that already exist (to avoid duplicates)
     * @param maxSuggestions Maximum number of category suggestions to return
     * @return List of suggested categories with descriptions and examples
     * @throws LLMException if suggestion fails
     */
    suspend fun suggestCategories(
        installedApps: List<String>,
        existingCategories: List<String>,
        maxSuggestions: Int = 5,
    ): List<SuggestedCategory>
}

/**
 * Exception thrown when LLM categorization fails.
 */
class LLMException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Enum of supported LLM providers.
 */
enum class LLMProviderType {
    GOOGLE_AI,
    CLAUDE,
    OPENAI,
    PERPLEXITY,
}
