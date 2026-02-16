package app.lawnchair.categorization

import android.content.Context
import android.widget.Toast
import app.lawnchair.appops.AppBatchOperationService
import app.lawnchair.ui.util.bottomSheetHandler
import com.android.launcher3.LauncherAppState
import com.android.launcher3.model.data.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles batch actions on tabs/categories.
 */
class TabActionController(private val context: Context) {

    private val batchService = AppBatchOperationService(context)
    private val categorizationManager = CategorizationManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Archives all apps in the given tab.
     *
     * @param tabName The name of the tab, or null for "Other"
     */
    fun archiveCategory(tabName: String?) {
        scope.launch {
            val packages = categorizationManager.getPackagesInTab(tabName)
            if (packages.isEmpty()) {
                Toast.makeText(context, "No apps found in this category", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(context, "Archiving ${packages.size} apps...", Toast.LENGTH_SHORT).show()

            withContext(Dispatchers.IO) {
                batchService.archiveApps(packages) { progress ->
                    // Optional: Show progress in UI
                }
            }

            Toast.makeText(context, "Category archived", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Restores (unarchives/enables) all apps in the given tab.
     */
    fun restoreCategory(tabName: String?) {
        scope.launch {
            // To restore, we need to know which apps WERE in this category.
            // Our getPackagesInTab(tabName) should work if they are still in the DB.
            val packages = categorizationManager.getPackagesInTab(tabName)

            if (packages.isEmpty()) {
                Toast.makeText(context, "No apps found to restore", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(context, "Restoring ${packages.size} apps...", Toast.LENGTH_SHORT).show()

            withContext(Dispatchers.IO) {
                // Try to enable them first (for disabled apps)
                batchService.enableApps(packages) { }
                // Then try to unarchive (for Android 15 archived apps)
                batchService.unarchiveApps(packages) { }
            }

            Toast.makeText(context, "Category restoration started", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun getInstance(context: Context) = TabActionController(context)
    }
}
