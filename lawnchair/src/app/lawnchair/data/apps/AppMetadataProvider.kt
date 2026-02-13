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
    private val packageManager = context.packageManager

    fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            // Only include apps with launch intents (user apps)
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                apps.add(createAppInfo(app))
            }
        }
        return apps
    }

    fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val app = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            createAppInfo(app)
        } catch (e: Exception) {
            null
        }
    }

    private fun createAppInfo(app: android.content.pm.ApplicationInfo): AppInfo {
        val packageInfo = try {
            packageManager.getPackageInfo(app.packageName, 0)
        } catch (e: Exception) {
            null
        }

        return AppInfo(
            packageName = app.packageName,
            label = packageManager.getApplicationLabel(app).toString(),
            category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                app.category.takeIf { it != android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED }
            } else {
                null
            },
            installedTime = packageInfo?.firstInstallTime ?: 0L,
            description = null, // Description can be fetched from Play Store in the future
        )
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
