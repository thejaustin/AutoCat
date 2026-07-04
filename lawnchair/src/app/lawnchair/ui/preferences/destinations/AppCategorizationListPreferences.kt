package app.lawnchair.ui.preferences.destinations

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.runtime.produceState
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
import app.lawnchair.categorization.CategoryFolderSyncService
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.AppTab
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.navigation.Search as SearchDestination
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
    var accuracyTracker by remember { mutableStateOf<AccuracyTracker?>(null) }

    val packageManager = context.packageManager

    var isLoading by remember { mutableStateOf(true) }
    var appTabs by remember { mutableStateOf<List<AppTab>>(emptyList()) }
    var availableCustomTabs by remember { mutableStateOf<List<CustomTab>>(emptyList()) }
    var editingApp by remember { mutableStateOf<AppTab?>(null) }
    var expandedTabs by remember { mutableStateOf(setOf<String>()) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var appLabels by remember { mutableStateOf(emptyMap<String, String>()) }

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

                Log.d("AppCategorization", "Initializing accuracy tracker...")
                accuracyTracker = AccuracyTracker(context)
                Log.d("AppCategorization", "Accuracy tracker initialized")

                Log.d("AppCategorization", "Loading tabs...")
                appTabs = database?.tabDao()?.getAllAppTabs() ?: emptyList()
                availableCustomTabs = database?.tabDao()?.getAllCustomTabs() ?: emptyList()
                Log.d("AppCategorization", "Loaded ${appTabs.size} app tabs and ${availableCustomTabs.size} custom tabs")
            } catch (e: Exception) {
                Log.e("AppCategorization", "Error initializing screen: ${e.message}", e)
                initializationError = "Failed to load tabs: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Pre-load all app labels on IO so search and list items never block the main thread.
    LaunchedEffect(appTabs) {
        if (appTabs.isEmpty()) return@LaunchedEffect
        val labels = withContext(Dispatchers.IO) {
            appTabs.associate { appTab ->
                appTab.packageName to try {
                    packageManager.getApplicationInfo(appTab.packageName, 0)
                        .loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    appTab.packageName
                }
            }
        }
        appLabels = labels
    }

    val filteredAppTabs = remember(appTabs, filterMode, searchQuery, appLabels) {
        try {
            var filtered = when (filterMode) {
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

            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter {
                    it.packageName.contains(searchQuery, ignoreCase = true) ||
                        appLabels[it.packageName]?.contains(searchQuery, ignoreCase = true) == true
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
                .filter { !it.tabName.isNullOrBlank() }
                .groupBy { it.tabName!! }
                .toSortedMap()
        } catch (e: Exception) {
            Log.e("AppCategorization", "Error grouping apps: ${e.message}", e)
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
                                text = "Error Loading Tabs",
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
                        text = "View and override app tab assignments. Changes are used to improve future auto-tabbing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Search bar for apps
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.DeleteSweep, null)
                                }
                            }
                        } else {
                            null
                        },
                        shape = CircleShape,
                        singleLine = true,
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
                            label = "Unassigned",
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

            if (!isLoading && appTabs.isEmpty() && initializationError == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "No apps categorized yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Run Smart Categories to automatically assign your apps to tabs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (groupedApps.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No apps found matching \"$searchQuery\"", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            groupedApps.forEach { (tabName, apps) ->
                item(contentType = "TabHeader") {
                    TabHeader(
                        tabName = tabName,
                        count = apps.size,
                        expanded = expandedTabs.contains(tabName) || filterMode != FilterMode.ALL || searchQuery.isNotEmpty(),
                        onClick = {
                            expandedTabs = if (expandedTabs.contains(tabName)) {
                                expandedTabs - tabName
                            } else {
                                expandedTabs + tabName
                            }
                        },
                    )
                }

                // Material 3 Expressive: Animated visibility for category expansion
                if (expandedTabs.contains(tabName) || filterMode != FilterMode.ALL || searchQuery.isNotEmpty()) {
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
                            AppTabItem(
                                appTab = appTab,
                                appName = appLabels[appTab.packageName] ?: appTab.packageName,
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
                onSave = { newTabName, newSubCategory ->
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Track accuracy if user changed the category
                            if (app.tabName != newTabName) {
                                accuracyTracker?.recordUserCorrection(
                                    packageName = app.packageName,
                                    oldCategory = app.tabName,
                                    newCategory = newTabName,
                                )
                            }

                            // Update categorization with user override flag
                            val updated = app.copy(
                                tabName = newTabName,
                                subCategory = newSubCategory,
                                isUserOverride = true,
                                source = AppTab.SOURCE_USER,
                                confidence = 1.0f,
                                lastUpdated = System.currentTimeMillis(),
                            )
                            tabDao.insertAppTab(updated)

                            // Reload categorizations
                            appTabs = tabDao.getAllAppTabs()

                            // Sync to folders if enabled
                            folderSyncService?.let { syncService ->
                                if (syncService.isSyncEnabled()) {
                                    val allAppTabs = tabDao.getAllAppTabs()
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
                            appTabs = tabDao.getAllAppTabs()
                            // Sync to folders if enabled
                            folderSyncService?.let { syncService ->
                                if (syncService.isSyncEnabled()) {
                                    val allAppTabs = tabDao.getAllAppTabs()
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
private fun TabHeader(
    tabName: String,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    // Material 3 Expressive: Enhanced header with gradient and spring animation
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    
    val categoryColor = remember(tabName, primary, secondary, tertiary) {
        val colors = listOf(primary, secondary, tertiary)
        colors[Math.abs(tabName.hashCode()) % colors.size]
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
                ambientColor = categoryColor.copy(alpha = 0.3f),
                spotColor = categoryColor.copy(alpha = 0.4f),
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
                            categoryColor.copy(alpha = 0.15f),
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
                // Material 3 Expressive: Enhanced category indicator with border
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 2.dp,
                            color = categoryColor.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                        .background(
                            color = categoryColor,
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
                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = categoryColor,
                modifier = Modifier.scale(if (expanded) 1.1f else 1.0f),
            )
        }
    }
}

@Composable
private fun AppTabItem(
    appTab: AppTab,
    appName: String,
    packageManager: PackageManager,
    onEditClick: () -> Unit,
) {
    val appIcon by produceState<Drawable?>(initialValue = null, appTab.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                packageManager.getApplicationIcon(appTab.packageName)
            } catch (e: Exception) {
                null
            }
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

                // Material 3 Expressive: Enhanced category tags with borders and gradients
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Category tag with expressive design
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

                    // Subcategory if available with expressive arrow
                    if (!appTab.subCategory.isNullOrBlank()) {
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
                                text = appTab.subCategory,
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
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit categorization",
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
    onAutoCategorize: (String) -> Unit, // New parameter
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
    var subCategory by remember { mutableStateOf(appTab.subCategory ?: "") }
    var expanded by remember { mutableStateOf(false) }    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
        ) {
            Text(
                text = "Change Tab",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Change tab for $appName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Tab dropdown - improved for better UX
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Subcategory (Folder) input
            OutlinedTextField(
                value = subCategory,
                onValueChange = { subCategory = it },
                label = { Text("Folder / Subcategory (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Show current reasoning if available
            if (!appTab.reasoning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "AI Reasoning",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appTab.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // Reset to AI button — only when user has overridden
                if (appTab.isUserOverride) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onAutoCategorize(appTab.packageName)
                            onDismiss()
                        },
                    ) {
                        Text("Reset to AI")
                    }
                } else if (appTab.source != AppTab.SOURCE_LLM && appTab.source != AppTab.SOURCE_ML) {
                    // Auto Categorize button for non-AI-sorted apps without override
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onAutoCategorize(appTab.packageName)
                            onDismiss()
                        },
                    ) {
                        Text("Auto")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                androidx.compose.material3.Button(
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
        }
    }   
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
