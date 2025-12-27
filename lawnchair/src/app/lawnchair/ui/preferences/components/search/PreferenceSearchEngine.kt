package app.lawnchair.ui.preferences.components.search

import app.lawnchair.ui.preferences.navigation.About
import app.lawnchair.ui.preferences.navigation.AppDrawer
import app.lawnchair.ui.preferences.navigation.DebugMenu
import app.lawnchair.ui.preferences.navigation.Dock
import app.lawnchair.ui.preferences.navigation.ExperimentalFeatures
import app.lawnchair.ui.preferences.navigation.Folders
import app.lawnchair.ui.preferences.navigation.General
import app.lawnchair.ui.preferences.navigation.Gestures
import app.lawnchair.ui.preferences.navigation.HomeScreen
import app.lawnchair.ui.preferences.navigation.PreferenceRoute
import app.lawnchair.ui.preferences.navigation.Quickstep
import app.lawnchair.ui.preferences.navigation.Smartspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Metadata for a preference screen to enable search functionality.
 *
 * @param route The navigation route to this preference
 * @param title The display title of the preference
 * @param description A brief description of what this preference does
 * @param keywords Additional searchable keywords
 * @param category The category this preference belongs to (for grouping)
 * @param weight Search ranking weight (higher = more important)
 */
data class PreferenceMetadata(
    val route: PreferenceRoute,
    val title: String,
    val description: String,
    val keywords: List<String> = emptyList(),
    val category: String,
    val weight: Int = 0,
)

/**
 * Search engine for preference screens.
 * Provides fast, fuzzy search across all preference metadata.
 */
object PreferenceSearchIndex {
    private val index = mutableListOf<PreferenceMetadata>()
    private var isInitialized = false

    /**
     * Builds the search index with all preference metadata.
     * Should be called once at app startup.
     */
    fun buildIndex() {
        if (isInitialized) return

        index.clear()

        // Root/Dashboard preferences
        index.add(
            PreferenceMetadata(
                route = General,
                title = "General",
                description = "App theme, icons, fonts, and basic settings",
                keywords = listOf("theme", "dark", "light", "icon", "font", "appearance", "rotation", "look", "style"),
                category = "Appearance",
                weight = 10,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = HomeScreen,
                title = "Home Screen",
                description = "Grid layout, widgets, and home screen customization",
                keywords = listOf("grid", "layout", "columns", "rows", "widgets", "spacing", "home", "desktop"),
                category = "Layout",
                weight = 10,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = Dock,
                title = "Dock",
                description = "Dock settings, search bar, and quick access",
                keywords = listOf("dock", "hotseat", "bottom", "search", "taskbar", "favorites"),
                category = "Layout",
                weight = 8,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = AppDrawer,
                title = "App Drawer",
                description = "App list, categorization, hidden apps, and drawer layout",
                keywords = listOf(
                    "drawer", "apps", "list", "categorization", "categories", "hidden",
                    "organize", "llm", "ai", "auto", "sort", "filter", "search",
                ),
                category = "Features",
                weight = 10,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = Folders,
                title = "Folders",
                description = "Folder appearance and behavior settings",
                keywords = listOf("folder", "group", "organize", "icon", "preview", "grid"),
                category = "Layout",
                weight = 5,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = Gestures,
                title = "Gestures",
                description = "Swipe gestures and touch interactions",
                keywords = listOf("gesture", "swipe", "tap", "double", "home", "back", "action", "control"),
                category = "Behavior",
                weight = 7,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = Quickstep,
                title = "Quickstep",
                description = "Recent apps and multitasking settings",
                keywords = listOf("recents", "recent", "multitask", "overview", "task", "switcher"),
                category = "Behavior",
                weight = 6,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = Smartspace,
                title = "Smartspace",
                description = "At-a-glance widget with weather and calendar",
                keywords = listOf("smartspace", "widget", "weather", "calendar", "glance", "information"),
                category = "Features",
                weight = 7,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = About,
                title = "About",
                description = "App version, contributors, and information",
                keywords = listOf("about", "version", "info", "contributors", "license", "credits", "help"),
                category = "System",
                weight = 3,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = ExperimentalFeatures,
                title = "Experimental Features",
                description = "Beta features and experimental settings",
                keywords = listOf("experimental", "beta", "test", "preview", "advanced", "flags"),
                category = "System",
                weight = 4,
            ),
        )

        index.add(
            PreferenceMetadata(
                route = DebugMenu,
                title = "Debug Menu",
                description = "Developer and debugging tools",
                keywords = listOf("debug", "developer", "dev", "tools", "diagnostics", "log"),
                category = "System",
                weight = 2,
            ),
        )

        // TODO: Add more specific sub-routes as needed
        // e.g., GeneralIconPack, HomeScreenGrid, etc.

        isInitialized = true
    }

