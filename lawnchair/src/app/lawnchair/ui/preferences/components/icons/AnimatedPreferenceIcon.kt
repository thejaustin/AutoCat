package app.lawnchair.ui.preferences.components.icons

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icon that animates between two states with crossfade.
 *
 * @param isActive Whether the active state icon should be shown
 * @param activeIcon Icon shown when active
 * @param inactiveIcon Icon shown when inactive
 * @param modifier Modifier for the icon
 * @param tint Icon tint color
 * @param size Icon size
 */
@Composable
fun AnimatedStateIcon(
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp,
) {
    Crossfade(
        targetState = isActive,
        animationSpec = tween(durationMillis = 200),
        label = "icon state crossfade",
        modifier = modifier,
    ) { active ->
        Icon(
            imageVector = if (active) activeIcon else inactiveIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Icon with pulse animation to draw attention.
 *
 * @param icon The icon to display
 * @param modifier Modifier for the icon
 * @param tint Icon tint color
 * @param size Icon size
 * @param shouldPulse Whether the pulse animation is active
 */
@Composable
fun PulseIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp,
    shouldPulse: Boolean = false,
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(shouldPulse) {
        if (shouldPulse) {
            while (true) {
                scale.animateTo(
                    targetValue = 1.1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        } else {
            scale.snapTo(1f)
        }
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

/**
 * Icon that rotates based on state.
 *
 * @param icon The icon to display
 * @param rotation Rotation angle in degrees
 * @param modifier Modifier for the icon
 * @param tint Icon tint color
 * @param size Icon size
 */
@Composable
fun RotatingIcon(
    icon: ImageVector,
    rotation: Float,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp,
) {
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = 300),
        label = "icon rotation",
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = animatedRotation
            },
    )
}

/**
 * Icon that fades in/out based on visibility.
 *
 * @param icon The icon to display
 * @param isVisible Whether the icon is visible
 * @param modifier Modifier for the icon
 * @param tint Icon tint color
 * @param size Icon size
 */
@Composable
fun FadeIcon(
    icon: ImageVector,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 24.dp,
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "icon alpha",
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.alpha = alpha
            },
    )
}

/**
 * Collection of common animated icon patterns.
 */
object AnimatedIcons {
    /**
     * Visibility toggle icon (eye open/closed).
     */
    @Composable
    fun Visibility(
        isVisible: Boolean,
        modifier: Modifier = Modifier,
        tint: Color = MaterialTheme.colorScheme.primary,
        size: Dp = 24.dp,
    ) {
        AnimatedStateIcon(
            isActive = isVisible,
            activeIcon = Icons.Rounded.Visibility,
            inactiveIcon = Icons.Rounded.VisibilityOff,
            modifier = modifier,
            tint = tint,
            size = size,
        )
    }

    /**
     * Success/Error state icon.
     */
    @Composable
    fun StatusIcon(
        isSuccess: Boolean,
        modifier: Modifier = Modifier,
        successTint: Color = MaterialTheme.colorScheme.primary,
        errorTint: Color = MaterialTheme.colorScheme.error,
        size: Dp = 24.dp,
    ) {
        Crossfade(
            targetState = isSuccess,
            animationSpec = tween(durationMillis = 200),
            label = "status icon",
            modifier = modifier,
        ) { success ->
            Icon(
                imageVector = if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                contentDescription = null,
                tint = if (success) successTint else errorTint,
                modifier = Modifier.size(size),
            )
        }
    }

    /**
     * Theme mode icon (light/dark).
     */
    @Composable
    fun ThemeMode(
        isDark: Boolean,
        modifier: Modifier = Modifier,
        tint: Color = MaterialTheme.colorScheme.primary,
        size: Dp = 24.dp,
    ) {
        AnimatedStateIcon(
            isActive = isDark,
            activeIcon = PreferenceIcons.DarkMode,
            inactiveIcon = PreferenceIcons.LightMode,
            modifier = modifier,
            tint = tint,
            size = size,
        )
    }
}
