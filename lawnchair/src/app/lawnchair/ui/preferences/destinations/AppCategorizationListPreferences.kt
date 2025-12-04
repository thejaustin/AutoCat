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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.data.category.entities.AppCategory
import app.lawnchair.data.category.entities.CustomCategory
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppCategorizationListPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { CategoryDatabase.getInstance(context) }
    val categoryDao = database.categoryDao()
    val packageManager = context.packageManager

    var categorizations by remember { mutableStateOf<List<AppCategory>>(emptyList()) }
    var availableCategories by remember { mutableStateOf<List<CustomCategory>>(emptyList()) }
    var editingApp by remember { mutableStateOf<AppCategory?>(null) }

    // Load categorizations and available categories
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            categorizations = categoryDao.getAllAppCategories()
            availableCategories = categoryDao.getVisibleCustomCategories()
        }
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
                }
            }

            items(categorizations, key = { it.packageName }) { appCategory ->
                AppCategorizationItem(
                    appCategory = appCategory,
                    packageManager = packageManager,
                    onEditClick = { editingApp = appCategory },
                )
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
            onSave = { newCategory ->
                scope.launch(Dispatchers.IO) {
                    // Update categorization with user override flag
                    val updated = app.copy(
                        category = newCategory,
                        isUserOverride = true,
                        source = AppCategory.SOURCE_USER,
                        confidence = 1.0f,
                        lastUpdated = System.currentTimeMillis(),
                    )
                    categoryDao.insertAppCategory(updated)

                    // Reload categorizations
                    categorizations = categoryDao.getAllAppCategories()
                    editingApp = null
                }
            },
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
                    text = appCategory.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                if (appCategory.isUserOverride) {
                    Text(
                        text = "• Override",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                Text(
                    text = "• ${(appCategory.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

                // Category dropdown
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
