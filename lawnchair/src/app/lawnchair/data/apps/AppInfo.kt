package app.lawnchair.data.apps

/**
 * Lightweight data class representing installed app metadata.
 *
 * Used by categorization system to analyze and categorize apps
 * without direct PackageManager dependencies.
 *
 * @property packageName The unique package identifier
 * @property label User-visible app name
 * @property category Android system category (API 26+), null if not set
 * @property installedTime Installation timestamp (milliseconds since epoch)
 * @property description App description from metadata (null if not available)
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val category: Int?,
    val installedTime: Long,
    val description: String? = null,
)
