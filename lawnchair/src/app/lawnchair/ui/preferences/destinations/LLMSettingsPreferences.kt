package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.categorization.llm.ModelRegistry
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences.rememberTransformAdapter
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.TextPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import kotlin.math.roundToLong
import kotlinx.coroutines.launch

@Composable
fun LLMSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()
    var testStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var categorizationManager by remember { mutableStateOf<CategorizationManager?>(null) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    // Initialize categorization manager safely
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                categorizationManager = CategorizationManager.getInstance(context)
            } catch (e: Exception) {
                android.util.Log.e("LLMSettings", "Error initializing: ${e.message}", e)
                initializationError = "Failed to initialize: ${e.message}"
            }
        }
    }

    val progress by (
        categorizationManager?.progress ?: kotlinx.coroutines.flow.MutableStateFlow(
            app.lawnchair.categorization.CategorizationProgress(),
        )
        ).collectAsState()

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

            // Circuit Breaker Settings
            item {
                PreferenceGroup(heading = "Circuit Breaker Settings") {
                    SwitchPreference(
                        adapter = prefs.circuitBreakerEnabled.getAdapter(),
                        label = "Enable Circuit Breaker",
                        description = "Temporarily disable LLM providers that are consistently failing.",
                    )

                    AnimatedVisibility(visible = prefs.circuitBreakerEnabled.get()) {
                        Column {
                            SliderPreference(
                                adapter = prefs.circuitBreakerFailureThreshold.getAdapter(),
                                label = "Failure Threshold",
                                valueRange = 1..10, // Use Int range
                                step = 1, // Use Int step
                                showUnit = " failures",
                            )
                            SliderPreference(
                                adapter = rememberTransformAdapter(
                                    adapter = prefs.circuitBreakerTimeoutMs.getAdapter(),
                                    transformGet = { (it as Long).toFloat() / 1000f }, // Convert ms to seconds
                                    transformSet = { (it as Float * 1000f).roundToLong() }, // Convert seconds to ms and round
                                ),
                                label = "Timeout Duration",
                                valueRange = 10f..300f, // 10s to 5min in seconds
                                step = 10f, // 10 second increments
                                showUnit = " seconds",
                            )
                            SliderPreference(
                                adapter = rememberTransformAdapter(
                                    adapter = prefs.circuitBreakerHalfOpenDurationMs.getAdapter(),
                                    transformGet = { (it as Long).toFloat() / 1000f }, // Convert ms to seconds
                                    transformSet = { (it as Float * 1000f).roundToLong() }, // Convert seconds to ms and round
                                ),
                                label = "Half-Open Test Duration",
                                valueRange = 5f..60f, // 5s to 1min in seconds
                                step = 5f, // 5 second increments
                                showUnit = " seconds",
                            )
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        categorizationManager?.resetCircuitBreakers()
                                    }
                                },
                                enabled = categorizationManager != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            ) {
                                Text("Reset All Circuit Breakers")
                            }
                        }
                    }
                }
            }

            // Operations Section
            item {
                PreferenceGroup(heading = "Categorization Operations") {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                categorizationManager?.recategorizeAll()
                            }
                        },
                        enabled = !progress.isRunning && categorizationManager != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(if (progress.isRunning) "Processing..." else "Re-categorize All Apps")
                    }

                    AnimatedVisibility(visible = progress.isRunning || progress.processedCount > 0) {
                        CategorizationStatus(progress)
                    }
                }
            }
        }
    }
}

@Composable
fun CategorizationStatus(
    progress: app.lawnchair.categorization.CategorizationProgress,
    modifier: Modifier = Modifier,
) {
    val logs = remember { mutableStateListOf<LLMLogger.LogEntry>() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        LLMLogger.logFlow.collect { log ->
            logs.add(log)
            if (logs.size > 100) logs.removeFirst()
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    // Animate progress bar
    val progressAnimated by animateFloatAsState(
        targetValue = progress.progressPercentage,
        label = "ProgressAnimation",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = progress.currentStage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (progress.isRunning) {
                    Text(
                        text = "${(progress.progressPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Processed: ${progress.processedCount}/${progress.totalCount}",
                    style = MaterialTheme.typography.bodySmall,
                )
                progress.batchProgressText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (progress.estimatedTimeMs > 0) {
                Text(
                    text = "Est. time: ${progress.estimatedTimeMs / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Live Logs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .padding(8.dp),
            ) {
                LazyColumn(state = listState) {
                    items(logs) { log ->
                        val color = when (log.level) {
                            LLMLogger.LogLevel.ERROR -> Color(0xFFFF6B6B)
                            LLMLogger.LogLevel.WARNING -> Color(0xFFFFD93D)
                            LLMLogger.LogLevel.DEBUG -> Color(0xFF888888)
                            else -> Color(0xFF4ECDC4)
                        }

                        Text(
                            text = "> ${log.message}",
                            color = color,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            ),
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
