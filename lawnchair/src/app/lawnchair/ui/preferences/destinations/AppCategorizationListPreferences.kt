package app.lawnchair.ui.preferences.destinations

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
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
    val database = remember { TabDatabase.getInstance(context) }
    val categoryDao = database.categoryDao()
    val packageManager = context.packageManager
    val folderSyncService = remember { CategoryFolderSyncService(context) }
    val appProvider = remember { AutoCatAppProvider.getInstance(context) }

    var appTabs by remember { mutableStateOf<List<AppTab>>(emptyList()) }
    var availableCustomTabs by remember { mutableStateOf<List<CustomCategoryTab>>(emptyList()) }
    var editingApp by remember { mutableStateOf<AppTab?>(null) }
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }

    // Load categorizations and available categories
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            appTabs = categoryDao.getAllAppCategories()
            availableCustomTabs = categoryDao.getAllCustomCategories()
        }
    }

    val filteredAppTabs = remember(appTabs, filterMode) {
        when (filterMode) {
            FilterMode.ALL -> appTabs
            FilterMode.UNCATEGORIZED -> appTabs.filter { it.tabName == "Other" }
            FilterMode.UNFOLDERED -> appTabs.filter { it.tabName != "Other" && it.subCategory.isNullOrBlank() }
            FilterMode.LLM_SORTED -> appTabs.filter { it.source == AppTab.SOURCE_LLM || it.source == AppTab.SOURCE_ML }
            FilterMode.SLBK_SORTED -> appTabs.filter { it.source == AppTab.SOURCE_BUILT_IN || it.source == AppTab.SOURCE_RULE }
        }
    }

    val groupedApps = remember(filteredAppTabs) {
        filteredAppTabs.groupBy { it.tabName }.toSortedMap()
    }

    PreferenceScaffold(
        label = "App Categorizations",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "View and override app categorizations. Changes are used to improve future categorization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filters
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        CategoryOverrideDialog(
            appCategory = app,
            availableCategories = availableCategories,
            packageManager = packageManager,
            onDismiss = { editingApp = null },
            onSave = { newTabName, newSubCategory ->
                scope.launch(Dispatchers.IO) {
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
                    if (folderSyncService.isSyncEnabled()) {
                        val allAppTabs = categoryDao.getAllAppCategories()
                        val categorizationMap = allAppTabs.associate { it.packageName to it.tabName }
                        folderSyncService.syncCategoriesToFolders(categorizationMap)
                    }

                    // Refresh app provider cache
                    appProvider.refreshCache()

                    editingApp = null
                }
            },
            onAutoCategorize = { packageName ->
                scope.launch(Dispatchers.IO) {
                    appProvider.categorizeNewApp(packageName)
                    // Reload categorizations after auto-categorization
                    appTabs = categoryDao.getAllAppCategories()
                    // Sync to folders if enabled
                    if (folderSyncService.isSyncEnabled()) {
                        val allAppTabs = categoryDao.getAllAppCategories()
                        val categorizationMap = allAppTabs.associate { it.packageName to it.tabName }
                        folderSyncService.syncCategoriesToFolders(categorizationMap)
                    }
                    editingApp = null // Dismiss the dialog
                }
            },
        )
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
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                modifier = Modifier.size(40.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = appCategory.tabName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (!appCategory.subCategory.isNullOrBlank()) {
                    Text(
                        text = "• ${appCategory.subCategory}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (appCategory.isUserOverride) {
                    Text(
                        text = "• Override",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // Show reasoning if available
            if (!appCategory.reasoning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appCategory.reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }
        }

        // Edit button
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit tab",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryOverrideDialog(
    appCategory: AppTab,
    availableCustomTabs: List<CustomCategoryTab>,
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

    LaunchedEffect(Unit) {
        expanded = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Tab")
        },
        text = {
            Column {
                Text(
                    text = "Change categorization for $appName",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tab dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedTabName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tab") },
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
                        availableCustomTabs.forEach { tab ->
                            DropdownMenuItem(
                                text = { Text(tab.name) },
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
                        Text("Auto Categorize")
                    }
                }

                TextButton(
                    onClick = {
                        onSave(
                            selectedCategory,
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
