package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import app.lawnchair.categorization.AccuracyTracker
import app.lawnchair.categorization.AdaptiveModelSelector
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.categorization.llm.ModelRegistry
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.categorization.llm.TestResult
import app.lawnchair.data.tab.entities.ModelAccuracyStats
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences.rememberTransformAdapter
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.controls.ListPreference
import app.lawnchair.ui.preferences.components.controls.ListPreferenceEntry
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
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

    var categorizationManager by remember { mutableStateOf<CategorizationManager?>(null) }
    var accuracyTracker by remember { mutableStateOf<AccuracyTracker?>(null) }
    var accuracyStats by remember { mutableStateOf<List<ModelAccuracyStats>>(emptyList()) }
    var autoSelectedProvider by remember { mutableStateOf<String?>(null) }

    // Track selected provider locally for UI logic
    val selectedProvider by prefs.llmProviderPreference.getAdapter()
    val isAutoSelect by prefs.llmAutoSelectBestModel.getAdapter()
    var showAllProviders by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                categorizationManager = CategorizationManager.getInstance(context)
                accuracyTracker = AccuracyTracker(context)
                accuracyStats = accuracyTracker!!.getAccuracyStats(daysBack = 30)
                val selector = AdaptiveModelSelector(context)
                autoSelectedProvider = selector.getBestProvider()
            } catch (e: Exception) {
                android.util.Log.e("LLMSettings", "Error initializing: ${e.message}", e)
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
            // ===== PROVIDER SELECTION =====
            item {
                PreferenceGroup(heading = "AI Provider") {
                    SwitchPreference(
                        adapter = prefs.llmAutoSelectBestModel.getAdapter(),
                        label = "Auto-Select Provider",
                        description = if (isAutoSelect && autoSelectedProvider != null) {
                            "Choosing the best provider based on your past corrections. Currently using: ${formatProviderName(autoSelectedProvider!!)}"
                        } else {
                            "Let AI pick the best provider based on your correction history."
                        },
                    )

                    ListPreference(
                        adapter = prefs.llmProviderPreference.getAdapter(),
                        label = "Manual Provider Selection",
                        enabled = !isAutoSelect,
                        entries = listOf(
                            ListPreferenceEntry("google_ai") { "Google AI (Gemini)" },
                            ListPreferenceEntry("claude") { "Anthropic Claude" },
                            ListPreferenceEntry("openai") { "OpenAI GPT" },
                            ListPreferenceEntry("perplexity") { "Perplexity" },
                        ),
                    )
                }
            }

            // ===== ACTIVE PROVIDER CONFIG =====
            val activeProvider = if (isAutoSelect) autoSelectedProvider ?: "google_ai" else selectedProvider

            item {
                if (activeProvider.isNotEmpty()) {
                    ProviderConfigSection(
                        providerId = activeProvider,
                        prefs = prefs,
                        testStatus = testStatus,
                        onTestConnection = {
                            scope.launch {
                                val result = when (activeProvider) {
                                    "google_ai" -> GoogleAIProvider(context).testConnection()
                                    "claude" -> ClaudeProvider(context).testConnection()
                                    "openai" -> OpenAIProvider(context).testConnection()
                                    "perplexity" -> PerplexityProvider(context).testConnection()
                                    else -> TestResult(false, "Unknown provider")
                                }
                                testStatus = testStatus + (activeProvider to if (result.success) "✅ Connected (${result.latencyMs}ms)" else "❌ ${result.message}")
                            }
                        },
                    )
                }
            }

            // ===== OTHER PROVIDERS =====
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAllProviders = !showAllProviders }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Configure Other Providers",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            imageVector = if (showAllProviders) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    AnimatedVisibility(visible = showAllProviders) {
                        Column {
                            val otherProviders = listOf("google_ai", "claude", "openai", "perplexity").filter { it != activeProvider }
                            otherProviders.forEach { pid ->
                                ProviderConfigSection(
                                    providerId = pid,
                                    prefs = prefs,
                                    testStatus = testStatus,
                                    onTestConnection = {
                                        scope.launch {
                                            val result = when (pid) {
                                                "google_ai" -> GoogleAIProvider(context).testConnection()
                                                "claude" -> ClaudeProvider(context).testConnection()
                                                "openai" -> OpenAIProvider(context).testConnection()
                                                "perplexity" -> PerplexityProvider(context).testConnection()
                                                else -> TestResult(false, "Unknown provider")
                                            }
                                            testStatus = testStatus + (pid to if (result.success) "✅ Connected (${result.latencyMs}ms)" else "❌ ${result.message}")
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ===== ADVANCED SETTINGS =====
            item {
                PreferenceGroup(heading = "Advanced Settings") {
                    SwitchPreference(
                        adapter = prefs.circuitBreakerEnabled.getAdapter(),
                        label = "Circuit Breaker",
                        description = "Automatically disable a provider if it fails multiple times in a row. Prevents slow performance when APIs are down.",
                    )

                    AnimatedVisibility(visible = prefs.circuitBreakerEnabled.get()) {
                        Column {
                            SliderPreference(
                                adapter = prefs.circuitBreakerFailureThreshold.getAdapter(),
                                label = "Failure Limit",
                                valueRange = 1..10,
                                step = 1,
                                showUnit = " failures",
                            )
                        }
                    }
                }
            }

            // ===== ACTIONS =====
            item {
                PreferenceGroup(heading = "Actions") {
                    ClickablePreference(
                        label = "Restart Categorization",
                        subtitle = "Retry failed batches and re-evaluate all apps",
                        onClick = {
                            scope.launch {
                                categorizationManager?.recategorizeAll()
                            }
                        },
                    )
                }
            }

            // ===== ACCURACY STATS =====
            if (accuracyStats.isNotEmpty()) {
                item {
                    PreferenceGroup(heading = "Accuracy (30 Days)") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            accuracyStats.forEach { stats ->
                                CompactAccuracyCard(stats, isSelected = stats.provider == activeProvider)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // ===== LIVE LOGS =====
            item {
                PreferenceGroup(heading = "System Status") {
                    AnimatedVisibility(visible = progress.isRunning || progress.processedCount > 0) {
                        CategorizationStatus(progress)
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderConfigSection(
    providerId: String,
    prefs: app.lawnchair.preferences.PreferenceManager,
    testStatus: Map<String, String>,
    modifier: Modifier = Modifier,
    onTestConnection: () -> Unit,
) {
    val name = formatProviderName(providerId)
    val apiKeyAdapter = when (providerId) {
        "google_ai" -> prefs.llmGoogleAIKey.getAdapter()
        "claude" -> prefs.llmClaudeKey.getAdapter()
        "openai" -> prefs.llmOpenAIKey.getAdapter()
        "perplexity" -> prefs.llmPerplexityKey.getAdapter()
        else -> return
    }

    val modelAdapter = when (providerId) {
        "google_ai" -> prefs.llmGoogleAIModel.getAdapter()
        "claude" -> prefs.llmClaudeModel.getAdapter()
        "openai" -> prefs.llmOpenAIModel.getAdapter()
        "perplexity" -> prefs.llmPerplexityModel.getAdapter()
        else -> return
    }

    PreferenceGroup(heading = name) {
        TextPreference(
            adapter = apiKeyAdapter,
            label = "API Key",
        )

        ListPreference(
            adapter = modelAdapter,
            label = "Model",
            entries = ModelRegistry.getAvailableModels(providerId).map { model ->
                ListPreferenceEntry(
                    value = model.id,
                    label = { model.displayName },
                )
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = onTestConnection) {
                Icon(Icons.Rounded.WifiTethering, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Connection")
            }

            testStatus[providerId]?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun CompactAccuracyCard(
    stats: ModelAccuracyStats,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                )
                Column {
                    Text(
                        text = formatProviderName(stats.provider),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${stats.total} categorizations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stats.accuracy >= 90) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp).padding(end = 4.dp))
                }
                Text(
                    text = "${stats.accuracy.toInt()}% Accuracy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (stats.accuracy > 80) Color(0xFF4CAF50) else Color(0xFFFF9800),
                )
            }
        }
    }
}

@Composable
fun CategorizationStatus(
    progress: app.lawnchair.categorization.CategorizationProgress,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<LLMLogger.LogEntry>() }

    LaunchedEffect(Unit) {
        LLMLogger.logFlow.collect { log ->
            logs.add(log)
            if (logs.size > 50) logs.removeFirst()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp).animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (progress.isRunning) "Running: ${progress.currentStage}" else "Idle",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (progress.isRunning) {
                        LinearProgressIndicator(
                            progress = { progress.progressPercentage },
                            modifier = Modifier.width(150.dp).padding(top = 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                        )
                    }
                }
                Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.height(150.dp).fillMaxWidth().background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp)).padding(8.dp),
                ) {
                    LazyColumn {
                        items(logs.reversed()) { log ->
                            Text(
                                text = "> ${log.message}",
                                color = if (log.level == LLMLogger.LogLevel.ERROR) Color(0xFFFF6B6B) else Color(0xFF4ECDC4),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatProviderName(provider: String): String = when (provider) {
    "google_ai" -> "Google AI"
    "claude" -> "Claude"
    "openai" -> "OpenAI"
    "perplexity" -> "Perplexity"
    else -> provider.replaceFirstChar { it.uppercase() }
}
