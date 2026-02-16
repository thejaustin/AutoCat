package app.lawnchair.ui.preferences.destinations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.navigation.AppDrawerDiagnostics
import app.lawnchair.ui.preferences.navigation.DebugMenu
import app.lawnchair.ui.preferences.navigation.ExperimentalFeatures
import app.lawnchair.util.restartLauncher
import com.android.launcher3.R

@Composable
fun DeveloperPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    PreferenceLayout(
        label = "Developer",
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceGroup {
            ClickablePreference(
                label = stringResource(id = R.string.debug_label),
                subtitle = "Fine-grained debug flags and feature toggles",
                icon = Icons.Rounded.Build,
                onClick = { navController.navigate(DebugMenu) },
            )
            ClickablePreference(
                label = stringResource(id = R.string.experimental_features_label),
                subtitle = "Test upcoming and unstable features",
                icon = Icons.Outlined.Science,
                onClick = { navController.navigate(ExperimentalFeatures) },
            )
            ClickablePreference(
                label = "Diagnostics",
                subtitle = "View crash logs, database stats, and LLM activity",
                icon = Icons.Rounded.BugReport,
                onClick = { navController.navigate(AppDrawerDiagnostics) },
            )
        }

        PreferenceGroup(heading = "Actions") {
            ClickablePreference(
                label = stringResource(id = R.string.debug_restart_launcher),
                icon = Icons.Rounded.Refresh,
                onClick = { restartLauncher(context) },
            )
        }
    }
}
