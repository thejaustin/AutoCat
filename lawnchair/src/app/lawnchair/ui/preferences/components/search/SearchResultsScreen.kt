package app.lawnchair.ui.preferences.components.search

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.animations.staggeredListAnimation
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.haptics.PreferenceHapticType
import app.lawnchair.ui.preferences.haptics.rememberPreferenceHaptics
import app.lawnchair.ui.preferences.navigation.PreferenceRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search results screen with staggered entry animations.
 *
 * @param results List of search results to display
 * @param query The search query (for highlighting matches)
 * @param onResultClick Callback when a result is clicked
 * @param contentPadding Padding for the results list
 * @param modifier Modifier for the screen
 */
@Composable
fun SearchResultsScreen(
    results: List<PreferenceMetadata>,
    query: String,
    onResultClick: (PreferenceRoute) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val haptics = rememberPreferenceHaptics()

    when {
        results.isEmpty() && query.isNotEmpty() -> {
            // No results found
            EmptySearchResults(
                query = query,
                modifier = modifier,
            )
        }

        results.isEmpty() -> {
            // No query yet or empty state
            SearchPlaceholder(modifier = modifier)
        }

        else -> {
            // Show results with staggered animations
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Group results by category
                val groupedResults = results.groupBy { it.category }

                groupedResults.forEach { (category, categoryResults) ->
                    // Category header
                    item(key = "header_$category") {
                        CategoryHeader(
                            category = category,
                            count = categoryResults.size,
                        )
                    }

                    // Category results
                    itemsIndexed(
                        items = categoryResults,
                        key = { _, item -> "${category}_${item.route}" },
                    ) { index, result ->
                        SearchResultItem(
                            result = result,
                            query = query,
                            animationDelay = index,
                            onClick = {
                                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                                onResultClick(result.route)
                            },
                        )
                    }

                    // Spacer between categories
                    item(key = "spacer_$category") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Individual search result item with highlighting and animations.
 */
@Composable
private fun SearchResultItem(
    result: PreferenceMetadata,
    query: String,
    animationDelay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }

    // Staggered entry animation
    LaunchedEffect(Unit) {
        delay((animationDelay * 50L).coerceAtMost(500))
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = staggeredListAnimation(animationDelay),
        label = "result item alpha",
    )

    PreferenceTemplate(
        title = {
            Text(
                text = highlightMatches(result.title, query),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        description = {
            Text(
                text = result.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        startWidget = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .animateContentSize()
            .clickable(onClick = onClick),
    )
}

/**
 * Category header for grouping search results.
 */
@Composable
private fun CategoryHeader(
    category: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = category.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "$count result${if (count != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Empty state shown when no results are found.
 */
@Composable
private fun EmptySearchResults(
    query: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp),
            )

            Text(
                text = "No results found",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Try different keywords or check your spelling",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (query.isNotEmpty()) {
                Text(
                    text = "Searched for: \"$query\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * Placeholder shown before any search is performed.
 */
@Composable
private fun SearchPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp),
            )

            Text(
                text = "Search Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Find any setting quickly by typing keywords",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Example searches
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Try searching for:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                listOf("theme", "grid", "gestures", "hidden apps", "icons").forEach { example ->
                    Text(
                        text = "• $example",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/**
 * Highlights matching portions of text in search results.
 * Makes query matches bold and colored.
 */
@Composable
private fun highlightMatches(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) {
        return buildAnnotatedString { append(text) }
    }

    val normalizedText = text.lowercase()
    val normalizedQuery = query.lowercase()

    return buildAnnotatedString {
        var currentIndex = 0

        while (currentIndex < text.length) {
            val matchIndex = normalizedText.indexOf(normalizedQuery, currentIndex)

            if (matchIndex == -1) {
                // No more matches, append remaining text
                append(text.substring(currentIndex))
                break
            }

            // Append text before match
            if (matchIndex > currentIndex) {
                append(text.substring(currentIndex, matchIndex))
            }

            // Append highlighted match
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append(text.substring(matchIndex, matchIndex + query.length))
            }

            currentIndex = matchIndex + query.length
        }
    }
}

/**
 * Performs a search with debouncing to avoid excessive queries.
 *
 * @param query The search query
 * @param debounceMs Debounce delay in milliseconds
 * @param onSearch Callback with search results
 */
@Composable
fun DebouncedSearch(
    query: String,
    debounceMs: Long = 300,
    onSearch: suspend (String) -> List<PreferenceMetadata>,
) {
    val scope = rememberCoroutineScope()
    var currentJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val currentOnSearch by androidx.compose.runtime.rememberUpdatedState(onSearch)

    LaunchedEffect(query) {
        currentJob?.cancel()
        currentJob = scope.launch {
            if (query.isNotEmpty()) {
                delay(debounceMs)
                currentOnSearch(query)
            }
        }
    }
}
