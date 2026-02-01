package app.lawnchair.ui.preferences.destinations

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lawnchair.categorization.llm.LLMLogger
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DiagnosticsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appCount by remember { mutableStateOf(0) }
    var tabCount by remember { mutableStateOf(0) }
    var overrideCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = TabDatabase.getInstance(context)
            val dao = db.tabDao()
            appCount = dao.getAllAppTabs().size
            tabCount = dao.getAllCustomTabs().size
            overrideCount = dao.getAllAppTabs().count { it.isUserOverride }
        }
    }

    PreferenceScaffold(
        label = "Diagnostics",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                PreferenceGroup(heading = "Database Stats") {
                    StatRow(icon = Icons.Rounded.Storage, label = "Total Categorized Apps", value = appCount.toString())
                    StatRow(icon = Icons.Rounded.BugReport, label = "User Overrides", value = overrideCount.toString())
                    StatRow(icon = Icons.Rounded.History, label = "Custom Tabs", value = tabCount.toString())
                }
            }

            item {
                PreferenceGroup(heading = "LLM Live Logs") {
                    LogViewer()
                }
            }

            item {
                PreferenceGroup(heading = "Debug Actions") {
                    ClickablePreference(
                        label = "Clear All Logs",
                        subtitle = "Reset the in-memory log buffer",
                        onClick = {
                            LLMLogger.cleanup()
                        },
                    )
                    
                    ClickablePreference(
                        label = "Clear Categorization Database",
                        subtitle = "CAUTION: Deletes all app assignments (not overrides)",
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                TabDatabase.getInstance(context).tabDao().deleteNonUserOverrides()
                                appCount = TabDatabase.getInstance(context).tabDao().getAllAppTabs().size
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
fun LogViewer() {
    val logs = remember { mutableStateListOf<LLMLogger.LogEntry>() }

    LaunchedEffect(Unit) {
        LLMLogger.logFlow.collect { log ->
            logs.add(log)
            if (logs.size > 100) logs.removeFirst()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
    ) {
        Box(modifier = Modifier.height(300.dp).padding(8.dp)) {
            LazyColumn {
                items(logs.reversed()) { log ->
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "[${log.level}] ${log.provider ?: "System"}",
                            color = getLogColor(log.level).copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
                        )
                        Text(
                            text = log.message,
                            color = getLogColor(log.level),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                        )
                        if (log.details.isNotEmpty()) {
                            Text(
                                text = log.details.toString(),
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp),
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getLogColor(level: LLMLogger.LogLevel): Color = when (level) {
    LLMLogger.LogLevel.ERROR -> Color(0xFFFF6B6B)
    LLMLogger.LogLevel.WARNING -> Color(0xFFFFD93D)
    LLMLogger.LogLevel.INFO -> Color(0xFF4ECDC4)
    LLMLogger.LogLevel.DEBUG -> Color(0xFF95A5A6)
}
