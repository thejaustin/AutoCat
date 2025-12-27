package app.lawnchair.ui.preferences.components.previews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.preferences.components.cards.PreferenceFilledCard

/**
 * Container for live previews of preference settings.
 * Wraps content in a styled card with proper spacing and background.
 *
 * @param title Optional title for the preview
 * @param height Height of the preview content area
 * @param modifier Modifier for the preview card
 * @param content The preview content
 */
@Composable
fun LivePreview(
    title: String? = null,
    height: Dp = 200.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    PreferenceFilledCard(
        modifier = modifier,
        header = if (title != null) {
            {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            null
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.medium,
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(
                targetState = content,
                animationSpec = tween(durationMillis = 300),
                label = "preview crossfade",
            ) { currentContent ->
                currentContent()
            }
        }
    }
}

/**
 * Preview component for icon pack and icon shape settings.
 * Shows a simplified app icon with the current settings.
 *
 * @param iconContent The icon to display
 * @param shape The shape of the icon container
 * @param label Label text below the icon
 * @param modifier Modifier for the preview
 */
@Composable
fun IconPreview(
    iconContent: @Composable () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = MaterialTheme.shapes.medium,
    label: String = "Preview",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            iconContent()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Preview component for home screen grid layout settings.
 * Shows a simplified grid representation.
 *
 * @param columns Number of columns to display
 * @param rows Number of rows to display
 * @param spacing Spacing between grid items
 * @param modifier Modifier for the preview
 */
@Composable
fun GridPreview(
    columns: Int,
    rows: Int,
    spacing: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(rows.coerceAtMost(4)) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                repeat(columns.coerceAtMost(6)) { columnIndex ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }
            }
        }

        // Info text
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$columns × $rows Grid",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Preview component for theme/color scheme settings.
 * Shows color swatches representing the theme.
 *
 * @param primaryColor Primary theme color
 * @param secondaryColor Secondary theme color
 * @param tertiaryColor Tertiary theme color
 * @param backgroundColor Surface/background color
 * @param label Label text below the preview
 * @param modifier Modifier for the preview
 */
@Composable
fun ThemePreview(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    backgroundColor: Color,
    label: String = "Theme Preview",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(color = primaryColor, label = "Primary")
            ColorSwatch(color = secondaryColor, label = "Secondary")
            ColorSwatch(color = tertiaryColor, label = "Tertiary")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Individual color swatch for theme previews.
 */
@Composable
private fun ColorSwatch(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.8f,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Preview component for text/font settings.
 * Shows sample text with the current font settings.
 *
 * @param sampleText The text to display
 * @param fontFamily The font family to use
 * @param modifier Modifier for the preview
 */
@Composable
fun FontPreview(
    sampleText: String = "Aa Bb Cc",
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = sampleText,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = fontFamily,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The quick brown fox jumps",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = fontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Generic preview container with custom content.
 * Useful for settings that don't fit other preview types.
 *
 * @param label Optional label text
 * @param modifier Modifier for the preview
 * @param content Custom preview content
 */
@Composable
fun GenericPreview(
    label: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.medium,
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
