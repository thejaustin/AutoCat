package app.lawnchair.override

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.launcher3.model.data.FolderInfo
import app.lawnchair.ui.preferences.PreferenceActivity
import app.lawnchair.ui.preferences.navigation.SelectFolderIcon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.navigation.SelectApp
import app.lawnchair.ui.util.App
import app.lawnchair.ui.util.apps
import app.lawnchair.ui.util.appsState
import com.android.launcher3.LauncherAppState
import com.android.launcher3.R
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.util.ComponentKey
import kotlinx.coroutines.launch
import android.content.ComponentName

import app.lawnchair.gestures.config.GestureHandlerConfig
import app.lawnchair.gestures.type.GestureType
import app.lawnchair.preferences2.preferenceManager2

@Composable
fun CustomizeFolderDialog(
    icon: Drawable,
    defaultTitle: String,
    folderInfo: FolderInfo,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(folderInfo.title?.toString() ?: defaultTitle) }
    var coverMode by remember { mutableStateOf(folderInfo.coverMode) }
    var coverApp by remember { mutableStateOf(folderInfo.coverApp) }
    val launcherAppState = LauncherAppState.getInstance(context)
    val scope = rememberCoroutineScope()
    val folderService = FolderService.INSTANCE.get(context)
    val allApps by appsState()
    val prefs2 = preferenceManager2()

    val selectIconRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        onClose()
    }

    val selectAppRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra("component_name")?.let { componentNameString ->
                coverApp = ComponentName.unflattenFromString(componentNameString)
            }
        }
    }

    val openIconPicker = {
        selectIconRequest.launch(PreferenceActivity.createIntent(context, SelectFolderIcon(folderInfo.id)))
    }

    DisposableEffect(Unit) {
        onDispose {
            val newTitle = if (title != defaultTitle) title else defaultTitle
            if (newTitle != folderInfo.title.toString()) {
                folderInfo.setTitle(newTitle, launcherAppState.model.modelWriter)
            }
            // Save cover mode and cover app
            if (coverMode != folderInfo.coverMode || coverApp != folderInfo.coverApp) {
                scope.launch {
                    folderService.updateFolderCover(folderInfo.id, coverMode, coverApp)
                    folderInfo.setCover(coverMode, coverApp)
                }
            }
        }
    }

    CustomizeDialog(
        icon = icon,
        title = title,
        onTitleChange = { title = it },
        defaultTitle = defaultTitle,
        launchSelectIcon = openIconPicker,
        modifier = modifier,
    ) {
        PreferenceGroup(heading = stringResource(R.string.folder_settings)) {
            SwitchPreference(
                checked = coverMode,
                onCheckedChange = { coverMode = it },
                label = stringResource(R.string.folder_cover_mode),
                description = stringResource(R.string.folder_cover_mode_description),
            )
            if (coverMode) {
                val currentCoverApp = coverApp?.let { component ->
                    allApps.find { it.componentName == component }
                }
                ClickablePreference(
                    label = stringResource(R.string.select_cover_app),
                    subtitle = currentCoverApp?.label ?: stringResource(R.string.none),
                    onClick = {
                        selectAppRequest.launch(PreferenceActivity.createIntent(context, SelectApp()))
                    },
                )
            }
        }
    }
}
