package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.preferences.preferenceManager
import kotlinx.coroutines.launch

private data class ProviderOption(val id: String, val label: String)

private val providers = listOf(
    ProviderOption("google_ai", "Google AI (Gemini)"),
    ProviderOption("claude", "Anthropic Claude"),
    ProviderOption("openai", "OpenAI GPT"),
    ProviderOption("perplexity", "Perplexity"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCategoriesOnboardingPreferences(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Step 1 state — provider + key
    var selectedProviderId by remember { mutableStateOf(prefs.llmProviderPreference.get()) }
    var apiKey by remember { mutableStateOf("") }
    var providerDropdownExpanded by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun complete() {
        prefs.smartCategoriesOnboardingCompleted.set(true)
        onFinish()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Step dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalSteps) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == step) 20.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i <= step) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                ),
                        )
                    }
                }
                TextButton(onClick = { complete() }) {
                    Text("Skip")
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 0) {
                    TextButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        when (step) {
                            0 -> step = 1

                            1 -> {
                                // Save provider + key, advance
                                prefs.llmProviderPreference.set(selectedProviderId)
                                when (selectedProviderId) {
                                    "google_ai" -> prefs.llmGoogleAIKey.set(apiKey)
                                    "claude" -> prefs.llmClaudeKey.set(apiKey)
                                    "openai" -> prefs.llmOpenAIKey.set(apiKey)
                                    "perplexity" -> prefs.llmPerplexityKey.set(apiKey)
                                }
                                step = 2
                            }

                            2 -> step = 3

                            3 -> complete()
                        }
                    },
                    modifier = Modifier.weight(if (step == 0) 2f else 1f),
                    enabled = step != 1 || apiKey.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = when (step) {
                            0 -> "Get Started"
                            1 -> "Continue"
                            else -> "Done"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "onboarding_step",
        ) { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep()

                1 -> AISetupStep(
                    selectedProviderId = selectedProviderId,
                    onProviderSelect = {
                        selectedProviderId = it
                        apiKey = ""
                        testStatus = null
                    },
                    apiKey = apiKey,
                    onApiKeyChange = {
                        apiKey = it
                        testStatus = null
                    },
                    providerDropdownExpanded = providerDropdownExpanded,
                    onDropdownExpand = { providerDropdownExpanded = it },
                    testStatus = testStatus,
                    isTesting = isTesting,
                    onTest = {
                        scope.launch {
                            isTesting = true
                            testStatus = null
                            try {
                                val provider = when (selectedProviderId) {
                                    "claude" -> ClaudeProvider(context, apiKey)
                                    "openai" -> OpenAIProvider(context, apiKey)
                                    "perplexity" -> PerplexityProvider(context, apiKey)
                                    else -> GoogleAIProvider(context, apiKey)
                                }
                                val result = provider.testConnection()
                                testStatus = if (result.success) {
                                    val model = result.modelVersion ?: "Connected"
                                    "✅ $model · ${result.latencyMs}ms"
                                } else {
                                    "❌ ${result.message}"
                                }
                            } catch (e: Exception) {
                                testStatus = "❌ ${e.message}"
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                )

                2 -> SmartFeaturesStep()

                3 -> ReadyStep()
            }
        }
    }
}

@Composable
fun SmartFeaturesStep(modifier: Modifier = Modifier) {
    val prefs = preferenceManager()
    var smartDockEnabled by remember { mutableStateOf(prefs.autoCatSmartDockEnabled.get()) }
    var genAIFolderNamingEnabled by remember { mutableStateOf(prefs.autoCatGenAIFolderNaming.get()) }
    var pwaIntegrationEnabled by remember { mutableStateOf(prefs.autoCatPwaIntegrationEnabled.get()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .padding(bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "Enable Smart Features",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Text(
            text = "Enhance your launcher with predictive and AI-generated features.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "Smart Dock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Predicts which apps you need in your dock based on time of day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = smartDockEnabled,
                    onCheckedChange = {
                        smartDockEnabled = it
                        prefs.autoCatSmartDockEnabled.set(it)
                    },
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "GenAI Folder Naming",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Automatically suggest names when creating folders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = genAIFolderNamingEnabled,
                    onCheckedChange = {
                        genAIFolderNamingEnabled = it
                        prefs.autoCatGenAIFolderNaming.set(it)
                    },
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "PWA & Web Shortcuts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Automatically categorize Progressive Web Apps and web shortcuts in the drawer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = pwaIntegrationEnabled,
                    onCheckedChange = {
                        pwaIntegrationEnabled = it
                        prefs.autoCatPwaIntegrationEnabled.set(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Smart Categories",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "AutoCat uses AI to automatically organize your apps into meaningful groups — so your drawer stays tidy without any manual sorting.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeatureBullet(
                    icon = Icons.Rounded.Psychology,
                    title = "AI-Powered",
                    description = "Categorizes apps using your choice of local ML, Gemini, Claude, GPT, or Perplexity.",
                )
                FeatureBullet(
                    icon = Icons.Rounded.Inventory,
                    title = "The Vault & Archiving",
                    description = "Smart predictive archiving keeps your drawer clean by automatically suggesting unused apps for cold storage.",
                )
                FeatureBullet(
                    icon = Icons.Rounded.Layers,
                    title = "Smart Organization",
                    description = "Declutter your home screen with Folder Covers and experimental Widget Stacking.",
                )
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AISetupStep(
    selectedProviderId: String,
    onProviderSelect: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    providerDropdownExpanded: Boolean,
    onDropdownExpand: (Boolean) -> Unit,
    testStatus: String?,
    isTesting: Boolean,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedProvider = providers.find { it.id == selectedProviderId } ?: providers.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Choose an AI Engine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a provider and enter your API key. You can change this later in AI Engine settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Provider dropdown
        ExposedDropdownMenuBox(
            expanded = providerDropdownExpanded,
            onExpandedChange = onDropdownExpand,
        ) {
            OutlinedTextField(
                value = selectedProvider.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("AI Provider") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = providerDropdownExpanded,
                onDismissRequest = { onDropdownExpand(false) },
            ) {
                providers.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onProviderSelect(option.id)
                            onDropdownExpand(false)
                        },
                    )
                }
            }
        }

        // API Key input
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
            placeholder = { Text("Paste your ${selectedProvider.label} key here") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // Test connection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onTest,
                enabled = apiKey.isNotBlank() && !isTesting,
                colors = ButtonDefaults.outlinedButtonColors(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing…")
                } else {
                    Text("Test Connection")
                }
            }

            testStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status.startsWith("✅")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }

        // Hint about where to get keys
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Free tiers are available for all providers. Google AI (Gemini) is recommended for getting started.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun ReadyStep(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your AI engine is configured. Tap Done to go back to Smart Categories and run your first categorization.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
