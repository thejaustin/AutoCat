package app.lawnchair.ui.preferences.destinations

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.navigation.AppDrawerAppCategorizations
import app.lawnchair.ui.preferences.navigation.AppDrawerDiagnostics
import app.lawnchair.ui.preferences.navigation.AppDrawerLLMSettings
import app.lawnchair.ui.preferences.navigation.AppDrawerTabManagement
import app.lawnchair.ui.preferences.navigation.Search
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
                                modifier = Modifier.weight(1f),
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

                            // Clear Overrides Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        database?.tabDao()?.deleteAllAppTabs()
                                        withContext(Dispatchers.Main) {
                                            categorizationStatus = "✅ All assignments cleared"
                                        }
                                    }
                                },
                                enabled = !progress.isRunning,
                                modifier = Modifier.weight(0.6f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset")
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

            // ===== INTELLIGENCE =====
            item {
                PreferenceGroup(heading = "Intelligence") {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DiscoveryCard(
                                title = "AI Category Suggestions",
                                description = "Let AI analyze your apps and create perfect tabs automatically.",
                                icon = Icons.Rounded.TipsAndUpdates,
                                onClick = { onNavigate(AppDrawerTabManagement) },
                            )

                            DiscoveryCard(
                                title = "Review Categorizations",
                                description = "See why AI chose specific categories and make manual corrections.",
                                icon = Icons.Rounded.Analytics,
                                onClick = { onNavigate(AppDrawerAppCategorizations) },
                            )

                            DiscoveryCard(
                                title = "Semantic Search",
                                description = "Find apps by their purpose or category instead of just their names.",
                                icon = Icons.Rounded.Search,
                                onClick = { onNavigate(Search(app.lawnchair.ui.preferences.destinations.SearchRoute.DRAWER_SEARCH)) },
                            )
                        }
                    }
                }
            }

            // ===== VISUALS =====
            item {
                PreferenceGroup(heading = "Visuals") {
                    val useTabs by prefs.autoCatUseTabs.getAdapter().state

                    SwitchPreference(
                        adapter = prefs.autoCatUseTabs.getAdapter(),
                        label = "App Drawer Tabs",
                        description = "Group apps into swipeable categories in your drawer.",
                    )

                    AnimatedVisibility(visible = useTabs) {
                        Column {
                            SwitchPreference(
                                adapter = prefs.hideWorkApps.getAdapter(),
                                label = "Hide Work Apps",
                                description = "Keep your work profile apps out of sight.",
                            )
                            SwitchPreference(
                                adapter = prefs.showWorkTab.getAdapter(),
                                label = "Dedicated Work Tab",
                                description = "Put all work apps in their own separate tab.",
                            )
                        }
                    }

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Automatic Folders",
                        description = "Create and maintain folders based on app categories.",
                    )

                    val syncFolders by prefs.autoCatSyncFolders.getAdapter().state
                    AnimatedVisibility(visible = syncFolders) {
                        ListPreference(
                            adapter = prefs.autoCatFolderSyncMode.getAdapter(),
                            entries = folderSyncModeEntries,
                            label = "Sync Location",
                        )
                    }
                }
            }

            // ===== AUTOMATION =====
            item {
                PreferenceGroup(heading = "Automation") {
                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Smart Batching",
                        description = "Processes apps in groups for faster organization and lower battery use.",
                    )

                    val enableBatching by prefs.llmEnableBatching.getAdapter().state
                    AnimatedVisibility(visible = enableBatching) {
                        Column {
                            SliderPreference(
                                label = "Batch Size",
                                adapter = prefs.llmBatchSize.getAdapter(),
                                valueRange = 0..100,
                                step = 5,
                                showUnit = if (prefs.llmBatchSize.get() == 0) " (Auto)" else " apps",
                            )

                            if (devMode) {
                                SwitchPreference(
                                    adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                                    label = "Parallel Processing",
                                    description = "Use multiple AI streams at once for near-instant results.",
                                )
                            }
                        }
                    }

                    SwitchPreference(
                        adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                        label = "Safe Mode",
                        description = "Adds delays between requests to stay within Free API limits.",
                    )
                }
            }

            // ===== MANAGEMENT =====
            item {
                PreferenceGroup(heading = "Management") {
                    NavigationActionPreference(
                        label = "AI Engine Settings",
                        subtitle = "Change API keys and model preferences.",
                        destination = AppDrawerLLMSettings,
                    )

                    ClickablePreference(
                        label = "Reset to Factory Defaults",
                        subtitle = "Clear all AutoCat settings and start over.",
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                prefs.autoCatUseTabs.set(true)
                                prefs.autoCatSyncFolders.set(false)
                                prefs.llmEnableBatching.set(true)
                                prefs.llmBatchSize.set(0)
                                prefs.llmAutoSelectBestModel.set(true)
                                prefs.circuitBreakerEnabled.set(true)
                                database?.tabDao()?.deleteAllAppTabs()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
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

@Composable
fun DiscoveryCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
