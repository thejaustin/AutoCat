package app.lawnchair.ui.preferences.destinations

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import app.lawnchair.ui.preferences.navigation.Search
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import app.lawnchair.AutoCatApp
import app.lawnchair.AutoCatLauncher
import app.lawnchair.backup.ui.restoreBackupOpener
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.observeAsState
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.OverflowMenu
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.AnnouncementPreference
import app.lawnchair.ui.preferences.components.DraggableSettingsCategoryGroup
import app.lawnchair.ui.preferences.components.controls.PreferenceCategory
import app.lawnchair.ui.preferences.components.controls.WarningPreference
import app.lawnchair.ui.preferences.components.layout.ClickableIcon
import app.lawnchair.ui.preferences.components.layout.DividerColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceDivider
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.data.liveinfo.SyncLiveInformation
import app.lawnchair.ui.preferences.navigation.About
import app.lawnchair.ui.preferences.navigation.AppDrawer
import app.lawnchair.ui.preferences.navigation.CreateBackup
import app.lawnchair.ui.preferences.navigation.DebugMenu
import app.lawnchair.ui.preferences.navigation.Dock
import app.lawnchair.ui.preferences.navigation.ExperimentalFeatures
import app.lawnchair.ui.preferences.navigation.Folders
import app.lawnchair.ui.preferences.navigation.General
import app.lawnchair.ui.preferences.navigation.Gestures
import app.lawnchair.ui.preferences.navigation.HomeScreen
import app.lawnchair.ui.preferences.navigation.PreferenceRootRoute
import app.lawnchair.ui.preferences.navigation.Quickstep
import app.lawnchair.ui.preferences.navigation.Search
import app.lawnchair.ui.preferences.navigation.Smartspace
import app.lawnchair.ui.theme.isSelectedThemeDark
import app.lawnchair.ui.theme.preferenceGroupColor
import app.lawnchair.ui.util.addIf
import app.lawnchair.util.isDefaultLauncher
import app.lawnchair.util.restartLauncher
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import kotlinx.coroutines.launch

@Composable
fun SettingsSearchBar(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .androidx.compose.foundation.layout.fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .height(52.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.search_settings),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreferencesDashboard(
    currentRoute: PreferenceRootRoute,
    onNavigate: (PreferenceRootRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SyncLiveInformation()
    val prefs = preferenceManager()
    val pref2 = preferenceManager2()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    // Settings category management
    val categoryManager = remember { app.lawnchair.ui.preferences.SettingsCategoryManager.getInstance(context) }
    val categories by categoryManager.categories.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }

    PreferenceLayout(
        label = stringResource(id = R.string.settings),
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        backArrowVisible = false,
        actions = {
            IconButton(onClick = { isEditMode = !isEditMode }) {
                Icon(
                    imageVector = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                    contentDescription = stringResource(id = R.string.action_customize),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            PreferencesOverflowMenu(currentRoute = currentRoute, onNavigate = onNavigate)
        },
    ) {
        SettingsSearchBar(onClick = { onNavigate(Search()) })

        AnnouncementPreference()

        val hideSettingsWarnings by prefs.hideSettingsWarnings.observeAsState()

        if ((BuildConfig.APPLICATION_ID.contains("nightly") || BuildConfig.DEBUG) && !hideSettingsWarnings) {
            PreferencesDebugWarning()
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!context.isDefaultLauncher()) {
            PreferencesSetDefaultLauncherWarning(
                onDismiss = { },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        DraggableSettingsCategoryGroup(
            categories = categories,
            currentRoute = currentRoute,
            isEditMode = isEditMode,
            onNavigate = onNavigate,
            onToggleEditMode = { isEditMode = !isEditMode },
            onReorder = { from, to -> categoryManager.reorderCategories(from, to) },
            onToggleVisibility = { categoryId -> categoryManager.toggleCategoryVisibility(categoryId) },
            deckLayoutEnabled = pref2.deckLayout.getAdapter().state.value,
            quickstepEnabled = AutoCatApp.isRecentsEnabled || BuildConfig.DEBUG && !prefs.hideQuickstepSettings.get(),
        )
    }
}

@Composable
fun PreferenceCategoryGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val color = preferenceGroupColor()

    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = color,
        tonalElevation = if (isSelectedThemeDark) 1.dp else 0.dp,
    ) {
        DividerColumn(
            content = content,
            startIndent = (-16).dp,
            endIndent = (-16).dp,
            color = MaterialTheme.colorScheme.surface,
            thickness = 2.dp,
        )
    }
}

@Composable
fun RowScope.PreferencesOverflowMenu(
    currentRoute: PreferenceRootRoute,
    onNavigate: (PreferenceRootRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enableDebug by preferenceManager().enableDebugMenu.observeAsState()
    val highlightColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    val highlightShape = MaterialTheme.shapes.large

    if (enableDebug) {
        ClickableIcon(
            imageVector = Icons.Rounded.Build,
            onClick = { onNavigate(DebugMenu) },
            modifier = Modifier.addIf(currentRoute == DebugMenu) {
                Modifier
                    .clip(highlightShape)
                    .background(highlightColor)
            },
        )
    }
    val navController = LocalNavController.current
    val openCreateBackup = { navController.navigate(CreateBackup) }
    val openRestoreBackup = restoreBackupOpener()
    OverflowMenu(
        modifier = modifier.addIf(
            listOf(ExperimentalFeatures).any {
                currentRoute == it
            },
        ) {
            Modifier
                .clip(highlightShape)
                .background(highlightColor)
        },
    ) {
        val context = LocalContext.current
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_about),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                openAppInfo(context)
                hideMenu()
            },
            text = {
                Text(text = stringResource(id = R.string.app_info_drop_target_label))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                restartLauncher(context)
                hideMenu()
            },
            text = {
                Text(text = stringResource(id = R.string.debug_restart_launcher))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                onNavigate(ExperimentalFeatures)
                hideMenu()
            },
            text = {
                Text(text = stringResource(id = R.string.experimental_features_label))
            },
        )
        PreferenceDivider(modifier = Modifier.padding(vertical = 8.dp))
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                openCreateBackup()
                hideMenu()
            },
            text = {
                Text(text = stringResource(id = R.string.create_backup))
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.SettingsBackupRestore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {
                openRestoreBackup()
                hideMenu()
            },
            text = {
                Text(text = stringResource(id = R.string.restore_backup))
            },
        )
    }
}

@Composable
fun PreferencesDebugWarning(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        WarningPreference(
            // Don't move to strings.xml, no need to translate this warning
            text = "You are using a development build, which may contain bugs and broken features. Use at your own risk!",
        )
    }
}

@Composable
fun PreferencesSetDefaultLauncherWarning(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    androidx.compose.material3.SwipeToDismissBox(
        state = androidx.compose.material3.rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart ||
                    dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
                ) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        ),
        backgroundContent = {},
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            PreferenceTemplate(
                modifier = Modifier.clickable {
                    Intent(Settings.ACTION_HOME_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .let { context.startActivity(it) }
                    (context as? Activity)?.finish()
                },
                title = {},
                description = {
                    Text(
                        text = stringResource(id = R.string.set_default_launcher_tip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                startWidget = {
                    Icon(
                        imageVector = Icons.Rounded.TipsAndUpdates,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

fun openAppInfo(context: Context) {
    val launcherApps = context.getSystemService<LauncherApps>()
    val componentName = ComponentName(context, AutoCatLauncher::class.java)
    launcherApps?.startAppDetailsActivity(componentName, Process.myUserHandle(), null, null)
}
