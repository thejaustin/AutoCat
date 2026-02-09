package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.navigation.AppDrawerAppCategorizations
import app.lawnchair.ui.preferences.navigation.AppDrawerDiagnostics
import app.lawnchair.ui.preferences.navigation.AppDrawerLLMSettings
import app.lawnchair.ui.preferences.navigation.AppDrawerTabManagement
import com.android.launcher3.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategorizationSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()

    var categorizationManager by remember { mutableStateOf<CategorizationManager?>(null) }
    var database by remember { mutableStateOf<TabDatabase?>(null) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    val progress by (
        categorizationManager?.progress ?: kotlinx.coroutines.flow.MutableStateFlow(
            app.lawnchair.categorization.CategorizationProgress(),
        )
        ).collectAsState()

    var categorizationStatus by remember { mutableStateOf("") }

    val folderSyncModeEntries = remember {
        listOf(
            ListPreferenceEntry("DRAWER") { stringResource(id = R.string.folder_sync_mode_drawer) },
            ListPreferenceEntry("HOME_SCREEN") { stringResource(id = R.string.folder_sync_mode_home) },
            ListPreferenceEntry("BOTH") { stringResource(id = R.string.folder_sync_mode_both) },
        )
    }

    // Initialize services
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                categorizationManager = CategorizationManager.getInstance(context)
                database = TabDatabase.getInstance(context)
            } catch (e: Exception) {
                android.util.Log.e("CategorizationSettings", "Error initializing: ${e.message}", e)
                initializationError = "Failed to initialize: ${e.message}"
            }
        }
    }

    val devMode by prefs.autoCatDevMode.getAdapter().state

    PreferenceScaffold(
        label = stringResource(R.string.smart_categories_label),
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // ===== PRIMARY ACTIONS =====
            item {
                PreferenceGroup(heading = "Actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Re-categorize Button
                            ElevatedButton(
                                onClick = {
                                    categorizationStatus = ""
                                    scope.launch {
                                        try {
                                            categorizationManager?.recategorizeAll()
                                            categorizationStatus = "✅ Categorization complete!"
                                        } catch (e: Exception) {
                                            categorizationStatus = "❌ Error: ${e.message}"
                                        }
                                    }
                                },
                                enabled = !progress.isRunning && categorizationManager != null,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (progress.isRunning) stringResource(R.string.categorizing) else stringResource(R.string.categorize_now),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        // Progress indicator
                        AnimatedVisibility(visible = progress.isRunning || progress.processedCount > 0) {
                            Column {
                                LinearProgressIndicator(
                                    progress = { progress.progressPercentage },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (progress.isRunning) {
                                    Text(
                                        text = "Processed: ${progress.processedCount}/${progress.totalCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                    )
                                }
                            }
                        }

                        // Status message
                        AnimatedVisibility(visible = categorizationStatus.isNotEmpty()) {
                            val isSuccess = categorizationStatus.startsWith("✅")
                            val isError = categorizationStatus.startsWith("❌")

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = categorizationStatus.removePrefix("✅ ").removePrefix("❌ "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            // ===== SETTINGS =====
            item {
                PreferenceGroup(
                    heading = "Display & Behavior",
                ) {
                    val useTabs by prefs.autoCatUseTabs.getAdapter().state

                    SwitchPreference(
                        adapter = prefs.autoCatUseTabs.getAdapter(),
                        label = "Use App Tabs",
                        description = "Organize apps into tabs in the app drawer",
                    )

                    AnimatedVisibility(visible = useTabs) {
                        Column {
                            SwitchPreference(
                                adapter = prefs.hideWorkApps.getAdapter(),
                                label = "Hide Work Apps",
                                description = "Hide work profile apps from the app drawer",
                            )
                            SwitchPreference(
                                adapter = prefs.showWorkTab.getAdapter(),
                                label = "Show Work Tab",
                                description = "Add a dedicated tab for work apps",
                            )
                        }
                    }

                    val syncFolders by prefs.autoCatSyncFolders.getAdapter().state

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to Folders",
                        description = "Automatically organize apps into folders based on their category.",
                    )

                    AnimatedVisibility(visible = syncFolders) {
                        ListPreference(
                            adapter = prefs.autoCatFolderSyncMode.getAdapter(),
                            entries = folderSyncModeEntries,
                            label = stringResource(id = R.string.folder_sync_mode_label),
                        )
                    }

                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Batch Processing",
                        description = stringResource(R.string.batch_processing_description),
                    )

                    val enableRateLimiting = prefs.autoCatEnableRateLimiting.getAdapter()
                    SwitchPreference(
                        adapter = enableRateLimiting,
                        label = "Rate Limiting",
                        description = "Slows down requests to prevent API blocks. Recommended for Free API tiers.",
                    )
                }
            }

            // ===== MANAGEMENT =====
            item {
                PreferenceGroup(heading = "Management") {
                    NavigationActionPreference(
                        label = "Manage Tabs",
                        subtitle = "Create, edit, delete, and auto-suggest tabs",
                        destination = AppDrawerTabManagement,
                    )

                    NavigationActionPreference(
                        label = "Review & Override",
                        subtitle = "Manually correct app categories",
                        destination = AppDrawerAppCategorizations,
                    )

                    NavigationActionPreference(
                        label = stringResource(R.string.provider_settings_label),
                        subtitle = stringResource(R.string.provider_settings_subtitle),
                        destination = AppDrawerLLMSettings,
                    )
                }
            }

            if (devMode) {
                item {
                    PreferenceGroup(heading = "Developer") {
                        NavigationActionPreference(
                            label = "Diagnostics",
                            subtitle = "View database stats and live LLM logs",
                            destination = AppDrawerDiagnostics,
                        )
                    }
                }
            }
        }
    }
}
