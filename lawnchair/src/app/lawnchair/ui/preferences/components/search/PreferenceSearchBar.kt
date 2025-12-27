package app.lawnchair.ui.preferences.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.haptics.PreferenceHapticType
import app.lawnchair.ui.preferences.haptics.rememberPreferenceHaptics
import kotlinx.coroutines.delay

/**
 * Material 3 search bar for preference screens.
 * Provides search functionality with haptic feedback and animations.
 *
 * @param query Current search query text
 * @param onQueryChange Callback when query text changes
 * @param onSearch Callback when search is submitted
 * @param active Whether the search bar is active/expanded
 * @param onActiveChange Callback when active state changes
 * @param placeholder Placeholder text shown when query is empty
 * @param modifier Modifier for the search bar
 * @param content Content shown when search is active (e.g., suggestions, recent searches)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search settings...",
    content: @Composable () -> Unit = {},
) {
    val haptics = rememberPreferenceHaptics()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus when becoming active
    LaunchedEffect(active) {
        if (active) {
            delay(100) // Small delay for animation
            focusRequester.requestFocus()
        }
    }

    SearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { searchQuery ->
            onSearch(searchQuery)
            if (searchQuery.isNotEmpty()) {
                haptics.perform(PreferenceHapticType.SEARCH_RESULT_FOUND)
            }
            keyboardController?.hide()
        },
        active = active,
        onActiveChange = { isActive ->
            onActiveChange(isActive)
            if (isActive) {
                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = {
            Text(text = placeholder)
        },
        leadingIcon = {
            if (active) {
                IconButton(
                    onClick = {
                        onActiveChange(false)
                        onQueryChange("")
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                        keyboardController?.hide()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Close search",
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                )
            }
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                    )
                }
            }
        },
        colors = SearchBarDefaults.colors(),
    ) {
        content()
    }
}

/**
 * Compact search bar variant for embedding in screens.
 * Does not expand to full-screen mode.
 *
 * @param query Current search query text
 * @param onQueryChange Callback when query text changes
 * @param onSearch Callback when search is submitted
 * @param placeholder Placeholder text
 * @param modifier Modifier for the search bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
) {
    val haptics = rememberPreferenceHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    androidx.compose.material3.TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                    )
                }
            }
        },
        singleLine = true,
        shape = androidx.compose.material3.SearchBarDefaults.inputFieldShape,
    )
}

/**
 * Recent search item composable.
 * Shows a clickable recent search query.
 *
 * @param query The recent search query
 * @param onClick Callback when the item is clicked
 * @param onRemove Callback to remove this item from recents
 * @param modifier Modifier for the item
 */
@Composable
fun RecentSearchItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPreferenceHaptics()

    androidx.compose.material3.ListItem(
        headlineContent = { Text(query) },
        leadingContent = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Search,
                contentDescription = null,
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                    onRemove()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove",
                )
            }
        },
        modifier = modifier.clickable {
            haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
            onClick()
        },
    )
}

/**
 * Search suggestion item composable.
 * Shows a suggested search query.
 *
 * @param suggestion The suggested query
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for the item
 */
@Composable
fun SearchSuggestionItem(
    suggestion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberPreferenceHaptics()

    androidx.compose.material3.ListItem(
        headlineContent = { Text(suggestion) },
        leadingContent = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Search,
                contentDescription = null,
            )
        },
        modifier = modifier.clickable {
            haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
            onClick()
        },
    )
}
