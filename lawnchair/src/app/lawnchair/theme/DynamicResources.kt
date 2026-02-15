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
    override fun getColor(id: Int, theme: Resources.Theme?): Int {
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
            // Material 3 Color Roles: Primary
            R.attr.autoCatPrimary -> colorScheme.primary.toAndroidColor()

            R.attr.autoCatPrimaryInverse -> colorScheme.onPrimary.toAndroidColor()

            // Assuming primaryInverse maps to onPrimary for now
            R.attr.autoCatOnPrimary -> colorScheme.onPrimary.toAndroidColor()

            R.attr.autoCatPrimaryContainer -> colorScheme.primaryContainer.toAndroidColor()

            R.attr.autoCatOnPrimaryContainer -> colorScheme.onPrimaryContainer.toAndroidColor()

            // Material 3 Color Roles: Secondary
            R.attr.autoCatSecondary -> colorScheme.secondary.toAndroidColor()

            R.attr.autoCatOnSecondary -> colorScheme.onSecondary.toAndroidColor()

            R.attr.autoCatSecondaryContainer -> colorScheme.secondaryContainer.toAndroidColor()

            R.attr.autoCatOnSecondaryContainer -> colorScheme.onSecondaryContainer.toAndroidColor()

            // Material 3 Color Roles: Tertiary
            R.attr.autoCatTertiary -> colorScheme.tertiary.toAndroidColor()

            R.attr.autoCatOnTertiary -> colorScheme.onTertiary.toAndroidColor()

            R.attr.autoCatTertiaryContainer -> colorScheme.tertiaryContainer.toAndroidColor()

            R.attr.autoCatOnTertiaryContainer -> colorScheme.onTertiaryContainer.toAndroidColor()

            // Material 3 Color Roles: Background & Surface
            R.attr.autoCatBackground -> colorScheme.background.toAndroidColor()

            R.attr.autoCatOnBackground -> colorScheme.onBackground.toAndroidColor()

            R.attr.autoCatSurface -> colorScheme.surface.toAndroidColor()

            R.attr.autoCatOnSurface -> colorScheme.onSurface.toAndroidColor()

            R.attr.autoCatSurfaceVariant -> colorScheme.surfaceVariant.toAndroidColor()

            R.attr.autoCatOnSurfaceVariant -> colorScheme.onSurfaceVariant.toAndroidColor()

            R.attr.autoCatSurfaceInverse -> colorScheme.surfaceInverse.toAndroidColor()

            R.attr.autoCatOnSurfaceInverse -> colorScheme.onSurfaceInverse.toAndroidColor()

            // Material 3 Color Roles: Error
            R.attr.autoCatError -> colorScheme.error.toAndroidColor()

            R.attr.autoCatOnError -> colorScheme.onError.toAndroidColor()

            R.attr.autoCatErrorContainer -> colorScheme.errorContainer.toAndroidColor()

            R.attr.autoCatOnErrorContainer -> colorScheme.onErrorContainer.toAndroidColor()

            // Material 3 Color Roles: Outline
            R.attr.autoCatOutline -> colorScheme.outline.toAndroidColor()

            R.attr.autoCatOutlineVariant -> colorScheme.outlineVariant.toAndroidColor()

            // For now, return null for attributes we don't override
            else -> null
        }
    }
}
