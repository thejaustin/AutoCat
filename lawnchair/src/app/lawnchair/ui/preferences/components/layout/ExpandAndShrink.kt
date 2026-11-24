package app.lawnchair.ui.preferences.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Animates the appearance and disappearance of [content] via an expressive expanding and shrinking animation.
 * Uses M3E Emphasized spring motion.
 * @param visible Defines whether the content should be visible
 * @param content Content to appear or disappear based on the value of [visible]
 */
@Composable
fun ExpandAndShrink(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val motionScheme = MaterialTheme.motionScheme
    val spatial = motionScheme.defaultSpatialSpec<Float>()
    val effects = motionScheme.defaultEffectsSpec<Float>()
    val sizeSpec = motionScheme.defaultSpatialSpec<IntSize>()

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = sizeSpec,
            expandFrom = Alignment.Top,
        ) + fadeIn(effects) + scaleIn(spatial, initialScale = 0.95f),
        exit = shrinkVertically(
            animationSpec = sizeSpec,
            shrinkTowards = Alignment.Top,
        ) + fadeOut(effects) + scaleOut(spatial, targetScale = 0.95f),
        content = content,
        modifier = modifier,
    )
}
