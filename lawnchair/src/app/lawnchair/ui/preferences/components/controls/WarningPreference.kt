package app.lawnchair.ui.preferences.components.controls

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.components.layout.ProvideDescriptionTextStyle
import app.lawnchair.ui.theme.AutoCatTheme
import app.lawnchair.ui.util.preview.PreferenceGroupPreviewContainer
import app.lawnchair.ui.util.preview.PreviewAutoCat
import com.android.launcher3.R

@Composable
fun WarningPreference(
    text: String,
    modifier: Modifier = Modifier,
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    iconTint: Color = MaterialTheme.colorScheme.error,
    textColor: Color = MaterialTheme.colorScheme.error,
) {
    WarningPreference(
        text = text,
        modifier = modifier,
        standalone = false,
        colors = colors,
        iconTint = iconTint,
        textColor = textColor,
    )
}

@Composable
fun WarningPreference(
    text: String,
    modifier: Modifier = Modifier,
    standalone: Boolean = true,
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    iconTint: Color = MaterialTheme.colorScheme.error,
    textColor: Color = MaterialTheme.colorScheme.error,
    onClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    PreferenceTemplate(
        modifier = modifier,
        onClick = onClick,
        endWidget = onDismiss?.let {
            {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        tint = iconTint,
                        contentDescription = stringResource(id = R.string.dismiss),
                    )
                }
            }
        },
        title = {
            ProvideDescriptionTextStyle {
                Text(
                    text = text,
                    color = textColor,
                )
            }
        },
        startWidget = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                tint = iconTint,
                contentDescription = null,
            )
        },
        shapes = if (standalone) {
            ListItemShapes(
                shape = MaterialTheme.shapes.large,
                selectedShape = MaterialTheme.shapes.large,
                pressedShape = MaterialTheme.shapes.large,
                focusedShape = MaterialTheme.shapes.large,
                hoveredShape = MaterialTheme.shapes.large,
                draggedShape = MaterialTheme.shapes.large,
            )
        } else {
            ListItemDefaults.shapes()
        },
        colors = colors,
    )
}

@PreviewAutoCat
@Composable
private fun WarningPreferencePreview() {
    AutoCatTheme {
        PreferenceGroupPreviewContainer {
            WarningPreference(
                text = "Text",
            )
        }
    }
}

@PreviewAutoCat
@Composable
private fun WarningPreferenceStandalonePreview() {
    AutoCatTheme {
        WarningPreference(
            text = "Text",
            standalone = true,
        )
    }
}
