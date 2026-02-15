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

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.ui.graphics.toArgb
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

    private val isDark: Boolean
        get() =
            (ares.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

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

    @Suppress("CyclomaticComplexMethod")
    private fun getDynamicColorOrNull(id: Int): Int? {
        return if (isDark) getDarkColor(id) else getLightColor(id)
    }

    private fun getDarkColor(id: Int): Int? {
        return when (id) {
            // Primary
            R.attr.autoCatPrimary -> colorScheme.primary(80).toArgb()

            R.attr.autoCatPrimaryInverse -> colorScheme.primary(40).toArgb()

            R.attr.autoCatOnPrimary -> colorScheme.primary(20).toArgb()

            R.attr.autoCatPrimaryContainer -> colorScheme.primary(30).toArgb()

            R.attr.autoCatOnPrimaryContainer -> colorScheme.primary(90).toArgb()

            // Secondary
            R.attr.autoCatSecondary -> colorScheme.secondary(80).toArgb()

            R.attr.autoCatOnSecondary -> colorScheme.secondary(20).toArgb()

            R.attr.autoCatSecondaryContainer -> colorScheme.secondary(30).toArgb()

            R.attr.autoCatOnSecondaryContainer -> colorScheme.secondary(90).toArgb()

            // Tertiary
            R.attr.autoCatTertiary -> colorScheme.tertiary(80).toArgb()

            R.attr.autoCatOnTertiary -> colorScheme.tertiary(20).toArgb()

            R.attr.autoCatTertiaryContainer -> colorScheme.tertiary(30).toArgb()

            R.attr.autoCatOnTertiaryContainer -> colorScheme.tertiary(90).toArgb()

            // Background & Surface
            R.attr.autoCatBackground -> colorScheme.neutral(10).toArgb()

            R.attr.autoCatOnBackground -> colorScheme.neutral(90).toArgb()

            R.attr.autoCatSurface -> colorScheme.neutral(10).toArgb()

            R.attr.autoCatOnSurface -> colorScheme.neutral(90).toArgb()

            R.attr.autoCatSurfaceVariant -> colorScheme.neutralVariant(30).toArgb()

            R.attr.autoCatOnSurfaceVariant -> colorScheme.neutralVariant(80).toArgb()

            R.attr.autoCatSurfaceInverse -> colorScheme.neutral(90).toArgb()

            R.attr.autoCatOnSurfaceInverse -> colorScheme.neutral(20).toArgb()

            // Error (fixed M3 values, monet has no error palette)
            R.attr.autoCatError -> ERROR_DARK

            R.attr.autoCatOnError -> ON_ERROR_DARK

            R.attr.autoCatErrorContainer -> ERROR_CONTAINER_DARK

            R.attr.autoCatOnErrorContainer -> ON_ERROR_CONTAINER_DARK

            // Outline
            R.attr.autoCatOutline -> colorScheme.neutralVariant(60).toArgb()

            R.attr.autoCatOutlineVariant -> colorScheme.neutralVariant(30).toArgb()

            else -> null
        }
    }

    private fun getLightColor(id: Int): Int? {
        return when (id) {
            // Primary
            R.attr.autoCatPrimary -> colorScheme.primary(40).toArgb()

            R.attr.autoCatPrimaryInverse -> colorScheme.primary(80).toArgb()

            R.attr.autoCatOnPrimary -> colorScheme.primary(100).toArgb()

            R.attr.autoCatPrimaryContainer -> colorScheme.primary(90).toArgb()

            R.attr.autoCatOnPrimaryContainer -> colorScheme.primary(10).toArgb()

            // Secondary
            R.attr.autoCatSecondary -> colorScheme.secondary(40).toArgb()

            R.attr.autoCatOnSecondary -> colorScheme.secondary(100).toArgb()

            R.attr.autoCatSecondaryContainer -> colorScheme.secondary(90).toArgb()

            R.attr.autoCatOnSecondaryContainer -> colorScheme.secondary(10).toArgb()

            // Tertiary
            R.attr.autoCatTertiary -> colorScheme.tertiary(40).toArgb()

            R.attr.autoCatOnTertiary -> colorScheme.tertiary(100).toArgb()

            R.attr.autoCatTertiaryContainer -> colorScheme.tertiary(90).toArgb()

            R.attr.autoCatOnTertiaryContainer -> colorScheme.tertiary(10).toArgb()

            // Background & Surface
            R.attr.autoCatBackground -> colorScheme.neutral(99).toArgb()

            R.attr.autoCatOnBackground -> colorScheme.neutralVariant(10).toArgb()

            R.attr.autoCatSurface -> colorScheme.neutral(99).toArgb()

            R.attr.autoCatOnSurface -> colorScheme.neutralVariant(10).toArgb()

            R.attr.autoCatSurfaceVariant -> colorScheme.neutralVariant(90).toArgb()

            R.attr.autoCatOnSurfaceVariant -> colorScheme.neutralVariant(30).toArgb()

            R.attr.autoCatSurfaceInverse -> colorScheme.neutral(20).toArgb()

            R.attr.autoCatOnSurfaceInverse -> colorScheme.neutral(95).toArgb()

            // Error (fixed M3 values)
            R.attr.autoCatError -> ERROR_LIGHT

            R.attr.autoCatOnError -> ON_ERROR_LIGHT

            R.attr.autoCatErrorContainer -> ERROR_CONTAINER_LIGHT

            R.attr.autoCatOnErrorContainer -> ON_ERROR_CONTAINER_LIGHT

            // Outline
            R.attr.autoCatOutline -> colorScheme.neutralVariant(50).toArgb()

            R.attr.autoCatOutlineVariant -> colorScheme.neutralVariant(80).toArgb()

            else -> null
        }
    }

    companion object {
        // M3 baseline error colors (not derived from monet)
        private const val ERROR_DARK = 0xFFFFB4AB.toInt()
        private const val ON_ERROR_DARK = 0xFF690005.toInt()
        private const val ERROR_CONTAINER_DARK = 0xFF93000A.toInt()
        private const val ON_ERROR_CONTAINER_DARK = 0xFFFFDAD6.toInt()
        private const val ERROR_LIGHT = 0xFFB3261E.toInt()
        private const val ON_ERROR_LIGHT = 0xFFFFFFFF.toInt()
        private const val ERROR_CONTAINER_LIGHT = 0xFFF9DEDC.toInt()
        private const val ON_ERROR_CONTAINER_LIGHT = 0xFF410E0B.toInt()
    }
}
