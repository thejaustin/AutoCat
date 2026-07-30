package app.lawnchair.allapps

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lawnchair.categorization.AppTabsController
import app.lawnchair.categorization.TabActionController
import app.lawnchair.theme.color.tokens.ColorTokens
import app.lawnchair.ui.ModalBottomSheetContent
import app.lawnchair.ui.util.bottomSheetHandler
import app.lawnchair.ui.util.rememberExpressiveHaptics
import com.android.launcher3.R
import kotlinx.coroutines.launch

@Composable
fun AppTabsView(
    modifier: Modifier = Modifier,
    onTabSelect: (Int) -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { AppTabsController.getInstance(context) }
    val tabActionController = remember { TabActionController.getInstance(context) }
    val tabs by controller.tabNames.collectAsState()
    val currentTab by controller.currentTabIndex.collectAsState()
    val haptics = rememberExpressiveHaptics()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val bottomSheetHandler = bottomSheetHandler

    // Scroll to selected tab when it changes
    LaunchedEffect(currentTab) {
        if (currentTab >= 0 && currentTab < tabs.size) {
            scope.launch {
                listState.animateScrollToItem(currentTab)
            }
        }
    }

    LazyRow(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(tabs) { index, title ->
            val tabName = controller.getTabNameForTab(index)
            val isSpecialTab = title == AppTabsController.TAB_ALL || title == "Discovery"

            TabItem(
                title = title,
                isSelected = index == currentTab,
                onClick = {
                    haptics.click()
                    controller.setCurrentTab(index)
                    onTabSelect(index)
                },
                onLongClick = {
                    haptics.longPress()
                },
                onArchive = if (!isSpecialTab) {
                    {
                        bottomSheetHandler.show {
                            CategoryActionConfirmDialog(
                                title = stringResource(id = R.string.archive_category_dialog_title, title),
                                description = stringResource(id = R.string.archive_category_dialog_message, title),
                                confirmLabel = stringResource(id = R.string.archive_category_label),
                                onConfirm = {
                                    haptics.success()
                                    tabActionController.archiveCategory(tabName)
                                },
                                onDismiss = { bottomSheetHandler.hide() },
                            )
                        }
                    }
                } else {
                    null
                },
                onRestore = if (!isSpecialTab) {
                    {
                        bottomSheetHandler.show {
                            CategoryActionConfirmDialog(
                                title = stringResource(id = R.string.restore_category_dialog_title, title),
                                description = stringResource(id = R.string.restore_category_dialog_message, title),
                                confirmLabel = stringResource(id = R.string.restore_category_label),
                                onConfirm = {
                                    haptics.success()
                                    tabActionController.restoreCategory(tabName)
                                },
                                onDismiss = { bottomSheetHandler.hide() },
                            )
                        }
                    }
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBackground",
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabContent",
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "tabScale",
    )

    val cornerSize by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "tabCorners",
    )

    Box {
        Box(
            modifier = Modifier
                .height(32.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(cornerSize))
                .background(backgroundColor)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    onLongClick = {
                        onLongClick()
                        if (onArchive != null || onRestore != null) {
                            showMenu = true
                        }
                    },
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            onArchive?.let {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.archive_category_label)) },
                    onClick = {
                        showMenu = false
                        it()
                    },
                )
            }
            onRestore?.let {
                DropdownMenuItem(
                    text = { Text(stringResource(id = R.string.restore_category_label)) },
                    onClick = {
                        showMenu = false
                        it()
                    },
                )
            }
        }
    }
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun CategoryActionConfirmDialog(
    title: String,
    description: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheetContent(
        modifier = modifier,
        title = { Text(text = title) },
        text = { Text(text = description) },
        buttons = {
            OutlinedButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
            Spacer(modifier = Modifier.requiredWidth(8.dp))
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(text = confirmLabel)
            }
        },
    )
}