    /**
     * Searches the preference index for matching items.
     * Performs case-insensitive fuzzy matching on titles, descriptions, and keywords.
     *
     * @param query The search query
     * @param maxResults Maximum number of results to return (default: 50)
     * @return List of matching preferences sorted by relevance
     */
    suspend fun search(query: String, maxResults: Int = 50): List<PreferenceMetadata> {
        if (query.isBlank()) return emptyList()

        return withContext(Dispatchers.Default) {
            val normalizedQuery = query.trim().lowercase()
            val queryWords = normalizedQuery.split(" ").filter { it.isNotEmpty() }

            index
                .asSequence()
                .map { metadata ->
                    val score = calculateRelevanceScore(metadata, normalizedQuery, queryWords)
                    metadata to score
                }
                .filter { (_, score) -> score > 0 }
                .sortedByDescending { (metadata, score) ->
                    // Combine relevance score with metadata weight
                    score * (1 + metadata.weight * 0.1f)
                }
                .take(maxResults)
                .map { (metadata, _) -> metadata }
                .toList()
        }
    }

    /**
     * Calculates relevance score for a preference based on query match.
     * Higher scores indicate better matches.
     */
    private fun calculateRelevanceScore(
        metadata: PreferenceMetadata,
        normalizedQuery: String,
        queryWords: List<String>,
    ): Int {
        var score = 0

        val titleLower = metadata.title.lowercase()
        val descriptionLower = metadata.description.lowercase()
        val categoryLower = metadata.category.lowercase()

        // Exact title match = highest score
        if (titleLower == normalizedQuery) {
            score += 100
        } else if (titleLower.startsWith(normalizedQuery)) {
            score += 80
        } else if (titleLower.contains(normalizedQuery)) {
            score += 50
        }

        // Category match
        if (categoryLower.contains(normalizedQuery)) {
            score += 30
        }

        // Description match
        if (descriptionLower.contains(normalizedQuery)) {
            score += 20
        }

        // Keyword matches
        metadata.keywords.forEach { keyword ->
            val keywordLower = keyword.lowercase()
            if (keywordLower == normalizedQuery) {
                score += 60
            } else if (keywordLower.startsWith(normalizedQuery)) {
                score += 40
            } else if (keywordLower.contains(normalizedQuery)) {
                score += 15
            }
        }

        // Multi-word query matching (all words must appear somewhere)
        if (queryWords.size > 1) {
            val allWordsMatch = queryWords.all { word ->
                titleLower.contains(word) ||
                    descriptionLower.contains(word) ||
                    categoryLower.contains(word) ||
                    metadata.keywords.any { it.lowercase().contains(word) }
            }

            if (allWordsMatch) {
                score += 25
            }
        }

        return score
    }

    /**
     * Gets all categories present in the index.
     * Useful for category filtering.
     */
    fun getCategories(): List<String> {
        return index.map { it.category }.distinct().sorted()
    }

    /**
     * Searches within a specific category.
     *
     * @param query The search query
     * @param category The category to search within
     * @param maxResults Maximum number of results
     */
    suspend fun searchInCategory(
        query: String,
        category: String,
        maxResults: Int = 50,
    ): List<PreferenceMetadata> {
        val allResults = search(query, maxResults * 2)
        return allResults
            .filter { it.category.equals(category, ignoreCase = true) }
            .take(maxResults)
    }

    /**
     * Gets suggestions based on partial query.
     * Returns common search terms that match the query.
     */
    fun getSuggestions(partialQuery: String, maxSuggestions: Int = 5): List<String> {
        if (partialQuery.isBlank()) return emptyList()

        val normalized = partialQuery.lowercase()
        val suggestions = mutableSetOf<String>()

        // Collect matching titles and keywords
        index.forEach { metadata ->
            if (metadata.title.lowercase().startsWith(normalized)) {
                suggestions.add(metadata.title)
            }

            metadata.keywords.forEach { keyword ->
                if (keyword.lowercase().startsWith(normalized) && keyword.length > normalized.length) {
                    suggestions.add(keyword)
                }
            }
        }

        return suggestions.take(maxSuggestions).sorted()
    }

    /**
     * Clears the search index.
     * Useful for testing or re-initialization.
     */
    fun clear() {
        index.clear()
        isInitialized = false
    }
}
