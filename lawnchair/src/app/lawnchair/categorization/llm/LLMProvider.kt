package app.lawnchair.categorization.llm

/**
 * Represents the result of an LLM categorization request.
 *
 * @property category The suggested category name
 * @property confidence Confidence score (0.0 to 1.0)
 * @property reasoning Brief explanation of why this category was chosen
 */
data class CategorizationResult(
    val tabName: String,
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
 * Result of testing an LLM provider connection
 */
data class TestResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long? = null,
    val modelVersion: String? = null,
    val error: Throwable? = null,
)

/**
 * Information about an app for batch categorization
 */
data class AppBatchInfo(
    val packageName: String,
    val appName: String,
    val appDescription: String?,
)

/**
 * Suggested folder with grouped apps
 */
data class SuggestedFolder(
    val name: String,
    val description: String,
    val packageNames: List<String>,
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
     * Gets the currently configured model for this provider.
     * Returns null if no model is configured or provider is not available.
     */
    suspend fun getCurrentModel(): ModelInfo?

    /**
     * Checks if the provider is properly configured and ready to use.
     * For providers requiring API keys, this validates the key is set.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Tests the connection to this provider and validates configuration.
     *
     * @return TestResult with success status, message, and latency
     */
    suspend fun testConnection(): TestResult

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
        availableTabs: List<String>,
    ): CategorizationResult

    /**
     * Categorizes multiple apps in a single batch request.
     * More efficient than individual requests, saves tokens and time.
     *
     * @param apps List of apps to categorize
     * @param availableCategories List of custom categories to choose from
     * @return Map of packageName → CategorizationResult
     * @throws LLMException if batch categorization fails
     */
    suspend fun categorizeAppBatch(
        apps: List<AppBatchInfo>,
        availableTabs: List<String>,
    ): Map<String, CategorizationResult>

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
        existingTabs: List<String>,
        maxSuggestions: Int = 5,
    ): List<SuggestedCategory>

    /**
     * Analyzes apps within a tab and suggests logical folder groupings.
     *
     * Used for auto-organizing imported apps (e.g., from Smart Launcher) that have
     * tab assignments but aren't organized into folders yet.
     *
     * @param tabName The tab/category these apps belong to
     * @param apps List of app info to organize into folders
     * @return List of suggested folders with app groupings
     * @throws LLMException if suggestion fails
     */
    suspend fun suggestFolders(
        tabName: String,
        apps: List<AppBatchInfo>,
    ): List<SuggestedFolder>
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
