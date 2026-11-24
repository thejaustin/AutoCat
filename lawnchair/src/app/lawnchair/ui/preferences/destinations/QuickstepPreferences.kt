package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.lawnchair.AutoCatApp
import app.lawnchair.backend.Backend
import app.lawnchair.backend.BackendMode
import app.lawnchair.backend.BackendSelector
import app.lawnchair.backend.BackendStatus
import app.lawnchair.backend.Capability
import app.lawnchair.preferences.PreferenceAdapter
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.observeAsState
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.preferences.components.QuickActionsPreferences
import app.lawnchair.ui.preferences.components.RecentsQuickAction
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.WarningPreference
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.theme.AutoCatTheme
import app.lawnchair.ui.util.preview.PreviewAutoCat
import app.lawnchair.util.isOnePlusStock
import com.android.launcher3.R
import com.android.launcher3.Utilities

@Composable
fun QuickstepPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val context = LocalContext.current
    val lensAvailable = remember {
        context.packageManager.getLaunchIntentForPackage("com.google.ar.lens") != null
    }

    val recentActionsPreferences: List<RecentsQuickAction> = listOfNotNull(
        RecentsQuickAction(
            id = 0,
            adapter = prefs.recentsActionScreenshot.getAdapter(),
            label = stringResource(id = R.string.action_screenshot),
        ).takeIf { !isOnePlusStock },
        RecentsQuickAction(
            id = 1,
            adapter = prefs.recentsActionShare.getAdapter(),
            label = stringResource(id = R.string.action_share),
        ),
        RecentsQuickAction(
            id = 2,
            adapter = prefs.recentsActionLens.getAdapter(),
            label = stringResource(id = R.string.action_lens),
        ).takeIf { lensAvailable },
        RecentsQuickAction(
            id = 3,
            adapter = prefs.recentsActionLocked.getAdapter(),
            label = stringResource(id = R.string.recents_lock_unlock),
            description = stringResource(id = R.string.recents_lock_unlock_description),
        ),
        RecentsQuickAction(
            id = 4,
            adapter = prefs.recentsActionClearAll.getAdapter(),
            label = stringResource(id = R.string.recents_clear_all),
        ),
    )

    PreferenceLayout(
        label = stringResource(id = R.string.quickstep_label),
        modifier = modifier,
    ) {
        if (!AutoCatApp.isRecentsEnabled) QuickSwitchIgnoredWarning()
        val recentsTranslucentBackground by prefs.recentsTranslucentBackground.observeAsState()
        PreferenceGroup(heading = stringResource(id = R.string.switcher_group_label)) {
            ListPreference(
                adapter = prefs.switcherLayout.getAdapter(),
                entries = listOf(
                    ListPreferenceEntry("carousel") { stringResource(R.string.switcher_layout_carousel) },
                    ListPreferenceEntry("grid") { stringResource(R.string.switcher_layout_grid) },
                ),
                label = stringResource(R.string.switcher_layout),
            )
            ListPreference(
                adapter = prefs.switcherGridRows.getAdapter(),
                entries = listOf(
                    ListPreferenceEntry("2") { stringResource(R.string.switcher_grid_rows_2) },
                    ListPreferenceEntry("3") { stringResource(R.string.switcher_grid_rows_3) },
                ),
                label = stringResource(R.string.switcher_grid_rows),
            )
            ListPreference(
                adapter = prefs.switcherBlur.getAdapter(),
                entries = listOf(
                    ListPreferenceEntry("none") { stringResource(R.string.switcher_blur_none) },
                    ListPreferenceEntry("soft") { stringResource(R.string.switcher_blur_soft) },
                    ListPreferenceEntry("strong") { stringResource(R.string.switcher_blur_strong) },
                ),
                label = stringResource(R.string.switcher_blur),
            )
            SwitchPreference(
                adapter = prefs.switcherMiniMode.getAdapter(),
                label = stringResource(R.string.switcher_mini_mode),
                description = stringResource(R.string.switcher_mini_mode_summary),
            )
            SwitchPreference(
                adapter = prefs.switcherShowLabel.getAdapter(),
                label = stringResource(R.string.switcher_show_label),
                description = stringResource(R.string.switcher_show_label_summary),
            )
            SwitchPreference(
                adapter = prefs.switcherGridCloseAll.getAdapter(),
                label = stringResource(R.string.switcher_grid_close_all),
                description = stringResource(R.string.switcher_grid_close_all_summary),
            )
            SwitchPreference(
                adapter = prefs.switcherSwapPills.getAdapter(),
                label = stringResource(R.string.switcher_swap_pills),
                description = stringResource(R.string.switcher_swap_pills_summary),
            )
            SwitchPreference(
                adapter = prefs.switcherActiveAppsPill.getAdapter(),
                label = stringResource(R.string.switcher_active_apps_pill),
                description = stringResource(R.string.switcher_active_apps_pill_summary),
            )
            SwitchPreference(
                adapter = prefs.switcherCloseAllPill.getAdapter(),
                label = stringResource(R.string.switcher_close_all_pill),
                description = stringResource(R.string.switcher_close_all_pill_summary),
            )
            ListPreference(
                adapter = prefs.switcherHeaderStyle.getAdapter(),
                entries = listOf(
                    ListPreferenceEntry("pixel") { stringResource(R.string.switcher_header_pixel) },
                    ListPreferenceEntry("oneui") { stringResource(R.string.switcher_header_oneui) },
                ),
                label = stringResource(R.string.switcher_header_style),
            )
            ListPreference(
                adapter = prefs.switcherLongPress.getAdapter(),
                entries = listOf(
                    ListPreferenceEntry("menu") { stringResource(R.string.switcher_long_press_menu) },
                    ListPreferenceEntry("split") { stringResource(R.string.switcher_split_screen) },
                    ListPreferenceEntry("popup") { stringResource(R.string.switcher_popup_view) },
                    ListPreferenceEntry("appinfo") { stringResource(R.string.switcher_long_press_appinfo) },
                ),
                label = stringResource(R.string.switcher_long_press),
            )
        }
        PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
            SwitchPreference(
                adapter = prefs.recentsTranslucentBackground.getAdapter(),
                label = stringResource(id = R.string.translucent_background),
                icon = Icons.Rounded.Opacity,
            )
            ExpandAndShrink(visible = recentsTranslucentBackground) {
                SliderPreference(
                    adapter = prefs.recentsTranslucentBackgroundAlpha.getAdapter(),
                    label = stringResource(id = R.string.translucent_background_alpha),
                    step = 0.05f,
                    valueRange = 0f..0.95f,
                    showAsPercentage = true,
                )
            }
        }

        PreferenceGroup(
            heading = stringResource(id = R.string.task_switcher_label),
            description = stringResource(id = R.string.task_switcher_takeover_description),
        ) {
            SwitchPreference(
                adapter = prefs.taskSwitcherTakeover.getAdapter(),
                label = stringResource(id = R.string.task_switcher_takeover_label),
                icon = Icons.Rounded.ViewAgenda,
            )
        }

        PrivilegedAccessGroup()

        QuickActionsPreferences(
            items = recentActionsPreferences,
            adapter = prefs.recentActionOrder.getAdapter(),
        )

        val overrideWindowCornerRadius by prefs.overrideWindowCornerRadius.observeAsState()
        PreferenceGroup(
            heading = stringResource(id = R.string.window_corner_radius_label),
            description = stringResource(id = (R.string.window_corner_radius_description)),
            showDescription = overrideWindowCornerRadius,
        ) {
            SwitchPreference(
                adapter = prefs.overrideWindowCornerRadius.getAdapter(),
                label = stringResource(id = R.string.override_window_corner_radius_label),
                icon = Icons.Rounded.RoundedCorner,
            )
            ExpandAndShrink(visible = overrideWindowCornerRadius) {
                SliderPreference(
                    label = stringResource(id = R.string.window_corner_radius_label),
                    adapter = prefs.windowCornerRadius.getAdapter(),
                    step = 0,
                    valueRange = 70..150,
                )
            }
        }

        if (Utilities.ATLEAST_S_V2) {
            PreferenceGroup(
                heading = stringResource(id = R.string.taskbar_label),
            ) {
                SwitchPreference(
                    adapter = prefs2.enableTaskbarOnPhone.getAdapter(),
                    label = stringResource(id = R.string.enable_taskbar_experimental),
                    icon = Icons.Rounded.ViewAgenda,
                )
            }
        }
    }
}

