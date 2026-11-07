package app.lawnchair.ui.preferences.destinations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.twotone.Apps
import androidx.compose.material.icons.twotone.Backup
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.Gesture
import androidx.compose.material.icons.twotone.Notifications
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import com.android.launcher3.R

private enum class IconStyle { ROUNDED, OUTLINED, TWO_TONE }

/** Representative settings rows, each with the same icon in the three styles. */
private data class SampleRow(
    val title: String,
    val subtitle: String,
    val rounded: ImageVector,
    val outlined: ImageVector,
    val twoTone: ImageVector,
) {
    fun icon(style: IconStyle): ImageVector = when (style) {
        IconStyle.ROUNDED -> rounded
        IconStyle.OUTLINED -> outlined
        IconStyle.TWO_TONE -> twoTone
    }
}

private val sampleRows = listOf(
    SampleRow("Appearance", "Colors, theme and icons", Icons.Rounded.Palette, Icons.Outlined.Palette, Icons.TwoTone.Palette),
    SampleRow("App drawer", "Layout and categories", Icons.Rounded.Apps, Icons.Outlined.Apps, Icons.TwoTone.Apps),
    SampleRow("Gestures", "Swipes and taps", Icons.Rounded.Gesture, Icons.Outlined.Gesture, Icons.TwoTone.Gesture),
    SampleRow("Search", "Drawer and web search", Icons.Rounded.Search, Icons.Outlined.Search, Icons.TwoTone.Search),
    SampleRow("Notifications", "Dots and badges", Icons.Rounded.Notifications, Icons.Outlined.Notifications, Icons.TwoTone.Notifications),
    SampleRow("Dark mode", "Theme schedule", Icons.Rounded.DarkMode, Icons.Outlined.DarkMode, Icons.TwoTone.DarkMode),
    SampleRow("Backup", "Save and restore", Icons.Rounded.Backup, Icons.Outlined.Backup, Icons.TwoTone.Backup),
    SampleRow("Advanced", "Developer options", Icons.Rounded.Tune, Icons.Outlined.Tune, Icons.TwoTone.Tune),
)

@Composable
fun IconStylePreviewPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val styleAdapter = prefs.settingsIconStylePreview.getAdapter()
    val style = IconStyle.entries.getOrElse(styleAdapter.state.value) { IconStyle.ROUNDED }

    PreferenceLayout(
        label = stringResource(R.string.settings_icon_style_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceGroup(heading = stringResource(R.string.settings_icon_style_label)) {
            ListPreference(
                adapter = styleAdapter,
                entries = listOf(
                    ListPreferenceEntry(IconStyle.ROUNDED.ordinal) { stringResource(R.string.settings_icon_style_rounded) },
                    ListPreferenceEntry(IconStyle.OUTLINED.ordinal) { stringResource(R.string.settings_icon_style_outlined) },
                    ListPreferenceEntry(IconStyle.TWO_TONE.ordinal) { stringResource(R.string.settings_icon_style_twotone) },
                ),
                label = stringResource(R.string.settings_icon_style_label),
                description = stringResource(R.string.settings_icon_style_description),
            )
        }

        PreferenceGroup(heading = stringResource(R.string.preview_label)) {
            sampleRows.forEach { row ->
                PreferenceTemplate(
                    title = { Text(text = row.title) },
                    description = { Text(text = row.subtitle) },
                    startWidget = {
                        Icon(
                            imageVector = row.icon(style),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}
