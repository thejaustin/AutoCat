package app.lawnchair.ui.preferences.components.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Abc
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shape
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Centralized icon mapping for preference settings.
 * Provides consistent, expressive iconography throughout the app.
 */
object PreferenceIcons {

    // General Settings Icons
    val Rotation = Icons.AutoMirrored.Rounded.RotateRight
    val Theme = Icons.Rounded.Palette
    val DarkMode = Icons.Rounded.DarkMode
    val LightMode = Icons.Rounded.LightMode
    val Font = Icons.Rounded.FontDownload
    val TextSize = Icons.Rounded.FormatSize
    val AppIcons = Icons.Rounded.Apps
    val IconShape = Shape
    val Style = Icons.Rounded.Style
    val Color = Icons.Rounded.ColorLens
    val Wallpaper = Icons.Rounded.Wallpaper

    // Layout Icons
    val Grid = Icons.Rounded.GridView
    val Columns = Icons.Rounded.ViewColumn
    val Rows = Icons.Rounded.ViewDay
    val Spacing = Icons.Rounded.Dashboard
    val Opacity = Icons.Rounded.Opacity

    // Organization Icons
    val Folder = Icons.Rounded.Folder
    val Category = Icons.Rounded.Category
    val Sort = Icons.Rounded.Sort
    val Filter = Icons.Rounded.Tune
    val Search = Icons.Rounded.Search

    // Feature Icons
    val Widget = Icons.Rounded.Widgets
    val Notification = Icons.Rounded.Notifications
    val AI = Icons.Rounded.SmartToy
    val Brush = Icons.Rounded.Brush

    // Action Icons
    val Save = Icons.Rounded.Save
    val Delete = Icons.Rounded.Delete
    val Refresh = Icons.Rounded.Refresh
    val Swap = Icons.Rounded.SwapHoriz

    // State Icons
    val Visible = Icons.Rounded.Visibility
    val Hidden = Icons.Rounded.VisibilityOff
    val Success = Icons.Rounded.CheckCircle
    val Error = Icons.Rounded.Error
    val Info = Icons.Rounded.Info

    // Misc Icons
    val Settings = Icons.Rounded.Settings
    val Text = Icons.Rounded.TextFields
    val Letters = Icons.Rounded.Abc
}

/**
 * Icon categories for semantic grouping.
 */
enum class IconCategory {
    APPEARANCE,
    LAYOUT,
    ORGANIZATION,
    FEATURES,
    ACTIONS,
    STATE,
}

/**
 * Maps preference types to their semantic icons.
 */
object PreferenceIconMapping {

    /**
     * Gets an appropriate icon for a preference based on keywords.
     */
    fun getIconForPreference(
        label: String,
        keywords: List<String> = emptyList(),
    ): ImageVector? {
        val allTerms = (listOf(label) + keywords).map { it.lowercase() }

        return when {
            // Theme & Appearance
            allTerms.any { it.contains("theme") || it.contains("dark") || it.contains("light") } ->
                PreferenceIcons.Theme

            allTerms.any { it.contains("color") || it.contains("palette") } ->
                PreferenceIcons.Color

            allTerms.any { it.contains("icon") && it.contains("shape") } ->
                PreferenceIcons.IconShape

            allTerms.any { it.contains("icon") } ->
                PreferenceIcons.AppIcons

            allTerms.any { it.contains("font") || it.contains("typeface") } ->
                PreferenceIcons.Font

            allTerms.any { it.contains("opacity") || it.contains("transparency") } ->
                PreferenceIcons.Opacity

            allTerms.any { it.contains("wallpaper") } ->
                PreferenceIcons.Wallpaper

            // Layout
            allTerms.any { it.contains("grid") } ->
                PreferenceIcons.Grid

            allTerms.any { it.contains("column") } ->
                PreferenceIcons.Columns

            allTerms.any { it.contains("row") } ->
                PreferenceIcons.Rows

            allTerms.any { it.contains("spacing") || it.contains("padding") } ->
                PreferenceIcons.Spacing

            allTerms.any { it.contains("rotation") || it.contains("rotate") } ->
                PreferenceIcons.Rotation

            // Organization
            allTerms.any { it.contains("folder") } ->
                PreferenceIcons.Folder

            allTerms.any { it.contains("category") || it.contains("categoriz") } ->
                PreferenceIcons.Category

            allTerms.any { it.contains("sort") } ->
                PreferenceIcons.Sort

            allTerms.any { it.contains("filter") } ->
                PreferenceIcons.Filter

            allTerms.any { it.contains("search") } ->
                PreferenceIcons.Search

            allTerms.any { it.contains("hidden") || it.contains("hide") } ->
                PreferenceIcons.Hidden

            allTerms.any { it.contains("visible") || it.contains("show") } ->
                PreferenceIcons.Visible

            // Features
            allTerms.any { it.contains("widget") || it.contains("smartspace") } ->
                PreferenceIcons.Widget

            allTerms.any { it.contains("notification") || it.contains("badge") } ->
                PreferenceIcons.Notification

            allTerms.any { it.contains("ai") || it.contains("llm") || it.contains("smart") } ->
                PreferenceIcons.AI

            else -> null
        }
    }

    /**
     * Gets category for an icon.
     */
    fun getCategoryForIcon(icon: ImageVector): IconCategory {
        return when (icon) {
            PreferenceIcons.Theme, PreferenceIcons.Color, PreferenceIcons.AppIcons,
            PreferenceIcons.IconShape, PreferenceIcons.Font, PreferenceIcons.Style,
            PreferenceIcons.Wallpaper, PreferenceIcons.Opacity,
            -> IconCategory.APPEARANCE

            PreferenceIcons.Grid, PreferenceIcons.Columns, PreferenceIcons.Rows,
            PreferenceIcons.Spacing, PreferenceIcons.Rotation,
            -> IconCategory.LAYOUT

            PreferenceIcons.Folder, PreferenceIcons.Category, PreferenceIcons.Sort,
            PreferenceIcons.Filter, PreferenceIcons.Search,
            -> IconCategory.ORGANIZATION

            PreferenceIcons.Widget, PreferenceIcons.Notification, PreferenceIcons.AI,
            -> IconCategory.FEATURES

            PreferenceIcons.Save, PreferenceIcons.Delete, PreferenceIcons.Refresh,
            PreferenceIcons.Swap,
            -> IconCategory.ACTIONS

            PreferenceIcons.Visible, PreferenceIcons.Hidden, PreferenceIcons.Success,
            PreferenceIcons.Error, PreferenceIcons.Info,
            -> IconCategory.STATE

            else -> IconCategory.APPEARANCE
        }
    }
}
