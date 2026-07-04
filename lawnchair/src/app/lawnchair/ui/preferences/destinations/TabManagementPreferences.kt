package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.categorization.llm.SuggestedCategory
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.util.rememberExpressiveHaptics
import com.android.launcher3.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TabManagementPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberExpressiveHaptics()

    var database by remember { mutableStateOf<TabDatabase?>(null) }
    var appProvider by remember { mutableStateOf<AutoCatAppProvider?>(null) }
    var initializationError by remember { mutableStateOf<String?>(null) }

    var tabs by remember { mutableStateOf<List<CustomTab>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTab by remember { mutableStateOf<CustomTab?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var suggestedTabs by remember { mutableStateOf<List<SuggestedCategory>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var suggestionsError by remember { mutableStateOf<String?>(null) }
    var suggestionsProvider by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Initialize services and load tabs safely
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                database = TabDatabase.getInstance(context)
                appProvider = AutoCatAppProvider.getInstance(context)
                tabs = database?.tabDao()?.getAllCustomTabs() ?: emptyList()
            } catch (e: Exception) {
                android.util.Log.e("TabManagement", "Error initializing: ${e.message}", e)
                initializationError = "Failed to initialize: ${e.message}"
            }
        }
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
                        text = stringResource(R.string.tab_management_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Text(
                    text = "Your Tabs",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (tabs.isEmpty() && initializationError == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "No tabs yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Create a custom tab or let AI suggest groupings for your apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            items(tabs, key = { it.id }) { tab ->
                TabItem(
                    tab = tab,
                    onEdit = { editingTab = it },
                    onDelete = {
                        haptics.click()
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            database?.tabDao()?.deleteCustomTab(it)
                            tabs = database?.tabDao()?.getAllCustomTabs() ?: emptyList()
                        }
                    },
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Manual Add
                    ElevatedButton(
                        onClick = {
                            haptics.click()
                            showAddDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Create Custom Tab", fontWeight = FontWeight.Bold)
                    }

                    // AI Discovery Section
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Explore, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Discover Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text(
                                text = "Let AI analyze your apps and suggest logical groupings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )

                            FilledTonalButton(
                                onClick = {
                                    haptics.click()
                                    isLoadingSuggestions = true
                                    suggestionsError = null
                                    scope.launch {
                                        try {
                                            val metadataProvider = AppMetadataProvider(context)
                                            val installedApps = metadataProvider.getInstalledApps().map { it.label }
                                            val existingTabs = tabs.map { it.name }

                                            val prefManager = app.lawnchair.preferences.PreferenceManager.getInstance(context)
                                            val providerId = prefManager.llmProviderPreference.get()
                                            val provider = when (providerId) {
                                                "google_ai" -> GoogleAIProvider(context)
                                                "claude" -> ClaudeProvider(context)
                                                "openai" -> OpenAIProvider(context)
                                                "perplexity" -> PerplexityProvider(context)
                                                else -> GoogleAIProvider(context)
                                            }

                                            suggestionsProvider = provider.name
                                            suggestedTabs = provider.suggestCategories(installedApps, existingTabs, 5)
                                            showSuggestionsDialog = true
                                            haptics.success()
                                        } catch (e: Exception) {
                                            android.util.Log.e("TabManagement", "Error getting suggestions: ${e.message}", e)
                                            suggestionsError = "Failed to get suggestions: ${e.message}"
                                            haptics.error()
                                        } finally {
                                            isLoadingSuggestions = false
                                        }
                                    }
                                },
                                enabled = !isLoadingSuggestions,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isLoadingSuggestions) "Analyzing..." else "Suggest Groups")
                            }
                        }
                    }

                    // Material 3 Expressive: Enhanced error message card

                    if (suggestionsError != null) {
                        Card(

                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),

                            shape = RoundedCornerShape(14.dp),

                        ) {
                            Text(

                                text = suggestionsError!!,

                                style = MaterialTheme.typography.bodySmall,

                                color = MaterialTheme.colorScheme.onErrorContainer,

                                modifier = Modifier.padding(12.dp),

                            )
                        }
                    }

                    // Material 3 Expressive: Enhanced success message card

                    if (successMessage != null) {
                        Card(

                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),

                            shape = RoundedCornerShape(14.dp),

                        ) {
                            Row(

                                modifier = Modifier.padding(12.dp),

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.spacedBy(8.dp),

                            ) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))

                                Text(

                                    text = successMessage!!,

                                    style = MaterialTheme.typography.bodySmall,

                                    color = MaterialTheme.colorScheme.onPrimaryContainer,

                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingTab != null) {
        TabDialog(
            tab = editingTab,
            onDismiss = {
                showAddDialog = false
                editingTab = null
            },
            onSave = { name, color, hideInZenMode ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (editingTab != null) {
                            // Edit existing
                            database?.tabDao()?.updateCustomTab(
                                editingTab!!.copy(
                                    name = name,
                                    colorHex = color,
                                    hideInZenMode = hideInZenMode,
                                ),
                            )
                            successMessage = "✓ Tab '$name' updated"
                        } else {
                            // Add new tab
                            val maxSortOrder = tabs.maxOfOrNull { it.sortOrder } ?: 0
                            android.util.Log.d("TabManagement", "Creating new tab: $name, sortOrder: ${maxSortOrder + 1}, isVisible: true")
                            val tabId = database?.tabDao()?.insertCustomTab(
                                CustomTab(
                                    name = name,
                                    colorHex = color,
                                    sortOrder = maxSortOrder + 1,
                                    isVisible = true,
                                    hideInZenMode = hideInZenMode,
                                ),
                            )
                            android.util.Log.d("TabManagement", "Tab created with ID: $tabId")

                            successMessage = "✓ Tab '$name' created! Use 'Re-categorize All Apps' in LLM Settings to assign apps."
                        }
                        tabs = database?.tabDao()?.getAllCustomTabs() ?: emptyList()
                        android.util.Log.d("TabManagement", "Total tabs after save: ${tabs.size}")
                        tabs.forEach { t ->
                            android.util.Log.d("TabManagement", "  - ${t.name} (visible: ${t.isVisible}, sortOrder: ${t.sortOrder})")
                        }
                        appProvider?.refreshCache()
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            haptics.success()
                            showAddDialog = false
                            editingTab = null
                            suggestionsError = null // Clear any previous errors
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TabManagement", "Error saving tab: ${e.message}", e)
                        successMessage = "❌ Error: ${e.message}"
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            haptics.error()
                        }
                    }
                }
            },
        )
    }

    // Suggestions Dialog
    if (showSuggestionsDialog) {
        SuggestionsDialog(
            suggestions = suggestedTabs,
            providerName = suggestionsProvider,
            onDismiss = { showSuggestionsDialog = false },
            onAddTab = { suggestion ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val maxSortOrder = tabs.maxOfOrNull { it.sortOrder } ?: 0
                        database?.tabDao()?.insertCustomTab(
                            CustomTab(
                                name = suggestion.name,
                                colorHex = "#4CAF50", // Default green color
                                sortOrder = maxSortOrder + 1,
                            ),
                        )
                        tabs = database?.tabDao()?.getAllCustomTabs() ?: emptyList()
                        appProvider?.refreshCache()

                        successMessage = "✓ Added '${suggestion.name}' tab! Use 'Re-categorize All Apps' in LLM Settings to assign apps."
                        suggestionsError = null
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            haptics.success()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TabManagement", "Error adding suggested tab: ${e.message}", e)
                        successMessage = "❌ Error: ${e.message}"
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            haptics.error()
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun TabItem(
    tab: CustomTab,
    onEdit: (CustomTab) -> Unit,
    onDelete: (CustomTab) -> Unit,
) {
    val tabColor = remember(tab.colorHex) { parseColor(tab.colorHex) }

    // Material 3 Expressive: Enhanced card with gradient and shadow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = tabColor.copy(alpha = 0.2f),
                spotColor = tabColor.copy(alpha = 0.3f),
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tabColor.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.0f),
                        ),
                    ),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                // Material 3 Expressive: Enhanced color indicator with border
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 2.5.dp,
                            color = tabColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .background(
                            color = tabColor,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Label,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = tab.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onEdit(tab) }) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { onDelete(tab) }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabDialog(
    tab: CustomTab?,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(tab?.name ?: "") }
    var colorHex by remember { mutableStateOf(tab?.colorHex ?: "#4CAF50") }
    var hideInZenMode by remember { mutableStateOf(tab?.hideInZenMode ?: false) }

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

                // Material 3 Expressive: Enhanced color picker with borders and animations
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    predefinedColors.chunked(5).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { (color, label) ->
                                val isSelected = colorHex == color
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 48.dp else 44.dp)
                                        .shadow(
                                            elevation = if (isSelected) 6.dp else 2.dp,
                                            shape = CircleShape,
                                            ambientColor = parseColor(color).copy(alpha = 0.3f),
                                            spotColor = parseColor(color).copy(alpha = 0.4f),
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = if (isSelected) {
                                                parseColor(color).copy(alpha = 0.6f)
                                            } else {
                                                parseColor(color).copy(alpha = 0.3f)
                                            },
                                            shape = CircleShape,
                                        )
                                        .background(
                                            color = parseColor(color),
                                            shape = CircleShape,
                                        )
                                        .clickable { colorHex = color }
                                        .animateContentSize(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .shadow(2.dp, CircleShape),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Custom hex color input
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = colorHex,
                            onValueChange = { input ->
                                val sanitized = if (input.startsWith("#")) input else "#$input"
                                if (sanitized.length <= 7) colorHex = sanitized
                            },
                            label = { Text("Custom (#rrggbb)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = parseColor(colorHex),
                                    shape = CircleShape,
                                )
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hideInZenMode = !hideInZenMode }
                            .padding(vertical = 8.dp),
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = hideInZenMode,
                            onCheckedChange = { hideInZenMode = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Hide in Zen Mode", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Hides this tab when Do Not Disturb is active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, colorHex, hideInZenMode) },
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
                Text(
                    "AI Tab Suggestions",
                    fontWeight = FontWeight.Bold,
                )
                if (providerName != null) {
                    Text(
                        text = "Powered by $providerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Based on your installed apps, here are some suggested tabs:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (suggestions.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "No suggestions available. Make sure you have enough apps installed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    // Material 3 Expressive: Enhanced suggestion cards
                    suggestions.forEach { suggestion ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(14.dp),
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 1.dp,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = suggestion.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    FilledTonalButton(
                                        onClick = { onAddTab(suggestion) },
                                        shape = RoundedCornerShape(10.dp),
                                    ) {
                                        Text("Add", fontWeight = FontWeight.Medium)
                                    }
                                }

                                Text(
                                    text = suggestion.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                if (suggestion.exampleApps.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp),
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = "Examples: ${suggestion.exampleApps.take(3).joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
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
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Medium)
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
