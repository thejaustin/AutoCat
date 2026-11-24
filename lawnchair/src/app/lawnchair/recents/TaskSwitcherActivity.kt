package app.lawnchair.recents

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.lawnchair.preferences.PreferenceManager
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = BLUR_RADIUS }
        }
        val repository = TaskSwitcherRepository(this)
        setContent {
            AutoCatTheme(darkTheme = true) {
                TaskSwitcherScreen(
                    repository = repository,
                    onDismiss = ::finish,
                )
            }
        }
    }

    companion object {
        private const val BLUR_RADIUS = 48
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
            .background(Color.Black.copy(alpha = 0.55f * entry.value.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TaskRow(
                            tasks = tasks,
                            repository = repository,
                            listState = listState,
                            onOpen = { task, bounds ->
                                scope.launch {
                                    if (repository.open(task.taskId, rootView, bounds)) onDismiss()
                                }
                            },
                            onRemove = { task ->
                                tasks.remove(task)
                                scope.launch { repository.remove(task.taskId) }
                            },
                            onClearAll = {
                                val removed = tasks.toList()
                                tasks.clear()
                                scope.launch {
                                    removed.forEach { repository.remove(it.taskId) }
                                    onDismiss()
                                }
                            },
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        ActionRow(
                            repository = repository,
                            focusedTask = tasks.getOrNull(focused),
                            onDone = onDismiss,
                        )
                    }
                }
            }
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
    onClearAll: () -> Unit,
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

@Composable
private fun TaskCard(
    task: RecentTask,
    repository: TaskSwitcherRepository,
    onOpen: (IntRect) -> Unit,
    onRemove: () -> Unit,
    centerProximity: () -> Float,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var bounds by remember { mutableStateOf(IntRect.Zero) }
    val dismissThreshold = with(LocalDensity.current) { 160.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val closeLabel = stringResource(R.string.accessibility_close)
    val thumbnail by produceState(initialValue = null as ImageBitmap?, task.taskId) {
        value = repository.loadThumbnail(task.taskId)
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
        horizontalAlignment = Alignment.CenterHorizontally,
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
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = task.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.app_info_drop_target_label)) },
                    onClick = {
                        menuOpen = false
                        repository.openAppInfo(task)
                    },
                )
            }
        }
        val pressSource = remember { MutableInteractionSource() }
        val pressed by pressSource.collectIsPressedAsState()
        // Shape morphs on press: a small M3 Expressive touch
        val corner by animateDpAsState(
            targetValue = if (pressed) 40.dp else 28.dp,
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
                .clickable(
                    interactionSource = pressSource,
                    indication = null,
                    onClickLabel = "Open ${task.label}",
                    onClick = { onOpen(bounds) },
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
private const val THUMBNAIL_ASPECT = 9f / 19.5f
