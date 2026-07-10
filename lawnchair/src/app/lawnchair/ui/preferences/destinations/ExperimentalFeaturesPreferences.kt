package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.VerticalDivider
import app.lawnchair.ui.preferences.components.layout.DividerColumn
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.WallpaperAccessPermissionDialog
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.WarningPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.navigation.GeneralIconShape
import app.lawnchair.util.FileAccessManager
import app.lawnchair.util.FileAccessState
import app.lawnchair.util.isGestureNavContractCompatible
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.util.MSDLPlayerWrapper
import com.android.systemui.shared.system.BlurUtils
import com.google.android.msdl.data.model.MSDLToken

@Composable
fun ExperimentalFeaturesPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()

    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(LocalContext.current)
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

        PreferenceGroup {
            SwitchPreference(
                adapter = prefs2.enableFontSelection.getAdapter(),
                label = stringResource(id = R.string.font_picker_label),
                description = stringResource(id = R.string.font_picker_description),
                icon = Icons.Rounded.FontDownload,
            )
            SwitchPreference(
                adapter = prefs2.enableSmartspaceCalendarSelection.getAdapter(),
                label = stringResource(id = R.string.smartspace_calendar_label),
                description = stringResource(id = R.string.smartspace_calendar_description),
                icon = Icons.Rounded.CalendarMonth,
            )
            SwitchPreference(
                adapter = prefs.workspaceIncreaseMaxGridSize.getAdapter(),
                label = stringResource(id = R.string.workspace_increase_max_grid_size_label),
                description = stringResource(id = R.string.workspace_increase_max_grid_size_description),
                icon = Icons.Rounded.GridView,
            )
            SwitchPreference(
                adapter = prefs2.alwaysReloadIcons.getAdapter(),
                label = stringResource(id = R.string.always_reload_icons_label),
                description = stringResource(id = R.string.always_reload_icons_description),
                icon = Icons.Rounded.Refresh,
            )
            SwitchPreference(
                adapter = prefs2.iconSwipeGestures.getAdapter(),
                label = stringResource(R.string.icon_swipe_gestures),
                description = stringResource(R.string.icon_swipe_gestures_description),
                icon = Icons.Rounded.TouchApp,
            )
            SwitchPreference(
                adapter = prefs2.showDeckLayout.getAdapter(),
                label = stringResource(R.string.show_deck_layout),
                description = stringResource(R.string.show_deck_layout_description),
                icon = Icons.Rounded.ViewColumn,
            )
            SwitchPreference(
                adapter = prefs.enableWidgetStacks.getAdapter(),
                label = "Widget Stacks",
                description = "Allow stacking multiple widgets of the same size and swiping between them.",
                icon = Icons.Rounded.ViewStream,
            )
            SwitchPreference(
                adapter = prefs.autoCatDevMode.getAdapter(),
                label = "AutoCat Developer Mode",
                description = "Show detailed diagnostics and error logs in categorization screens",
                icon = Icons.Rounded.Code,
            )
            SwitchPreference(
                adapter = prefs.hideQuickstepSettings.getAdapter(),
                label = "Hide Quickstep Settings",
                description = "Hides the Quickstep settings entry from the main preferences dashboard.",
                icon = Icons.Rounded.VisibilityOff,
            )
            SwitchPreference(
                adapter = prefs.hideSettingsWarnings.getAdapter(),
                label = "Hide Settings Warnings",
                description = "Hides development build warnings and other informational messages at the top of settings.",
                icon = Icons.Rounded.NotificationsOff,
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
            ExpandAndShrink(visible = hasPermission && enableWallpaperBlur.state.value) {
                DividerColumn {
                    SliderPreference(
                        label = stringResource(id = R.string.wallpaper_background_blur),
                        adapter = prefs.wallpaperBlur.getAdapter(),
                        step = 5,
                        valueRange = 0..100,
                        showUnit = "%",
                    )
                    SliderPreference(
                        label = stringResource(id = R.string.wallpaper_background_blur_factor),
                        adapter = prefs.wallpaperBlurFactorThreshold.getAdapter(),
                        step = 1F,
                        valueRange = 0F..10F,
                    )
                }
            }
        }

        val folderIconShapeAdapter = prefs2.folderShape.getAdapter()
        val folderIconShapeSubtitle = iconShapeEntries(context)
            .firstOrNull { it.value == folderIconShapeAdapter.state.value }
            ?.label?.invoke()
            ?: stringResource(id = R.string.custom)

        PreferenceGroup(
            Modifier,
            stringResource(R.string.workspace_label),
        ) {
            Item {
                val getFolderIconShapeCustomizationAdapter = prefs2.enableFolderIconShapeCustomization.getAdapter()
                val enableFolderIconShapeCustomizationAdapter = remember(prefs2) {
                    getFolderIconShapeCustomizationAdapter
                }

                val folderShapeAdapter = prefs2.folderShape.getAdapter()
                val folderShapeDefault = prefs2.folderShape.defaultValue

                val enabled = enableFolderIconShapeCustomizationAdapter.state.value

                NavigationActionPreference(
                    label = stringResource(id = R.string.experimental_folder_shape_modify_label),
                    destination = if (enabled) GeneralIconShape(ShapeRoute.FOLDER_SHAPE) else null,
                    subtitle = folderIconShapeSubtitle,
                    endWidget = {
                        if (enabled) {
                            VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                mMSDLPlayerWrapper.playToken(if (it) MSDLToken.SWITCH_ON else MSDLToken.SWITCH_OFF)
                                enableFolderIconShapeCustomizationAdapter.onChange(it)
                                if (!it) {
                                    folderShapeAdapter.onChange(folderShapeDefault)
                                }
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    },
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

        val alwaysReloadIconsAdapter = prefs2.alwaysReloadIcons.getAdapter()
        val enableGncAdapter = prefs.enableGnc.getAdapter()

        PreferenceGroup(
            Modifier,
            stringResource(R.string.internal_label),
            stringResource(R.string.internal_description),
        ) {
            Item {
                SwitchPreference(
                    adapter = alwaysReloadIconsAdapter,
                    label = stringResource(id = R.string.always_reload_icons_label),
                    description = stringResource(id = R.string.always_reload_icons_description),
                )
            }
            Item(
                "always_reload_icons_warning",
                alwaysReloadIconsAdapter.state.value,
            ) {
                WarningPreference(stringResource(R.string.always_reload_icons_warning))
            }

            Item {
                SwitchPreference(
                    adapter = enableGncAdapter,
                    label = stringResource(id = R.string.gesturenavcontract_label),
                    description = stringResource(id = R.string.gesturenavcontract_description),
                    enabled = Utilities.ATLEAST_Q,
                )
            }
            Item(
                "gesturenavcontract_warning",
                enableGncAdapter.state.value && !isGestureNavContractCompatible,
            ) {
                WarningPreference(stringResource(R.string.gesturenavcontract_warning_incompatibility))
            }
        }
    }
}
