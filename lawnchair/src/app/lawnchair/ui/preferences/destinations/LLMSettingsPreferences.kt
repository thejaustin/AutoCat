package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var isCategorizing by remember { mutableStateOf(false) }
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
                                isCategorizing = true
                                categorizationStatus = "Starting categorization..."
                                scope.launch {
                                    try {
                                        val manager = CategorizationManager.getInstance(context)
                                        manager.recategorizeAll()
                                        categorizationStatus = "✅ Categorization complete! Check your app drawer."
                                        isCategorizing = false
                                    } catch (e: Exception) {
                                        categorizationStatus = "❌ Error: ${e.message}"
                                        isCategorizing = false
                                    }
                                }
                            },
                            enabled = !isCategorizing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (isCategorizing) "Categorizing..." else "Re-categorize All Apps")
                        }

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
