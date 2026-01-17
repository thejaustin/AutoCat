package app.lawnchair.ui.preferences.destinations

import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.categorization.AccuracyTracker
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.TabFolderSyncService
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTabAssignmentPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var initializationError by remember { mutableStateOf<String?>(null) }
    var database by remember { mutableStateOf<TabDatabase?>(null) }
    var folderSyncService by remember { mutableStateOf<TabFolderSyncService?>(null) }
    var appProvider by remember { mutableStateOf<AutoCatAppProvider?>(null) }
    var accuracyTracker by remember { mutableStateOf<AccuracyTracker?>(null) }

    val packageManager = context.packageManager

    var appTabs by remember { mutableStateOf<List<AppTab>>(emptyList()) }
    var availableCustomTabs by remember { mutableStateOf<List<CustomTab>>(emptyList()) }
    var editingApp by remember { mutableStateOf<AppTab?>(null) }
    var expandedTabs by remember { mutableStateOf(setOf<String>()) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }

    // Initialize services safely
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("AppTabAssignment", "Initializing database...")
                database = TabDatabase.getInstance(context)
                Log.d("AppTabAssignment", "Database initialized")

                Log.d("AppTabAssignment", "Initializing folder sync service...")
                folderSyncService = TabFolderSyncService(context)
                Log.d("AppTabAssignment", "Folder sync service initialized")

                Log.d("AppTabAssignment", "Initializing app provider...")
                appProvider = AutoCatAppProvider.getInstance(context)
                Log.d("AppTabAssignment", "App provider initialized")

                Log.d("AppTabAssignment", "Initializing accuracy tracker...")
                accuracyTracker = AccuracyTracker(context)
                Log.d("AppTabAssignment", "Accuracy tracker initialized")

                Log.d("AppTabAssignment", "Loading tab assignments...")
                appTabs = database?.tabDao()?.getAllAppTabs() ?: emptyList()
                availableCustomTabs = database?.tabDao()?.getAllCustomTabs() ?: emptyList()
                Log.d("AppTabAssignment", "Loaded ${appTabs.size} app tab assignments and ${availableCustomTabs.size} custom tabs")
            } catch (e: Exception) {
                Log.e("AppTabAssignment", "Error initializing screen: ${e.message}", e)
                initializationError = "Failed to load tab assignments: ${e.message}"
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
                    it.tabName.isNotEmpty() && it.tabName != "Other" && it.folderName.isNullOrBlank()
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
            Log.e("AppTabAssignment", "Error filtering apps: ${e.message}", e)
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
            Log.e("AppTabAssignment", "Error grouping apps: ${e.message}", e)
            emptyMap()
        }
    }

    PreferenceScaffold(
        label = "App Tab Assignments",
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
                                text = "Error Loading Tab Assignments",
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
                        text = "View and manually change app tab assignments. Changes are used to improve future categorization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Material 3 Expressive: Enhanced filter chips with personality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ExpressiveFilterChip(
                            selected = filterMode == FilterMode.ALL,
                            onClick = { filterMode = FilterMode.ALL },
                            label = "All",
                        )
                        ExpressiveFilterChip(
                            selected = filterMode == FilterMode.UNCATEGORIZED,
                            onClick = { filterMode = FilterMode.UNCATEGORIZED },
                            label = "Uncategorized",
                        )
                        ExpressiveFilterChip(
                            selected = filterMode == FilterMode.UNFOLDERED,
                            onClick = { filterMode = FilterMode.UNFOLDERED },
                            label = "Unfoldered",
                        )
                        ExpressiveFilterChip(
                            selected = filterMode == FilterMode.LLM_SORTED,
                            onClick = { filterMode = FilterMode.LLM_SORTED },
                            label = "LLM Sorted",
                        )
                        ExpressiveFilterChip(
                            selected = filterMode == FilterMode.SLBK_SORTED,
                            onClick = { filterMode = FilterMode.SLBK_SORTED },
                            label = "SLBK Sorted",
                        )
                    }
                }
            }

            groupedApps.forEach { (tabName, apps) ->
                item(contentType = "TabHeader") {
                    TabHeader(
                        tabName = tabName,
                        count = apps.size,
                        expanded = expandedTabs.contains(tabName) || filterMode != FilterMode.ALL,
                        onClick = {
                            expandedTabs = if (expandedTabs.contains(tabName)) {
                                expandedTabs - tabName
                            } else {
                                expandedTabs + tabName
                            }
                        },
                    )
                }

                // Material 3 Expressive: Animated visibility for tab expansion
                if (expandedTabs.contains(tabName) || filterMode != FilterMode.ALL) {
                    items(apps, key = { it.packageName }) { appTab ->
                        // Spring-based entrance animation
                        AnimatedVisibility(
                            visible = true,
                            enter = expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            ) + fadeIn(
                                animationSpec = tween(300),
                            ),
                            exit = shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            ) + fadeOut(
                                animationSpec = tween(200),
                            ),
                        ) {
                            AppTabAssignmentItem(
                                appTab = appTab,
                                packageManager = packageManager,
                                onEditClick = { editingApp = appTab },
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }

    // Edit dialog
    editingApp?.let { app ->
        val tabDao = database?.tabDao()
        if (tabDao != null) {
            TabOverrideDialog(
                appTab = app,
                availableCustomTabs = availableCustomTabs,
                packageManager = packageManager,
                onDismiss = { editingApp = null },
                onSave = { newTabName, newFolderName ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Track accuracy if user changed the tab
                            if (app.tabName != newTabName) {
                                accuracyTracker?.recordUserCorrection(
                                    packageName = app.packageName,
                                    oldTab = app.tabName,
                                    newTab = newTabName,
                                )
                            }

                            // Update tab assignment with user override flag
                            val updated = app.copy(
                                tabName = newTabName,
                                folderName = newFolderName,
                                isUserOverride = true,
                                source = AppTab.SOURCE_USER,
                                confidence = 1.0f,
                                lastUpdated = System.currentTimeMillis(),
                            )
                            tabDao.insertAppTab(updated)

                            // Reload tab assignments
                            appTabs = tabDao.getAllAppTabs()

                            // Sync to folders if enabled
                            folderSyncService?.let { syncService ->
                                if (syncService.isSyncEnabled()) {
                                    val allAppTabs = tabDao.getAllAppTabs()
                                    val tabAssignments = allAppTabs.associate { it.packageName to it.tabName }
                                    syncService.syncCategoriesToFolders(tabAssignments)
                                }
                            }

                            // Refresh app provider cache
                            appProvider?.refreshCache()

                            editingApp = null
                        } catch (e: Exception) {
                            Log.e("AppTabAssignment", "Error saving tab assignment: ${e.message}", e)
                        }
                    }
                },
                onAutoCategorize = { packageName ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            appProvider?.categorizeNewApp(packageName)
                            // Reload tab assignments after auto-categorization
                            appTabs = tabDao.getAllAppTabs()
                            // Sync to folders if enabled
                            folderSyncService?.let { syncService ->
                                if (syncService.isSyncEnabled()) {
                                    val allAppTabs = tabDao.getAllAppTabs()
                                    val tabAssignments = allAppTabs.associate { it.packageName to it.tabName }
                                    syncService.syncCategoriesToFolders(tabAssignments)
                                }
                            }
                            editingApp = null // Dismiss the dialog
                        } catch (e: Exception) {
                            Log.e("AppTabAssignment", "Error auto-categorizing: ${e.message}", e)
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
private fun TabHeader(
    tabName: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    // Material 3 Expressive: Enhanced header with gradient and spring animation
    val tabColor = remember(tabName) {
        Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(
                    (tabName.hashCode() % 360).toFloat(),
                    0.65f,
                    0.85f,
                ),
            ),
        )
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            .shadow(
                elevation = if (expanded) 4.dp else 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = tabColor.copy(alpha = 0.3f),
                spotColor = tabColor.copy(alpha = 0.4f),
            ),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (expanded) 3.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            tabColor.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.0f),
                        ),
                    ),
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Material 3 Expressive: Enhanced tab indicator with border
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 2.dp,
                            color = tabColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .background(
                            color = tabColor,
                            shape = CircleShape,
                        )
                        .scale(if (expanded) 1.1f else 1.0f),
                )

                Column {
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Medium,
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
                tint = tabColor,
                modifier = Modifier.scale(if (expanded) 1.1f else 1.0f),
            )
        }
    }
}

