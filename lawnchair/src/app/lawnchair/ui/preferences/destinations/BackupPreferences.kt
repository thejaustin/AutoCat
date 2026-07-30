package app.lawnchair.ui.preferences.destinations

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lawnchair.backup.ui.restoreBackupOpener
import app.lawnchair.categorization.importer.SmartLauncherImporter
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.navigation.CreateBackup
import app.lawnchair.ui.util.rememberExpressiveHaptics
import com.android.launcher3.R
import kotlinx.coroutines.launch

@Composable
fun BackupPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val openRestoreBackup = restoreBackupOpener()
    val haptics = rememberExpressiveHaptics()

    val slImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, "Importing Smart Launcher backup...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val importer = SmartLauncherImporter(context)
                val result = importer.importFromUri(uri)
                when (result) {
                    is SmartLauncherImporter.ImportResult.Success -> {
                        haptics.success()
                        val sortResult = app.lawnchair.categorization.FolderAutoSortService.getInstance(context).autoSortAll()
                        Toast.makeText(context, "✅ Imported ${result.count} apps. Created ${sortResult.foldersCreated} folders.", Toast.LENGTH_LONG).show()
                    }

                    is SmartLauncherImporter.ImportResult.Error -> {
                        haptics.error()
                        Toast.makeText(context, "❌ Import failed: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    PreferenceLayout(
        label = stringResource(id = R.string.create_backup),
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceGroup(heading = "Lawnchair Backup") {
            ClickablePreference(
                label = stringResource(id = R.string.create_backup),
                icon = Icons.Outlined.Backup,
                onClick = { navController.navigate(CreateBackup) },
            )
            ClickablePreference(
                label = stringResource(id = R.string.restore_backup),
                icon = Icons.Rounded.SettingsBackupRestore,
                onClick = { openRestoreBackup() },
            )
        }

        PreferenceGroup(heading = "External Importers") {
            ClickablePreference(
                label = "Import Smart Launcher",
                subtitle = "Import categories and workspace from a Smart Launcher backup (.slbk)",
                icon = Icons.Rounded.Download,
                onClick = { slImportLauncher.launch("*/*") },
            )
        }
    }
}
