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
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.SuggestedCategory
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.data.category.entities.CustomCategory
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
    val database = remember { CategoryDatabase.getInstance(context) }
    val categoryDao = database.categoryDao()
    val categorizationManager = remember { CategorizationManager.getInstance(context) }
    val appProvider = remember { AutoCatAppProvider.getInstance(context) }

    var categories by remember { mutableStateOf<List<CustomCategory>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var suggestedCategories by remember { mutableStateOf<List<SuggestedCategory>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }

    // Load categories
    LaunchedEffect(Unit) {
        categories = categoryDao.getAllCustomCategories()
    }

    PreferenceScaffold(
        label = "Manage Categories",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create custom categories for organizing your apps. The LLM will learn to auto-assign apps to these categories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(categories, key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { editingCategory = it },
                    onDelete = {
                        scope.launch {
                            categoryDao.deleteCustomCategory(it)
                            categories = categoryDao.getAllCustomCategories()
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
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Category")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingSuggestions = true
                                try {
                                    val llmProvider = GoogleAIProvider(context)
                                    if (llmProvider.isAvailable()) {
                                        val metadataProvider = AppMetadataProvider(context)
                                        val installedApps = metadataProvider.getInstalledApps()
                                        val appNames = installedApps.map { it.label }
                                        val existingCategories = categories.map { it.name }

                                        suggestedCategories = llmProvider.suggestCategories(
                                            installedApps = appNames,
                                            existingCategories = existingCategories,
                                            maxSuggestions = 5,
                                        )
                                        showSuggestionsDialog = true
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("CategoryManagement", "Failed to get suggestions", e)
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
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog || editingCategory != null) {
        CategoryDialog(
            category = editingCategory,
            onDismiss = {
                showAddDialog = false
                editingCategory = null
            },
            onSave = { name, color ->
                scope.launch {
                    if (editingCategory != null) {
                        // Edit existing
                        categoryDao.updateCustomCategory(
                            editingCategory!!.copy(
                                name = name,
                                colorHex = color,
                            ),
                        )
                    } else {
                        // Add new category
                        val maxSortOrder = categories.maxOfOrNull { it.sortOrder } ?: 0
                        categoryDao.insertCustomCategory(
                            CustomCategory(
                                name = name,
                                colorHex = color,
                                sortOrder = maxSortOrder + 1,
                            ),
                        )

                        // Trigger recategorization so LLM can assign apps to the new category
                        // This runs in background and won't block the UI
                        categorizationManager.recategorizeAll()
                    }
                    categories = categoryDao.getAllCustomCategories()
                    appProvider.refreshCache()
                    showAddDialog = false
                    editingCategory = null
                }
            },
        )
    }

    // Suggestions Dialog
    if (showSuggestionsDialog) {
        SuggestionsDialog(
            suggestions = suggestedCategories,
            onDismiss = { showSuggestionsDialog = false },
            onAddCategory = { suggestion ->
                scope.launch {
                    val maxSortOrder = categories.maxOfOrNull { it.sortOrder } ?: 0
                    categoryDao.insertCustomCategory(
                        CustomCategory(
                            name = suggestion.name,
                            colorHex = "#4CAF50", // Default green color
                            sortOrder = maxSortOrder + 1,
                        ),
                    )
                    categories = categoryDao.getAllCustomCategories()
                    appProvider.refreshCache()

                    // Trigger recategorization
                    categorizationManager.recategorizeAll()
                }
            },
        )
    }
}

@Composable
private fun CategoryItem(
    category: CustomCategory,
    onEdit: (CustomCategory) -> Unit,
    onDelete: (CustomCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(category) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = parseColor(category.colorHex),
                        shape = CircleShape,
                    ),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row {
            IconButton(onClick = { onEdit(category) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { onDelete(category) }) {
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
    category: CustomCategory?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#4CAF50") }

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
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
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
    onDismiss: () -> Unit,
    onAddCategory: (SuggestedCategory) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Category Suggestions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Based on your installed apps, here are some suggested categories:",
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
