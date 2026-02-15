/*
 * Copyright 2024, AutoCat
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

package app.lawnchair.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import dev.kdrag0n.monet.theme.ColorScheme

/**
 * A ContextWrapper that provides a [DynamicResources] instance to bridge
 * Material You colors to the legacy XML theming system.
 */
class DynamicThemeContextWrapper(
    base: Context,
    colorScheme: ColorScheme,
) : ContextWrapper(base) {

    private val resources by lazy {
        DynamicResources(super.getResources(), colorScheme)
    }

    override fun getResources(): Resources = resources
}
