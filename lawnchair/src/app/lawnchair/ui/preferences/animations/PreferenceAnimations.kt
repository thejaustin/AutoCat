package app.lawnchair.ui.preferences.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Centralized animation specifications for preference screens.
 * Provides consistent timing, easing, and animation patterns across all settings.
 */
object PreferenceAnimationDefaults {
    // Entry/Exit transition durations
    const val ENTER_DURATION = 300
    const val EXIT_DURATION = 200

    // Interaction feedback durations
    const val PRESS_DURATION = 100
    const val RIPPLE_DURATION = 350

    // Content reveal durations
    const val STAGGER_DELAY = 50 // Delay per item in staggered lists
    const val REVEAL_DURATION = 400

    // Expand/Collapse durations
    const val EXPAND_DURATION = 300
    const val COLLAPSE_DURATION = 250

    // Container transform durations
    const val CONTAINER_TRANSFORM_DURATION = 300

    // Spring animation specifications
    val SPRING_PRESS = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val SPRING_BOUNCE = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    val SPRING_STIFF = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    val SPRING_SMOOTH = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
}

/**
 * Creates a fade-through enter transition with vertical slide.
 * Used when navigating forward to a new preference screen.
 */
@Composable
fun preferenceEnterTransition(
    durationMillis: Int = PreferenceAnimationDefaults.ENTER_DURATION,
): EnterTransition {
    val slideDistance = with(LocalDensity.current) { 30.dp.roundToPx() }

    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideInVertically(
        animationSpec = tween(durationMillis = durationMillis),
        initialOffsetY = { slideDistance / 2 },
    )
}

/**
 * Creates a fade-through exit transition with vertical slide.
 * Used when navigating away from current preference screen.
 */
@Composable
fun preferenceExitTransition(
    durationMillis: Int = PreferenceAnimationDefaults.EXIT_DURATION,
): ExitTransition {
    val slideDistance = with(LocalDensity.current) { 30.dp.roundToPx() }

    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + slideOutVertically(
        animationSpec = tween(durationMillis = durationMillis),
        targetOffsetY = { -slideDistance / 2 },
    )
}

/**
 * Creates a container transform enter transition.
 * Used for hierarchical navigation (e.g., category card to detail screen).
 */
fun containerTransformEnter(
    durationMillis: Int = PreferenceAnimationDefaults.CONTAINER_TRANSFORM_DURATION,
): EnterTransition {
    return fadeIn(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleIn(
        animationSpec = tween(durationMillis = durationMillis),
        initialScale = 0.92f,
    )
}

/**
 * Creates a container transform exit transition.
 * Used for hierarchical navigation (e.g., category card to detail screen).
 */
fun containerTransformExit(
    durationMillis: Int = PreferenceAnimationDefaults.EXIT_DURATION,
): ExitTransition {
    return fadeOut(
        animationSpec = tween(durationMillis = durationMillis),
    ) + scaleOut(
        animationSpec = tween(durationMillis = durationMillis),
        targetScale = 1.05f,
    )
}

/**
 * Creates a staggered animation spec for list items.
 * Each item gets a progressively delayed animation based on its index.
 *
 * @param index The index of the item in the list (0-based)
 * @param maxItems Maximum number of items to stagger (items beyond this get no delay)
 * @return Animation spec with appropriate delay
 */
fun staggeredListAnimation(
    index: Int,
    maxItems: Int = 10,
): AnimationSpec<Float> {
    val cappedIndex = index.coerceAtMost(maxItems)
    val delay = cappedIndex * PreferenceAnimationDefaults.STAGGER_DELAY

    return tween(
        durationMillis = PreferenceAnimationDefaults.REVEAL_DURATION,
        delayMillis = delay,
    )
}

/**
 * Creates a container transform animation spec.
 * Used for smooth morphing between states.
 */
fun containerTransformSpec(): FiniteAnimationSpec<Float> {
    return tween(
        durationMillis = PreferenceAnimationDefaults.CONTAINER_TRANSFORM_DURATION,
    )
}

/**
 * Creates an expand animation spec.
 * Used for collapsible sections and expandable content.
 */
fun expandAnimationSpec(): FiniteAnimationSpec<Float> {
    return tween(
        durationMillis = PreferenceAnimationDefaults.EXPAND_DURATION,
    )
}

/**
 * Creates a collapse animation spec.
 * Used for collapsible sections and expandable content.
 */
fun collapseAnimationSpec(): FiniteAnimationSpec<Float> {
    return tween(
        durationMillis = PreferenceAnimationDefaults.COLLAPSE_DURATION,
    )
}
