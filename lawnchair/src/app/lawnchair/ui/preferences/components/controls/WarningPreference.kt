package app.lawnchair.ui.preferences.components.controls

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.theme.AutoCatTheme
import app.lawnchair.ui.util.preview.PreferenceGroupPreviewContainer
import app.lawnchair.ui.util.preview.PreviewAutoCat

@Composable
fun WarningPreference(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    PreferenceTemplate(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        title = {},
        description = {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.error,
            )
        },
        startWidget = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = null,
            )
        },
        endWidget = if (onDismiss != null) {
            {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = "Dismiss",
                    )
                }
            }
        } else {
            null
        },
    )
}

@PreviewAutoCat
@Composable
private fun WarningPreferencePreview() {
    AutoCatTheme {
        PreferenceGroupPreviewContainer {
            Item {
                WarningPreference(
                    text = "Text",
                )
            }
        }
    }
}
