package app.lawnchair.ui.preferences.destinations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.WallpaperAccessPermissionDialog
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.WarningPreference
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.navigation.ExperimentalIconStylePreview
import app.lawnchair.util.FileAccessManager
import app.lawnchair.util.FileAccessState
import app.lawnchair.util.isGestureNavContractCompatible
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.google.android.msdl.data.model.FeedbackLevel

@Composable
fun ExperimentalFeaturesPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()

    PreferenceLayout(
        label = stringResource(id = R.string.experimental_features_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        val enableWallpaperBlur = prefs.enableWallpaperBlur.getAdapter()
        val context = LocalContext.current
        val fileAccessManager = remember { FileAccessManager.getInstance(context) }
        val allFilesAccessState by fileAccessManager.allFilesAccessState.collectAsStateWithLifecycle()
        val wallpaperAccessState by fileAccessManager.wallpaperAccessState.collectAsStateWithLifecycle()
        val hasPermission = wallpaperAccessState != FileAccessState.Denied
        var showPermissionDialog by remember { mutableStateOf(false) }

        val folderIconShapeAdapter = prefs2.folderShape.getAdapter()
        val folderIconShapeSubtitle = iconShapeEntries(context)
            .firstOrNull { it.value == folderIconShapeAdapter.state.value }
            ?.label?.invoke()
            ?: stringResource(id = R.string.custom)

        PreferenceGroup(
            modifier = Modifier,
            heading = stringResource(R.string.workspace_label),
        ) {
            SwitchPreference(
                adapter = prefs2.enableFontSelection.getAdapter(),
                label = stringResource(id = R.string.font_picker_label),
                icon = Icons.Rounded.FontDownload,
                description = stringResource(id = R.string.font_picker_description),
            )
            SwitchPreference(
                adapter = prefs.workspaceIncreaseMaxGridSize.getAdapter(),
                label = stringResource(id = R.string.workspace_increase_max_grid_size_label),
                icon = Icons.Rounded.GridView,
                description = stringResource(id = R.string.workspace_increase_max_grid_size_description),
            )
            SwitchPreference(
                adapter = prefs2.showDeckLayout.getAdapter(),
                label = stringResource(R.string.show_deck_layout),
                icon = Icons.Rounded.ViewColumn,
                description = stringResource(R.string.show_deck_layout_description),
            )
            // AutoCat-specific experiments
            SwitchPreference(
                adapter = prefs2.enableSmartspaceCalendarSelection.getAdapter(),
                label = stringResource(id = R.string.smartspace_calendar_label),
                icon = Icons.Rounded.CalendarMonth,
                description = stringResource(id = R.string.smartspace_calendar_description),
            )
            SwitchPreference(
                adapter = prefs.enableWidgetStacks.getAdapter(),
                label = "Widget Stacks",
                icon = Icons.Rounded.ViewStream,
                description = "Allow stacking multiple widgets of the same size and swiping between them.",
            )
            SwitchPreference(
                adapter = prefs.autoCatDevMode.getAdapter(),
                label = "AutoCat Developer Mode",
                icon = Icons.Rounded.Code,
                description = "Show detailed diagnostics and error logs in categorization screens",
            )
            SwitchPreference(
                adapter = prefs.hideQuickstepSettings.getAdapter(),
                label = "Hide Quickstep Settings",
                icon = Icons.Rounded.VisibilityOff,
                description = "Hides the Quickstep settings entry from the main preferences dashboard.",
            )
            SwitchPreference(
                adapter = prefs.hideSettingsWarnings.getAdapter(),
                label = "Hide Settings Warnings",
                icon = Icons.Rounded.NotificationsOff,
                description = "Hides development build warnings and other informational messages at the top of settings.",
            )
            SliderPreference(
                label = "Settings Card Roundness",
                adapter = prefs.settingsCardRadius.getAdapter(),
                step = 2,
                valueRange = 0..48,
                showUnit = "dp",
            )
            SwitchPreference(
                checked = hasPermission && enableWallpaperBlur.state.value,
                onCheckedChange = {
                    if (!hasPermission) {
                        showPermissionDialog = true
                    } else {
                        enableWallpaperBlur.onChange(it)
                    }
                },
                label = stringResource(id = R.string.wallpaper_blur),
                icon = Icons.Rounded.BlurOn,
            )

            val canBlur = hasPermission && enableWallpaperBlur.state.value
            ExpandAndShrink(visible = canBlur) {
                SliderPreference(
                    label = stringResource(id = R.string.wallpaper_background_blur),
                    adapter = prefs.wallpaperBlur.getAdapter(),
                    step = 5,
                    valueRange = 0..100,
                    showUnit = "%",
                )
            }
            ExpandAndShrink(visible = canBlur) {
                SliderPreference(
                    label = stringResource(id = R.string.wallpaper_background_blur_factor),
                    adapter = prefs.wallpaperBlurFactorThreshold.getAdapter(),
                    step = 1F,
                    valueRange = 0F..10F,
                )
            }
        }
        if (showPermissionDialog) {
            WallpaperAccessPermissionDialog(
                managedFilesChecked = allFilesAccessState != FileAccessState.Denied,
                onDismiss = {
                    showPermissionDialog = false
                },
                onPermissionRequest = { fileAccessManager.refresh() },
            )
        }
        LifecycleResumeEffect(Unit) {
            showPermissionDialog = false
            fileAccessManager.refresh()
            onPauseOrDispose { }
        }

        val enableGncAdapter = prefs.enableGnc.getAdapter()
        val vibrationFeedbackLevelAdapter = prefs.vibrationFeedbackLevel.getAdapter()

        PreferenceGroup(
            modifier = Modifier,
            heading = stringResource(R.string.internal_label),
            description = stringResource(R.string.internal_description),
        ) {
            NavigationActionPreference(
                label = stringResource(R.string.settings_icon_style_label),
                destination = ExperimentalIconStylePreview,
                subtitle = stringResource(R.string.settings_icon_style_description),
            )
            SwitchPreference(
                adapter = enableGncAdapter,
                label = stringResource(id = R.string.gesturenavcontract_label),
                description = stringResource(id = R.string.gesturenavcontract_description),
                enabled = Utilities.ATLEAST_Q,
            )
            ExpandAndShrink(visible = enableGncAdapter.state.value && !isGestureNavContractCompatible) {
                WarningPreference(stringResource(R.string.gesturenavcontract_warning_incompatibility))
            }
            ListPreference(
                adapter = vibrationFeedbackLevelAdapter,
                entries = listOf(
                    ListPreferenceEntry(FeedbackLevel.NO_FEEDBACK.ordinal) {
                        stringResource(R.string.vibration_feedback_no_feedback_choice)
                    },
                    ListPreferenceEntry(FeedbackLevel.MINIMAL.ordinal) {
                        stringResource(R.string.vibration_feedback_minimal_choice)
                    },
                    ListPreferenceEntry(FeedbackLevel.DEFAULT.ordinal) {
                        stringResource(R.string.vibration_feedback_default_choice)
                    },
                    ListPreferenceEntry(FeedbackLevel.EXPRESSIVE.ordinal) {
                        stringResource(R.string.vibration_feedback_expressive_choice)
                    },
                ),
                label = stringResource(R.string.vibration_feedback_level_label),
            )
        }
    }
}
