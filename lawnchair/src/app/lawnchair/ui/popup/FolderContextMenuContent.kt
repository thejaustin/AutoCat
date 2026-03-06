package app.lawnchair.ui.popup

import android.os.Process
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.appops.AppBatchOperationService
import app.lawnchair.appops.AppBatchOperationService.OperationResult
import com.android.launcher3.R
import com.android.launcher3.util.ApplicationInfoWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FolderAppState(
    val activeCount: Int,
    val archivedCount: Int,
    val systemCount: Int,
)

private sealed class FolderMenuScreen {
    data object Loading : FolderMenuScreen()

    data class Menu(val appState: FolderAppState) : FolderMenuScreen()

    data class Progress(
        val current: Int,
        val total: Int,
        val currentLabel: String,
        val isArchiving: Boolean,
    ) : FolderMenuScreen()

    data class Done(
        val results: Map<String, OperationResult>,
        val isArchiving: Boolean,
    ) : FolderMenuScreen()
}

/**
 * Bottom sheet shown when the user long-presses a folder icon on the workspace.
 * Shows an M3 action list with Restore All, Archive All, and Move Folder options.
 * Transitions to an in-progress view with a determinate progress bar during batch ops,
 * then shows a result summary when done.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolderContextMenuContent(
    folderName: String,
    packages: List<String>,
    onMove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf<FolderMenuScreen>(FolderMenuScreen.Loading) }

    // Classify each package as active / archived / system on IO to populate the action menu.
    LaunchedEffect(packages) {
        val state = withContext(Dispatchers.IO) {
            val user = Process.myUserHandle()
            val service = AppBatchOperationService(context)
            var activeCount = 0
            var archivedCount = 0
            var systemCount = 0
            packages.forEach { pkg ->
                when {
                    service.isSystemApp(pkg) -> systemCount++
                    ApplicationInfoWrapper(context, pkg, user).isArchived() -> archivedCount++
                    else -> activeCount++
                }
            }
            FolderAppState(
                activeCount = activeCount,
                archivedCount = archivedCount,
                systemCount = systemCount,
            )
        }
        screen = FolderMenuScreen.Menu(state)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .safeDrawingPadding(),
    ) {
        Box(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Text(
                text = folderName,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // contentKey = screen::class so progress updates don't retrigger the fade transition.
        AnimatedContent(
            targetState = screen,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FolderMenuScreen",
        ) { currentScreen ->
            when (currentScreen) {
                is FolderMenuScreen.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                }

                is FolderMenuScreen.Menu -> {
                    MenuContent(
                        appState = currentScreen.appState,
                        onArchive = {
                            scope.launch {
                                val service = AppBatchOperationService(context)
                                val results = service.archiveApps(packages) { progress ->
                                    val label = service.getAppLabel(progress.currentPackage)
                                    scope.launch(Dispatchers.Main.immediate) {
                                        screen = FolderMenuScreen.Progress(
                                            current = progress.current,
                                            total = progress.total,
                                            currentLabel = label,
                                            isArchiving = true,
                                        )
                                    }
                                }
                                screen = FolderMenuScreen.Done(results = results, isArchiving = true)
                            }
                        },
                        onRestore = {
                            scope.launch {
                                val service = AppBatchOperationService(context)
                                val results = service.unarchiveApps(packages) { progress ->
                                    val label = service.getAppLabel(progress.currentPackage)
                                    scope.launch(Dispatchers.Main.immediate) {
                                        screen = FolderMenuScreen.Progress(
                                            current = progress.current,
                                            total = progress.total,
                                            currentLabel = label,
                                            isArchiving = false,
                                        )
                                    }
                                }
                                screen = FolderMenuScreen.Done(results = results, isArchiving = false)
                            }
                        },
                        onMove = {
                            onDismiss()
                            onMove()
                        },
                        onDismiss = onDismiss,
                    )
                }

                is FolderMenuScreen.Progress -> {
                    ProgressContent(screen = currentScreen)
                }

                is FolderMenuScreen.Done -> {
                    DoneContent(
                        screen = currentScreen,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuContent(
    appState: FolderAppState,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onMove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val disabledAlpha = 0.38f
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = modifier) {
        if (appState.systemCount > 0) {
            Text(
                text = stringResource(R.string.folder_system_note, appState.systemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        val restoreEnabled = appState.archivedCount > 0
        ListItem(
            headlineContent = { Text(stringResource(R.string.restore_folder_label)) },
            supportingContent = {
                Text(
                    if (restoreEnabled) {
                        stringResource(R.string.restore_folder_count, appState.archivedCount)
                    } else {
                        stringResource(R.string.restore_folder_none)
                    },
                )
            },
            leadingContent = {
                Icon(Icons.Rounded.Unarchive, contentDescription = null)
            },
            modifier = Modifier
                .alpha(if (restoreEnabled) 1f else disabledAlpha)
                .then(if (restoreEnabled) Modifier.clickable(onClick = onRestore) else Modifier),
        )

        val archiveEnabled = appState.activeCount > 0
        val archiveTint = if (archiveEnabled) errorColor else LocalContentColor.current
        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.archive_folder_label),
                    color = if (archiveEnabled) errorColor else MaterialTheme.colorScheme.onSurface,
                )
            },
            supportingContent = {
                Text(
                    if (archiveEnabled) {
                        stringResource(R.string.archive_folder_count, appState.activeCount)
                    } else {
                        stringResource(R.string.archive_folder_none)
                    },
                )
            },
            leadingContent = {
                Icon(Icons.Rounded.Archive, contentDescription = null, tint = archiveTint)
            },
            modifier = Modifier
                .alpha(if (archiveEnabled) 1f else disabledAlpha)
                .then(if (archiveEnabled) Modifier.clickable(onClick = onArchive) else Modifier),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.move_folder_label)) },
            leadingContent = {
                Icon(Icons.Rounded.OpenWith, contentDescription = null)
            },
            modifier = Modifier.clickable(onClick = onMove),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    }
}

@Composable
private fun ProgressContent(
    screen: FolderMenuScreen.Progress,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (screen.isArchiving) {
                stringResource(R.string.folder_progress_archiving, screen.current, screen.total)
            } else {
                stringResource(R.string.folder_progress_restoring, screen.current, screen.total)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        LinearProgressIndicator(
            progress = { screen.current.toFloat() / screen.total.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (screen.currentLabel.isNotEmpty()) {
            Text(
                text = screen.currentLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DoneContent(
    screen: FolderMenuScreen.Done,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val succeeded = screen.results.values.count { it is OperationResult.Success }
    val skipped = screen.results.values.count { it is OperationResult.Skipped }
    val failed = screen.results.values.count { it is OperationResult.Failed }
    val pending = screen.results.values.count { it is OperationResult.RequiresUserConfirmation }

    val parts = buildList {
        if (succeeded > 0) {
            add(
                stringResource(
                    if (screen.isArchiving) R.string.folder_result_archived else R.string.folder_result_restored,
                    succeeded,
                ),
            )
        }
        if (skipped > 0) add(stringResource(R.string.folder_result_skipped, skipped))
        if (failed > 0) add(stringResource(R.string.folder_result_failed, failed))
        if (pending > 0) add(stringResource(R.string.folder_result_pending, pending))
    }
    val summary = if (parts.isEmpty()) stringResource(R.string.folder_result_nothing) else parts.joinToString(" · ")

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.done))
            }
        }
    }
}
