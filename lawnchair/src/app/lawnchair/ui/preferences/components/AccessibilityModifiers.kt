package app.lawnchair.ui.preferences.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities and modifiers for preference components.
 * Ensures WCAG compliance and proper touch target sizes.
 */
object AccessibilityConstants {
    /**
     * Minimum touch target size as per Material Design guidelines.
     * This ensures that interactive elements are easily tappable.
     */
    val MIN_TOUCH_TARGET_SIZE = 48.dp

    /**
     * Minimum size for icon-only buttons.
     */
    val MIN_ICON_BUTTON_SIZE = 48.dp

    /**
     * Standard icon size within a touch target.
     */
    val STANDARD_ICON_SIZE = 24.dp

    /**
     * Large icon size for prominent actions.
     */
    val LARGE_ICON_SIZE = 32.dp
}

/**
 * Ensures the composable meets minimum touch target size requirements.
 * Adds padding if necessary to reach the minimum size.
 *
 * @param minSize Minimum touch target size (default: 48.dp)
 */
fun Modifier.minimumTouchTargetSize(
    minSize: Dp = AccessibilityConstants.MIN_TOUCH_TARGET_SIZE,
): Modifier = this.then(
    Modifier.size(minSize),
)

/**
 * Adds semantic information for screen readers.
 *
 * @param label Content description for accessibility
 * @param role The semantic role of this element
 */
fun Modifier.accessibilityLabel(
    label: String,
    role: Role? = null,
): Modifier = this.semantics {
    contentDescription = label
    role?.let { this.role = it }
}

/**
 * Marks a composable as a clickable element for accessibility.
 */
fun Modifier.clickableAccessibility(
    label: String,
): Modifier = this.semantics {
    contentDescription = label
    role = Role.Button
}

/**
 * Marks a composable as a toggleable element for accessibility.
 */
fun Modifier.toggleableAccessibility(
    label: String,
): Modifier = this.semantics {
    contentDescription = label
    role = Role.Switch
}
