package app.lawnchair.backup.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.lawnchair.backup.AutoCatBackup
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.DummyLauncherBox
import app.lawnchair.ui.preferences.components.controls.FlagSwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R
import kotlinx.coroutines.launch

@Composable
fun CreateBackupScreen(
    viewModel: CreateBackupViewModel,
    modifier: Modifier = Modifier,
) {
    val screenshot by viewModel.screenshot.collectAsStateWithLifecycle()
    val contents by viewModel.backupContents.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    var creatingBackup by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        scope.launch {
            creatingBackup = true
            try {
                AutoCatBackup.create(context, contents, screenshot, uri)
                Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } catch (t: Throwable) {
                android.util.Log.e("CreateBackupScreen", "Failed to create backup", t)
                Toast.makeText(context, R.string.backup_create_error, Toast.LENGTH_SHORT).show()
            } finally {
                creatingBackup = false
            }
        }
    }

    fun startCreateBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = AutoCatBackup.MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, AutoCatBackup.generateBackupFileName())
        }
        createDocumentLauncher.launch(intent)
    }

    PreferenceLayout(
        label = stringResource(id = R.string.create_backup),
        modifier = modifier,
        backArrowVisible = !LocalIsExpandedScreen.current,
    ) {
        DummyLauncherBox(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(0.6f)
                .weight(1f)
                .align(Alignment.CenterHorizontally)
                .clip(MaterialTheme.shapes.large),
            darkText = false,
        ) {
            Image(
                bitmap = screenshot.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillHeight,
            )
        }

        PreferenceGroup(
            heading = stringResource(id = R.string.what_to_backup),
        ) {
            FlagSwitchPreference(
                flags = contents,
                setFlags = viewModel::setBackupContents,
                mask = AutoCatBackup.INCLUDE_LAYOUT_AND_SETTINGS,
                label = stringResource(id = R.string.backup_content_layout_and_settings),
            )
            FlagSwitchPreference(
                flags = contents,
                setFlags = viewModel::setBackupContents,
                mask = AutoCatBackup.INCLUDE_WALLPAPER,
                label = stringResource(id = R.string.backup_content_wallpaper),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = { startCreateBackup() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(),
                enabled = contents != 0 && !creatingBackup,
            ) {
                Text(text = stringResource(id = R.string.create_backup))
            }
        }
    }
}
