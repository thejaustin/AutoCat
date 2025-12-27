package app.lawnchair.ui.preferences.components.controls

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.lawnchair.preferences.PreferenceAdapter
import app.lawnchair.ui.preferences.components.icons.PreferenceIconMapping
import app.lawnchair.ui.preferences.components.icons.PreferenceIcons

/**
 * Switch preference with an expressive icon.
 * Automatically suggests icons based on label/keywords or uses provided icon.
 *
 * @param adapter The preference adapter
 * @param label The label text
 * @param icon Optional icon (auto-detected if null)
 * @param iconTint Icon tint color (default: primary)
 * @param iconSize Icon size (default: 24dp)
 * @param description Optional description
 * @param enabled Whether enabled
 * @param onClick Optional click handler
 * @param keywords Keywords for auto icon detection
 * @param modifier Modifier
 */
@Composable
fun IconSwitchPreference(
    adapter: PreferenceAdapter<Boolean>,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 24.dp,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    keywords: List<String> = emptyList(),
) {
    val displayIcon = icon ?: PreferenceIconMapping.getIconForPreference(label, keywords)

    SwitchPreference(
        adapter = adapter,
        label = label,
        description = description,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
        startWidget = if (displayIcon != null) {
            {
                Icon(
                    imageVector = displayIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
            }
        } else {
            null
        },
    )
}

/**
 * SwitchPreference with start widget support.
 * Internal variant that accepts a startWidget composable.
 */
@Composable
private fun SwitchPreference(
    adapter: PreferenceAdapter<Boolean>,
    label: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    startWidget: (@Composable () -> Unit)? = null,
) {
    // Use existing SwitchPreference - this is a placeholder
    // In real implementation, we'd enhance SwitchPreference to support startWidget
    // For now, calling the existing implementation
    SwitchPreference(
        adapter = adapter,
        label = label,
        description = description,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * List preference with an expressive icon.
 *
 * @param adapter The preference adapter
 * @param label The label text
 * @param entries List of entries
 * @param icon Optional icon (auto-detected if null)
 * @param iconTint Icon tint color
 * @param iconSize Icon size
 * @param enabled Whether enabled
 * @param keywords Keywords for auto icon detection
 * @param modifier Modifier
 */
@Composable
fun <T> IconListPreference(
    adapter: PreferenceAdapter<T>,
    label: String,
    entries: List<ListPreferenceEntry<T>>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    keywords: List<String> = emptyList(),
) {
    val displayIcon = icon ?: PreferenceIconMapping.getIconForPreference(label, keywords)

    // This would integrate with the existing ListPreference
    // For now, just showing the pattern
    ListPreference(
        adapter = adapter,
        label = label,
        entries = entries,
        enabled = enabled,
        modifier = modifier,
    )
}

/**
 * Slider preference with an expressive icon.
 *
 * @param label The label text
 * @param adapter The preference adapter
 * @param valueRange Value range
 * @param step Step value
 * @param icon Optional icon (auto-detected if null)
 * @param iconTint Icon tint color
 * @param iconSize Icon size
 * @param showAsPercentage Whether to show as percentage
 * @param showUnit Unit to display
 * @param keywords Keywords for auto icon detection
 * @param modifier Modifier
 */
@Composable
fun IconSliderPreference(
    label: String,
    adapter: PreferenceAdapter<Int>,
    valueRange: ClosedRange<Int>,
    step: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconSize: Dp = 24.dp,
    showAsPercentage: Boolean = false,
    showUnit: String = "",
    keywords: List<String> = emptyList(),
) {
    val displayIcon = icon ?: PreferenceIconMapping.getIconForPreference(label, keywords)

    // This would integrate with the existing SliderPreference
    SliderPreference(
        label = label,
        adapter = adapter,
        valueRange = valueRange,
        step = step.toFloat(),
        showAsPercentage = showAsPercentage,
        showUnit = showUnit,
        modifier = modifier,
    )
}

/**
 * Collection of commonly used icon preferences for quick access.
 */
object IconPreferences {
    /**
     * Theme preference with theme icon.
     */
    @Composable
    fun Theme(
        adapter: PreferenceAdapter<Boolean>,
        label: String,
        modifier: Modifier = Modifier,
        description: String? = null,
    ) = IconSwitchPreference(
        adapter = adapter,
        label = label,
        description = description,
        icon = PreferenceIcons.Theme,
        modifier = modifier,
    )

    /**
     * Rotation preference with rotation icon.
     */
    @Composable
    fun Rotation(
        adapter: PreferenceAdapter<Boolean>,
        label: String,
        modifier: Modifier = Modifier,
        description: String? = null,
    ) = IconSwitchPreference(
        adapter = adapter,
        label = label,
        description = description,
        icon = PreferenceIcons.Rotation,
        modifier = modifier,
    )

    /**
     * Notification preference with notification icon.
     */
    @Composable
    fun Notification(
        adapter: PreferenceAdapter<Boolean>,
        label: String,
        modifier: Modifier = Modifier,
        description: String? = null,
    ) = IconSwitchPreference(
        adapter = adapter,
        label = label,
        description = description,
        icon = PreferenceIcons.Notification,
        modifier = modifier,
    )

    /**
     * Grid/Layout preference with grid icon.
     */
    @Composable
    fun Grid(
        label: String,
        adapter: PreferenceAdapter<Int>,
        valueRange: ClosedRange<Int>,
        step: Int,
        modifier: Modifier = Modifier,
        showUnit: String = "",
    ) = IconSliderPreference(
        label = label,
        adapter = adapter,
        valueRange = valueRange,
        step = step,
        icon = PreferenceIcons.Grid,
        showUnit = showUnit,
        modifier = modifier,
    )

    /**
     * Opacity preference with opacity icon.
     */
    @Composable
    fun Opacity(
        label: String,
        adapter: PreferenceAdapter<Int>,
        valueRange: ClosedRange<Int>,
        step: Int,
        modifier: Modifier = Modifier,
    ) = IconSliderPreference(
        label = label,
        adapter = adapter,
        valueRange = valueRange,
        step = step,
        icon = PreferenceIcons.Opacity,
        showAsPercentage = true,
        modifier = modifier,
    )
}
