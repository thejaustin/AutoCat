package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.lawnchair.categorization.local.DeviceCapabilityChecker
import app.lawnchair.categorization.local.LocalEndpointProvider
import app.lawnchair.categorization.local.LocalModelInfo
import app.lawnchair.categorization.local.LocalModelRegistry
import app.lawnchair.categorization.local.LocalModelType
import app.lawnchair.categorization.local.MediaPipeLLMProvider
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
import java.io.File
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
    var showAdvanced by remember { mutableStateOf(false) }

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
        label = "AI Engine",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // ===== PRIMARY ENGINE SELECTION =====
            item {
                PreferenceGroup(heading = "Engine Configuration") {
                    SwitchPreference(
                        adapter = prefs.llmUseLocalModel.getAdapter(),
                        label = "Local AI Engine",
                        description = "Use on-device or local-server AI before cloud providers. Saves battery and data.",
                    )

                    val localEnabled by prefs.llmUseLocalModel.getAdapter().state
                    AnimatedVisibility(
                        visible = localEnabled,
                        enter = androidx.compose.animation.expandVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                    ) {
                        LocalAiSection(prefs = prefs, scope = scope)
                    }

                    SwitchPreference(
                        adapter = prefs.llmAutoSelectBestModel.getAdapter(),
                        label = "Auto-Pilot Mode",
                        description = if (isAutoSelect && autoSelectedProvider != null) {
                            "Choosing the best provider based on your past corrections. Currently using: ${formatProviderName(autoSelectedProvider!!)}"
                        } else {
                            "Let AI pick the best provider based on your correction history."
                        },
                    )

                    AnimatedVisibility(visible = !isAutoSelect) {
                        ListPreference(
                            adapter = prefs.llmProviderPreference.getAdapter(),
                            label = "Preferred Cloud Engine",
                            entries = listOf(
                                ListPreferenceEntry("google_ai") { "Google AI (Gemini)" },
                                ListPreferenceEntry("claude") { "Anthropic Claude" },
                                ListPreferenceEntry("openai") { "OpenAI GPT" },
                                ListPreferenceEntry("perplexity") { "Perplexity" },
                            ),
                        )
                    }

                    ListPreference(
                        adapter = prefs.llmPromptLanguage.getAdapter(),
                        label = "Thinking Language",
                        description = "The language the AI uses to analyze your apps.",
                        entries = listOf(
                            ListPreferenceEntry("System Default") { "System Default" },
                            ListPreferenceEntry("English") { "English" },
                            ListPreferenceEntry("Spanish") { "Spanish" },
                            ListPreferenceEntry("French") { "French" },
                            ListPreferenceEntry("German") { "German" },
                            ListPreferenceEntry("Chinese") { "Chinese" },
                            ListPreferenceEntry("Japanese") { "Japanese" },
                            ListPreferenceEntry("Korean") { "Korean" },
                            ListPreferenceEntry("Portuguese") { "Portuguese" },
                            ListPreferenceEntry("Russian") { "Russian" },
                        ),
                    )

                    SwitchPreference(
                        adapter = prefs.llmOnlyOnWifi.getAdapter(),
                        label = "Only on Wi-Fi",
                        description = "Postpone large cloud-based re-categorizations until connected to Wi-Fi.",
                    )

                    SwitchPreference(
                        adapter = prefs.llmOnlyWhileCharging.getAdapter(),
                        label = "Only while Charging",
                        description = "Perform heavy AI analysis only when the device is plugged in.",
                    )

                    SliderPreference(
                        label = "Minimum Battery Level",
                        adapter = prefs.llmMinBatteryLevel.getAdapter(),
                        valueRange = 0..100,
                        step = 5,
                        showAsPercentage = true,
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
                                // Mark as testing
                                testStatus = testStatus + (activeProvider to "⏳ Testing…")
                                val provider = when (activeProvider) {
                                    "google_ai" -> GoogleAIProvider(context)
                                    "claude" -> ClaudeProvider(context)
                                    "openai" -> OpenAIProvider(context)
                                    "perplexity" -> PerplexityProvider(context)
                                    else -> null
                                }
                                if (provider == null) {
                                    testStatus = testStatus + (activeProvider to "❌ Unknown provider")
                                    return@launch
                                }
                                val result = provider.testConnection()
                                val statusText = if (result.success) {
                                    val model = result.modelVersion ?: "Connected"
                                    val latency = result.latencyMs?.let { "${it}ms" } ?: ""
                                    // Follow up with a sample categorization to confirm
                                    // the model can actually classify, not just respond.
                                    val sampleResult = runCatching {
                                        provider.categorizeApp(
                                            appName = "Spotify",
                                            appPackage = "com.spotify.music",
                                            appDescription = "Music streaming",
                                            availableTabs = listOf("Music", "Entertainment", "Social", "Productivity", "Other"),
                                        )
                                    }.getOrNull()
                                    if (sampleResult != null) {
                                        "✅ $model · $latency · Spotify → ${sampleResult.tabName}"
                                    } else {
                                        "✅ $model · $latency (reachable, categorization test skipped)"
                                    }
                                } else {
                                    // Surface specific error detail from the provider
                                    val detail = result.error?.message?.takeIf { it.isNotBlank() }
                                        ?: result.message
                                    "❌ $detail"
                                }
                                testStatus = testStatus + (activeProvider to statusText)
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
                            text = "Other Available Engines",
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
                                            testStatus = testStatus + (pid to "⏳ Testing…")
                                            val provider = when (pid) {
                                                "google_ai" -> GoogleAIProvider(context)
                                                "claude" -> ClaudeProvider(context)
                                                "openai" -> OpenAIProvider(context)
                                                "perplexity" -> PerplexityProvider(context)
                                                else -> null
                                            }
                                            if (provider == null) {
                                                testStatus = testStatus + (pid to "❌ Unknown provider")
                                                return@launch
                                            }
                                            val result = provider.testConnection()
                                            val statusText = if (result.success) {
                                                val model = result.modelVersion ?: "Connected"
                                                val latency = result.latencyMs?.let { "${it}ms" } ?: ""
                                                val sampleResult = runCatching {
                                                    provider.categorizeApp(
                                                        appName = "Spotify",
                                                        appPackage = "com.spotify.music",
                                                        appDescription = "Music streaming",
                                                        availableTabs = listOf("Music", "Entertainment", "Social", "Productivity", "Other"),
                                                    )
                                                }.getOrNull()
                                                if (sampleResult != null) {
                                                    "✅ $model · $latency · Spotify → ${sampleResult.tabName}"
                                                } else {
                                                    "✅ $model · $latency (reachable, categorization test skipped)"
                                                }
                                            } else {
                                                val detail = result.error?.message?.takeIf { it.isNotBlank() }
                                                    ?: result.message
                                                "❌ $detail"
                                            }
                                            testStatus = testStatus + (pid to statusText)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                AnimatedVisibility(visible = showAdvanced) {
                    PreferenceGroup(heading = "Reliability & Safety") {
                        SwitchPreference(
                            adapter = prefs.circuitBreakerEnabled.getAdapter(),
                            label = "Smart Fallback",
                            description = "Automatically skip engines that are currently offline or unresponsive.",
                        )

                        AnimatedVisibility(visible = prefs.circuitBreakerEnabled.get()) {
                            Column {
                                SliderPreference(
                                    adapter = prefs.circuitBreakerFailureThreshold.getAdapter(),
                                    label = "Error Tolerance",
                                    valueRange = 1..10,
                                    step = 1,
                                    showUnit = " consecutive errors",
                                )
                            }
                        }

                        val devMode by prefs.autoCatDevMode.getAdapter().state
                        if (devMode) {
                            SwitchPreference(
                                adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                                label = "Force Parallel Streams",
                                description = "Run batches simultaneously ignoring rate limits. HIGH RISK.",
                            )
                        }
                    }
                }
            }

            // ===== ACCURACY STATS =====
            if (accuracyStats.isNotEmpty()) {
                item {
                    PreferenceGroup(heading = "Performance (30 Days)") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            accuracyStats.forEach { stats ->
                                CompactAccuracyCard(stats, isSelected = stats.provider == activeProvider)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            TextButton(
                                onClick = {
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        accuracyTracker?.resetStats()
                                        accuracyStats = emptyList()
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text("Reset Accuracy Metrics")
                            }
                        }
                    }
                }
            }

            // ===== ENGINE STATUS (under Advanced) =====
            item {
                AnimatedVisibility(visible = showAdvanced && (progress.isRunning || progress.processedCount > 0)) {
                    PreferenceGroup(heading = "Engine Status") {
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
                            progress = progress.progressPercentage,
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

@Composable
private fun LocalAiSection(
    prefs: app.lawnchair.preferences.PreferenceManager,
    scope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Device analysis state
    var capsLoading by remember { mutableStateOf(true) }
    var ramText by remember { mutableStateOf("") }
    var gpuText by remember { mutableStateOf("") }
    var androidText by remember { mutableStateOf("") }
    var recommendedId by remember { mutableStateOf<String?>(null) }
    var compatibleModelIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Connection test state
    var endpointTestStatus by remember { mutableStateOf<String?>(null) }
    var endpointTesting by remember { mutableStateOf(false) }
    var modelTestStatus by remember { mutableStateOf<String?>(null) }
    var modelTesting by remember { mutableStateOf(false) }

    val endpointEnabled by prefs.localEndpointEnabled.getAdapter().state
    val endpointUrl by prefs.localEndpointUrl.getAdapter().state
    val endpointModelId by prefs.localEndpointModelId.getAdapter().state
    val customModelPath by prefs.localCustomModelPath.getAdapter().state
    val selectedModelId by prefs.selectedLocalModelId.getAdapter().state

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val caps = DeviceCapabilityChecker.getCapabilities(context)
            val recommended = LocalModelRegistry.getRecommendedModel(caps)
            val compatible = LocalModelRegistry.getCompatibleModels(caps).map { it.id }.toSet()
            val totalGb = caps.totalRamMb / 1024.0
            ramText = "%.1f GB RAM".format(totalGb)
            gpuText = caps.gpuFamily.name.lowercase().replaceFirstChar { it.uppercase() }
            androidText = "Android ${caps.androidVersion}"
            recommendedId = recommended?.id
            compatibleModelIds = compatible
            capsLoading = false
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        // ── Device Analysis Card ─────────────────────────────────────────────
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.elevatedCardColors(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Device Analysis",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (capsLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(ramText, style = MaterialTheme.typography.bodySmall)
                        Text("·", style = MaterialTheme.typography.bodySmall)
                        Text(gpuText, style = MaterialTheme.typography.bodySmall)
                        Text("·", style = MaterialTheme.typography.bodySmall)
                        Text(androidText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // ── Local Server Section ─────────────────────────────────────────────
        Text(
            text = "Local Server (Ollama / LM Studio)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Ollama: http://localhost:11434  ·  LM Studio: http://localhost:1234",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        SwitchPreference(
            adapter = prefs.localEndpointEnabled.getAdapter(),
            label = "Enable local server",
            description = "Connect to an OpenAI-compatible server on your network.",
        )

        AnimatedVisibility(visible = endpointEnabled) {
            Column {
                TextPreference(
                    adapter = prefs.localEndpointUrl.getAdapter(),
                    label = "Server URL",
                )
                TextPreference(
                    adapter = prefs.localEndpointModelId.getAdapter(),
                    label = "Model name (blank = auto-detect)",
                    description = { it.ifBlank { "e.g. llama3.2, mistral, phi3" } },
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                endpointTesting = true
                                endpointTestStatus = null
                                val result = LocalEndpointProvider(
                                    context,
                                    endpointUrl,
                                    endpointModelId,
                                ).testConnection()
                                endpointTestStatus = if (result.success) {
                                    "✅ ${result.modelVersion ?: "Connected"} · ${result.latencyMs}ms"
                                } else {
                                    "❌ ${result.message}"
                                }
                                endpointTesting = false
                            }
                        },
                        enabled = !endpointTesting && endpointUrl.isNotBlank(),
                        modifier = Modifier.animateContentSize(),
                    ) {
                        Icon(Icons.Rounded.WifiTethering, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (endpointTesting) "Testing…" else "Test connection")
                    }

                    endpointTestStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── On-Device Model Browser ──────────────────────────────────────────
        Text(
            text = "On-Device Model",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text = "Place downloaded .bin files in ${context.filesDir}/local_models/",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        val mediaPipeModels = LocalModelRegistry.ALL_MODELS.filter {
            it.type == LocalModelType.MEDIAPIPE || it.type == LocalModelType.AICORE
        }
        mediaPipeModels.forEach { model ->
            val isCompatible = model.id in compatibleModelIds
            val isSelected = model.id == selectedModelId
            val isDownloaded = when (model.type) {
                LocalModelType.AICORE -> isCompatible

                LocalModelType.MEDIAPIPE -> File(
                    context.filesDir,
                    "local_models/${model.id}.bin",
                ).exists()

                else -> false
            }
            LocalModelCard(
                model = model,
                isCompatible = isCompatible,
                isRecommended = model.id == recommendedId,
                isSelected = isSelected,
                isDownloaded = isDownloaded,
                onSelect = {
                    scope.launch {
                        prefs.selectedLocalModelId.set(if (isSelected) "" else model.id)
                    }
                },
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Custom Model File Section ────────────────────────────────────────
        Text(
            text = "Custom Model File",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        TextPreference(
            adapter = prefs.localCustomModelPath.getAdapter(),
            label = "Full path to .bin file",
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        modelTesting = true
                        modelTestStatus = null
                        val result = MediaPipeLLMProvider(context, customModelPath).testConnection()
                        modelTestStatus = if (result.success) {
                            "✅ Loaded · ${result.latencyMs}ms"
                        } else {
                            "❌ ${result.message}"
                        }
                        modelTesting = false
                    }
                },
                enabled = !modelTesting && customModelPath.isNotBlank(),
                modifier = Modifier.animateContentSize(),
            ) {
                Icon(Icons.Outlined.Memory, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (modelTesting) "Loading…" else "Test model")
            }

            modelTestStatus?.let { status ->
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
private fun LocalModelCard(
    model: LocalModelInfo,
    isCompatible: Boolean,
    isRecommended: Boolean,
    isSelected: Boolean,
    isDownloaded: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isCompatible, onClick = onSelect),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = borderColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = if (isCompatible) onSelect else null,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompatible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (model.sizeMb > 0) {
                        val sizeLabel = if (model.sizeMb >= 1024) "%.1f GB".format(model.sizeMb / 1024f) else "${model.sizeMb} MB"
                        ModelChip(sizeLabel)
                    }
                    when {
                        isDownloaded -> ModelChip("Ready", MaterialTheme.colorScheme.primary)
                        !isCompatible -> ModelChip("Incompatible", MaterialTheme.colorScheme.error)
                        model.downloadUrl != null -> ModelChip("Not downloaded")
                    }
                    model.recommendationBadge?.let { ModelChip(it, MaterialTheme.colorScheme.tertiary) }
                }
                if (!isCompatible) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Requires Android ${model.minSdkVersion}+, ${model.minRamMb / 1024} GB RAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (!isDownloaded && model.type == LocalModelType.MEDIAPIPE && model.downloadUrl != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Download from: ${model.downloadUrl}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelChip(label: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.2f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun formatProviderName(provider: String): String = when (provider) {
    "google_ai" -> "Google AI"
    "claude" -> "Claude"
    "openai" -> "OpenAI"
    "perplexity" -> "Perplexity"
    else -> provider.replaceFirstChar { it.uppercase() }
}
