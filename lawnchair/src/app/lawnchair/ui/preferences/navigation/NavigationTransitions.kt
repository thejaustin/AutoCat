package app.lawnchair.ui.preferences.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Enhanced navigation transitions for preference screens.
 * Provides smooth, Material Design-compliant animations.
 *
 * Remembers the slide distance for shared axis transitions.
 * Responsive to screen size (30dp default).
 */
@Composable
fun rememberSlideDistance(): Int {
    return with(LocalDensity.current) { 30.dp.roundToPx() }
}

/**
 * Fade through transition for entering screens.
 * Combines fade-in with subtle vertical slide.
 *
 * @param durationMillis Duration of the animation
 */
fun fadeThrough(
    durationMillis: Int = 300,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideInVertically(
        animationSpec = tween(durationMillis = durationMillis),
        initialOffsetY = { it / 20 },
    )
}

/**
 * Fade through transition for exiting screens.
 * Combines fade-out with subtle vertical slide.
 *
 * @param durationMillis Duration of the animation
 */
fun fadeThroughExit(
    durationMillis: Int = 200,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideOutVertically(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetY = { -it / 20 },
    )
}

/**
 * Shared axis X (horizontal) enter transition.
 * Used for forward navigation.
 *
 * @param slideDistance The distance to slide (in pixels)
 * @param durationMillis Duration of the animation
 */
fun sharedAxisXEnter(
    slideDistance: Int,
    durationMillis: Int = 300,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideInHorizontally(
        animationSpec = tween(durationMillis = durationMillis),
        initialOffsetX = { slideDistance },
    )
}

/**
 * Shared axis X (horizontal) exit transition.
 * Used for forward navigation.
 *
 * @param slideDistance The distance to slide (in pixels)
 * @param durationMillis Duration of the animation
 */
fun sharedAxisXExit(
    slideDistance: Int,
    durationMillis: Int = 200,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideOutHorizontally(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetX = { -slideDistance },
    )
}

/**
 * Shared axis X (horizontal) pop enter transition.
 * Used for back navigation.
 *
 * @param slideDistance The distance to slide (in pixels)
 * @param durationMillis Duration of the animation
 */
fun sharedAxisXPopEnter(
    slideDistance: Int,
    durationMillis: Int = 300,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideInHorizontally(
        animationSpec = tween(durationMillis = durationMillis),
        initialOffsetX = { -slideDistance },
    )
}

/**
 * Shared axis X (horizontal) pop exit transition.
 * Used for back navigation.
 *
 * @param slideDistance The distance to slide (in pixels)
 * @param durationMillis Duration of the animation
 */
fun sharedAxisXPopExit(
    slideDistance: Int,
    durationMillis: Int = 200,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideOutHorizontally(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetX = { slideDistance },
    )
}

/**
 * Container transform enter transition.
 * Used for hierarchical navigation (e.g., category to detail).
 * Creates a morphing effect.
 *
 * @param durationMillis Duration of the animation
 * @param initialScale Starting scale (default: 0.92f for subtle effect)
 */
fun containerTransformIn(
    durationMillis: Int = 300,
    initialScale: Float = 0.92f,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleIn(
        animationSpec = tween(durationMillis = durationMillis),
        initialScale = initialScale,
    )
}

/**
 * Container transform exit transition.
 * Used for hierarchical navigation.
 *
 * @param durationMillis Duration of the animation
 * @param targetScale Ending scale (default: 1.05f for expansion)
 */
fun containerTransformOut(
    durationMillis: Int = 200,
    targetScale: Float = 1.05f,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleOut(
        animationSpec = tween(durationMillis = durationMillis),
        targetScale = targetScale,
    )
}

/**
 * Container transform pop enter transition.
 * Used when returning from detail to category view.
 *
 * @param durationMillis Duration of the animation
 * @param initialScale Starting scale
 */
fun containerTransformPopIn(
    durationMillis: Int = 300,
    initialScale: Float = 1.05f,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleIn(
        animationSpec = tween(durationMillis = durationMillis),
        initialScale = initialScale,
    )
}

/**
 * Container transform pop exit transition.
 * Used when returning from detail to category view.
 *
 * @param durationMillis Duration of the animation
 * @param targetScale Ending scale
 */
fun containerTransformPopOut(
    durationMillis: Int = 200,
    targetScale: Float = 0.92f,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleOut(
        animationSpec = tween(durationMillis = durationMillis),
        targetScale = targetScale,
    )
}

/**
 * Elevation scale enter transition.
 * Creates a "lifting up" effect.
 *
 * @param durationMillis Duration of the animation
 */
fun elevationScaleEnter(
    durationMillis: Int = 300,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleIn(
        animationSpec = tween(durationMillis = durationMillis),
        initialScale = 0.95f,
    )
}

/**
 * Elevation scale exit transition.
 * Creates a "lowering down" effect.
 *
 * @param durationMillis Duration of the animation
 */
fun elevationScaleExit(
    durationMillis: Int = 200,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleOut(
        animationSpec = tween(durationMillis = durationMillis),
        targetScale = 0.95f,
    )
}

/**
 * Modal enter transition.
 * Used for modal dialogs and bottom sheets.
 *
 * @param durationMillis Duration of the animation
 */
fun modalEnter(
    durationMillis: Int = 300,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideInVertically(
        animationSpec = tween(durationMillis = durationMillis),
        initialOffsetY = { it / 2 },
    ) + scaleIn(
        animationSpec = tween(durationMillis = durationMillis),
        initialScale = 0.9f,
    )
}

/**
 * Modal exit transition.
 * Used for modal dialogs and bottom sheets.
 *
 * @param durationMillis Duration of the animation
 */
fun modalExit(
    durationMillis: Int = 200,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideOutVertically(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetY = { it / 2 },
    ) + scaleOut(
        animationSpec = tween(durationMillis = durationMillis),
        targetScale = 0.9f,
    )
}
