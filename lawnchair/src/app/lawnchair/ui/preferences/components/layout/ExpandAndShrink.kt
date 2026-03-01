package app.lawnchair.ui.preferences.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
    val emphasizedSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
    )

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            expandFrom = Alignment.Top,
        ) + fadeIn(emphasizedSpring) + scaleIn(emphasizedSpring, initialScale = 0.95f),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            shrinkTowards = Alignment.Top,
        ) + fadeOut(emphasizedSpring) + scaleOut(emphasizedSpring, targetScale = 0.95f),
        content = content,
        modifier = modifier,
    )
}
