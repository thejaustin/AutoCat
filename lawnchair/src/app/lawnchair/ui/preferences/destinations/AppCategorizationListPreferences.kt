package app.lawnchair.ui.preferences.destinations

import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.CategoryFolderSyncService
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.theme.preferenceGroupColor
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategorizationListPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var initializationError by remember { mutableStateOf<String?>(null) }
    var database by remember { mutableStateOf<TabDatabase?>(null) }
    var folderSyncService by remember { mutableStateOf<CategoryFolderSyncService?>(null) }
    var appProvider by remember { mutableStateOf<AutoCatAppProvider?>(null) }

    val packageManager = context.packageManager

    var appTabs by remember { mutableStateOf<List<AppTab>>(emptyList()) }
    var availableCustomTabs by remember { mutableStateOf<List<CustomTab>>(emptyList()) }
    var editingApp by remember { mutableStateOf<AppTab?>(null) }
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }

    // Initialize services safely
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("AppCategorization", "Initializing database...")
                database = TabDatabase.getInstance(context)
                Log.d("AppCategorization", "Database initialized")

                Log.d("AppCategorization", "Initializing folder sync service...")
                folderSyncService = CategoryFolderSyncService(context)
                Log.d("AppCategorization", "Folder sync service initialized")

                Log.d("AppCategorization", "Initializing app provider...")
                appProvider = AutoCatAppProvider.getInstance(context)
                Log.d("AppCategorization", "App provider initialized")

                Log.d("AppCategorization", "Loading categorizations...")
                appTabs = database?.categoryDao()?.getAllAppCategories() ?: emptyList()
                availableCustomTabs = database?.categoryDao()?.getAllCustomCategories() ?: emptyList()
                Log.d("AppCategorization", "Loaded ${appTabs.size} app categorizations and ${availableCustomTabs.size} custom tabs")
            } catch (e: Exception) {
                Log.e("AppCategorization", "Error initializing screen: ${e.message}", e)
                initializationError = "Failed to load categorizations: ${e.message}"
            }
        }
    }

    val filteredAppTabs = remember(appTabs, filterMode) {
        try {
            val filtered = when (filterMode) {
                FilterMode.ALL -> appTabs

                FilterMode.UNCATEGORIZED -> appTabs.filter {
                    it.tabName == "Other" || it.tabName == "Uncategorized" || it.tabName.isEmpty()
                }

                FilterMode.UNFOLDERED -> appTabs.filter {
                    it.tabName.isNotEmpty() && it.tabName != "Other" && it.subCategory.isNullOrBlank()
                }

                FilterMode.LLM_SORTED -> appTabs.filter {
                    it.source == AppTab.SOURCE_LLM || it.source == AppTab.SOURCE_ML
                }

                FilterMode.SLBK_SORTED -> appTabs.filter {
                    it.source == AppTab.SOURCE_BUILT_IN || it.source == AppTab.SOURCE_RULE
                }
            }
            // Ensure we're not returning null values that could cause issues
            filtered.filter { it.tabName != null }
        } catch (e: Exception) {
            Log.e("AppCategorization", "Error filtering apps: ${e.message}", e)
            emptyList()
        }
    }

    val groupedApps = remember(filteredAppTabs) {
        try {
            filteredAppTabs
                .filter { !it.tabName.isNullOrBlank() } // Filter out any apps with empty or null tab names
                .groupBy { it.tabName!! } // Use !! since we filtered out nulls above
                .toSortedMap()
        } catch (e: Exception) {
            Log.e("AppCategorization", "Error grouping apps: ${e.message}", e)
            emptyMap()
        }
    }

    PreferenceScaffold(
        label = "App Categorizations",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            // Show error if initialization failed
            initializationError?.let { error ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Error Loading Categorizations",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "View and override app categorizations. Changes are used to improve future categorization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Filters - organized in a horizontal scroll container for better mobile experience
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = filterMode == FilterMode.ALL,
                            onClick = { filterMode = FilterMode.ALL },
                            label = { Text("All") },
                        )
                        FilterChip(
                            selected = filterMode == FilterMode.UNCATEGORIZED,
                            onClick = { filterMode = FilterMode.UNCATEGORIZED },
                            label = { Text("Uncategorized") },
                        )
                        FilterChip(
                            selected = filterMode == FilterMode.UNFOLDERED,
                            onClick = { filterMode = FilterMode.UNFOLDERED },
                            label = { Text("Unfoldered") },
                        )
                        FilterChip(
                            selected = filterMode == FilterMode.LLM_SORTED,
                            onClick = { filterMode = FilterMode.LLM_SORTED },
                            label = { Text("LLM Sorted") },
                        )
                        FilterChip(
                            selected = filterMode == FilterMode.SLBK_SORTED,
                            onClick = { filterMode = FilterMode.SLBK_SORTED },
                            label = { Text("SLBK Sorted") },
                        )
                    }
                }
            }

            groupedApps.forEach { (tabName, apps) ->
                item(contentType = "CategoryHeader") {
                    CategoryHeader(
                        tabName = tabName,
                        count = apps.size,
                        expanded = expandedCategories.contains(tabName) || filterMode != FilterMode.ALL,
                        onClick = {
                            expandedCategories = if (expandedCategories.contains(tabName)) {
                                expandedCategories - tabName
                            } else {
                                expandedCategories + tabName
                            }
                        },
                    )
                }

                if (expandedCategories.contains(tabName) || filterMode != FilterMode.ALL) {
                    items(apps, key = { it.packageName }) { appCategory ->
                        Surface(
                            color = preferenceGroupColor(),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            AppCategorizationItem(
                                appCategory = appCategory,
                                packageManager = packageManager,
                                onEditClick = { editingApp = appCategory },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }

    // Edit dialog
    editingApp?.let { app ->
        val categoryDao = database?.categoryDao()
        if (categoryDao != null) {
            CategoryOverrideDialog(
                appCategory = app,
                availableCustomTabs = availableCustomTabs,
                packageManager = packageManager,
                onDismiss = { editingApp = null },
                onSave = { newTabName, newSubCategory ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Update categorization with user override flag
                            val updated = app.copy(
                                tabName = newTabName,
                                subCategory = newSubCategory,
                                isUserOverride = true,
                                source = AppTab.SOURCE_USER,
                                confidence = 1.0f,
                                lastUpdated = System.currentTimeMillis(),
                            )
                            categoryDao.insertAppCategory(updated)

                            // Reload categorizations
                            appTabs = categoryDao.getAllAppCategories()

                            // Sync to folders if enabled
                            folderSyncService?.let { syncService ->
                                if (syncService.isSyncEnabled()) {
                                    val allAppTabs = categoryDao.getAllAppCategories()
                                    val categorizationMap = allAppTabs.associate { it.packageName to it.tabName }
                                    syncService.syncCategoriesToFolders(categorizationMap)
                                }
                            }

                            // Refresh app provider cache
                            appProvider?.refreshCache()

                            editingApp = null
                        } catch (e: Exception) {
                            Log.e("AppCategorization", "Error saving categorization: ${e.message}", e)
                        }
                    }
                },
                onAutoCategorize = { packageName ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            appProvider?.categorizeNewApp(packageName)
                            // Reload categorizations after auto-categorization
                            appTabs = categoryDao.getAllAppCategories()
                            // Sync to folders if enabled
                            folderSyncService?.let { syncService ->
                                if (syncService.isSyncEnabled()) {
                                    val allAppTabs = categoryDao.getAllAppCategories()
                                    val categorizationMap = allAppTabs.associate { it.packageName to it.tabName }
                                    syncService.syncCategoriesToFolders(categorizationMap)
                                }
                            }
                            editingApp = null // Dismiss the dialog
                        } catch (e: Exception) {
                            Log.e("AppCategorization", "Error auto-categorizing: ${e.message}", e)
                        }
                    }
                },
            )
        }
    }
}

