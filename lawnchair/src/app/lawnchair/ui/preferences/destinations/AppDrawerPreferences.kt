/*
 * Copyright 2021, AutoCat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AppTabsController
import app.lawnchair.preferences.PreferenceAdapter
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.AppDrawerHapticFeedbackPreference
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.SuggestionsPreference
import app.lawnchair.ui.preferences.components.colorpreference.ColorPreference
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreferenceWithPreview
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.navigation.AppDrawerCategorizationSettings
import app.lawnchair.ui.preferences.navigation.AppDrawerHiddenApps
import com.android.launcher3.R

object AppDrawerRoutes {
    const val HIDDEN_APPS = "hiddenApps"
}

@Composable
fun AppDrawerPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()
    val context = LocalContext.current
    val resources = context.resources

    PreferenceLayout(
        label = stringResource(id = R.string.app_drawer_label),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        val drawerListAdapter = prefs.drawerList.getAdapter()
        Column {
            DrawerLayoutPreference(drawerListAdapter)
            ExpandAndShrink(visible = drawerListAdapter.state.value) {
                AppDrawerFolderPreferenceItem()
            }
        }
        val hiddenApps = prefs2.hiddenApps.getAdapter().state.value
        PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
            Item {
                NavigationActionPreference(
                    label = "AutoCat",
                    subtitle = stringResource(R.string.smart_categories_subtitle),
                    destination = AppDrawerCategorizationSettings,
                    icon = Icons.Rounded.AutoAwesome,
                )
            }
            Item {
                NavigationActionPreference(
                    label = stringResource(id = R.string.hidden_apps_label),
                    destination = AppDrawerHiddenApps,
                    subtitle = resources.getQuantityString(R.plurals.apps_count, hiddenApps.size, hiddenApps.size),
                    icon = Icons.Rounded.VisibilityOff,
                )
            }
            Item { SearchBarPreference(SearchRoute.DRAWER_SEARCH, showLabel = false) }
            SuggestionsPreference()
            AppDrawerHapticFeedbackPreference()
        }
        PreferenceGroup(heading = stringResource(R.string.style)) {
            Item { ColorPreference(preference = prefs2.appDrawerBackgroundColor) }
            Item {
                SliderPreference(
                    label = stringResource(id = R.string.background_opacity),
                    adapter = prefs.drawerOpacity.getAdapter(),
                    step = 0.1f,
                    valueRange = 0F..1F,
                    showAsPercentage = true,
                )
            }
            Item { ColorPreference(preference = prefs2.workProfileTabBackgroundColor) }
            Item {
                SwitchPreference(
                    label = stringResource(id = R.string.work_profile_tab_container_background_label),
                    adapter = prefs2.workProfileTabContainerBackground.getAdapter(),
                )
            }
            Item {
                SwitchPreference(
                    label = stringResource(id = R.string.pref_all_apps_search_bar_background),
                    adapter = prefs2.appDrawerSearchBarBackground.getAdapter(),
                )
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.grid)) {
            Item {
                SliderPreference(
                    label = stringResource(id = R.string.app_drawer_columns),
                    adapter = prefs2.drawerColumns.getAdapter(),
                    step = 1,
                    valueRange = 3..10,
                )
            }
            Item {
                SliderPreference(
                    adapter = prefs2.drawerCellHeightFactor.getAdapter(),
                    label = stringResource(id = R.string.row_height_label),
                    valueRange = 0.3F..1.5F,
                    step = 0.1F,
                    showAsPercentage = true,
                )
            }
            Item {
                SliderPreference(
                    adapter = prefs2.drawerLeftRightMarginFactor.getAdapter(),
                    label = stringResource(id = R.string.app_drawer_indent_label),
                    valueRange = 0.0F..1.5F,
                    step = 0.05F,
                    showAsPercentage = true,
                )
            }
        }
        val showDrawerLabels = prefs2.showIconLabelsInDrawer.getAdapter()
        PreferenceGroup(heading = stringResource(id = R.string.icons)) {
            SliderPreference(
                label = stringResource(id = R.string.icon_sizes),
                adapter = prefs2.drawerIconSizeFactor.getAdapter(),
                step = 0.1f,
                valueRange = 0.5F..1.5F,
                showAsPercentage = true,
            )
            val showDrawerLabels = prefs2.showIconLabelsInDrawer.getAdapter()
            SwitchPreference(
                adapter = showDrawerLabels,
                label = stringResource(id = R.string.show_labels),
                icon = Icons.Rounded.Label,
            )
            ExpandAndShrink(visible = showDrawerLabels.state.value) {
                DividerColumn {
                    SliderPreference(
                        label = stringResource(id = R.string.label_size),
                        adapter = prefs2.drawerIconLabelSizeFactor.getAdapter(),
                        step = 0.1F,
                        valueRange = 0.5F..1.5F,
                        showAsPercentage = true,
                    )
                    SwitchPreference(
                        adapter = prefs2.twoLineAllApps.getAdapter(),
                        label = stringResource(R.string.twoline_label),
                        icon = Icons.Rounded.Notes,
                    )
                }
            }
        }
        PreferenceGroup(heading = stringResource(id = R.string.advanced)) {
            SwitchPreference(
                label = stringResource(id = R.string.pref_all_apps_bulk_icon_loading_title),
                description = stringResource(id = R.string.pref_all_apps_bulk_icon_loading_description),
                adapter = prefs.allAppBulkIconLoading.getAdapter(),
                icon = Icons.Rounded.Bolt,
            )
            SwitchPreference(
                label = stringResource(id = R.string.pref_all_apps_remember_position_title),
                description = stringResource(id = R.string.pref_all_apps_remember_position_description),
                adapter = prefs2.rememberPosition.getAdapter(),
                icon = Icons.Rounded.BookmarkBorder,
            )
            SwitchPreference(
                label = stringResource(id = R.string.pref_all_apps_show_scrollbar_title),
                adapter = prefs2.showScrollbar.getAdapter(),
                icon = Icons.Rounded.SwapVert,
            )
            Item { DefaultDrawerTabPreference() }
        }
    }
}

@Composable
private fun DefaultDrawerTabPreference() {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val adapter = prefs.autoCatDefaultDrawerTab.getAdapter()
    val controller = remember { AppTabsController.getInstance(context) }
    val tabNames by controller.tabNames.collectAsState()
    val allAppsLabel = stringResource(id = R.string.default_drawer_tab_all_apps)

    // Build entries: first entry is "All Apps" (stored as ""), then each real tab name
    val entries = remember(tabNames) {
        val list = mutableListOf(
            ListPreferenceEntry(value = "") { allAppsLabel },
        )
        tabNames.drop(1).forEach { name ->
            list.add(ListPreferenceEntry(value = name) { name })
        }
        list
    }

    ListPreference(
        adapter = adapter,
        entries = entries,
        label = stringResource(id = R.string.default_drawer_tab_title),
        description = stringResource(id = R.string.default_drawer_tab_summary),
        icon = Icons.Rounded.Tab,
    )
}

@Composable
private fun DrawerLayoutPreference(drawerListAdapter: PreferenceAdapter<Boolean>) {
    SwitchPreferenceWithPreview(
        label = stringResource(id = R.string.layout),
        checked = !drawerListAdapter.state.value,
        onCheckedChange = { drawerListAdapter.onChange(!it) },
        disabledLabel = stringResource(id = R.string.feed_default),
        disabledContent = {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.8f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp),
                    ),
            )

            Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        },
        enabledLabel = stringResource(id = R.string.caddy_beta),
        enabledContent = {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.8f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp),
                    ),
            )
            Row(modifier = Modifier, horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(2) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier) {
                        repeat(2) {
                            Row(
                                modifier = Modifier,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repeat(2) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape,
                                            ),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        },
    )
}