/** Mode selector, one switch per backend and a live status line for each (see [BackendSelector]). */
@Composable
private fun PrivilegedAccessGroup(modifier: Modifier = Modifier) {
    val prefs = preferenceManager()
    val context = LocalContext.current
    val selector = remember { BackendSelector.getInstance(context) }
    var refresh by remember { mutableIntStateOf(0) }
    // Re-checked on resume, so granting Shizuku or root elsewhere shows up when coming back
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    val statuses by produceState<Map<Backend, BackendStatus>>(emptyMap(), refresh) {
        value = selector.statusAll(forceRefresh = true)
    }

    val modeEntries = remember {
        BackendMode.entries.map { mode ->
            // A pinned backend must serve recents and app operations too, not only the screen lock
            val pinned = mode.pinned
            val servesAll = pinned == null || (pinned.supports(Capability.TaskControl) && pinned.supports(Capability.AppState))
            ListPreferenceEntry(mode.name, enabled = servesAll) {
                val name = stringResource(
                    when (mode) {
                        BackendMode.Automatic -> R.string.privileged_access_mode_automatic
                        BackendMode.Root -> R.string.privileged_access_backend_root
                        BackendMode.ShizukuPlus -> R.string.privileged_access_backend_shizuku_plus
                        BackendMode.Shizuku -> R.string.privileged_access_backend_shizuku
                        BackendMode.Dhizuku -> R.string.privileged_access_backend_dhizuku
                        BackendMode.Accessibility -> R.string.privileged_access_backend_accessibility
                    },
                )
                if (servesAll) name else stringResource(R.string.privileged_access_screen_lock_only, name)
            }
        }
    }

    PreferenceGroup(
        heading = stringResource(id = R.string.privileged_access_label),
        description = stringResource(id = R.string.privileged_access_description),
        modifier = modifier,
    ) {
        ListPreference(
            adapter = prefs.backendMode.getAdapter(),
            entries = modeEntries,
            label = stringResource(id = R.string.privileged_access_mode_label),
            description = stringResource(id = R.string.privileged_access_mode_description),
            icon = Icons.Rounded.Security,
        )
        BackendSwitch(Backend.Root, R.string.privileged_access_backend_root, prefs.backendRootEnabled.getAdapter(), statuses)
        BackendSwitch(Backend.ShizukuPlus, R.string.privileged_access_backend_shizuku_plus, prefs.backendShizukuPlusEnabled.getAdapter(), statuses)
        BackendSwitch(Backend.Shizuku, R.string.privileged_access_backend_shizuku, prefs.backendShizukuEnabled.getAdapter(), statuses)
        BackendSwitch(Backend.Dhizuku, R.string.privileged_access_backend_dhizuku, prefs.backendDhizukuEnabled.getAdapter(), statuses)
        BackendSwitch(Backend.Accessibility, R.string.privileged_access_backend_accessibility, prefs.backendAccessibilityEnabled.getAdapter(), statuses)
    }
}

@Composable
private fun BackendSwitch(
    backend: Backend,
    label: Int,
    adapter: PreferenceAdapter<Boolean>,
    statuses: Map<Backend, BackendStatus>,
) {
    val status = statuses[backend]
    val description = when {
        status == null -> R.string.privileged_access_status_checking
        status == BackendStatus.Available -> R.string.privileged_access_status_available
        status == BackendStatus.Detected && backend == Backend.Dhizuku -> R.string.privileged_access_status_dhizuku_detected
        status == BackendStatus.Detected -> R.string.privileged_access_status_plus_detected
        else -> R.string.privileged_access_status_unavailable
    }
    SwitchPreference(
        adapter = adapter,
        label = stringResource(id = label),
        description = stringResource(id = description),
    )
}

@PreviewAutoCat
@Composable
private fun QuickSwitchIgnoredWarning(
    modifier: Modifier = Modifier,
) {
    AutoCatTheme {
        WarningPreference(
            text = stringResource(id = R.string.quickswitch_ignored_warning),
            modifier = modifier.padding(horizontal = 16.dp),
            standalone = true,
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        )
    }
}
