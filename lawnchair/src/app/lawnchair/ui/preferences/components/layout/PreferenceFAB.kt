package app.lawnchair.ui.preferences.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.animations.pressableScale
import app.lawnchair.ui.preferences.haptics.PreferenceHapticType
import app.lawnchair.ui.preferences.haptics.rememberPreferenceHaptics

/**
 * Floating action button for search functionality.
 * Displays a prominent search icon that opens global settings search.
 *
 * @param onClick Callback when the FAB is clicked
 * @param visible Whether the FAB is visible
 * @param modifier Modifier for the FAB
 */
@Composable
fun SearchPreferenceFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = {
                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                onClick()
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 6.dp,
                focusedElevation = 4.dp,
                hoveredElevation = 6.dp,
            ),
            modifier = Modifier.pressableScale(),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search settings",
            )
        }
    }
}

/**
 * Extended floating action button with text label.
 * Auto-collapses to icon-only when scrolling down.
 *
 * @param text The text label to display
 * @param icon The icon to display
 * @param onClick Callback when the FAB is clicked
 * @param expanded Whether the FAB should show the text label
 * @param visible Whether the FAB is visible
 * @param modifier Modifier for the FAB
 */
@Composable
fun ExtendedPreferenceFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    visible: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = modifier,
    ) {
        ExtendedFloatingActionButton(
            onClick = {
                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                onClick()
            },
            expanded = expanded,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            },
            text = { Text(text) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 6.dp,
            ),
            modifier = Modifier.pressableScale(),
        )
    }
}

/**
 * Generic floating action button with custom icon.
 *
 * @param icon The icon to display
 * @param contentDescription Content description for accessibility
 * @param onClick Callback when the FAB is clicked
 * @param visible Whether the FAB is visible
 * @param modifier Modifier for the FAB
 */
@Composable
fun PreferenceFAB(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = modifier,
    ) {
        FloatingActionButton(
            onClick = {
                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                onClick()
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 6.dp,
            ),
            modifier = Modifier.pressableScale(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
            )
        }
    }
}

/**
 * Small floating action button variant.
 * Useful for secondary actions or compact layouts.
 *
 * @param icon The icon to display
 * @param contentDescription Content description for accessibility
 * @param onClick Callback when the FAB is clicked
 * @param visible Whether the FAB is visible
 * @param modifier Modifier for the FAB
 */
@Composable
fun SmallPreferenceFAB(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = modifier,
    ) {
        androidx.compose.material3.SmallFloatingActionButton(
            onClick = {
                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                onClick()
            },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
            ),
            modifier = Modifier.pressableScale(targetScale = 0.92f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
            )
        }
    }
}

/**
 * Multi-action FAB that reveals multiple action buttons.
 * Useful for providing quick access to related actions.
 *
 * @param mainIcon The main FAB icon
 * @param mainContentDescription Content description for main FAB
 * @param expanded Whether the action buttons are revealed
 * @param onMainClick Callback when main FAB is clicked (toggles expansion)
 * @param modifier Modifier for the FAB group
 * @param actions List of secondary actions to display when expanded
 */
@Composable
fun MultiActionFAB(
    expanded: Boolean,
    onMainClick: () -> Unit,
    actions: List<FABAction>,
    modifier: Modifier = Modifier,
    mainIcon: ImageVector = Icons.Default.Add,
    mainContentDescription: String = "Actions",
) {
    val haptics = rememberPreferenceHaptics()
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fab rotation",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Secondary actions (shown when expanded)
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Row {
                actions.forEach { action ->
                    SmallPreferenceFAB(
                        icon = action.icon,
                        contentDescription = action.contentDescription,
                        onClick = action.onClick,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = {
                haptics.perform(PreferenceHapticType.PREFERENCE_CLICK)
                onMainClick()
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .pressableScale()
                .graphicsLayer {
                    rotationZ = rotation
                },
        ) {
            Icon(
                imageVector = mainIcon,
                contentDescription = mainContentDescription,
            )
        }
    }
}

/**
 * Data class representing a secondary FAB action.
 */
data class FABAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
)
