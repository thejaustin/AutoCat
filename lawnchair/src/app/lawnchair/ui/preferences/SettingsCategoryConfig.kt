package app.lawnchair.ui.preferences

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.lawnchair.ui.preferences.navigation.About
import app.lawnchair.ui.preferences.navigation.AppDrawer
import app.lawnchair.ui.preferences.navigation.AppDrawerCategorizationSettings
import app.lawnchair.ui.preferences.navigation.BackupAndRestore
import app.lawnchair.ui.preferences.navigation.Developer
import app.lawnchair.ui.preferences.navigation.Dock
import app.lawnchair.ui.preferences.navigation.Folders
import app.lawnchair.ui.preferences.navigation.General
import app.lawnchair.ui.preferences.navigation.Gestures
import app.lawnchair.ui.preferences.navigation.HomeScreen
import app.lawnchair.ui.preferences.navigation.PreferenceRootRoute
import app.lawnchair.ui.preferences.navigation.Quickstep
import app.lawnchair.ui.preferences.navigation.Search
import app.lawnchair.ui.preferences.navigation.Smartspace

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
            id = "autocat",
            labelResId = com.android.launcher3.R.string.smart_categories_label,
            descriptionResId = com.android.launcher3.R.string.smart_categories_subtitle,
            iconResId = com.android.launcher3.R.drawable.ic_autocat,
            route = AppDrawerCategorizationSettings,
            order = 0,
        ),
        SettingsCategory(
            id = "general",
            labelResId = com.android.launcher3.R.string.general_label,
            descriptionResId = com.android.launcher3.R.string.general_description,
            iconResId = com.android.launcher3.R.drawable.ic_general,
            route = General,
            order = 1,
        ),
        SettingsCategory(
            id = "home_screen",
            labelResId = com.android.launcher3.R.string.home_screen_label,
            descriptionResId = com.android.launcher3.R.string.home_screen_description,
            iconResId = com.android.launcher3.R.drawable.ic_home_screen,
            route = HomeScreen,
            order = 2,
        ),
        SettingsCategory(
            id = "smartspace",
            labelResId = com.android.launcher3.R.string.smartspace_widget,
            descriptionResId = com.android.launcher3.R.string.smartspace_widget_description,
            iconResId = com.android.launcher3.R.drawable.ic_smartspace,
            route = Smartspace,
            order = 3,
        ),
        SettingsCategory(
            id = "dock",
            labelResId = com.android.launcher3.R.string.dock_label,
            descriptionResId = com.android.launcher3.R.string.dock_description,
            iconResId = com.android.launcher3.R.drawable.ic_dock,
            route = Dock,
            order = 4,
        ),
        SettingsCategory(
            id = "app_drawer",
            labelResId = com.android.launcher3.R.string.app_drawer_label,
            descriptionResId = com.android.launcher3.R.string.app_drawer_description,
            iconResId = com.android.launcher3.R.drawable.ic_app_drawer,
            route = AppDrawer,
            order = 5,
            isConditional = true,
        ),
        SettingsCategory(
            id = "search",
            labelResId = com.android.launcher3.R.string.search_bar_label,
            descriptionResId = com.android.launcher3.R.string.drawer_search_description,
            iconResId = com.android.launcher3.R.drawable.ic_search,
            route = Search(),
            order = 6,
        ),
        SettingsCategory(
            id = "folders",
            labelResId = com.android.launcher3.R.string.folders_label,
            descriptionResId = com.android.launcher3.R.string.folders_description,
            iconResId = com.android.launcher3.R.drawable.ic_folder,
            route = Folders,
            order = 7,
        ),
        SettingsCategory(
            id = "gestures",
            labelResId = com.android.launcher3.R.string.gestures_label,
            descriptionResId = com.android.launcher3.R.string.gestures_description,
            iconResId = com.android.launcher3.R.drawable.ic_gestures,
            route = Gestures,
            order = 8,
        ),
        SettingsCategory(
            id = "quickstep",
            labelResId = com.android.launcher3.R.string.quickstep_label,
            descriptionResId = com.android.launcher3.R.string.quickstep_description,
            iconResId = com.android.launcher3.R.drawable.ic_quickstep,
            route = Quickstep,
            order = 9,
            isConditional = true,
        ),
        SettingsCategory(
            id = "backup",
            labelResId = com.android.launcher3.R.string.create_backup,
            descriptionResId = com.android.launcher3.R.string.restore_backup,
            iconResId = com.android.launcher3.R.drawable.ic_upload,
            route = BackupAndRestore,
            order = 10,
        ),
        SettingsCategory(
            id = "developer",
            labelResId = com.android.launcher3.R.string.debug_label,
            descriptionResId = com.android.launcher3.R.string.experimental_features_label,
            iconResId = com.android.launcher3.R.drawable.ic_warning,
            route = Developer,
            order = 11,
        ),
        SettingsCategory(
            id = "about",
            labelResId = com.android.launcher3.R.string.about_label,
            descriptionResId = com.android.launcher3.R.string.about_label, // Will be overridden with version
            iconResId = com.android.launcher3.R.drawable.ic_about,
            route = About,
            order = 12,
        ),
    )
}
