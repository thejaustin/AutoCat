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
import androidx.compose.runtime.mutableIntStateOf
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
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.controls.TextFieldPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DiagnosticsPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferenceManager2 = PreferenceManager2.getInstance(context)
    var appCount by remember { mutableIntStateOf(0) }
    var tabCount by remember { mutableIntStateOf(0) }
    var overrideCount by remember { mutableIntStateOf(0) }
    val crashLogs = remember { mutableStateListOf<File>() }
    var logsVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = TabDatabase.getInstance(context)
            val dao = db.categoryDao()
            val allAppCategories = dao.getAllAppCategories()
            appCount = allAppCategories.size
            tabCount = dao.getAllCustomCategories().size
            overrideCount = allAppCategories.count { it.isUserOverride }
            crashLogs.clear()
            crashLogs.addAll(app.lawnchair.bugreport.AutoCatBugReporter.INSTANCE.get(context).getLogs())
        }
    }

    PreferenceScaffold(
        label = "Diagnostics",
        modifier = modifier,
        isExpandedScreen = LocalIsExpandedScreen.current,
    ) {
        PreferenceLazyColumn(it) {
            item {
                PreferenceGroup(heading = "Crash Reporting") {
                    SwitchPreference(
                        adapter = preferenceManager2.showLocalCrashUi.getAdapter(),
                        label = "Show Local Crash UI",
                        description = "Display a detailed report inside the app on launch after a crash",
                    )
                }
            }

            item {
                PreferenceGroup(heading = "Database Stats") {
                    StatRow(icon = Icons.Rounded.Storage, label = "Total Categorized Apps", value = appCount.toString())
                    StatRow(icon = Icons.Rounded.BugReport, label = "User Overrides", value = overrideCount.toString())
                    StatRow(icon = Icons.Rounded.History, label = "Custom Tabs", value = tabCount.toString())
                }
            }

            item {
                PreferenceGroup(heading = "App Crash Logs") {
                    if (crashLogs.isEmpty()) {
                        Text(
                            text = "No crash logs found",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        crashLogs.forEach { file ->
                            CrashLogItem(file)
                        }
                    }
                }
            }

            item {
                PreferenceGroup(heading = "LLM Live Logs") {
                    LogViewer(logsVersion = logsVersion)
                }
            }

            item {
                PreferenceGroup(heading = "Debug Actions") {
                    ClickablePreference(
                        label = "Trigger Test Crash",
                        subtitle = "Immediately crash the app to test reporting",
                        onClick = {
                            throw RuntimeException("Test Crash triggered from Diagnostics")
                        },
                    )

                    ClickablePreference(
                        label = "Clear All Logs",
                        subtitle = "Reset the in-memory log buffer",
                        onClick = {
                            LLMLogger.clearLogs()
                            logsVersion++
                        },
                    )

                    ClickablePreference(
                        label = "Clear Categorization Database",
                        subtitle = "CAUTION: Deletes all app assignments (not overrides)",
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                TabDatabase.getInstance(context).categoryDao().deleteNonUserOverrides()
                                appCount = TabDatabase.getInstance(context).categoryDao().getAllAppCategories().size
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun StatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
fun LogViewer(
    logsVersion: Int,
    modifier: Modifier = Modifier,
) {
    val logs = remember(logsVersion) { mutableStateListOf<LLMLogger.LogEntry>() }

    LaunchedEffect(logsVersion) {
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
                        if (log.details?.isNotEmpty() == true) {
                            Text(
                                text = log.details?.toString() ?: "{}",
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

@Composable
fun CrashLogItem(
    file: File,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val content = remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(expanded) {
        if (expanded && content.value.isEmpty()) {
            withContext(Dispatchers.IO) {
                content.value = file.readText()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize(),
        onClick = { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = file.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified())),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val text = try {
                            file.readText()
                        } catch (e: Exception) {
                            "Failed to read crash log: ${e.message}"
                        }
                        withContext(Dispatchers.Main) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Crash Log"))
                        }
                    }
                }) {
                    Icon(Icons.Rounded.BugReport, null)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                ) {
                    Text(
                        text = content.value,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.LightGray,
                    )
                }
            }
        }
    }
}
