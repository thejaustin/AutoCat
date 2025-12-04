package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Get your API key at docs.perplexity.ai. If you get a 401 error, verify your API key is correct and active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
