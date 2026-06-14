package app.lawnchair.ui.preferences.components.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import app.lawnchair.ui.preferences.animations.PreferenceAnimationDefaults
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.haptics.PreferenceHapticType
import app.lawnchair.ui.preferences.haptics.rememberPreferenceHaptics

/**
 * Collapsible preference group that shows/hides its content with accordion-style animation.
 * Provides visual and haptic feedback when expanding/collapsing.
 *
 * @param heading The title text for the group
 * @param description Optional description text shown below the heading
 * @param defaultExpanded Whether the group starts expanded (default: true)
 * @param persistKey Optional key for persisting expand/collapse state across sessions
 * @param enabled Whether the group can be toggled
 * @param modifier Modifier for the entire group
 * @param content The collapsible content of the group
 */
@Composable
fun CollapsiblePreferenceGroup(
    heading: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    defaultExpanded: Boolean = true,
    persistKey: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by if (persistKey != null) {
        rememberSaveable(key = persistKey) { mutableStateOf(defaultExpanded) }
    } else {
        remember { mutableStateOf(defaultExpanded) }
    }

    val haptics = rememberPreferenceHaptics()

    // Animate chevron rotation
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = PreferenceAnimationDefaults.EXPAND_DURATION,
        ),
        label = "chevron rotation",
    )

    val stateDescriptionText = if (expanded) "Expanded" else "Collapsed"
    val contentDescriptionText = "$heading, $stateDescriptionText"

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Header that can be clicked to expand/collapse
        PreferenceTemplate(
            title = {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            description = {
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            endWidget = {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClickLabel = if (expanded) "Collapse group" else "Expand group",
                ) {
                    expanded = !expanded
                    haptics.perform(
                        if (expanded) {
                            PreferenceHapticType.EXPAND_SECTION
                        } else {
                            PreferenceHapticType.COLLAPSE_SECTION
                        },
                    )
                }
                .semantics {
                    role = Role.Button
                    stateDescription = stateDescriptionText
                    contentDescription = contentDescriptionText
                },
        )

        // Collapsible content
        ExpandAndShrink(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                content()
            }
        }
    }
}

/**
 * Simplified collapsible preference group with minimal styling.
 * Useful for nested groups or secondary organization.
 *
 * @param heading The title text for the group
 * @param defaultExpanded Whether the group starts expanded (default: true)
 * @param enabled Whether the group can be toggled
 * @param modifier Modifier for the entire group
 * @param content The collapsible content of the group
 */
@Composable
fun SimpleCollapsibleGroup(
    heading: String,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = true,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val haptics = rememberPreferenceHaptics()

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = PreferenceAnimationDefaults.EXPAND_DURATION,
        ),
        label = "chevron rotation",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Simple header
        PreferenceTemplate(
            title = {
                Text(
                    text = heading,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            endWidget = {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) {
                    expanded = !expanded
                    haptics.perform(
                        if (expanded) {
                            PreferenceHapticType.EXPAND_SECTION
                        } else {
                            PreferenceHapticType.COLLAPSE_SECTION
                        },
                    )
                },
        )

        // Content
        ExpandAndShrink(visible = expanded) {
            content()
        }
    }
}
