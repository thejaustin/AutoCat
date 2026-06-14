package app.lawnchair.ui.preferences.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Modifier that applies a spring-based scale animation when the composable is pressed.
 * Provides visual feedback for interactive elements.
 *
 * @param enabled Whether the animation is enabled
 * @param targetScale The target scale when pressed (default 0.95f)
 * @param onPress Callback invoked when press gesture is detected
 */
fun Modifier.pressableScale(
    enabled: Boolean = true,
    targetScale: Float = 0.95f,
    onPress: () -> Unit = {},
): Modifier = if (enabled) {
    composed {
        val scale = remember { Animatable(1f) }
        val coroutineScope = rememberCoroutineScope()

        this
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onPress()

                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = targetScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }

                    val up = waitForUpOrCancellation()

                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                }
            }
    }
} else {
    this
}

/**
 * Modifier that applies a press-and-hold scale animation.
 * Similar to pressableScale but with a more pronounced effect for long-press actions.
 *
 * @param enabled Whether the animation is enabled
 * @param pressedScale The scale when pressed (default 0.92f)
 * @param onPress Callback invoked when press starts
 * @param onRelease Callback invoked when press ends
 */
fun Modifier.pressAndHoldScale(
    enabled: Boolean = true,
    pressedScale: Float = 0.92f,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
): Modifier = if (enabled) {
    composed {
        val scale = remember { Animatable(1f) }
        val coroutineScope = rememberCoroutineScope()

        this
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onPress()

                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = pressedScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessHigh,
                            ),
                        )
                    }

                    waitForUpOrCancellation()
                    onRelease()

                    coroutineScope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                }
            }
    }
} else {
    this
}

/**
 * Modifier that applies a subtle bounce animation.
 * Useful for drawing attention to important UI elements.
 *
 * @param animatable The animatable float to control the bounce
 */
fun Modifier.bounce(
    animatable: Animatable<Float, AnimationVector1D>,
): Modifier = graphicsLayer {
    val bounceValue = animatable.value
    scaleX = 1f + bounceValue
    scaleY = 1f + bounceValue
}
