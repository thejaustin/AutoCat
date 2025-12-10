package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.LLMProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.categorization.llm.SuggestedCategory
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import kotlinx.coroutines.launch

@Composable
fun CategoryManagementPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { TabDatabase.getInstance(context) }
    val categoryDao = database.categoryDao()
    val categorizationManager = remember { CategorizationManager.getInstance(context) }
    val appProvider = remember { AutoCatAppProvider.getInstance(context) }

    var tabs by remember { mutableStateOf<List<CustomCategoryTab>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTab by remember { mutableStateOf<CustomCategoryTab?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var suggestedCategories by remember { mutableStateOf<List<SuggestedCategory>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var suggestionsError by remember { mutableStateOf<String?>(null) }
    var suggestionsProvider by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Load categories
    LaunchedEffect(Unit) {
        tabs = categoryDao.getAllCustomCategories()
    }

    PreferenceScaffold(
        label = "Manage Tabs",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create custom tabs for organizing your apps. The LLM will learn to auto-assign apps to these tabs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(tabs, key = { it.id }) { tab ->
                CategoryItem(
                    tab = tab,
                    onEdit = { editingTab = it },
                    onDelete = {
                        scope.launch {
                            categoryDao.deleteCustomCategory(it)
                            tabs = categoryDao.getAllCustomCategories()
                        }
                    },
                )
            }

            item {
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
                                    val installedApps = metadataProvider.getInstalledApps()

                                    if (installedApps.isEmpty()) {
                                        suggestionsError = "No apps found to analyze"
                                        return@launch
                                    }

                                    val appNames = installedApps.map { it.label }
                                    val existingTabs = tabs.map { it.name }

                                    android.util.Log.d("CategoryManagement", "Requesting suggestions for ${appNames.size} apps")

                                    // Try each provider until one succeeds
                                    var lastError: Exception? = null
                                    for (provider in providers) {
                                        try {
                                            if (!provider.isAvailable()) {
                                                android.util.Log.d("CategoryManagement", "${provider.name} not available, trying next")
                                                continue
                                            }

                                            android.util.Log.d("CategoryManagement", "Trying ${provider.name}")
                                            suggestedCategories = provider.suggestCategories(
                                                installedApps = appNames,
                                                existingTabs = existingTabs,
                                                maxSuggestions = 5,
                                            )

                                            android.util.Log.d("CategoryManagement", "Got ${suggestedCategories.size} suggestions from ${provider.name}")

                                            if (suggestedCategories.isEmpty()) {
                                                suggestionsError = "No new tabs suggested. You may already have all the useful tabs for your apps!"
                                            } else {
                                                suggestionsProvider = provider.name
                                                showSuggestionsDialog = true
                                            }
                                            return@launch // Success, exit
                                        } catch (e: Exception) {
                                            android.util.Log.e("CategoryManagement", "${provider.name} failed: ${e.message}")
                                            lastError = e
                                            // Try next provider
                                        }
                                    }

                                    // All providers failed
                                    suggestionsError = "All LLM providers failed. Please configure at least one API key in LLM Settings.\nLast error: ${lastError?.message}"
                                    android.util.Log.e("CategoryManagement", "All providers failed", lastError)
                                } catch (e: Exception) {
                                    android.util.Log.e("CategoryManagement", "Failed to get suggestions", e)
                                    suggestionsError = "Error: ${e.message}"
                                } finally {
                                    isLoadingSuggestions = false
                                }
                            }
                        },
                        enabled = !isLoadingSuggestions,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "AI Suggestions")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isLoadingSuggestions) "Analyzing..." else "Get AI Suggestions")
                    }

                    // Show error if any
                    if (suggestionsError != null) {
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = suggestionsError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // Show success message if any
                    if (successMessage != null) {
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = successMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
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
                    if (editingTab != null) {
                        // Edit existing
                        categoryDao.updateCustomCategory(
                            editingTab!!.copy(
                                name = name,
                                colorHex = color,
                            ),
                        )
                        successMessage = "✓ Tab '$name' updated"
                    } else {
                        // Add new tab
                        val maxSortOrder = tabs.maxOfOrNull { it.sortOrder } ?: 0
                        android.util.Log.d("CategoryManagement", "Creating new tab: $name, sortOrder: ${maxSortOrder + 1}, isVisible: true")
                        val tabId = categoryDao.insertCustomCategory(
                            CustomCategoryTab(
                                name = name,
                                colorHex = color,
                                sortOrder = maxSortOrder + 1,
                                isVisible = true,
                            ),
                        )
                        android.util.Log.d("CategoryManagement", "Tab created with ID: $tabId")

                        successMessage = "✓ Tab '$name' created! Use 'Re-categorize All Apps' in LLM Settings to assign apps."
                    }
                    tabs = categoryDao.getAllCustomCategories()
                    android.util.Log.d("CategoryManagement", "Total tabs after save: ${tabs.size}")
                    tabs.forEach { t ->
                        android.util.Log.d("CategoryManagement", "  - ${t.name} (visible: ${t.isVisible}, sortOrder: ${t.sortOrder})")
                    }
                    appProvider.refreshCache()
                    showAddDialog = false
                    editingTab = null
                    suggestionsError = null // Clear any previous errors
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
                    val maxSortOrder = tabs.maxOfOrNull { it.sortOrder } ?: 0
                    categoryDao.insertCustomCategory(
                        CustomCategoryTab(
                            name = suggestion.name,
                            colorHex = "#4CAF50", // Default green color
                            sortOrder = maxSortOrder + 1,
                        ),
                    )
                    tabs = categoryDao.getAllCustomCategories()
                    appProvider.refreshCache()

                    successMessage = "✓ Added '${suggestion.name}' tab! Use 'Re-categorize All Apps' in LLM Settings to assign apps."
                    suggestionsError = null
                }
            },
        )
    }
}

@Composable
private fun CategoryItem(
    tab: CustomCategoryTab,
    onEdit: (CustomCategoryTab) -> Unit,
    onDelete: (CustomCategoryTab) -> Unit,
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
    tab: CustomCategoryTab?,
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

                // Color picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    predefinedColors.chunked(5).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { (color, label) ->
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
                        text = "No suggestions available. Make sure you have enough apps installed.",
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
                                TextButton(onClick = { onAddCategory(suggestion) }) {
                                    Text("Add")
                                }
                            }

                            Text(
                                text = suggestion.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (suggestion.exampleApps.isNotEmpty()) {
                                Text(
                                    text = "Examples: ${suggestion.exampleApps.take(3).joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
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
