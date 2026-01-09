package app.lawnchair.categorization

/**
 * Defines where category folders should be synced.
 */
enum class FolderSyncMode(val displayName: String, val description: String) {
    /** Sync to app drawer folders only (using caddy folder service) */
    DRAWER(
        "App drawer only",
        "Create folders in the app drawer (current default)",
    ),

    /** Sync to home screen workspace folders only (using Launcher3 workspace) */
    HOME_SCREEN(
        "Home screen only",
        "Create folders on the home screen workspace",
    ),

    /** Sync to both app drawer and home screen */
    BOTH(
        "Both app drawer and home screen",
        "Create folders in both locations",
    ),
    ;

    companion object {
        fun fromString(value: String): FolderSyncMode {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: DRAWER
        }
    }
}
