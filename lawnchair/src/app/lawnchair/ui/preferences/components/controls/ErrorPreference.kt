package app.lawnchair.ui.preferences.components.controls

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.theme.LawnchairTheme
import app.lawnchair.ui.util.preview.PreferenceGroupPreviewContainer
import app.lawnchair.ui.util.preview.PreviewLawnchair

/**
 * Error preference component with optional retry action.
 * Displays an error message with a distinct visual style.
 *
 * @param text The error message to display
 * @param modifier Modifier for the component
 * @param showRetry Whether to show a retry button
 * @param onRetry Callback when retry button is clicked
 */
@Composable
fun ErrorPreference(
    text: String,
    modifier: Modifier = Modifier,
    showRetry: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    PreferenceTemplate(
        modifier = modifier.clickable(enabled = showRetry && onRetry != null) {
            onRetry?.invoke()
        },
        title = {},
        description = {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        startWidget = {
            Icon(
                imageVector = Icons.Rounded.Error,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = "Error",
            )
        },
        endWidget = if (showRetry && onRetry != null) {
            {
                IconButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Retry",
                    )
                }
            }
        } else {
            null
        },
    )
}

@PreviewLawnchair
@Composable
private fun ErrorPreferencePreview() {
    AutoCatTheme {
        PreferenceGroupPreviewContainer {
            ErrorPreference(
                text = "Failed to load settings. Tap to retry.",
                showRetry = true,
                onRetry = {},
            )
        }
    }
}
