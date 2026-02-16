/*
 * Copyright 2021, AutoCat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.about

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.launcher3.R
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateLink(
    updateState: UpdateState,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onViewChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (updateState) {
        UpdateState.Hidden, UpdateState.UpToDate -> stringResource(id = R.string.check_for_update)
        UpdateState.Checking -> "Checking..."
        is UpdateState.Available -> "Update Available"
        is UpdateState.Downloading -> "Downloading..."
        is UpdateState.Downloaded -> "Ready to Install"
        UpdateState.Failed -> "Update Failed"
    }

    val iconAlpha by animateFloatAsState(
        targetValue = if (updateState is UpdateState.Checking) 0.5f else 1f,
        label = "iconAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                when (updateState) {
                    UpdateState.UpToDate, UpdateState.Failed, UpdateState.Hidden -> onCheck()
                    is UpdateState.Available -> onViewChanges()
                    is UpdateState.Downloading -> { /* progress is shown */ }
                    is UpdateState.Downloaded -> onInstall(updateState.file)
                    UpdateState.Checking -> { /* already checking */ }
                }
            },
    ) {
        when (updateState) {
            is UpdateState.Checking -> {
                LoadingIndicator(modifier = Modifier.size(24.dp))
            }

            is UpdateState.Downloading -> {
                CircularProgressIndicator(
                    progress = updateState.progress,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }

            else -> {
                Image(
                    painterResource(id = R.drawable.ic_download),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color = LocalContentColor.current),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(alpha = iconAlpha),
                )
            }
        }
        Spacer(modifier = Modifier.requiredHeight(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
