package app.lawnchair.data.tab.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-defined custom tab for organizing apps.
 *
 * Users can create custom tabs beyond the default ones, assign custom
 * colors for visual distinction, and control visibility and ordering.
 *
 * @property id Auto-generated unique identifier
 * @property name The display name of the tab
 * @property colorHex The color in hex format (e.g., "#FF5722")
 * @property sortOrder Display order in the app drawer (lower = earlier)
 * @property isVisible Whether this tab should be shown in the drawer
 * @property createdAt Timestamp of tab creation (milliseconds since epoch)
 */
@Entity(tableName = "custom_categories")
data class CustomTab(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,

    @ColumnInfo(name = "is_visible")
    val isVisible: Boolean = true,

    @ColumnInfo(name = "icon")
    val icon: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "hide_in_zen_mode")
    val hideInZenMode: Boolean = false,
) {
    companion object {
        // Default tab colors
        const val COLOR_GAMES = "#4CAF50"
        const val COLOR_SOCIAL = "#2196F3"
        const val COLOR_PRODUCTIVITY = "#FF9800"
        const val COLOR_TOOLS = "#9E9E9E"
        const val COLOR_ENTERTAINMENT = "#E91E63"
        const val COLOR_PHOTOGRAPHY = "#00BCD4"
        const val COLOR_COMMUNICATION = "#3F51B5"

        /**
         * Creates default tabs with standard colors and ordering.
         */
        fun getDefaultTabs(): List<CustomTab> = listOf(
            CustomTab(name = "Games", colorHex = COLOR_GAMES, sortOrder = 0),
            CustomTab(name = "Social", colorHex = COLOR_SOCIAL, sortOrder = 1),
            CustomTab(name = "Productivity", colorHex = COLOR_PRODUCTIVITY, sortOrder = 2),
            CustomTab(name = "Tools", colorHex = COLOR_TOOLS, sortOrder = 3),
            CustomTab(name = "Entertainment", colorHex = COLOR_ENTERTAINMENT, sortOrder = 4),
            CustomTab(name = "Photography", colorHex = COLOR_PHOTOGRAPHY, sortOrder = 5),
            CustomTab(name = "Communication", colorHex = COLOR_COMMUNICATION, sortOrder = 6),
        )
    }
}
