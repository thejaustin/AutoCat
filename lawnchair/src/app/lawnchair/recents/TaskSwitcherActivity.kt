package app.lawnchair.recents

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.ui.preferences.PreferenceActivity
import app.lawnchair.ui.theme.AutoCatTheme
import com.android.launcher3.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Movement and fades use the theme's MotionScheme (MaterialTheme.motionScheme); the dismiss
// spring is AOSP's expressive_dismiss_task_trans_y_* from quickstep/res/values/config.xml
private val DismissSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 850f)
private const val TAG = "TaskSwitcher"

private enum class WindowMode { Split, Popup }
private const val FLING_DISMISS_VELOCITY = 700f

/**
 * AutoCat's own recents screen. It works without root by reading and controlling tasks
 * through Shizuku, so it can't replace the system's swipe-up recents, but it can be opened
 * from launcher gestures or any app that can launch an activity.
 */
class TaskSwitcherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The screen runs its own entry spring, so the default window slide would fight it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
        val blurRadius = blurRadiusFor(PreferenceManager.getInstance(this).switcherBlur.get())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = blurRadius }
        }
        val repository = TaskSwitcherRepository(this)
        setContent {
            AutoCatTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Samsung's compositor reports no window blur, so blur the app underneath ourselves
                    if (blurRadius > 0 && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled)) {
                        BlurredBackdrop(repository = repository, radius = blurRadius.dp / 4)
                    }
                    TaskSwitcherScreen(
                        repository = repository,
                        onDismiss = ::finish,
                    )
                }
            }
        }
    }

    companion object {
        private fun blurRadiusFor(setting: String) = when (setting) {
            "none" -> 0
            "soft" -> 48
            else -> 140
        }
    }
}

