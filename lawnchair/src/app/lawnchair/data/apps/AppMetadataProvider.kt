package app.lawnchair.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Provides app metadata from PackageManager with a clean interface.
 *
 * Abstracts PackageManager complexity and provides app information
 * needed for categorization. Handles API level differences.
 */
class AppMetadataProvider(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Gets information for all installed user apps (excludes system apps).
     *
     * @return List of user-installed apps with metadata
     */
    fun getInstalledApps(): List<AppInfo> {
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        return packages
            .filter { isUserApp(it) }
            .mapNotNull { appInfo ->
                try {
                    AppInfo(
                        packageName = appInfo.packageName,
                        label = appInfo.loadLabel(packageManager).toString(),
                        category = getCategoryCompat(appInfo),
                        installedTime = getInstallTimeCompat(appInfo.packageName),
                        description = getAppDescription(appInfo.packageName),
                    )
                } catch (e: Exception) {
                    // Skip apps we can't read metadata for
                    null
                }
            }
    }

    /**
     * Gets metadata for a specific app by package name.
     *
     * @param packageName The package to query
     * @return AppInfo if found, null otherwise
     */
    fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

            AppInfo(
                packageName = appInfo.packageName,
                label = appInfo.loadLabel(packageManager).toString(),
                category = getCategoryCompat(appInfo),
                installedTime = getInstallTimeCompat(packageName),
                description = getAppDescription(packageName),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Determines if an app is a user-installed app (not system app).
     */
    private fun isUserApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    /**
     * Gets the app category with API level compatibility.
     *
     * Returns null for API < 26 or if category is not set.
     */
    private fun getCategoryCompat(appInfo: ApplicationInfo): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val category = appInfo.category
            if (category == ApplicationInfo.CATEGORY_UNDEFINED) null else category
        } else {
            null
        }
    }

    /**
     * Gets the app installation time.
     */
    private fun getInstallTimeCompat(packageName: String): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Extracts app description from PackageManager metadata.
     *
     * Tries to get the description from:
     * 1. Application description (if available)
     * 2. Package summary text
     *
     * @param packageName The package to query
     * @return Description string if available, null otherwise
     */
    private fun getAppDescription(packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

            // Try to load description from CharSequence
            val description = appInfo.loadDescription(packageManager)
            if (description != null && description.isNotEmpty()) {
                return description.toString().trim()
            }

            // Fallback: return null if no description available
            null
        } catch (e: Exception) {
            // Description not available or error reading
            null
        }
    }

    companion object {
        /**
         * Maps Android system categories to AutoCat tab names.
         */
        fun getCategoryName(category: Int?): String? {
            return when (category) {
                ApplicationInfo.CATEGORY_GAME -> "Games"
                ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
                ApplicationInfo.CATEGORY_VIDEO -> "Entertainment"
                ApplicationInfo.CATEGORY_IMAGE -> "Photography"
                ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                ApplicationInfo.CATEGORY_NEWS -> "News"
                ApplicationInfo.CATEGORY_MAPS -> "Navigation"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else -> null
            }
        }
    }
}
