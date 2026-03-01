
/*
 * Copyright 2022, Lawnchair
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

package app.lawnchair.ui.preferences.components.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.ExperimentalSharedTransitionApi
import app.lawnchair.ui.preferences.LocalAnimatedVisibilityScope
import app.lawnchair.ui.preferences.LocalSharedTransitionScope
...
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PreferenceScaffold(
    label: String,
    isExpandedScreen: Boolean,
    modifier: Modifier = Modifier,
    id: String? = null,
    backArrowVisible: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = { BottomSpacer() },
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = if (isExpandedScreen) TopAppBarDefaults.pinnedScrollBehavior() else TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    Scaffold(
        modifier = modifier
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null && id != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = "category_$id"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                } else Modifier
            )
            .nestedScroll(scrollBehavior.nestedScrollConnection),
...
        topBar = {
            TopBar(
                backArrowVisible = backArrowVisible,
                label = label,
                isExpandedScreen = isExpandedScreen,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
    ) {
        content(it)
    }
}
