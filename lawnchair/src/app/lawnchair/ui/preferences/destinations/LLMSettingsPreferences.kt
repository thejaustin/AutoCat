package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.ModelRegistry
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.TextPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun LLMSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()
    val categorizationManager = remember { CategorizationManager.getInstance(context) }
    val progress by categorizationManager.progress.collectAsState()
    var categorizationStatus by remember { mutableStateOf("") }
    var testStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    PreferenceScaffold(
        label = "LLM Settings",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Configure LLM providers for intelligent app categorization. " +
                            "AutoCat uses AI to automatically assign apps to your custom categories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Provider Selection
            item {
                PreferenceGroup(heading = "Provider Selection") {
                    ListPreference(
                        adapter = prefs.llmProviderPreference.getAdapter(),
                        label = "Preferred LLM Provider",
                        entries = listOf(
                            ListPreferenceEntry(
                                value = "google_ai",
                                label = { "Google AI (Gemini 2.0 Flash)" },
                            ),
                            ListPreferenceEntry(
                                value = "claude",
                                label = { "Anthropic Claude 3.5" },
                            ),
                            ListPreferenceEntry(
                                value = "openai",
                                label = { "OpenAI GPT" },
                            ),
                            ListPreferenceEntry(
                                value = "perplexity",
                                label = { "Perplexity (Llama 3.1)" },
                            ),
                        ),
                    )
                }
            }

            // Google AI Configuration
            item {
                PreferenceGroup(heading = "Google AI (Gemini) - Free Tier") {
                    TextPreference(
                        adapter = prefs.llmGoogleAIKey.getAdapter(),
                        label = "Google AI API Key ${if (prefs.llmGoogleAIKey.get().isNotEmpty()) "✓" else ""}",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ListPreference(
                        adapter = prefs.llmGoogleAIModel.getAdapter(),
                        label = "Gemini Model",
                        entries = ModelRegistry.getAvailableModels("google_ai").map { model ->
                            ListPreferenceEntry(
                                value = model.id,
                                label = { "${model.displayName} - ${model.speedTier.name}" },
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val provider = GoogleAIProvider(context)
                                val result = provider.testConnection()
                                testStatus = testStatus + (
                                    "google_ai" to if (result.success) {
                                        "✅ Connected (${result.latencyMs}ms)"
                                    } else {
                                        "❌ ${result.message}"
                                    }
                                    )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Test Google AI Connection")
                    }

                    testStatus["google_ai"]?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Get your free API key at ai.google.dev/gemini-api",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            // Claude Configuration
            item {
                PreferenceGroup(heading = "Anthropic Claude") {
                    TextPreference(
                        adapter = prefs.llmClaudeKey.getAdapter(),
                        label = "Claude API Key ${if (prefs.llmClaudeKey.get().isNotEmpty()) "✓" else ""}",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ListPreference(
                        adapter = prefs.llmClaudeModel.getAdapter(),
                        label = "Claude Model",
                        entries = ModelRegistry.getAvailableModels("claude").map { model ->
                            ListPreferenceEntry(
                                value = model.id,
                                label = { "${model.displayName} - ${model.costTier.name}" },
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val provider = ClaudeProvider(context)
                                val result = provider.testConnection()
                                testStatus = testStatus + (
                                    "claude" to if (result.success) {
                                        "✅ Connected (${result.latencyMs}ms)"
                                    } else {
                                        "❌ ${result.message}"
                                    }
                                    )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Test Claude Connection")
                    }

                    testStatus["claude"]?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // OpenAI Configuration
            item {
                PreferenceGroup(heading = "OpenAI (ChatGPT)") {
                    TextPreference(
                        adapter = prefs.llmOpenAIKey.getAdapter(),
                        label = "OpenAI API Key ${if (prefs.llmOpenAIKey.get().isNotEmpty()) "✓" else ""}",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ListPreference(
                        adapter = prefs.llmOpenAIModel.getAdapter(),
                        label = "OpenAI Model",
                        entries = ModelRegistry.getAvailableModels("openai").map { model ->
                            ListPreferenceEntry(
                                value = model.id,
                                label = { "${model.displayName} - ${model.costTier.name}" },
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val provider = OpenAIProvider(context)
                                val result = provider.testConnection()
                                testStatus = testStatus + (
                                    "openai" to if (result.success) {
                                        "✅ Connected (${result.latencyMs}ms)"
                                    } else {
                                        "❌ ${result.message}"
                                    }
                                    )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Test OpenAI Connection")
                    }

                    testStatus["openai"]?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // Perplexity Configuration
            item {
                PreferenceGroup(heading = "Perplexity") {
                    TextPreference(
                        adapter = prefs.llmPerplexityKey.getAdapter(),
                        label = "Perplexity API Key ${if (prefs.llmPerplexityKey.get().isNotEmpty()) "✓" else ""}",
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ListPreference(
                        adapter = prefs.llmPerplexityModel.getAdapter(),
                        label = "Perplexity Model",
                        entries = ModelRegistry.getAvailableModels("perplexity").map { model ->
                            ListPreferenceEntry(
                                value = model.id,
                                label = { "${model.displayName} - ${model.qualityTier.name}" },
                            )
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val provider = PerplexityProvider(context)
                                val result = provider.testConnection()
                                testStatus = testStatus + (
                                    "perplexity" to if (result.success) {
                                        "✅ Connected (${result.latencyMs}ms)"
                                    } else {
                                        "❌ ${result.message}"
                                    }
                                    )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Test Perplexity Connection")
                    }

                    testStatus["perplexity"]?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("✅")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // Batch Processing Settings
            item {
                PreferenceGroup(heading = "Performance Settings") {
                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Enable Batch Processing",
                        description = "Process multiple apps per API call (20x faster, 68% token savings)",
                    )

                    if (prefs.llmEnableBatching.get()) {
                        Spacer(modifier = Modifier.height(16.dp))

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
                }
            }

            // Categorization Actions
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "This will re-categorize all apps using LLM (custom categories) and built-in (fallback). " +
                                "${if (prefs.llmEnableBatching.get()) "Batch processing enabled for 20x faster categorization! " else ""}" +
                                "${if (prefs.autoCatSyncFolders.get()) "Folders will be created automatically. " else ""}" +
                                "User overrides will be preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
