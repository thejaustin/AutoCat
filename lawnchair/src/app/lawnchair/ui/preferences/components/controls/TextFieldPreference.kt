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

package app.lawnchair.ui.preferences.components.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lawnchair.preferences.getAdapter
import com.patrykmichalik.opto.domain.Preference

@Composable
fun TextFieldPreference(
    label: String,
    preference: Preference<String, *, *>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
) {
    val adapter = preference.getAdapter()
    TextPreference(
        adapter = adapter,
        label = label,
        description = { it.ifEmpty { subtitle } ?: "" },
        enabled = enabled,
        modifier = modifier,
    )
}
