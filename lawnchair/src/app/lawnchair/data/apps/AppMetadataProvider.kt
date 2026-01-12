package app.lawnchair.data.apps

import android.content.Context
import android.content.pm.PackageManager

/**
 * Represents app metadata for AutoCat categorization.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val category: Int? = null,
    val installedTime: Long = 0L,
    val description: String? = null,
)

/**
 * Simple metadata provider stub.
 * Returns empty list since app info is handled by launcher's AppInfo model.
 */
class AppMetadataProvider(private val context: Context) {
    fun getInstalledApps(): List<AppInfo> {
        return emptyList()
    }

    fun getAppInfo(packageName: String): AppInfo? {
        return null
    }

    companion object {
        /**
         * Maps Android system categories to AutoCat tab names.
         */
        fun getCategoryName(category: Int?): String? {
            return when (category) {
                android.content.pm.ApplicationInfo.CATEGORY_GAME -> "Games"
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> "Photography"
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                android.content.pm.ApplicationInfo.CATEGORY_NEWS -> "News"
                android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Navigation"
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> null
            }
        }
    }
}