private enum class FilterMode {
    ALL,
    UNCATEGORIZED,
    UNFOLDERED,
    LLM_SORTED,
    SLBK_SORTED,
}

@Composable
private fun CategoryHeader(
    tabName: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Add a colored indicator circle for the category
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color(
                                android.graphics.Color.HSVToColor(
                                    floatArrayOf(
                                        (tabName.hashCode() % 360).toFloat(),
                                        0.6f,
                                        0.8f,
                                    ),
                                ),
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
                )

                Column {
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$count apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppCategorizationItem(
    appCategory: AppTab,
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            if (appIcon != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(appIcon)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category and subcategory info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Category tag
                    androidx.compose.foundation.background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = appCategory.tabName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    // Subcategory if available
                    if (!appCategory.subCategory.isNullOrBlank()) {
                        Text(
                            text = "→ ${appCategory.subCategory}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // User override indicator
                    if (appCategory.isUserOverride) {
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = "Override",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                // Show reasoning if available
                if (!appCategory.reasoning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = appCategory.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }

            // Edit button
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .padding(start = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit categorization",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryOverrideDialog(
    appCategory: AppTab,
    availableCustomTabs: List<CustomTab>,
    packageManager: PackageManager,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onAutoCategorize: (String) -> Unit, // New parameter
) {
    val appName = remember(appCategory.packageName) {
        try {
            val appInfo = packageManager.getApplicationInfo(appCategory.packageName, 0)
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            appCategory.packageName
        }
    }

    var selectedTabName by remember { mutableStateOf(appCategory.tabName) }
    var subCategory by remember { mutableStateOf(appCategory.subCategory ?: "") }
    var expanded by remember { mutableStateOf(false) }

    // Ensure dropdown is properly initialized
    LaunchedEffect(availableCustomTabs) {
        // If the current tab name is not in available tabs, add it to the list
        if (!availableCustomTabs.any { it.name == selectedTabName }) {
            // Keep the current selection if it's valid
        }
    }

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

                // Tab dropdown - improved for better UX
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                ) {
                    OutlinedTextField(
                        value = selectedTabName,
                        onValueChange = {}, // Read-only for dropdown
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
                        onDismissRequest = {
                            expanded = false
                        },
                    ) {
                        availableCustomTabs.forEach { tab ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = parseColor(tab.colorHex),
                                                    shape = CircleShape,
                                                ),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = tab.name)
                                    }
                                },
                                onClick = {
                                    selectedTabName = tab.name
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subcategory (Folder) input
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = { subCategory = it },
                    label = { Text("Folder / Subcategory (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Show current reasoning if available
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
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                }
            }
        },
        confirmButton = {
            Row {
                // Auto Categorize button (conditional)
                if (appCategory.source != AppTab.SOURCE_LLM &&
                    appCategory.source != AppTab.SOURCE_ML &&
                    !appCategory.isUserOverride
                ) {
                    TextButton(
                        onClick = {
                            onAutoCategorize(appCategory.packageName)
                            onDismiss() // Dismiss dialog after triggering auto-categorization
                        },
                    ) {
                        Text("Auto")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        onSave(
                            selectedTabName,
                            subCategory.takeIf { it.isNotBlank() },
                        )
                    },
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// Helper function to parse color - added here for the dropdown improvement
private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
