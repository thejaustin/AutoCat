package app.lawnchair.ui.preferences

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.lawnchair.ui.preferences.navigation.*

/**
 * Represents a settings category that can be reordered and hidden
 */
data class SettingsCategory(
    val id: String,
    val labelResId: Int,
    val descriptionResId: Int,
    val iconResId: Int,
    val route: PreferenceRootRoute,
    val order: Int,
    val isVisible: Boolean = true,
    val isConditional: Boolean = false, // For items like Quickstep that may not always show
)

/**
 * Default settings categories in their original order
 */
object DefaultSettingsCategories {
    fun getDefaults(): List<SettingsCategory> = listOf(
        SettingsCategory(
            id = "general",
            labelResId = com.android.launcher3.R.string.general_label,
            descriptionResId = com.android.launcher3.R.string.general_description,
            iconResId = com.android.launcher3.R.drawable.ic_general,
            route = General,
            order = 0
        ),
        SettingsCategory(
            id = "home_screen",
            labelResId = com.android.launcher3.R.string.home_screen_label,
            descriptionResId = com.android.launcher3.R.string.home_screen_description,
            iconResId = com.android.launcher3.R.drawable.ic_home_screen,
            route = HomeScreen,
            order = 1
        ),
        SettingsCategory(
            id = "smartspace",
            labelResId = com.android.launcher3.R.string.smartspace_widget,
            descriptionResId = com.android.launcher3.R.string.smartspace_widget_description,
            iconResId = com.android.launcher3.R.drawable.ic_smartspace,
            route = Smartspace,
            order = 2
        ),
        SettingsCategory(
            id = "dock",
            labelResId = com.android.launcher3.R.string.dock_label,
            descriptionResId = com.android.launcher3.R.string.dock_description,
            iconResId = com.android.launcher3.R.drawable.ic_dock,
            route = Dock,
            order = 3
        ),
        SettingsCategory(
            id = "app_drawer",
            labelResId = com.android.launcher3.R.string.app_drawer_label,
            descriptionResId = com.android.launcher3.R.string.app_drawer_description,
            iconResId = com.android.launcher3.R.drawable.ic_app_drawer,
            route = AppDrawer,
            order = 4,
            isConditional = true
        ),
        SettingsCategory(
            id = "search",
            labelResId = com.android.launcher3.R.string.search_bar_label,
            descriptionResId = com.android.launcher3.R.string.drawer_search_description,
            iconResId = com.android.launcher3.R.drawable.ic_search,
            route = Search(),
            order = 5
        ),
        SettingsCategory(
            id = "folders",
            labelResId = com.android.launcher3.R.string.folders_label,
            descriptionResId = com.android.launcher3.R.string.folders_description,
            iconResId = com.android.launcher3.R.drawable.ic_folder,
            route = Folders,
            order = 6
        ),
        SettingsCategory(
            id = "gestures",
            labelResId = com.android.launcher3.R.string.gestures_label,
            descriptionResId = com.android.launcher3.R.string.gestures_description,
            iconResId = com.android.launcher3.R.drawable.ic_gestures,
            route = Gestures,
            order = 7
        ),
        SettingsCategory(
            id = "quickstep",
            labelResId = com.android.launcher3.R.string.quickstep_label,
            descriptionResId = com.android.launcher3.R.string.quickstep_description,
            iconResId = com.android.launcher3.R.drawable.ic_quickstep,
            route = Quickstep,
            order = 8,
            isConditional = true
        ),
        SettingsCategory(
            id = "about",
            labelResId = com.android.launcher3.R.string.about_label,
            descriptionResId = com.android.launcher3.R.string.about_label, // Will be overridden with version
            iconResId = com.android.launcher3.R.drawable.ic_about,
            route = About,
            order = 9
        )
    )
}
