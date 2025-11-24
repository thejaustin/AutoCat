package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.TextPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout

@Composable
fun LLMSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val prefs = preferenceManager()

    PreferenceLayout(
        label = "LLM Settings",
        backArrowVisible = !LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
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
                            label = { "Google AI (Gemini)" },
                            description = { "Free tier: 15 requests/min" },
                        ),
                        ListPreferenceEntry(
                            value = "claude",
                            label = { "Anthropic Claude" },
                            description = { "Requires API key" },
                        ),
                        ListPreferenceEntry(
                            value = "openai",
                            label = { "OpenAI ChatGPT" },
                            description = { "Requires API key" },
                        ),
                        ListPreferenceEntry(
                            value = "perplexity",
                            label = { "Perplexity" },
                            description = { "Requires API key" },
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
                    placeholder = "Enter your API key",
                    isPassword = true,
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
                    placeholder = "Enter your API key",
                    isPassword = true,
                )

                Spacer(modifier = Modifier.padding(8.dp))

                TextPreference(
                    adapter = prefs.llmOpenAIKey.getAdapter(),
                    label = "OpenAI API Key",
                    placeholder = "Enter your API key",
                    isPassword = true,
                )

                Spacer(modifier = Modifier.padding(8.dp))

                TextPreference(
                    adapter = prefs.llmPerplexityKey.getAdapter(),
                    label = "Perplexity API Key",
                    placeholder = "Enter your API key",
                    isPassword = true,
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
    }
}
