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

import android.content.res.Resources
import com.android.launcher3.R
import dev.kdrag0n.monet.theme.ColorScheme

/**
 * A custom Resources implementation that intercepts color lookups and substitutes them
 * with dynamic colors from a Material You ColorScheme.
 */
class DynamicResources(
    private val ares: Resources,
    private val colorScheme: ColorScheme,
) : Resources(ares.assets, ares.displayMetrics, ares.configuration) {

    @Throws(NotFoundException::class)
    override fun getColor(id: Int, theme: Theme?): Int {
        val color = getDynamicColorOrNull(id)
        if (color != null) {
            return color
        }
        return ares.getColor(id, theme)
    }

    @Throws(NotFoundException::class)
    override fun getColor(id: Int): Int {
        val color = getDynamicColorOrNull(id)
        if (color != null) {
            return color
        }
        return ares.getColor(id)
    }

    private fun getDynamicColorOrNull(id: Int): Int? {
        return when (id) {
            // Map our custom XML attributes to the dynamic ColorScheme
            R.attr.colorPrimary -> colorScheme.accent1[6].toAndroidColor()
            R.attr.colorOnPrimary -> colorScheme.accent1[0].toAndroidColor()
            R.attr.colorPrimaryContainer -> colorScheme.accent1[2].toAndroidColor()
            R.attr.colorOnPrimaryContainer -> colorScheme.accent1[10].toAndroidColor()

            R.attr.colorSecondary -> colorScheme.accent2[6].toAndroidColor()
            R.attr.colorOnSecondary -> colorScheme.accent2[0].toAndroidColor()
            R.attr.colorSecondaryContainer -> colorScheme.accent2[2].toAndroidColor()
            R.attr.colorOnSecondaryContainer -> colorScheme.accent2[10].toAndroidColor()

            R.attr.colorTertiary -> colorScheme.accent3[6].toAndroidColor()
            R.attr.colorOnTertiary -> colorScheme.accent3[0].toAndroidColor()
            R.attr.colorTertiaryContainer -> colorScheme.accent3[2].toAndroidColor()
            R.attr.colorOnTertiaryContainer -> colorScheme.accent3[10].toAndroidColor()

            R.attr.colorBackground -> colorScheme.neutral1[1].toAndroidColor()
            R.attr.colorOnBackground -> colorScheme.neutral1[10].toAndroidColor()

            R.attr.colorSurface -> colorScheme.neutral1[1].toAndroidColor()
            R.attr.colorOnSurface -> colorScheme.neutral1[10].toAndroidColor()
            R.attr.colorSurfaceVariant -> colorScheme.neutral2[2].toAndroidColor()
            R.attr.colorOnSurfaceVariant -> colorScheme.neutral2[8].toAndroidColor()

            R.attr.colorOutline -> colorScheme.neutral2[6].toAndroidColor()

            // For now, return null for attributes we don't override
            else -> null
        }
    }
}