@Composable
private fun TaskSwitcherScreen(
    repository: TaskSwitcherRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val rootView = LocalView.current
    // Mini mode only applies to the grid, where it moves the cards down to the thumb
    val switcherPrefs0 = remember { PreferenceManager.getInstance(rootView.context) }
    val gridLayout = remember { switcherPrefs0.switcherLayout.get() == "grid" }
    // A blurred backdrop needs less dimming to keep the cards readable
    val scrimAlpha = remember { if (switcherPrefs0.switcherBlur.get() == "none") 0.55f else 0.32f }
    var availability by remember { mutableStateOf(repository.availability()) }
    var loading by remember { mutableStateOf(true) }
    val tasks = remember { mutableStateListOf<RecentTask>() }
    val entry = remember { Animatable(0f) }
    val entrySpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) { entry.animateTo(1f, entrySpec) }

    // Re-checked on resume, so granting Shizuku access in its dialog takes effect right away
    LifecycleResumeEffect(Unit) {
        availability = repository.availability()
        onPauseOrDispose { }
    }
    LaunchedEffect(availability) {
        if (availability != TaskSwitcherRepository.Availability.Ready) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        tasks.clear()
        // The Shizuku service can still be binding on the first open; retry before showing an empty list
        var loaded = repository.loadTasks()
        repeat(LOAD_RETRIES) {
            if (loaded != null) return@repeat
            delay(LOAD_RETRY_DELAY_MS)
            loaded = repository.loadTasks()
        }
        tasks.addAll(loaded.orEmpty())
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha * entry.value.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.then(if (gridLayout) Modifier.fillMaxSize() else Modifier).graphicsLayer {
                val t = entry.value
                scaleX = 0.92f + 0.08f * t
                scaleY = 0.92f + 0.08f * t
                alpha = t.coerceIn(0f, 1f)
            },
            contentAlignment = Alignment.Center,
        ) {
            when {
                availability == TaskSwitcherRepository.Availability.ShizukuNotRunning -> SetupMessage(
                    text = "Recent apps need Shizuku. Start Shizuku, then open this again.",
                )

                availability == TaskSwitcherRepository.Availability.BackendDisabled -> SetupMessage(
                    text = stringResource(R.string.recents_backend_disabled),
                )

                availability == TaskSwitcherRepository.Availability.PermissionNeeded -> SetupMessage(
                    text = "Allow AutoCat to use Shizuku to show your recent apps.",
                    actionLabel = "Allow",
                    onAction = repository::requestPermission,
                )

                loading -> CircularProgressIndicator()

                tasks.isEmpty() -> Text(
                    text = stringResource(R.string.recents_empty_message),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )

                else -> {
                    val listState = rememberLazyListState()
                    val focused by remember(listState) {
                        derivedStateOf { listState.focusedIndex() }
                    }
                    // Light tick as each card passes the centre, like AOSP's recents scroll
                    val view = LocalView.current
                    LaunchedEffect(listState) {
                        var last = -1
                        snapshotFlow { focused }.collect {
                            if (last != -1 && it != last) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            last = it
                        }
                    }
                    val switcherPrefs = remember { PreferenceManager.getInstance(rootView.context) }
                    val grid = gridLayout
                    val closeAllPill = remember { switcherPrefs.switcherCloseAllPill.get() }
                    val open: (RecentTask, IntRect) -> Unit = { task, bounds ->
                        scope.launch {
                            if (repository.open(task.taskId, rootView, bounds)) onDismiss()
                        }
                    }
                    val remove: (RecentTask) -> Unit = { task ->
                        tasks.remove(task)
                        scope.launch { repository.remove(task.taskId) }
                    }
                    val windowMode: (RecentTask, WindowMode) -> Unit = { task, mode ->
                        scope.launch {
                            val started = when (mode) {
                                WindowMode.Split -> repository.openSplit(task.taskId)
                                WindowMode.Popup -> repository.openPopup(task.taskId)
                            }
                            if (started) onDismiss()
                        }
                    }
                    val clearAll: () -> Unit = {
                        val removed = tasks.toList()
                        tasks.clear()
                        scope.launch {
                            removed.forEach { repository.remove(it.taskId) }
                            onDismiss()
                        }
                    }
                    Column(
                        modifier = if (grid) Modifier.fillMaxSize() else Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (grid) {
                            TaskGrid(
                                tasks = tasks,
                                repository = repository,
                                onOpen = open,
                                onRemove = remove,
                                onWindowMode = windowMode,
                                rows = switcherPrefs.switcherGridRows.get().toIntOrNull()?.coerceIn(2, 3) ?: 2,
                                mini = switcherPrefs.switcherMiniMode.get(),
                                showPill = remember { switcherPrefs.switcherActiveAppsPill.get() },
                                showClearAll = remember { switcherPrefs.switcherGridCloseAll.get() },
                                swapPills = remember { switcherPrefs.switcherSwapPills.get() },
                                showOverflow = remember { switcherPrefs.switcherOverflowMenu.get() },
                                onClearAll = clearAll,
                            )
                        } else {
                            TaskRow(
                                tasks = tasks,
                                repository = repository,
                                listState = listState,
                                onOpen = open,
                                onRemove = remove,
                                onWindowMode = windowMode,
                                onClearAll = clearAll,
                                showClearAll = !closeAllPill,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            ActionRow(
                                repository = repository,
                                focusedTask = tasks.getOrNull(focused),
                                onDone = onDismiss,
                            )
                        }
                        if (!grid && closeAllPill) {
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = clearAll,
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier.width(220.dp).height(52.dp),
                            ) {
                                Text(text = stringResource(R.string.recents_clear_all), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The newest task's snapshot, blurred, behind the cards: a stand-in for window blur where the device has none. */
@Composable
private fun BlurredBackdrop(repository: TaskSwitcherRepository, radius: Dp) {
    val image by produceState(initialValue = null as ImageBitmap?) {
        value = repository.loadTasks()?.firstOrNull()?.let { repository.loadThumbnail(it.taskId) }
    }
    Crossfade(targetState = image, animationSpec = tween(250), label = "backdrop") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(radius, BlurredEdgeTreatment.Unbounded),
            )
        }
    }
}

/** Index of the item nearest the viewport centre: the task the actions apply to. */
private fun LazyListState.focusedIndex(): Int {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo
        .minByOrNull { abs(it.offset + it.size / 2 - center) }
        ?.index ?: 0
}

/** 1 when the keyed item sits at the viewport centre, falling to 0 one card-width away. Read in draw phase only. */
private fun LazyListState.proximityToCenter(key: Any): Float {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    val distance = abs(item.offset + item.size / 2 - center)
    return (1f - distance / item.size.toFloat()).coerceIn(0f, 1f)
}

// Indices match recentActionOrder; Locked and Clear all have no button in this row
private enum class SwitcherAction { Screenshot, Share, Lens, Locked, ClearAll }

@Composable
private fun ActionRow(
    repository: TaskSwitcherRepository,
    focusedTask: RecentTask?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Same prefs as the launcher's own overview actions, so one settings page drives both
    val prefs = remember { PreferenceManager.getInstance(context) }
    val actions = remember {
        val enabled = buildSet {
            if (prefs.recentsActionScreenshot.get()) add(SwitcherAction.Screenshot)
            if (prefs.recentsActionShare.get()) add(SwitcherAction.Share)
            if (prefs.recentsActionLens.get() && repository.isLensAvailable()) add(SwitcherAction.Lens)
        }
        val order = prefs.recentActionOrder.get().split(",").mapNotNull { it.toIntOrNull() }
        order.mapNotNull { SwitcherAction.entries.getOrNull(it) }.filter { it in enabled }
    }
    val task = focusedTask
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            val label = stringResource(
                when (action) {
                    SwitcherAction.Screenshot -> R.string.action_screenshot
                    SwitcherAction.Share -> R.string.action_share
                    SwitcherAction.Lens -> R.string.action_lens
                    else -> R.string.recents_clear_all
                },
            )
            val failed = stringResource(R.string.task_switcher_action_failed)
            val icon = when (action) {
                SwitcherAction.Screenshot -> R.drawable.ic_screenshot
                SwitcherAction.Share -> R.drawable.ic_share
                SwitcherAction.Lens -> R.drawable.ic_lens
                else -> R.drawable.ic_clear_all_recents
            }
            FilledTonalButton(
                enabled = task != null,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                onClick = {
                    val id = task?.taskId ?: return@FilledTonalButton
                    scope.launch {
                        val ok = when (action) {
                            SwitcherAction.Screenshot -> repository.saveScreenshot(id)
                            SwitcherAction.Share -> repository.shareSnapshot(id, lens = false)
                            else -> repository.shareSnapshot(id, lens = true)
                        }
                        if (ok) onDone() else Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    tasks: List<RecentTask>,
    repository: TaskSwitcherRepository,
    listState: LazyListState,
    onOpen: (RecentTask, IntRect) -> Unit,
    onRemove: (RecentTask) -> Unit,
    onWindowMode: (RecentTask, WindowMode) -> Unit,
    onClearAll: () -> Unit,
    showClearAll: Boolean,
    modifier: Modifier = Modifier,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val cardWidth = (screenWidth * 0.62f).dp
    // Centres the first and last card, like the Pixel switcher
    val edge = ((screenWidth - cardWidth.value) / 2f).dp
    // Tasks are newest first; reversing puts the newest on the right with older ones to its left
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        reverseLayout = true,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(horizontal = edge),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(tasks, key = { it.taskId }) { task ->
            TaskCard(
                task = task,
                repository = repository,
                onOpen = { bounds -> onOpen(task, bounds) },
                onRemove = { onRemove(task) },
                onWindowMode = { mode -> onWindowMode(task, mode) },
                centerProximity = { listState.proximityToCenter(task.taskId) },
                modifier = Modifier
                    .width(cardWidth)
                    .animateItem(
                        fadeInSpec = null,
                        placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    ),
            )
        }
        // Like Pixel, Clear all waits past the oldest task
        if (showClearAll) {
            item(key = "clear_all") {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .fillMaxHeight()
                        .animateItem(),
                    contentAlignment = Alignment.Center,
                ) {
                    FilledTonalButton(onClick = onClearAll) {
                        Text(text = stringResource(R.string.recents_clear_all))
                    }
                }
            }
        }
    }
}

/**
 * One UI's Good Lock grid: two or three rows of cards that scroll sideways, newest at the right
 * like the carousel. The grid fills the screen with the cards at the bottom, so a card flung up
 * leaves through the top of the screen instead of being cut off at the edge of the grid.
 */
@Composable
private fun TaskGrid(
    tasks: List<RecentTask>,
    repository: TaskSwitcherRepository,
    onOpen: (RecentTask, IntRect) -> Unit,
    onRemove: (RecentTask) -> Unit,
    onWindowMode: (RecentTask, WindowMode) -> Unit,
    rows: Int,
    mini: Boolean,
    showPill: Boolean,
    showClearAll: Boolean,
    swapPills: Boolean,
    showOverflow: Boolean,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = LocalConfiguration.current
    // Cards are sized so every row fits in a share of the screen height: less of it in mini mode
    val budget = (
        config.screenHeightDp * if (mini) {
            0.5f
        } else if (rows >= 3) {
            0.74f
        } else {
            0.62f
        }
        ).dp
    val spacing = 16.dp
    val labelHeight = 56.dp
    val footer = 84.dp
    val fittedWidth = ((budget - spacing * (rows - 1)) / rows - labelHeight) * THUMBNAIL_ASPECT
    val cardWidth = minOf(fittedWidth, (config.screenWidthDp * 0.29f).dp)
    val rowHeight = cardWidth / THUMBNAIL_ASPECT + labelHeight
    val gridHeight = rowHeight * rows + spacing * (rows - 1)
    val glass = Color.White.copy(alpha = 0.14f)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = (maxHeight - footer - gridHeight).coerceAtLeast(0.dp),
                bottom = footer,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(tasks, key = { it.taskId }) { task ->
                TaskCard(
                    task = task,
                    repository = repository,
                    onOpen = { bounds -> onOpen(task, bounds) },
                    onRemove = { onRemove(task) },
                    onWindowMode = { mode -> onWindowMode(task, mode) },
                    centerProximity = { 1f },
                    startAligned = true,
                    iconOnly = cardWidth < ICON_ONLY_WIDTH,
                    modifier = Modifier
                        .width(cardWidth)
                        .animateItem(),
                )
            }
        }
        val countPill: @Composable (Modifier) -> Unit = { m ->
            Text(
                text = stringResource(R.string.switcher_active_apps, tasks.size),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = m
                    .background(glass, CircleShape)
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        val clearAllPill: @Composable (Modifier) -> Unit = { m ->
            OutlinedButton(
                onClick = onClearAll,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = glass, contentColor = Color.White),
                modifier = m.height(52.dp),
            ) {
                Text(text = stringResource(R.string.recents_clear_all), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (showOverflow) {
            var open by remember { mutableStateOf(false) }
            val context = LocalContext.current
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = footer + gridHeight + 16.dp),
            ) {
                IconButton(
                    onClick = { open = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(glass, CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = Color.White)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.recents_clear_all)) },
                        onClick = {
                            open = false
                            onClearAll()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.switcher_settings)) },
                        onClick = {
                            open = false
                            runCatching {
                                context.startActivity(
                                    Intent(context, PreferenceActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
                }
            }
        }
        val topSlot = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = footer + gridHeight + 16.dp)
        val footerSlot = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 24.dp)
        if (swapPills) {
            if (showClearAll) clearAllPill(topSlot.width(160.dp))
            if (showPill) countPill(footerSlot)
        } else {
            if (showPill) countPill(topSlot)
            if (showClearAll) clearAllPill(footerSlot.width(220.dp))
        }
    }
}

@Composable
private fun TaskCard(
    task: RecentTask,
    repository: TaskSwitcherRepository,
    onOpen: (IntRect) -> Unit,
    onRemove: () -> Unit,
    onWindowMode: (WindowMode) -> Unit,
    centerProximity: () -> Float,
    modifier: Modifier = Modifier,
    startAligned: Boolean = false,
    iconOnly: Boolean = false,
) {
    val view = LocalView.current
    val prefs = remember { PreferenceManager.getInstance(view.context) }
    val oneUiHeader = remember { startAligned || prefs.switcherHeaderStyle.get() == "oneui" }
    val showLabel = remember { prefs.switcherShowLabel.get() } && !iconOnly
    val longPress = remember { prefs.switcherLongPress.get() }
    var bounds by remember { mutableStateOf(IntRect.Zero) }
    val dismissThreshold = with(LocalDensity.current) { 160.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val closeLabel = stringResource(R.string.accessibility_close)
    val thumbnail by produceState(initialValue = null as ImageBitmap?, task.taskId) {
        value = repository.loadThumbnail(task.taskId) ?: run {
            // A snapshot can be missing for a moment after an app is backgrounded
            delay(700)
            repository.loadThumbnail(task.taskId)
        }
    }

    val returnSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    var crossedThreshold by remember { mutableStateOf(false) }
    // Accumulated synchronously: a quick flick delivers several deltas before a launched snapTo runs
    var rawOffset by remember { mutableFloatStateOf(0f) }
    var dismissing by remember { mutableStateOf(false) }
    // While a finger is down the card follows rawOffset directly; the Animatable only runs after release,
    // so a late queued snapTo can no longer cancel the return animation and leave the card parked
    var dragging by remember { mutableStateOf(false) }
    val dragState = rememberDraggableState { delta ->
        val next = (rawOffset + delta).coerceAtMost(0f)
        rawOffset = next
        val crossed = next < -dismissThreshold
        if (crossed != crossedThreshold) {
            crossedThreshold = crossed
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                // Neighbours recede slightly, like Pixel's carousel; read here so scrolling never recomposes
                val scale = 0.92f + 0.08f * centerProximity()
                scaleX = scale
                scaleY = scale
                val y = if (dragging) rawOffset else offsetY.value
                translationY = y
                alpha = 1f - (abs(y) / (dismissThreshold * 2)).coerceAtMost(if (dismissing) 1f else 0.8f)
            }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                enabled = !dismissing,
                onDragStarted = {
                    rawOffset = offsetY.value.coerceAtMost(0f)
                    dragging = true
                    offsetY.stop()
                    Log.i(TAG, "drag start at $rawOffset")
                },
                onDragStopped = { velocity ->
                    crossedThreshold = false
                    offsetY.snapTo(rawOffset)
                    dragging = false
                    // Like AOSP's TaskViewDismissTouchController: a fling's direction beats the card's position
                    val flingUp = velocity < -FLING_DISMISS_VELOCITY
                    val flingDown = velocity > FLING_DISMISS_VELOCITY
                    val dismiss = flingUp || (!flingDown && rawOffset < -dismissThreshold)
                    Log.i(TAG, "drag stop velocity=$velocity offset=$rawOffset dismiss=$dismiss")
                    if (dismiss) {
                        // Same spring AOSP's overview uses for dismissed tasks, seeded with the flick's velocity
                        dismissing = true
                        var removed = false
                        // Remove as soon as the card has faded out; waiting for the spring to settle left it hanging
                        offsetY.animateTo(-dismissThreshold * 6, DismissSpring, velocity) {
                            if (!removed && value < -dismissThreshold * 2f) {
                                removed = true
                                onRemove()
                            }
                        }
                        if (!removed) onRemove()
                    } else {
                        offsetY.animateTo(0f, returnSpec, velocity)
                        rawOffset = 0f
                    }
                    crossedThreshold = false
                },
            )
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("$closeLabel ${task.label}") {
                        onRemove()
                        true
                    },
                )
            },
        horizontalAlignment = if (oneUiHeader) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { menuOpen = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                task.icon?.let { icon ->
                    Image(
                        painter = rememberDrawablePainter(icon),
                        contentDescription = null,
                        modifier = Modifier.size(if (startAligned) 30.dp else 28.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (showLabel) {
                    Text(
                        text = task.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.app_info_drop_target_label)) },
                    onClick = {
                        menuOpen = false
                        repository.openAppInfo(task)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.switcher_split_screen)) },
                    onClick = {
                        menuOpen = false
                        onWindowMode(WindowMode.Split)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.switcher_popup_view)) },
                    onClick = {
                        menuOpen = false
                        onWindowMode(WindowMode.Popup)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.accessibility_close)) },
                    onClick = {
                        menuOpen = false
                        onRemove()
                    },
                )
            }
        }
        val pressSource = remember { MutableInteractionSource() }
        val pressed by pressSource.collectIsPressedAsState()
        // Shape morphs on press: a small M3 Expressive touch
        val corner by animateDpAsState(
            // Small grid cards get One UI's tighter corners; the big carousel card keeps the rounder shape
            targetValue = if (startAligned) (if (pressed) 20.dp else 14.dp) else (if (pressed) 40.dp else 28.dp),
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "card corner",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT)
                .onGloballyPositioned { coords ->
                    val rect = coords.boundsInWindow()
                    bounds = IntRect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt())
                }
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .combinedClickable(
                    interactionSource = pressSource,
                    indication = null,
                    onClickLabel = "Open ${task.label}",
                    onClick = { onOpen(bounds) },
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        when (longPress) {
                            "split" -> onWindowMode(WindowMode.Split)
                            "popup" -> onWindowMode(WindowMode.Popup)
                            "appinfo" -> repository.openAppInfo(task)
                            else -> menuOpen = true
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = thumbnail, animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(), label = "thumbnail") { image ->
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = task.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Tint the placeholder with the icon's average colour, like One UI's icon cards
                    val tint = remember(task.icon) {
                        runCatching {
                            val px = task.icon!!.toBitmap(1, 1).getPixel(0, 0)
                            Color(px).copy(alpha = 0.22f)
                        }.getOrDefault(Color.Transparent)
                    }
                    Box(modifier = Modifier.fillMaxSize().background(tint), contentAlignment = Alignment.Center) {
                        task.icon?.let { icon ->
                            Image(
                                painter = rememberDrawablePainter(icon),
                                contentDescription = task.label,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupMessage(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

private const val LOAD_RETRIES = 3
private const val LOAD_RETRY_DELAY_MS = 700L

// Phones are tall; most snapshots are about this shape
// Cards narrower than this show just the app icon above them
private val ICON_ONLY_WIDTH = 96.dp
private const val THUMBNAIL_ASPECT = 9f / 19.5f
