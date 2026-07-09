package app.lawnchair.ui.theme.m3e

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButtonDefaults
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideExpressiveShapes
import androidx.compose.material3.ProvideExpressiveTypography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.lawnchair.ui.theme.AutoCatTheme

/**
 * Material 3 Expressive theme wrapper for AutoCat.
 *
 * Provides consistent M3E styling across all settings screens:
 * - Expressive shapes (extra-small through extra-large)
 * - M3E typography scale
 * - Dynamic color integration
 * - Motion preferences respect
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MySettingsScreen() {
 *     M3ETheme {
 *         // Your settings UI with M3E styling
 *         ClickablePreference(
 *             label = "Example",
 *             icon = Icons.Rounded.Settings
 *         )
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3ETheme(
    content: @Composable () -> Unit,
) {
    AutoCatTheme {
        content()
    }
}

/**
 * Haptic feedback helper for M3E interactions.
 *
 * Provides consistent haptic feedback across all interactive elements
 * while respecting accessibility settings.
 *
 * Features:
 * - Respects system haptic feedback settings
 * - Different patterns for different interactions
 * - Accessible fallbacks
 *
 * Usage:
 * ```kotlin
 * val haptics = rememberExpressiveHaptics()
 *
 * Button(
 *     onClick = {
 *         haptics.click()
 *         // Handle click
 *     }
 * ) {
 *     Text("Click me")
 * }
 * ```
 */
class ExpressiveHaptics(
    private val view: View,
    private val context: Context,
) {

    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    /**
     * Check if haptic feedback is enabled.
     */
    private val isHapticEnabled: Boolean
        get() = !accessibilityManager.isTouchExplorationEnabled &&
            view.isHapticFeedbackEnabled

    /**
     * Perform click haptic feedback.
     * Light tap for button clicks and toggles.
     */
    fun click() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /**
     * Perform selection haptic feedback.
     * Medium tap for list item selection.
     */
    fun select() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Perform state change haptic feedback.
     * Distinct feedback for switches and checkboxes.
     */
    fun stateChange() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    /**
     * Perform scroll haptic feedback.
     * Subtle feedback for scroll boundaries.
     */
    fun scrollBoundary() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        }
    }
}

/**
 * Remember haptic feedback helper.
 */
@Composable
fun rememberExpressiveHaptics(): ExpressiveHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    return remember(context, view) {
        ExpressiveHaptics(view, context)
    }
}

/**
 * M3E-style clickable wrapper with haptics and indication.
 *
 * Usage:
 * ```kotlin
 * M3EClickable(
 *     onClick = { /* handle click */ },
 *     label = "Settings"
 * ) {
 *     Text("Open Settings")
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun M3EClickable(
    onClick: () -> Unit,
    label: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val haptics = rememberExpressiveHaptics()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptics.click()
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
    ) {
        content()
    }
}

/**
 * M3E expressive spacing values.
 */
object M3ESpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
    val huge = 48.dp
}

/**
 * M3E expressive corner radii.
 */
object M3ECorners {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val full = 9999.dp
}
