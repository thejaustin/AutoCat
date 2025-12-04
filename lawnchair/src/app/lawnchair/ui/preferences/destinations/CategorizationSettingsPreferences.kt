package app.lawnchair.ui.preferences.destinations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import app.lawnchair.categorization.importer.SmartLauncherImporter
import app.lawnchair.categorization.importer.SmartLauncherImporter.ImportResult

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
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            categorizationStatus = "Importing..."
            scope.launch {
                val importer = SmartLauncherImporter(context)
                val result = importer.importFromUri(uri)
                categorizationStatus = when (result) {
                    is ImportResult.Success -> "✅ Imported ${result.count} apps from Smart Launcher!"
                    is ImportResult.Error -> "❌ Import failed: ${result.message}"
                }
            }
        }
    }

    PreferenceScaffold(
        label = "Categorization Settings",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // Batch Processing Settings
            item {
                PreferenceGroup(heading = "Performance") {
                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Enable Batch Processing",
                        description = "Process multiple apps per API call (20x faster, 68% token savings)",
                    )

                    if (prefs.llmEnableBatching.get()) {
                        val batchSizeValue = prefs.llmBatchSize.get()
                        var sliderValue by remember { mutableStateOf(batchSizeValue.toFloat()) }

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Batch Size: ${if (sliderValue.toInt() == 0) "Auto" else sliderValue.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    prefs.llmBatchSize.set(sliderValue.roundToInt())
                                },
                                valueRange = 0f..50f,
                                steps = 49,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (sliderValue.toInt() == 0) {
                                    "Auto: Automatically calculates optimal batch size based on model context window"
                                } else {
                                    "Manual: Process ${sliderValue.toInt()} apps per batch"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to App Drawer Folders",
                        description = "Automatically create folders in app drawer for each category",
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SwitchPreference(
                        adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                        label = "Enable Rate Limiting",
                        description = "Add 1-second delay between batches. Recommended for Google AI (free tier). " +
                            "Disable for faster categorization with Perplexity, Claude, or OpenAI.",
                    )
                }
            }

            // Re-categorize Action (at bottom)
            item {
                PreferenceGroup(heading = "Actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                            Text(if (progress.isRunning) "Categorizing..." else "Re-categorize All Apps")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Import Smart Launcher Backup
                        OutlinedButton(
                            onClick = {
                                slImportLauncher.launch("*/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Import from Smart Launcher Backup (.slbk)")
                        }

                        // Progress indicator
                        if (progress.isRunning) {
                            Spacer(modifier = Modifier.height(16.dp))

                            val animatedProgress by animateFloatAsState(
                                targetValue = progress.progressPercentage,
                                animationSpec = tween(durationMillis = 300),
                                label = "progress",
                            )

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Stage: ${progress.currentStage}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${progress.processedCount} / ${progress.totalCount}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            // Show batch progress if available
                            if (progress.batchProgressText != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = progress.batchProgressText!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )

                                    if (progress.currentProvider != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${progress.currentProvider}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    if (progress.estimatedTimeMs > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val seconds = (progress.estimatedTimeMs / 1000).toInt()
                                        Text(
                                            text = "• ~${seconds}s remaining",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            if (progress.currentAppName != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Processing: ${progress.currentAppName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Status message
                        if (categorizationStatus.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = categorizationStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (categorizationStatus.startsWith("✅")) {
                                    MaterialTheme.colorScheme.primary
                                } else if (categorizationStatus.startsWith("❌")) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }

                        // Developer Diagnostics (only when dev mode enabled)
                        if (prefs.autoCatDevMode.get()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            var showDiagnostics by remember { mutableStateOf(false) }

                            OutlinedButton(
                                onClick = { showDiagnostics = !showDiagnostics },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = if (showDiagnostics) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (showDiagnostics) "Hide Diagnostics" else "Show Diagnostics")
                            }

                            if (showDiagnostics) {
                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "🔧 Developer Diagnostics",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Current Progress Stats
                                        Text(
                                            text = "Current Progress:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "• Stage: ${progress.currentStage}\n" +
                                                "• Processed: ${progress.processedCount}/${progress.totalCount}\n" +
                                                "• Batch: ${progress.currentBatch}/${progress.totalBatches}\n" +
                                                "• Provider: ${progress.currentProvider ?: "N/A"}\n" +
                                                "• Batch Size: ${progress.batchSize}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Database Stats
                                        var dbStats by remember { mutableStateOf("Loading...") }
                                        LaunchedEffect(Unit) {
                                            withContext(Dispatchers.IO) {
                                                val totalCategorized = categoryDao.getAllAppCategories().size
                                                val userOverrides = categoryDao.getUserOverriddenApps().size
                                                val llmCategorized = categoryDao.getAppsBySource("llm").size
                                                val builtInCategorized = categoryDao.getAppsBySource("built-in").size

                                                dbStats = "• Total categorized: $totalCategorized\n" +
                                                    "• LLM categorized: $llmCategorized\n" +
                                                    "• Built-in categorized: $builtInCategorized\n" +
                                                    "• User overrides: $userOverrides"
                                            }
                                        }

                                        Text(
                                            text = "Database Stats:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = dbStats,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Settings Info
                                        Text(
                                            text = "Settings:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = "• Batching: ${if (prefs.llmEnableBatching.get()) "Enabled" else "Disabled"}\n" +
                                                "• Batch size: ${if (prefs.llmBatchSize.get() == 0) "Auto" else prefs.llmBatchSize.get()}\n" +
                                                "• Folder sync: ${if (prefs.autoCatSyncFolders.get()) "Enabled" else "Disabled"}\n" +
                                                "• Provider: ${prefs.llmProviderPreference.get()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "This will re-categorize all apps using LLM (custom categories) and built-in (fallback). " +
                                "${if (prefs.llmEnableBatching.get()) "Batch processing enabled for 20x faster categorization! " else ""}" +
                                "${if (prefs.autoCatSyncFolders.get()) "Folders will be created automatically. " else ""}" +
                                "Only apps without user overrides will be re-categorized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
