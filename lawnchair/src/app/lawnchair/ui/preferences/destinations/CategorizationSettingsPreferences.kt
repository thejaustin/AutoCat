package app.lawnchair.ui.preferences.destinations

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.CategoryTabsController
import app.lawnchair.categorization.importer.SmartLauncherImporter
import app.lawnchair.categorization.importer.SmartLauncherImporter.ImportResult
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.categorization.llm.SuggestedCategory
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CategorizationSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()

    var categorizationManager by remember { mutableStateOf<CategorizationManager?>(null) }
    var database by remember { mutableStateOf<TabDatabase?>(null) }
    var appProvider by remember { mutableStateOf<AutoCatAppProvider?>(null) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    val progress by (
        categorizationManager?.progress ?: kotlinx.coroutines.flow.MutableStateFlow(
            app.lawnchair.categorization.CategorizationProgress(),
        )
        ).collectAsState()

    var categorizationStatus by remember { mutableStateOf("") }
    var tabs by remember { mutableStateOf<List<CustomTab>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTab by remember { mutableStateOf<CustomTab?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var suggestedCategories by remember { mutableStateOf<List<SuggestedCategory>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var suggestionsError by remember { mutableStateOf<String?>(null) }
    var suggestionsProvider by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Initialize services and load tabs safely
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                categorizationManager = CategorizationManager.getInstance(context)
                database = TabDatabase.getInstance(context)
                appProvider = AutoCatAppProvider.getInstance(context)
                tabs = database?.categoryDao()?.getAllCustomCategories() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("CategorizationSettings", "Error initializing: ${e.message}", e)
                initializationError = "Failed to initialize: ${e.message}"
            }
        }
    }

    val slImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            categorizationStatus = "Importing..."
            scope.launch {
                val importer = SmartLauncherImporter(context)
                val result = importer.importFromUri(uri)
                categorizationStatus = when (result) {
                    is ImportResult.Success -> {
                        CategoryTabsController.getInstance(context).refresh()
                        // Auto-sort apps into folders
                        val sortResult = app.lawnchair.categorization.FolderAutoSortService.getInstance(context).autoSortAll()
                        "✅ Imported ${result.count} apps. Created ${sortResult.foldersCreated} folders with ${sortResult.appsSorted} apps."
                    }

                    is ImportResult.Error -> "❌ Import failed: ${result.message}"
                }
            }
        }
    }

    PreferenceScaffold(
        label = "App Categorization",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // ===== QUICK ACTIONS =====
            item {
                PreferenceGroup(heading = "Quick Actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Material 3 Expressive: Enhanced primary action button
                        ElevatedButton(
                            onClick = {
                                categorizationStatus = ""
                                scope.launch {
                                    try {
                                        categorizationManager?.recategorizeAll()
                                        categorizationStatus = "✅ Categorization complete! Check your app drawer."
                                    } catch (e: Exception) {
                                        categorizationStatus = "❌ Error: ${e.message}"
                                    }
                                }
                            },
                            enabled = !progress.isRunning && categorizationManager != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.elevatedButtonElevation(
                                defaultElevation = 3.dp,
                                pressedElevation = 6.dp,
                            ),
                        ) {
                            Text(
                                if (progress.isRunning) "Categorizing..." else "Re-categorize All Apps",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        OutlinedButton(
                            onClick = { slImportLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Import from Smart Launcher (.slbk)")
                        }

                        // Progress indicator
                        AnimatedVisibility(visible = progress.isRunning || progress.processedCount > 0) {
                            CategorizationProgress(progress)
                        }

                        // Material 3 Expressive: Enhanced status message card
                        if (categorizationStatus.isNotEmpty()) {
                            val isSuccess = categorizationStatus.startsWith("✅")
                            val isError = categorizationStatus.startsWith("❌")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium,
                                        ),
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isSuccess -> MaterialTheme.colorScheme.primaryContainer
                                        isError -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (isSuccess || isError) {
                                        Icon(
                                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (isSuccess) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                    Text(
                                        text = categorizationStatus.removePrefix("✅ ").removePrefix("❌ "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = when {
                                            isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isError -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ===== MANAGE TABS =====
            item {
                PreferenceGroup(heading = "Manage Tabs") {
                    Column {
                        // Tab List
                        tabs.forEach { tab ->
                            CategoryItem(
                                tab = tab,
                                onEdit = { editingTab = it },
                                onDelete = { target ->
                                    scope.launch(Dispatchers.IO) {
                                        database?.categoryDao()?.deleteCustomCategory(target)
                                        tabs = database?.categoryDao()?.getAllCustomCategories() ?: emptyList()
                                    }
                                },
                            )
                        }

                        // Add Tab / AI Suggestions Buttons
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Tab")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Tab")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoadingSuggestions = true
                                        suggestionsError = null
                                        successMessage = null
                                        try {
                                            // Get user's preferred provider
                                            val prefManager = app.lawnchair.preferences.PreferenceManager.getInstance(context)
                                            val preferredProviderId = prefManager.llmProviderPreference.get()

                                            val googleProvider = GoogleAIProvider(context)
                                            val allProviderMap = mapOf(
                                                "google_ai" to googleProvider,
                                                "claude" to ClaudeProvider(context),
                                                "openai" to OpenAIProvider(context),
                                                "perplexity" to PerplexityProvider(context),
                                            )

                                            // Order providers: Preferred first, then others as fallback
                                            val primary = allProviderMap[preferredProviderId] ?: googleProvider
                                            val fallbacks = allProviderMap.values.filter { it.name != primary.name }
                                            val providers = listOf(primary) + fallbacks

                                            val metadataProvider = AppMetadataProvider(context)
                                            val installedApps = withContext(Dispatchers.IO) { metadataProvider.getInstalledApps() }

                                            if (installedApps.isEmpty()) {
                                                suggestionsError = "No apps found to analyze"
                                                return@launch
                                            }

                                            val appNames = installedApps.map { it.label }
                                            val existingTabs = tabs.map { it.name }

                                            // Try each provider until one succeeds
                                            var lastError: Exception? = null
                                            for (provider in providers) {
                                                try {
                                                    if (!provider.isAvailable()) continue

                                                    suggestedCategories = withContext(Dispatchers.IO) {
                                                        provider.suggestCategories(
                                                            installedApps = appNames,
                                                            existingTabs = existingTabs,
                                                            maxSuggestions = 5,
                                                        )
                                                    }

                                                    if (suggestedCategories.isEmpty()) {
                                                        suggestionsError = "No new tabs suggested."
                                                    } else {
                                                        suggestionsProvider = provider.name
                                                        showSuggestionsDialog = true
                                                    }
                                                    return@launch // Success
                                                } catch (e: Exception) {
                                                    lastError = e
                                                }
                                            }
                                            suggestionsError = "All providers failed. Last error: ${lastError?.message}"
                                        } catch (e: Exception) {
                                            suggestionsError = "Error: ${e.message}"
                                        } finally {
                                            isLoadingSuggestions = false
                                        }
                                    }
                                },
                                enabled = !isLoadingSuggestions,
                            ) {
                                Icon(Icons.Rounded.Psychology, contentDescription = "AI Suggestions")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isLoadingSuggestions) "Analyzing..." else "Get AI Suggestions")
                            }

                            if (isLoadingSuggestions) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            if (suggestionsError != null) {
                                Text(
                                    text = suggestionsError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }

                            if (successMessage != null) {
                                Text(
                                    text = successMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // ===== NAVIGATION =====
            item {
                PreferenceGroup(heading = "Other Settings") {
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
                }
            }

            // ===== DISPLAY OPTIONS =====
            item {
                PreferenceGroup(heading = "Display Options") {
                    val useTabs by prefs.autoCatUseTabs.getAdapter().state

                    SwitchPreference(
                        adapter = prefs.autoCatUseTabs.getAdapter(),
                        label = "Use App Tabs",
                        description = "Show tabs in app drawer. Apps are organized by tab.",
                    )

                    AnimatedVisibility(visible = useTabs) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            SwitchPreference(
                                adapter = prefs.hideWorkApps.getAdapter(),
                                label = "Hide Work Apps",
                                description = "Hide work profile apps from the app drawer",
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SwitchPreference(
                                adapter = prefs.showWorkTab.getAdapter(),
                                label = "Show Work Tab",
                                description = "Add a dedicated Work tab",
                            )
                        }
                    }
                }
            }

            // ===== BEHAVIOR & PERFORMANCE =====
            item {
                PreferenceGroup(heading = "Behavior & Performance") {
                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Batch Processing",
                        description = "Process multiple apps per API call (20x faster)",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to App Drawer Folders",
                        description = "Automatically create folders in app drawer for each tab",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SwitchPreference(
                        adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                        label = "Rate Limiting",
                        description = "Add delays between batches (Recommended for free tier)",
                    )
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingTab != null) {
        CategoryDialog(
            tab = editingTab,
            onDismiss = {
                showAddDialog = false
                editingTab = null
            },
            onSave = { name, color ->
                scope.launch {
                    try {
                        if (editingTab != null) {
                            withContext(Dispatchers.IO) {
                                database?.categoryDao()?.updateCustomCategory(editingTab!!.copy(name = name, colorHex = color))
                            }
                            successMessage = "✓ Tab '$name' updated"
                        } else {
                            withContext(Dispatchers.IO) {
                                val maxSortOrder = tabs.maxOfOrNull { it.sortOrder } ?: 0
                                database?.categoryDao()?.insertCustomCategory(
                                    CustomTab(name = name, colorHex = color, sortOrder = maxSortOrder + 1, isVisible = true),
                                )
                            }
                            successMessage = "✓ Tab '$name' created!"
                        }
                        tabs = withContext(Dispatchers.IO) { database?.categoryDao()?.getAllCustomCategories() ?: emptyList() }
                        appProvider?.refreshCache()
                        showAddDialog = false
                        editingTab = null
                    } catch (e: Exception) {
                        android.util.Log.e("CategorizationSettings", "Error saving tab: ${e.message}", e)
                        successMessage = "❌ Error: ${e.message}"
                    }
                }
            },
        )
    }

    // Suggestions Dialog
    if (showSuggestionsDialog) {
        SuggestionsDialog(
            suggestions = suggestedCategories,
            providerName = suggestionsProvider,
            onDismiss = { showSuggestionsDialog = false },
            onAddTab = { suggestion ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val maxSortOrder = tabs.maxOfOrNull { it.sortOrder } ?: 0
                            database?.categoryDao()?.insertCustomCategory(
                                CustomTab(name = suggestion.name, colorHex = "#4CAF50", sortOrder = maxSortOrder + 1),
                            )
                            tabs = database?.categoryDao()?.getAllCustomCategories() ?: emptyList()
                        }
                        appProvider?.refreshCache()
                        successMessage = "✓ Added '${suggestion.name}' tab!"
                    } catch (e: Exception) {
                        android.util.Log.e("CategorizationSettings", "Error adding suggested tab: ${e.message}", e)
                        successMessage = "❌ Error: ${e.message}"
                    }
                }
            },
        )
    }
}

@Composable
private fun CategoryItem(
    tab: CustomTab,
    onEdit: (CustomTab) -> Unit,
    onDelete: (CustomTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(tab) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = parseColor(tab.colorHex),
                        shape = CircleShape,
                    ),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = tab.name,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row {
            IconButton(onClick = { onEdit(tab) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { onDelete(tab) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    tab: CustomTab?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(tab?.name ?: "") }
    var colorHex by remember { mutableStateOf(tab?.colorHex ?: "#4CAF50") }

    val predefinedColors = listOf(
        "#4CAF50" to "Green",
        "#2196F3" to "Blue",
        "#FF9800" to "Orange",
        "#E91E63" to "Pink",
        "#9C27B0" to "Purple",
        "#00BCD4" to "Cyan",
        "#FF5722" to "Red",
        "#9E9E9E" to "Gray",
        "#FFC107" to "Amber",
        "#3F51B5" to "Indigo",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tab == null) "Add Tab" else "Edit Tab") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tab Name") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.padding(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    predefinedColors.chunked(5).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { (color, _) ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = parseColor(color),
                                            shape = CircleShape,
                                        )
                                        .clickable { colorHex = color },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (colorHex == color) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, colorHex) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SuggestionsDialog(
    suggestions: List<SuggestedCategory>,
    providerName: String?,
    onDismiss: () -> Unit,
    onAddTab: (SuggestedCategory) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("AI Tab Suggestions")
                if (providerName != null) {
                    Text(
                        text = "Powered by $providerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Based on your installed apps, here are some suggested tabs:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (suggestions.isEmpty()) {
                    Text(
                        text = "No suggestions available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    suggestions.forEach { suggestion ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = suggestion.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { onAddTab(suggestion) }) {
                                    Text("Add")
                                }
                            }
                            Text(
                                text = suggestion.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
