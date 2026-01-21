package app.lawnchair.ui.preferences.destinations

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.importer.SmartLauncherImporter
import app.lawnchair.categorization.importer.SmartLauncherImporter.ImportResult
import app.lawnchair.data.tab.TabDatabase
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
import app.lawnchair.ui.preferences.navigation.AppDrawerTabManagement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        // Auto-sort apps into folders
                        val sortResult = app.lawnchair.categorization.FolderAutoSortService.getInstance(context).autoSortAll()
                        "✅ Imported ${result.count} apps. Created ${sortResult.foldersCreated} folders."
                    }

                    is ImportResult.Error -> "❌ Import failed: ${result.message}"
                }
            }
        }
    }

    PreferenceScaffold(
        label = "AI Categorization",
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
                                modifier = Modifier.weight(1.1f),
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
                                    if (progress.isRunning) "Running..." else "Run AutoCat",
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            // Import Button
                            OutlinedButton(
                                onClick = { slImportLauncher.launch("*/*") },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import SL")
                            }
                        }

                        // Progress indicator
                        AnimatedVisibility(visible = progress.isRunning || progress.processedCount > 0) {
                            Column {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
                        icon = { Icon(Icons.Rounded.Layers, null) },
                    )

                    AnimatedVisibility(visible = useTabs) {
                        Column {
                            SwitchPreference(
                                adapter = prefs.hideWorkApps.getAdapter(),
                                label = "Hide Work Apps",
                                description = "Hide work profile apps from the app drawer",
                                icon = { Icon(Icons.Rounded.Work, null) },
                            )
                            SwitchPreference(
                                adapter = prefs.showWorkTab.getAdapter(),
                                label = "Show Work Tab",
                                description = "Add a dedicated tab for work apps",
                                icon = { Icon(Icons.Rounded.Category, null) },
                            )
                        }
                    }

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to Folders",
                        description = "Create folders for each category in your app drawer and home screen.",
                        icon = { Icon(Icons.Rounded.CloudSync, null) },
                    )

                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Batch Processing",
                        description = "Process multiple apps in a single AI request. Recommended for speed and lower costs.",
                        icon = { Icon(Icons.Rounded.Speed, null) },
                    )

                    val enableRateLimiting = prefs.autoCatEnableRateLimiting.getAdapter()
                    SwitchPreference(
                        adapter = enableRateLimiting,
                        label = "Rate Limiting",
                        description = "Slows down requests to prevent API blocks. Recommended for Free API tiers.",
                        icon = { Icon(Icons.Rounded.Timer, null) },
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
                        icon = { Icon(Icons.Rounded.Category, null) },
                    )

                    NavigationActionPreference(
                        label = "Review & Override",
                        subtitle = "Manually correct app categories",
                        destination = AppDrawerAppCategorizations,
                        icon = { Icon(Icons.Rounded.History, null) },
                    )

                    NavigationActionPreference(
                        label = "AI Provider Settings",
                        subtitle = "Configure API keys and models (Google, OpenAI, etc.)",
                        destination = AppDrawerLLMSettings,
                        icon = { Icon(Icons.Rounded.Settings, null) },
                    )
                }
            }
        }
    }
}
