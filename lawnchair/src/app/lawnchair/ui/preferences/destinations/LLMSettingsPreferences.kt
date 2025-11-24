package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.TextPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
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

            item {
                PreferenceGroup(heading = "Provider Selection") {
                    ListPreference(
                        adapter = prefs.llmProviderPreference.getAdapter(),
                        label = "LLM Provider",
                        entries = listOf(
                            ListPreferenceEntry(
                                value = "google_ai",
                                label = { "Google AI (Gemini) - Free tier: 15 requests/min" },
                            ),
                            ListPreferenceEntry(
                                value = "claude",
                                label = { "Anthropic Claude - Requires API key" },
                            ),
                            ListPreferenceEntry(
                                value = "openai",
                                label = { "OpenAI ChatGPT - Requires API key" },
                            ),
                            ListPreferenceEntry(
                                value = "perplexity",
                                label = { "Perplexity - Requires API key" },
                            ),
                        ),
                    )
                }
            }

            item {
                PreferenceGroup(heading = "API Keys") {
                    TextPreference(
                        adapter = prefs.llmGoogleAIKey.getAdapter(),
                        label = "Google AI API Key",
                    )

                    Spacer(modifier = Modifier.padding(4.dp))

                    Text(
                        text = "Get your free API key at ai.google.dev",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    TextPreference(
                        adapter = prefs.llmClaudeKey.getAdapter(),
                        label = "Claude API Key",
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    TextPreference(
                        adapter = prefs.llmOpenAIKey.getAdapter(),
                        label = "OpenAI API Key",
                    )

                    Spacer(modifier = Modifier.padding(8.dp))

                    TextPreference(
                        adapter = prefs.llmPerplexityKey.getAdapter(),
                        label = "Perplexity API Key",
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Note: LLM categorization only runs for apps that don't have built-in Android categories. " +
                            "Rate limiting is automatically applied to stay within free tier limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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

                        // Progress indicator with animated progress bar
                        if (progress.isRunning) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Animated progress value
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress.progressPercentage,
                                animationSpec = tween(durationMillis = 300),
                                label = "progress",
                            )

                            // Linear progress bar (Material You style)
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress text with stage info
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

                            // Current app being processed
                            if (progress.currentAppName != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Processing: ${progress.currentAppName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Status message after completion
                        if (categorizationStatus.isNotEmpty()) {
                            Spacer(modifier = Modifier.padding(8.dp))
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

                        Spacer(modifier = Modifier.padding(4.dp))

                        Text(
                            text = "This will re-categorize all apps using Stage 1 (built-in) and Stage 2 (LLM). " +
                                "User overrides will be preserved. May take a few minutes for large app collections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
