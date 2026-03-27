package app.lawnchair.ui.preferences.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.SettingsCategory
import app.lawnchair.ui.preferences.components.controls.PreferenceCategory

/**
 * A draggable settings category card with edit mode support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableSettingsCategory(
    category: SettingsCategory,
    description: String,
    isSelected: Boolean,
    isEditMode: Boolean,
    onNavigate: () -> Unit,
    onToggleVisibility: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(
        targetValue = if (isEditMode) 4.dp else 0.dp,
        label = "elevation",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (!category.isVisible && !isEditMode) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Drag handle (only visible in edit mode)
        if (isEditMode) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Main category card
        Box(
            modifier = Modifier.weight(1f),
        ) {
            PreferenceCategory(
                label = stringResource(id = category.labelResId),
                description = description,
                iconResource = category.iconResId,
                onNavigate = run {
                    val emptyAction: () -> Unit = {}
                    if (!isEditMode) onNavigate else emptyAction
                },
                isSelected = isSelected && !isEditMode,
                modifier = Modifier.combinedClickable(
                    onClick = { if (!isEditMode) onNavigate() },
                    onLongClick = { if (!isEditMode) onLongPress() },
                ),
            )
        }

        // Visibility toggle (only visible in edit mode)
        if (isEditMode) {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(
                    imageVector = if (category.isVisible) {
                        Icons.Rounded.Visibility
                    } else {
                        Icons.Rounded.VisibilityOff
                    },
                    contentDescription = if (category.isVisible) "Hide" else "Show",
                    tint = if (category.isVisible) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

/**
 * Group composable that manages all settings categories with drag-and-drop support
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DraggableSettingsCategoryGroup(
    categories: List<app.lawnchair.ui.preferences.SettingsCategory>,
    currentRoute: app.lawnchair.ui.preferences.navigation.PreferenceRootRoute,
    isEditMode: Boolean,
    onNavigate: (app.lawnchair.ui.preferences.navigation.PreferenceRootRoute) -> Unit,
    onToggleEditMode: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onToggleVisibility: (String) -> Unit,
    deckLayoutEnabled: Boolean,
    quickstepEnabled: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = modifier) {
        // Edit mode controls
        if (isEditMode) {
            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Text(
                            text = "Customize Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        androidx.compose.material3.TextButton(
                            onClick = onToggleEditMode,
                        ) {
                            androidx.compose.material3.Text("Done")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        text = "Drag items to reorder them.\nTap the visibility icon to hide or show categories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // Category list with drag support
        app.lawnchair.ui.preferences.destinations.PreferenceCategoryGroup {
            var draggedItem by remember { mutableStateOf<Int?>(null) }
            var targetItem by remember { mutableStateOf<Int?>(null) }

            categories.forEachIndexed { index, category ->
                // Filter based on conditional visibility
                val shouldShow = when {
                    category.id == "app_drawer" && deckLayoutEnabled -> false
                    category.id == "quickstep" && !quickstepEnabled -> false
                    !category.isVisible && !isEditMode -> false
                    else -> true
                }

                if (shouldShow) {
                    val description = if (category.id == "about") {
                        "${context.getString(com.android.launcher3.R.string.derived_app_name)} ${com.android.launcher3.BuildConfig.MAJOR_VERSION}"
                    } else {
                        androidx.compose.ui.res.stringResource(id = category.descriptionResId)
                    }

                    Box(
                        modifier = if (isEditMode) {
                            Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    draggedItem = index
                                },
                            )
                        } else {
                            Modifier
                        },
                    ) {
                        DraggableSettingsCategory(
                            category = category,
                            description = description,
                            isSelected = currentRoute::class == category.route::class,
                            isEditMode = isEditMode,
                            onNavigate = { onNavigate(category.route) },
                            onToggleVisibility = { onToggleVisibility(category.id) },
                            onLongPress = onToggleEditMode,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                }
            }
        }

        // Floating button to enter edit mode (when not in edit mode)
        if (!isEditMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onToggleEditMode,
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text("Customize Layout")
                }
            }
        }
    }
}
