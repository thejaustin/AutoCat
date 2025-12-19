package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.CategorizationProgress
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
import kotlinx.coroutines.launch

@Composable
fun CategorizationOverviewPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()

    var categorizationManager by remember {
        mutableStateOf<CategorizationManager?>(null)
    }

    // Initialize categorization manager safely
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                categorizationManager = CategorizationManager.getInstance(context)
            } catch (e: Exception) {
                android.util.Log.e("CategorizationOverview", "Error initializing: ${e.message}", e)
            }
        }
    }

    val progress by (
        categorizationManager?.progress ?: kotlinx.coroutines.flow.MutableStateFlow(
            CategorizationProgress(),
        )
        ).collectAsState()

    PreferenceScaffold(
        label = "Categorization Overview",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manage and review your app categorization system. " +
                            "Configure AI providers, manage categories, and review app assignments.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Progress section if categorization is running
            if (progress.isRunning) {
                item {
                    PreferenceGroup(heading = "Categorization in Progress") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = progress.currentStage,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress.progressPercentage },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress.progressPercentage * 100).toInt()}% - " +
                                    "${progress.processedCount}/${progress.totalCount} apps processed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Quick Actions
            item {
                PreferenceGroup(heading = "Quick Actions") {
                    NavigationActionPreference(
                        label = "Manage Categories",
                        subtitle = "Create, edit, and delete app categories",
                        destination = app.lawnchair.ui.preferences.navigation.CategoryManagement,
                        icon = Icons.Rounded.Category,
                    )

                    NavigationActionPreference(
                        label = "Review & Override",
                        subtitle = "View and manually override app categorizations",
                        destination = AppDrawerAppCategorizations,
                        icon = Icons.Rounded.Edit,
                    )

                    NavigationActionPreference(
                        label = "LLM Provider Settings",
                        subtitle = "Configure AI providers and test connections",
                        destination = AppDrawerLLMSettings,
                        icon = Icons.Rounded.Psychology,
                    )

                    // Re-categorize button
                    androidx.compose.foundation.background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        categorizationManager?.recategorizeAll()
                                    }
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (progress.isRunning) "Processing..." else "Re-categorize All Apps",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Use AI to reassign all apps to categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Display Options
            item {
                PreferenceGroup(heading = "Display Options") {
                    SwitchPreference(
                        adapter = prefs.autoCatUseTabs.getAdapter(),
                        label = "Use App Tabs",
                        description = "Show tabs in app drawer. Apps are organized by tab.",
                    )

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to App Drawer Folders",
                        description = "Automatically create folders in app drawer for each tab",
                    )
                }
            }

            // Performance Options
            item {
                PreferenceGroup(heading = "Performance & Behavior") {
                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Batch Processing",
                        description = "Process multiple apps per API call (20x faster)",
                    )

                    SwitchPreference(
                        adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                        label = "Rate Limiting",
                        description = "Add delays between batches (Recommended for free tier)",
                    )

                    SwitchPreference(
                        adapter = prefs.circuitBreakerEnabled.getAdapter(),
                        label = "Enable Circuit Breaker",
                        description = "Temporarily disable failing LLM providers",
                    )
                }
            }
        }
    }
}
