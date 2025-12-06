package app.lawnchair.ui.preferences.destinations

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.CategoryTabsController
import app.lawnchair.categorization.importer.SmartLauncherImporter
import app.lawnchair.categorization.importer.SmartLauncherImporter.ImportResult
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.navigation.AppDrawerAppCategorizations
import app.lawnchair.ui.preferences.navigation.AppDrawerLLMSettings
import app.lawnchair.ui.preferences.navigation.AppDrawerManageCategories
import kotlinx.coroutines.launch

@Composable
fun CategorizationSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()
    val categorizationManager = remember { CategorizationManager.getInstance(context) }
    val progress by categorizationManager.progress.collectAsState()
    val categoryDao = remember { CategoryDatabase.getInstance(context).categoryDao() }

    var categorizationStatus by remember { mutableStateOf("") }

    val slImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            categorizationStatus = "Importing..."
            scope.launch {
                val importer = SmartLauncherImporter(context)
                val result = importer.importFromUri(uri)
                categorizationStatus = when (result) {
                    is ImportResult.Success -> {
                        CategoryTabsController.getInstance(context).refresh()
                        "✅ Imported ${result.count} apps from Smart Launcher!"
                    }
                    is ImportResult.Error -> "❌ Import failed: ${result.message}"
                }
            }
        }
    }

    PreferenceScaffold(
        label = "App Categorization",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // ===== QUICK ACTIONS =====
            item {
                PreferenceGroup(heading = "Quick Actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                categorizationStatus = ""
                                scope.launch {
                                    try {
                                        categorizationManager.recategorizeAll()
                                        categorizationStatus = "✅ Categorization complete! Check your app drawer."
                                    } catch (e: Exception) {
                                        categorizationStatus = "❌ Error: ${e.message}"
                                    }
                                }
                            },
                            enabled = !progress.isRunning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (progress.isRunning) "Categorizing..." else "Categorize All Apps")
                        }

                        OutlinedButton(
                            onClick = { slImportLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Import from Smart Launcher (.slbk)")
                        }

                        // Progress indicator
                        AnimatedVisibility(visible = progress.isRunning || progress.processedCount > 0) {
                            CategorizationProgress(progress)
                        }

                        // Status message
                        if (categorizationStatus.isNotEmpty()) {
                            Text(
                                text = categorizationStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    categorizationStatus.startsWith("✅") -> MaterialTheme.colorScheme.primary
                                    categorizationStatus.startsWith("❌") -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }

                        Text(
                            text = "Categorize all apps using AI and built-in rules. " +
                                "${if (prefs.llmEnableBatching.get()) "Batch processing enabled for faster categorization. " else ""}" +
                                "${if (prefs.autoCatSyncFolders.get()) "Folders will be created automatically. " else ""}" +
                                "Only apps without user overrides will be re-categorized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ===== NAVIGATION TO SUBPAGES =====
            item {
                PreferenceGroup(heading = "Manage") {
                    NavigationActionPreference(
                        label = "Categories",
                        subtitle = "Create and manage custom categories for your apps",
                        destination = AppDrawerManageCategories,
                    )

                    NavigationActionPreference(
                        label = "Review & Override",
                        subtitle = "View and manually override app categorizations",
                        destination = AppDrawerAppCategorizations,
                    )

                    NavigationActionPreference(
                        label = "LLM Provider Settings",
                        subtitle = "Configure AI providers and test connections",
                        destination = AppDrawerLLMSettings,
                    )
                }
            }

            // ===== SETTINGS =====
            item {
                PreferenceGroup(heading = "Settings") {
                    val useTabs by prefs.autoCatUseTabs.getAdapter().state

                    SwitchPreference(
                        adapter = prefs.autoCatUseTabs.getAdapter(),
                        label = "Use Category Tabs",
                        description = "Show categories as tabs in app drawer (like Smart Launcher). Apps are organized by category with folders still available within each tab.",
                    )

                    // Show work apps options only when category tabs are enabled
                    AnimatedVisibility(visible = useTabs) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            SwitchPreference(
                                adapter = prefs.hideWorkApps.getAdapter(),
                                label = "Hide Work Apps",
                                description = "Hide work profile apps from the app drawer (except on Work tab if enabled)",
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SwitchPreference(
                                adapter = prefs.showWorkTab.getAdapter(),
                                label = "Show Work Tab",
                                description = "Add a dedicated Work tab at the end to show only work profile apps",
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Batch Processing",
                        description = "Process multiple apps per API call (20x faster, 68% token savings)",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to App Drawer Folders",
                        description = "Automatically create folders in app drawer for each category",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SwitchPreference(
                        adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                        label = "Rate Limiting",
                        description = "Add 1-second delay between batches. Recommended for Google AI free tier.",
                    )
                }
            }

            // ===== DEVELOPER DIAGNOSTICS =====
            if (prefs.autoCatDevMode.get()) {
                item {
                    PreferenceGroup(heading = "Developer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "Debug Information",
                                style = MaterialTheme.typography.titleSmall,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "• Stage: ${progress.currentStage}\n" +
                                    "• Progress: ${progress.processedCount}/${progress.totalCount}\n" +
                                    "• Batch: ${progress.currentBatch}/${progress.totalBatches}\n" +
                                    "• Provider: ${progress.currentProvider ?: "N/A"}\n" +
                                    "• Batch Size: ${progress.batchSize}\n" +
                                    "• Batching: ${if (prefs.llmEnableBatching.get()) "Enabled" else "Disabled"}\n" +
                                    "• Folder Sync: ${if (prefs.autoCatSyncFolders.get()) "Enabled" else "Disabled"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
