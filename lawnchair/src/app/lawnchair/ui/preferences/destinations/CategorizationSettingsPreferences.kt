package app.lawnchair.ui.preferences.destinations

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.CategorizationManager
import app.lawnchair.categorization.CategoryFolderSyncService
import app.lawnchair.categorization.importer.SmartLauncherImporter
import app.lawnchair.categorization.importer.SmartLauncherImporter.ImportResult
import app.lawnchair.categorization.llm.ClaudeProvider
import app.lawnchair.categorization.llm.GoogleAIProvider
import app.lawnchair.categorization.llm.OpenAIProvider
import app.lawnchair.categorization.llm.PerplexityProvider
import app.lawnchair.categorization.llm.SuggestedCategory
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.data.category.entities.AppCategory
import app.lawnchair.data.category.entities.CustomCategory
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.TextPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CategorizationSettingsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val prefs = preferenceManager()
    val scope = rememberCoroutineScope()
    val categorizationManager = remember { CategorizationManager.getInstance(context) }
    val progress by categorizationManager.progress.collectAsState()
    val categoryDao = remember { CategoryDatabase.getInstance(context).categoryDao() }
    val packageManager = context.packageManager
    val folderSyncService = remember { CategoryFolderSyncService(context) }
    val appProvider = remember { AutoCatAppProvider.getInstance(context) }

    var categorizationStatus by remember { mutableStateOf("") }

    // Category management state
    var categories by remember { mutableStateOf<List<CustomCategory>>(emptyList()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var suggestedCategories by remember { mutableStateOf<List<SuggestedCategory>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    var suggestionsError by remember { mutableStateOf<String?>(null) }
    var suggestionsProvider by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // App categorization state
    var categorizations by remember { mutableStateOf<List<AppCategory>>(emptyList()) }
    var groupedCategories by remember { mutableStateOf<Map<String, List<AppCategory>>>(emptyMap()) }
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var editingApp by remember { mutableStateOf<AppCategory?>(null) }

    val slImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            categorizationStatus = "Importing..."
            scope.launch {
                val importer = SmartLauncherImporter(context)
                val result = importer.importFromUri(uri)
                categorizationStatus = when (result) {
                    is ImportResult.Success -> "✅ Imported ${result.count} apps from Smart Launcher!"
                    is ImportResult.Error -> "❌ Import failed: ${result.message}"
                }
            }
        }
    }

    // Load data
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            categories = categoryDao.getAllCustomCategories()
            categorizations = categoryDao.getAllAppCategories()
            groupedCategories = categorizations.groupBy { it.category }
        }
    }

    PreferenceScaffold(
        label = "App Categorization",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // ===== QUICK ACTIONS SECTION =====
            item {
                PreferenceGroup(heading = "Quick Actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                categorizationStatus = ""
                                scope.launch {
                                    try {
                                        categorizationManager.recategorizeAll()
                                        categories = categoryDao.getAllCustomCategories()
                                        categorizations = categoryDao.getAllAppCategories()
                                        groupedCategories = categorizations.groupBy { cat -> cat.category }
                                        categorizationStatus = "✅ Categorization complete! Check your app drawer."
                                    } catch (e: Exception) {
                                        categorizationStatus = "❌ Error: ${e.message}"
                                    }
                                }
                            },
                            enabled = !progress.isRunning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (progress.isRunning) "Categorizing..." else "Categorize All Apps")
                        }

                        OutlinedButton(
                            onClick = { navController.navigate("llmSettings") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LLM Provider Settings")
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

                        // Status message
                        if (categorizationStatus.isNotEmpty()) {
                            Text(
                                text = categorizationStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    categorizationStatus.startsWith("✅") -> MaterialTheme.colorScheme.primary
                                    categorizationStatus.startsWith("❌") -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }

                        Text(
                            text = "This will categorize all apps using LLM and built-in rules. " +
                                "${if (prefs.llmEnableBatching.get()) "Batch processing enabled for faster categorization. " else ""}" +
                                "${if (prefs.autoCatSyncFolders.get()) "Folders will be created automatically. " else ""}" +
                                "Only apps without user overrides will be re-categorized.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ===== MANAGE CATEGORIES SECTION =====
            item {
                PreferenceGroup(heading = "Categories") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Create custom categories for organizing your apps. The LLM will automatically assign apps to these categories.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Category list
            items(categories, key = { it.id }) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { editingCategory = it },
                    onDelete = {
                        scope.launch {
                            categoryDao.deleteCustomCategory(it)
                            categories = categoryDao.getAllCustomCategories()
                            categorizations = categoryDao.getAllAppCategories()
                            groupedCategories = categorizations.groupBy { cat -> cat.category }
                        }
                    },
                )
            }

            // Category management buttons
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Category")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoadingSuggestions = true
                                suggestionsError = null
                                successMessage = null
                                try {
                                    val preferredProviderId = prefs.llmProviderPreference.get()

                                    val googleProvider = GoogleAIProvider(context)
                                    val allProviderMap = mapOf(
                                        "google_ai" to googleProvider,
                                        "claude" to ClaudeProvider(context),
                                        "openai" to OpenAIProvider(context),
                                        "perplexity" to PerplexityProvider(context),
                                    )

                                    val primary = allProviderMap[preferredProviderId] ?: googleProvider
                                    val fallbacks = allProviderMap.values.filter { provider -> provider.name != primary.name }
                                    val providers = listOf(primary) + fallbacks

                                    val metadataProvider = AppMetadataProvider(context)
                                    val installedApps = metadataProvider.getInstalledApps()

                                    if (installedApps.isEmpty()) {
                                        suggestionsError = "No apps found to analyze"
                                        return@launch
                                    }

                                    val appNames = installedApps.map { app -> app.label }
                                    val existingCategories = categories.map { cat -> cat.name }

                                    var lastError: Exception? = null
                                    for (provider in providers) {
                                        try {
                                            if (!provider.isAvailable()) continue

                                            suggestedCategories = provider.suggestCategories(
                                                installedApps = appNames,
                                                existingCategories = existingCategories,
                                                maxSuggestions = 5,
                                            )

                                            if (suggestedCategories.isEmpty()) {
                                                suggestionsError = "No new categories suggested. You may already have all the useful categories for your apps!"
                                            } else {
                                                suggestionsProvider = provider.name
                                                showSuggestionsDialog = true
                                            }
                                            return@launch
                                        } catch (e: Exception) {
                                            lastError = e
                                        }
                                    }

                                    suggestionsError = "All LLM providers failed. Please configure at least one API key in LLM Settings.\nLast error: ${lastError?.message}"
                                } catch (e: Exception) {
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

                    if (suggestionsError != null) {
                        Text(
                            text = suggestionsError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    if (successMessage != null) {
                        Text(
                            text = successMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // ===== APP CATEGORIZATIONS SECTION =====
            item {
                PreferenceGroup(heading = "Review & Override") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "View and manually override app categorizations. Tap a category to expand and see apps. Your changes help improve future categorization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Grouped categorizations by category
            groupedCategories.forEach { (categoryName, apps) ->
                item(key = "category_$categoryName") {
                    CategoryGroupHeader(
                        categoryName = categoryName,
                        appCount = apps.size,
                        isExpanded = expandedCategories.contains(categoryName),
                        onToggleExpand = {
                            expandedCategories = if (expandedCategories.contains(categoryName)) {
                                expandedCategories - categoryName
                            } else {
                                expandedCategories + categoryName
                            }
                        },
                    )
                }

                // Show apps when expanded
                if (expandedCategories.contains(categoryName)) {
                    items(apps, key = { app -> app.packageName }) { appCategory ->
                        AppCategorizationItem(
                            appCategory = appCategory,
                            packageManager = packageManager,
                            onEditClick = { editingApp = appCategory },
                        )
                    }
                }
            }

            // ===== SETTINGS SECTION =====
            item {
                PreferenceGroup(heading = "Settings") {
                    SwitchPreference(
                        adapter = prefs.llmEnableBatching.getAdapter(),
                        label = "Batch Processing",
                        description = "Process multiple apps per API call (20x faster, 68% token savings)",
                    )

                    if (prefs.llmEnableBatching.get()) {
                        val batchSizeValue = prefs.llmBatchSize.get()
                        var sliderValue by remember { mutableStateOf(batchSizeValue.toFloat()) }

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "Batch Size: ${if (sliderValue.toInt() == 0) "Auto" else sliderValue.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    prefs.llmBatchSize.set(sliderValue.roundToInt())
                                },
                                valueRange = 0f..50f,
                                steps = 49,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (sliderValue.toInt() == 0) {
                                    "Auto: Automatically calculates optimal batch size based on model context window"
                                } else {
                                    "Manual: Process ${sliderValue.toInt()} apps per batch"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SwitchPreference(
                        adapter = prefs.autoCatSyncFolders.getAdapter(),
                        label = "Sync to App Drawer Folders",
                        description = "Automatically create folders in app drawer for each category",
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SwitchPreference(
                        adapter = prefs.autoCatEnableRateLimiting.getAdapter(),
                        label = "Rate Limiting",
                        description = "Add 1-second delay between batches. Recommended for Google AI (free tier). " +
                            "Disable for faster categorization with Perplexity, Claude, or OpenAI.",
                    )
                }
            }

            // ===== DEVELOPER DIAGNOSTICS (if enabled) =====
            if (prefs.autoCatDevMode.get()) {
                item {
                    PreferenceGroup(heading = "Developer Diagnostics") {
                        var showDiagnostics by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showDiagnostics = !showDiagnostics },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Icon(
                                imageVector = if (showDiagnostics) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showDiagnostics) "Hide Diagnostics" else "Show Diagnostics")
                        }

                        if (showDiagnostics) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🔧 Developer Diagnostics",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Current Progress:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "• Stage: ${progress.currentStage}\n" +
                                            "• Processed: ${progress.processedCount}/${progress.totalCount}\n" +
                                            "• Batch: ${progress.currentBatch}/${progress.totalBatches}\n" +
                                            "• Provider: ${progress.currentProvider ?: "N/A"}\n" +
                                            "• Batch Size: ${progress.batchSize}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    var dbStats by remember { mutableStateOf("Loading...") }
                                    LaunchedEffect(Unit) {
                                        withContext(Dispatchers.IO) {
                                            val totalCategorized = categoryDao.getAllAppCategories().size
                                            val userOverrides = categoryDao.getUserOverriddenApps().size
                                            val llmCategorized = categoryDao.getAppsBySource("llm").size
                                            val builtInCategorized = categoryDao.getAppsBySource("built-in").size

                                            dbStats = "• Total categorized: $totalCategorized\n" +
                                                "• LLM categorized: $llmCategorized\n" +
                                                "• Built-in categorized: $builtInCategorized\n" +
                                                "• User overrides: $userOverrides"
                                        }
                                    }

                                    Text(
                                        text = "Database Stats:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = dbStats,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Settings:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "• Batching: ${if (prefs.llmEnableBatching.get()) "Enabled" else "Disabled"}\n" +
                                            "• Batch size: ${if (prefs.llmBatchSize.get() == 0) "Auto" else prefs.llmBatchSize.get()}\n" +
                                            "• Folder sync: ${if (prefs.autoCatSyncFolders.get()) "Enabled" else "Disabled"}\n" +
                                            "• Provider: ${prefs.llmProviderPreference.get()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Category Add/Edit Dialog
    if (showAddCategoryDialog || editingCategory != null) {
        CategoryDialog(
            category = editingCategory,
            onDismiss = {
                showAddCategoryDialog = false
                editingCategory = null
            },
            onSave = { name, color ->
                scope.launch {
                    if (editingCategory != null) {
                        categoryDao.updateCustomCategory(
                            editingCategory!!.copy(
                                name = name,
                                colorHex = color,
                            ),
                        )
                        successMessage = "✓ Category '$name' updated"
                    } else {
                        val maxSortOrder = categories.maxOfOrNull { cat -> cat.sortOrder } ?: 0
                        categoryDao.insertCustomCategory(
                            CustomCategory(
                                name = name,
                                colorHex = color,
                                sortOrder = maxSortOrder + 1,
                                isVisible = true,
                            ),
                        )
                        successMessage = "✓ Category '$name' created! Use 'Categorize All Apps' to assign apps."
                    }
                    categories = categoryDao.getAllCustomCategories()
                    categorizations = categoryDao.getAllAppCategories()
                    groupedCategories = categorizations.groupBy { cat -> cat.category }
                    appProvider.refreshCache()
                    showAddCategoryDialog = false
                    editingCategory = null
                    suggestionsError = null
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
            onAddCategory = { suggestion ->
                scope.launch {
                    val maxSortOrder = categories.maxOfOrNull { cat -> cat.sortOrder } ?: 0
                    categoryDao.insertCustomCategory(
                        CustomCategory(
                            name = suggestion.name,
                            colorHex = "#4CAF50",
                            sortOrder = maxSortOrder + 1,
                        ),
                    )
                    categories = categoryDao.getAllCustomCategories()
                    categorizations = categoryDao.getAllAppCategories()
                    groupedCategories = categorizations.groupBy { cat -> cat.category }
                    appProvider.refreshCache()

                    successMessage = "✓ Added '${suggestion.name}' category! Use 'Categorize All Apps' to assign apps."
                    suggestionsError = null
                }
            },
        )
    }

    // App Edit Dialog
    editingApp?.let { app ->
        CategoryOverrideDialog(
            appCategory = app,
            availableCategories = categories.filter { it.isVisible },
            packageManager = packageManager,
            onDismiss = { editingApp = null },
            onSave = { newCategory ->
                scope.launch(Dispatchers.IO) {
                    val updated = app.copy(
                        category = newCategory,
                        isUserOverride = true,
                        source = AppCategory.SOURCE_USER,
                        confidence = 1.0f,
                        lastUpdated = System.currentTimeMillis(),
                    )
                    categoryDao.insertAppCategory(updated)

                    categories = categoryDao.getAllCustomCategories()
                    categorizations = categoryDao.getAllAppCategories()
                    groupedCategories = categorizations.groupBy { cat -> cat.category }

                    if (folderSyncService.isSyncEnabled()) {
                        val allCategories = categoryDao.getAllAppCategories()
                        val categorizationMap = allCategories.associate { cat -> cat.packageName to cat.category }
                        folderSyncService.syncCategoriesToFolders(categorizationMap)
                    }

                    appProvider.refreshCache()
                    editingApp = null
                }
            },
        )
    }
}

@Composable
private fun CategorizationProgress(progress: app.lawnchair.categorization.CategorizationProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress.progressPercentage,
                animationSpec = tween(durationMillis = 300),
                label = "progress",
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Stage: ${progress.currentStage}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${progress.processedCount} / ${progress.totalCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (progress.batchProgressText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = progress.batchProgressText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (progress.currentProvider != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${progress.currentProvider}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (progress.estimatedTimeMs > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val seconds = (progress.estimatedTimeMs / 1000).toInt()
                        Text(
                            text = "• ~${seconds}s remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (progress.currentAppName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Processing: ${progress.currentAppName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
    onAddCategory: (SuggestedCategory) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("AI Category Suggestions")
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

@Composable
private fun CategoryGroupHeader(
    categoryName: String,
    appCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            )
            .clickable(onClick = onToggleExpand)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$appCount ${if (appCount == 1) "app" else "apps"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AppCategorizationItem(
    appCategory: AppCategory,
    packageManager: PackageManager,
    onEditClick: () -> Unit,
) {
    val appName = remember(appCategory.packageName) {
        try {
            val appInfo = packageManager.getApplicationInfo(appCategory.packageName, 0)
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            appCategory.packageName
        }
    }

    val appIcon = remember(appCategory.packageName) {
        try {
            packageManager.getApplicationIcon(appCategory.packageName)
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(horizontal = 32.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (appIcon != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(appIcon)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (appCategory.isUserOverride) {
                    Text(
                        text = "Override",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Text(
                    text = "${(appCategory.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!appCategory.reasoning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = appCategory.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                )
            }
        }

        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit category",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryOverrideDialog(
    appCategory: AppCategory,
    availableCategories: List<CustomCategory>,
    packageManager: PackageManager,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val appName = remember(appCategory.packageName) {
        try {
            val appInfo = packageManager.getApplicationInfo(appCategory.packageName, 0)
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            appCategory.packageName
        }
    }

    var selectedCategory by remember { mutableStateOf(appCategory.category) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Category")
        },
        text = {
            Column {
                Text(
                    text = "Change category for $appName",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category.name
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                if (!appCategory.reasoning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Current reasoning:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = appCategory.reasoning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedCategory) },
                enabled = selectedCategory != appCategory.category,
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

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