@Composable
private fun AppTabAssignmentItem(
    appTab: AppTab,
    packageManager: PackageManager,
    onEditClick: () -> Unit,
) {
    val appName = remember(appTab.packageName) {
        try {
            val appInfo = packageManager.getApplicationInfo(appTab.packageName, 0)
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            appTab.packageName
        }
    }

    val appIcon = remember(appTab.packageName) {
        try {
            packageManager.getApplicationIcon(appTab.packageName)
        } catch (e: Exception) {
            null
        }
    }

    // Material 3 Expressive: Enhanced card with shadow and animation
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
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

                // Material 3 Expressive: Enhanced tab tags with borders and gradients
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Tab tag with expressive design
                    Box(
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    ),
                                ),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = appTab.tabName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    // Folder name if available with expressive arrow
                    if (!appTab.folderName.isNullOrBlank()) {
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = appTab.folderName!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }

                    // User override indicator with enhanced styling
                    if (appTab.isUserOverride) {
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "Override",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                // Show reasoning if available
                if (!appTab.reasoning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall,
                            )
                            .padding(8.dp),
                    ) {
                        Text(
                            text = appTab.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
                    contentDescription = "Edit tab assignment",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabOverrideDialog(
    appTab: AppTab,
    availableCustomTabs: List<CustomTab>,
    packageManager: PackageManager,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onAutoCategorize: (String) -> Unit,
) {
    val appName = remember(appTab.packageName) {
        try {
            val appInfo = packageManager.getApplicationInfo(appTab.packageName, 0)
            appInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            appTab.packageName
        }
    }

    var selectedTabName by remember { mutableStateOf(appTab.tabName) }
    var folderName by remember { mutableStateOf(appTab.folderName ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Tab")
        },
        text = {
            Column {
                Text(
                    text = "Change tab for $appName",
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

                // Folder input
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                // Show current reasoning if available
                if (!appTab.reasoning.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Current reasoning:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = appTab.reasoning,
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
                if (appTab.source != AppTab.SOURCE_LLM &&
                    appTab.source != AppTab.SOURCE_ML &&
                    !appTab.isUserOverride
                ) {
                    TextButton(
                        onClick = {
                            onAutoCategorize(appTab.packageName)
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
                            folderName.takeIf { it.isNotBlank() },
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

/**
 * Material 3 Expressive: Custom filter chip with enhanced visual design
 */
@Composable
private fun ExpressiveFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                borderColor = MaterialTheme.colorScheme.primary,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                borderWidth = 2.dp,
                selectedBorderWidth = 2.dp,
            )
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = if (selected) 3.dp else 1.dp,
            pressedElevation = 4.dp,
            focusedElevation = 4.dp,
            hoveredElevation = 3.dp,
        ),
    )
}
