package app.lawnchair.ui.preferences.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Default values for preference cards following Material Design 3 guidelines.
 */
object PreferenceCardDefaults {
    /**
     * Default color for filled preference cards.
     * Uses surfaceContainerHighest for prominent elevation in Material 3.
     */
    val filledCardColor: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest

    /**
     * Default elevation for elevated preference cards.
     * Creates subtle depth with 1dp shadow and no tonal elevation.
     */
    val elevatedCardElevation
        @Composable get() = CardDefaults.elevatedCardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 1.dp,
            hoveredElevation = 2.dp,
            draggedElevation = 4.dp,
            disabledElevation = 0.dp,
        )

    /**
     * Default border for outlined preference cards.
     * Uses 1dp outline color for clear but subtle boundaries.
     */
    val outlinedCardBorder: BorderStroke
        @Composable get() = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )

    /**
     * Default content padding for all card variants.
     */
    val contentPadding = 16.dp

    /**
     * Spacing between header and content sections.
     */
    val headerSpacing = 12.dp

    /**
     * Spacing between content and actions sections.
     */
    val actionsSpacing = 16.dp
}

/**
 * Filled card variant for preferences.
 * Uses surfaceContainerHighest for a prominent, elevated appearance.
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler for the entire card
 * @param enabled Whether the card is enabled
 * @param header Optional header composable shown at the top
 * @param content Main content of the card
 * @param actions Optional actions row shown at the bottom
 */
@Composable
fun PreferenceFilledCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = PreferenceCardDefaults.filledCardColor,
        ),
    ) {
        PreferenceCardContent(
            header = header,
            content = content,
            actions = actions,
        )
    }
}

/**
 * Elevated card variant for preferences.
 * Uses subtle shadow elevation (1dp) for depth without tonal changes.
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler for the entire card
 * @param enabled Whether the card is enabled
 * @param header Optional header composable shown at the top
 * @param content Main content of the card
 * @param actions Optional actions row shown at the bottom
 */
@Composable
fun PreferenceElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        elevation = PreferenceCardDefaults.elevatedCardElevation,
    ) {
        PreferenceCardContent(
            header = header,
            content = content,
            actions = actions,
        )
    }
}

/**
 * Outlined card variant for preferences.
 * Uses 1dp outline for clear boundaries without elevation.
 *
 * @param modifier Modifier for the card
 * @param onClick Optional click handler for the entire card
 * @param enabled Whether the card is enabled
 * @param header Optional header composable shown at the top
 * @param content Main content of the card
 * @param actions Optional actions row shown at the bottom
 */
@Composable
fun PreferenceOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        border = PreferenceCardDefaults.outlinedCardBorder,
    ) {
        PreferenceCardContent(
            header = header,
            content = content,
            actions = actions,
        )
    }
}

/**
 * Internal composable for card content layout.
 * Provides consistent spacing and structure across all card variants.
 */
@Composable
private fun PreferenceCardContent(
    header: (@Composable () -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PreferenceCardDefaults.contentPadding),
        verticalArrangement = Arrangement.spacedBy(PreferenceCardDefaults.headerSpacing),
    ) {
        // Optional header
        header?.let {
            it()
            Spacer(modifier = Modifier.height(PreferenceCardDefaults.headerSpacing))
        }

        // Main content
        content()

        // Optional actions
        actions?.let {
            Spacer(modifier = Modifier.height(PreferenceCardDefaults.actionsSpacing))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                it()
            }
        }
    }
}

/**
 * Simple surface card for preferences that don't need Material 3 card styling.
 * Uses basic surface color without elevation or borders.
 *
 * @param modifier Modifier for the surface
 * @param onClick Optional click handler
 * @param content Content of the card
 */
@Composable
fun PreferenceSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PreferenceCardDefaults.contentPadding),
        ) {
            content()
        }
    }
}
