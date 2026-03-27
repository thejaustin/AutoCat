package app.lawnchair.ui.preferences.destinations

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import app.lawnchair.AutoCatApp
import app.lawnchair.AutoCatLauncher
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.observeAsState
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.AnnouncementPreference
import app.lawnchair.ui.preferences.components.DraggableSettingsCategoryGroup
import app.lawnchair.ui.preferences.components.controls.WarningPreference
import app.lawnchair.ui.preferences.components.layout.DividerColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.data.liveinfo.SyncLiveInformation
import app.lawnchair.ui.preferences.navigation.General
import app.lawnchair.ui.preferences.navigation.PreferenceRootRoute
import app.lawnchair.ui.preferences.navigation.Root
import app.lawnchair.ui.preferences.navigation.Search
import app.lawnchair.ui.theme.isSelectedThemeDark
import app.lawnchair.ui.theme.preferenceGroupColor
import app.lawnchair.util.isDefaultLauncher
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsSearchBar(
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SearchThrob")
    val weight by infiniteTransition.animateFloat(
        initialValue = 400f,
        targetValue = 550f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "weight",
    )

    with(sharedTransitionScope) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .height(52.dp)
                .sharedElement(
                    rememberSharedContentState(key = "category_search"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Search settings",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PreferencesDashboard(
    currentRoute: PreferenceRootRoute,
    onNavigate: (PreferenceRootRoute) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
        },
    ) {
        val emphasizedSpring = spring<androidx.compose.ui.unit.IntOffset>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
        )

        var showDashboardContent by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            showDashboardContent = true
        }

        AnimatedVisibility(
            visible = showDashboardContent,
            enter = fadeIn(spring(stiffness = Spring.StiffnessVeryLow)) +
                slideInVertically(emphasizedSpring) { it / 8 },
        ) {
            Column {
                SettingsSearchBar(
                    onClick = { onNavigate(Search()) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )

                AnnouncementPreference()

                val hideSettingsWarnings by prefs.hideSettingsWarnings.observeAsState()

                if ((BuildConfig.APPLICATION_ID.contains("nightly") || BuildConfig.DEBUG) && !hideSettingsWarnings) {
                    PreferencesDebugWarning()
                    Spacer(modifier = Modifier.height(8.dp))
                }

                var defaultLauncherTipDismissed by remember { mutableStateOf(false) }
                if (!context.isDefaultLauncher() && !defaultLauncherTipDismissed) {
                    PreferencesSetDefaultLauncherWarning(
                        onDismiss = { defaultLauncherTipDismissed = true },
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
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreferencesDebugWarning() {
    WarningPreference(
        text = "You are using a development build, which may contain bugs and broken features. Use at your own risk!",
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreferencesSetDefaultLauncherWarning(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.clickable {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            context.startActivity(intent)
            onDismiss()
        },
    ) {
        WarningPreference(
            text = stringResource(id = R.string.set_default_launcher_tip),
        )
    }
}

private fun openAppInfo(context: Context) {
    val launcherApps = context.getSystemService<LauncherApps>()
    val componentName = ComponentName(context, AutoCatLauncher::class.java)
    launcherApps?.startAppDetailsActivity(componentName, Process.myUserHandle(), null, null)
}
