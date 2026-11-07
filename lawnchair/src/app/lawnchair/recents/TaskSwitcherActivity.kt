package app.lawnchair.recents

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.lawnchair.ui.theme.AutoCatTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * AutoCat's own recents screen. It works without root by reading and controlling tasks
 * through Shizuku, so it can't replace the system's swipe-up recents, but it can be opened
 * from launcher gestures or any app that can launch an activity.
 */
class TaskSwitcherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    var availability by remember { mutableStateOf(repository.availability()) }
    var loading by remember { mutableStateOf(true) }
    val tasks = remember { mutableStateListOf<RecentTask>() }

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
        tasks.addAll(repository.loadTasks().orEmpty())
        loading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            availability == TaskSwitcherRepository.Availability.ShizukuNotRunning -> SetupMessage(
                text = "Recent apps need Shizuku. Start Shizuku, then open this again.",
            )

            availability == TaskSwitcherRepository.Availability.PermissionNeeded -> SetupMessage(
                text = "Allow AutoCat to use Shizuku to show your recent apps.",
                actionLabel = "Allow",
                onAction = repository::requestPermission,
            )

            loading -> CircularProgressIndicator()

            tasks.isEmpty() -> Text(
                text = "No recent apps",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TaskRow(
                    tasks = tasks,
                    repository = repository,
                    onOpen = { task ->
                        scope.launch {
                            if (repository.open(task.taskId)) onDismiss()
                        }
                    },
                    onRemove = { task ->
                        tasks.remove(task)
                        scope.launch { repository.remove(task.taskId) }
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = {
                        val removed = tasks.toList()
                        tasks.clear()
                        scope.launch {
                            removed.forEach { repository.remove(it.taskId) }
                            onDismiss()
                        }
                    },
                ) {
                    Text(text = "Clear all")
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    tasks: List<RecentTask>,
    repository: TaskSwitcherRepository,
    onOpen: (RecentTask) -> Unit,
    onRemove: (RecentTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardWidth = (LocalConfiguration.current.screenWidthDp * 0.62f).dp
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(tasks, key = { it.taskId }) { task ->
            TaskCard(
                task = task,
                repository = repository,
                onOpen = { onOpen(task) },
                onRemove = { onRemove(task) },
                modifier = Modifier
                    .width(cardWidth)
                    .animateItem(),
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: RecentTask,
    repository: TaskSwitcherRepository,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val dismissThreshold = with(LocalDensity.current) { 160.dp.toPx() }
    val offsetY = remember { Animatable(0f) }
    val thumbnail by produceState(initialValue = null as ImageBitmap?, task.taskId) {
        value = repository.loadThumbnail(task.taskId)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                translationY = offsetY.value
                alpha = 1f - (abs(offsetY.value) / (dismissThreshold * 2)).coerceAtMost(0.8f)
            }
            .pointerInput(task.taskId) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        scope.launch { offsetY.snapTo((offsetY.value + dragAmount).coerceAtMost(0f)) }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetY.value < -dismissThreshold) {
                                offsetY.animateTo(-dismissThreshold * 4)
                                onRemove()
                            } else {
                                offsetY.animateTo(0f)
                            }
                        }
                    },
                )
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Close ${task.label}") {
                        onRemove()
                        true
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(THUMBNAIL_ASPECT)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClickLabel = "Open ${task.label}", onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            val image = thumbnail
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = task.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
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

// Phones are tall; most snapshots are about this shape
private const val THUMBNAIL_ASPECT = 9f / 19.5f
