package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.categorization.stages.BuiltInCategorizer
import app.lawnchair.data.apps.AppMetadataProvider
import app.lawnchair.data.category.CategoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the app categorization pipeline.
 *
 * Coordinates categorization stages, database initialization, and
 * provides a clean interface for triggering categorization.
 *
 * Currently implements Stage 1 (Built-in categorizer).
 * Future: Rule-based, ML, and user override stages.
 */
class CategorizationManager(private val context: Context) {

    private val database = CategoryDatabase.getInstance(context)
    private val categoryDao = database.categoryDao()
    private val metadataProvider = AppMetadataProvider(context)
    private val builtInCategorizer = BuiltInCategorizer(categoryDao)

    /**
     * Initializes categorization system on first run.
     *
     * - Ensures default categories exist
     * - Categorizes all installed apps using built-in categorizer
     *
     * Safe to call multiple times (idempotent).
     */
    suspend fun initializeCategorization() = withContext(Dispatchers.IO) {
        try {
            // Ensure default categories are initialized
            categoryDao.initializeDefaultCategoriesIfNeeded()

            // Get all installed apps
            val apps = metadataProvider.getInstalledApps()

            // Categorize using built-in categorizer (Stage 1)
            val categorized = builtInCategorizer.categorizeBatch(apps)

            android.util.Log.d(
                TAG,
                "Categorization complete: $categorized/${apps.size} apps categorized",
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during categorization", e)
        }
    }

    /**
     * Re-categorizes all apps (useful after installing/updating apps).
     *
     * Only re-categorizes apps that don't have user overrides.
     */
    suspend fun recategorizeAll() = withContext(Dispatchers.IO) {
        try {
            // Delete non-user-override categories
            categoryDao.deleteNonUserOverrides()

            // Re-run categorization
            initializeCategorization()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error during re-categorization", e)
        }
    }

    /**
     * Categorizes a single newly installed app.
     *
     * @param packageName The package name of the new app
     */
    suspend fun categorizeNewApp(packageName: String) = withContext(Dispatchers.IO) {
        try {
            val appInfo = metadataProvider.getAppInfo(packageName) ?: return@withContext

            // Try built-in categorizer
            val categorized = builtInCategorizer.categorize(appInfo)

            if (categorized) {
                android.util.Log.d(TAG, "Categorized new app: $packageName")
            }
            // Future: Try other categorization stages if built-in fails
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error categorizing app: $packageName", e)
        }
    }

    companion object {
        private const val TAG = "CategorizationManager"

        @Volatile
        private var instance: CategorizationManager? = null

        /**
         * Gets singleton instance of CategorizationManager.
         */
        fun getInstance(context: Context): CategorizationManager {
            return instance ?: synchronized(this) {
                instance ?: CategorizationManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
