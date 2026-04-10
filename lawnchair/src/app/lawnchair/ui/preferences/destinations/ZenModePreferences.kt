package app.lawnchair.ui.preferences.destinations

import android.app.appsearch.exceptions.SecurityException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FocusMode
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.R
import app.lawnchair.categorization.zenmode.ZenModeManager
import app.lawnchair.categorization.zenmode.ZenModeManager.FocusRule
import app.lawnchair.ui.preferences.components.controls.PreferenceCategory
import kotlinx.coroutines.launch

/**
 * Zen Mode (Focus Mode) preferences screen.
 *
 * Allows users to configure which categories are hidden/shown during Focus Mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenModePreferences(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val zenModeManager = remember { ZenModeManager.getInstance(context) }

    var focusRules by remember { mutableStateOf<List<FocusRule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var recommendedToHide by remember { mutableStateOf<List<String>>(emptyList()) }
    var recommendedToShow by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        focusRules = zenModeManager.getFocusRules()
        recommendedToHide = zenModeManager.getRecommendedCategoriesToHide()
        recommendedToShow = zenModeManager.getRecommendedCategoriesToShow()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zen Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Introduction Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.FocusMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Focus Mode Integration",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        text = "Automatically hide distracting apps when Focus Mode is active. Show only essential apps for productivity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (isLoading) {
                Text(
                    text = "Loading categories...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                // Recommended to Hide
                if (recommendedToHide.isNotEmpty()) {
                    Text(
                        text = "Recommended to Hide During Focus",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    recommendedToHide.forEach { categoryName ->
                        val rule = focusRules.find { it.categoryName == categoryName }
                        val currentRule = rule ?: FocusRule(categoryName, hideDuringFocus = true)

                        ZenModeRuleItem(
                            categoryName = categoryName,
                            rule = currentRule,
                            onRuleChange = { newRule ->
                                scope.launch {
                                    zenModeManager.updateFocusRule(newRule)
                                    focusRules = zenModeManager.getFocusRules()
                                }
                            },
                            highlightType = HighlightType.HIDE,
                        )
                    }
                }

                // Recommended to Show
                if (recommendedToShow.isNotEmpty()) {
                    Text(
                        text = "Recommended to Show During Focus",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    recommendedToShow.forEach { categoryName ->
                        val rule = focusRules.find { it.categoryName == categoryName }
                        val currentRule = rule ?: FocusRule(categoryName, showOnlyDuringFocus = true)

                        ZenModeRuleItem(
                            categoryName = categoryName,
                            rule = currentRule,
                            onRuleChange = { newRule ->
                                scope.launch {
                                    zenModeManager.updateFocusRule(newRule)
                                    focusRules = zenModeManager.getFocusRules()
                                }
                            },
                            highlightType = HighlightType.SHOW,
                        )
                    }
                }

                // All Categories
                Text(
                    text = "All Categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )

                focusRules.forEach { rule ->
                    ZenModeRuleItem(
                        categoryName = rule.categoryName,
                        rule = rule,
                        onRuleChange = { newRule ->
                            scope.launch {
                                zenModeManager.updateFocusRule(newRule)
                                focusRules = zenModeManager.getFocusRules()
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class HighlightType {
    NONE,
    HIDE,
    SHOW,
}

@Composable
fun ZenModeRuleItem(
    categoryName: String,
    rule: FocusRule,
    onRuleChange: (FocusRule) -> Unit,
    modifier: Modifier = Modifier,
    highlightType: HighlightType = HighlightType.NONE,
) {
    val backgroundColor = when (highlightType) {
        HighlightType.HIDE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        HighlightType.SHOW -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        HighlightType.NONE -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (rule.hideDuringFocus) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.HideSource,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Hide during Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (rule.showOnlyDuringFocus) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Show only during Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (!rule.hideDuringFocus && !rule.showOnlyDuringFocus) {
                        Text(
                            text = "Always visible",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Switch(
                checked = rule.hideDuringFocus || rule.showOnlyDuringFocus,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        // Default to hide if not configured
                        onRuleChange(rule.copy(hideDuringFocus = true))
                    } else {
                        // Disable both
                        onRuleChange(rule.copy(hideDuringFocus = false, showOnlyDuringFocus = false))
                    }
                },
            )
        }
    }
}
