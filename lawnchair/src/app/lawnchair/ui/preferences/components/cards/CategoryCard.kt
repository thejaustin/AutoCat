package app.lawnchair.ui.preferences.components.cards

import androidx.compose.foundation.background
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.animations.pressableScale
import app.lawnchair.ui.preferences.haptics.PreferenceHapticType
import app.lawnchair.ui.preferences.haptics.rememberPreferenceHaptics

/**
 * Enhanced category card for the preferences dashboard.
 * Features larger icons, dynamic tinting, optional gradients, and haptic feedback.
 *
 * @param label The category title
 * @param description Optional description text
 * @param icon The icon resource or ImageVector
 * @param iconSize Size of the icon (default: 40dp for expressive design)
 * @param iconTint Color tint for the icon
 * @param backgroundGradient Optional gradient for the card background
 * @param onClick Click handler for navigation
 * @param enabled Whether the card is enabled
 * @param modifier Modifier for the card
 */
@Composable
fun CategoryCard(
    label: String,
    icon: Any, // Can be Int (resource), ImageVector, or Painter
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    iconSize: Dp = 40.dp,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    backgroundGradient: Brush? = null,
    enabled: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .pressableScale(
                enabled = enabled,
                onPress = {
                    haptics.perform(PreferenceHapticType.CATEGORY_SELECT)
                },
            ),
        onClick = onClick,
        enabled = enabled,
        elevation = PreferenceCardDefaults.elevatedCardElevation,
    ) {
        Box {
            // Optional gradient background
            if (backgroundGradient != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(backgroundGradient),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon container with subtle background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    when (icon) {
                        is Int -> Icon(
                            painter = painterResource(id = icon),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(iconSize),
                        )

                        is ImageVector -> Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(iconSize),
                        )

                        is Painter -> Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (description != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact category card variant with smaller footprint.
 * Useful for secondary categories or grid layouts.
 *
 * @param label The category title
 * @param icon The icon resource or ImageVector
 * @param iconSize Size of the icon (default: 32dp)
 * @param iconTint Color tint for the icon
 * @param onClick Click handler for navigation
 * @param enabled Whether the card is enabled
 * @param modifier Modifier for the card
 */
@Composable
fun CompactCategoryCard(
    label: String,
    icon: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    val haptics = rememberPreferenceHaptics()

    ElevatedCard(
        modifier = modifier
            .pressableScale(
                enabled = enabled,
                onPress = {
                    haptics.perform(PreferenceHapticType.CATEGORY_SELECT)
                },
            ),
        onClick = onClick,
        enabled = enabled,
        elevation = PreferenceCardDefaults.elevatedCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when (icon) {
                    is Int -> Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(iconSize),
                    )

                    is ImageVector -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(iconSize),
                    )

                    is Painter -> Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Creates a gradient brush for category cards based on color scheme.
 * Provides a subtle visual enhancement for important categories.
 *
 * @param startColor Starting color of the gradient
 * @param endColor Ending color of the gradient
 * @param alpha Opacity of the gradient overlay (default: 0.1f)
 */
@Composable
fun categoryGradient(
    startColor: Color = MaterialTheme.colorScheme.primary,
    endColor: Color = MaterialTheme.colorScheme.tertiary,
    alpha: Float = 0.1f,
): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            startColor.copy(alpha = alpha),
            endColor.copy(alpha = alpha),
        ),
    )
}
