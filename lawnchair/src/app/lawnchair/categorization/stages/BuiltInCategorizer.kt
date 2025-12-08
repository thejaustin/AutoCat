package app.lawnchair.categorization.stages

import app.lawnchair.data.apps.AppInfo
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.entities.AppTab

/**
 * First stage categorizer that uses Android's built-in app categories.
 *
 * Available on Android 8.0+ (API 26), this categorizer uses the system-provided
 * category from ApplicationInfo. Has highest confidence (0.95) since it comes
 * directly from the app's manifest.
 *
 * Categories supported:
 * - Games, Entertainment, Photography, Social, News
 * - Navigation, Productivity, and more
 */
class BuiltInCategorizer(
    private val categoryDao: TabDao,
) {

    /**
     * Attempts to categorize an app using its system category.
     *
     * @param appInfo App metadata including system category
     * @return true if app was categorized, false if no system category available
     */
    suspend fun categorize(appInfo: AppInfo): Boolean {
        val categoryName = AppMetadataProvider.getCategoryName(appInfo.category)
            ?: return false

        val appCategory = AppTab(
            packageName = appInfo.packageName,
            category = categoryName,
            confidence = CONFIDENCE_BUILT_IN,
            source = AppTab.SOURCE_BUILT_IN,
            isUserOverride = false,
        )

        categoryDao.insertAppCategory(appCategory)
        return true
    }

    /**
     * Categorizes multiple apps in batch.
     *
     * @param apps List of apps to categorize
     * @return Number of apps successfully categorized
     */
    suspend fun categorizeBatch(apps: List<AppInfo>): Int {
        var categorizedCount = 0

        apps.forEach { app ->
            if (categorize(app)) {
                categorizedCount++
            }
        }

        return categorizedCount
    }

    companion object {
        private const val CONFIDENCE_BUILT_IN = 0.95f
    }
}
