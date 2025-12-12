package app.lawnchair.allapps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.categorization.CategoryTabsController
import app.lawnchair.data.folder.model.FolderOrderUtils
import app.lawnchair.data.folder.model.FolderViewModel
import app.lawnchair.flowerpot.Flowerpot
import app.lawnchair.launcher
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.InvariantDeviceProfile.OnIDPChangeListener
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.AlphabeticalAppsList
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem
import com.android.launcher3.allapps.PrivateProfileManager
import com.android.launcher3.allapps.WorkProfileManager
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.views.ActivityContext
import com.patrykmichalik.opto.core.onEach
import java.util.function.Predicate

@Suppress("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN")
class LawnchairAlphabeticalAppsList<T>(
    private val context: T,
    private val appsStore: AllAppsStore<T>,
    workProfileManager: WorkProfileManager?,
    privateProfileManager: PrivateProfileManager?,
) : AlphabeticalAppsList<T>(context, appsStore, workProfileManager, privateProfileManager),
    OnIDPChangeListener
    where T : Context, T : ActivityContext {

    companion object {
        private const val TAG = "LawnchairAlphabeticalAppsList"
    }

    private var hiddenApps: Set<String> = setOf()
    private val prefs2 = PreferenceManager2.getInstance(context)
    private val prefs = PreferenceManager.getInstance(context)

    private val viewModel: FolderViewModel by (context as ComponentActivity).viewModels()
    private var folderList = mutableListOf<FolderInfo>()
    private val filteredList = mutableListOf<AppInfo>()

    private val folderOrder = FolderOrderUtils.stringToIntList(prefs.drawerListOrder.get())
    private val potsManager = Flowerpot.Manager.getInstance(context)
    private val autoCatProvider = AutoCatAppProvider.getInstance(context)
    private val categoryTabsController = CategoryTabsController.getInstance(context)
    private var cachedCategorizedApps: Map<String, Map<String, List<app.lawnchair.data.apps.AppInfo>>>? = null

    private fun app.lawnchair.data.apps.AppInfo.toLauncherAppInfo(): com.android.launcher3.model.data.AppInfo? {
        // Get proper LauncherActivityInfo from LauncherApps service
        val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)

        // Try to find the launcher activity for this package
        val userHandle = android.os.UserHandle.CURRENT
        val activities = launcherApps?.getActivityList(this.packageName, userHandle)

        if (activities.isNullOrEmpty()) {
            Log.w(TAG, "No launcher activities found for package: ${this.packageName}")
            return null
        }

        // Use the first launcher activity to create AppInfo properly with proper icon loading
        val launcherActivityInfo = activities[0]
        return com.android.launcher3.model.data.AppInfo(context, launcherActivityInfo, userHandle)
    }

    private fun com.android.launcher3.model.data.AppInfo.toAutoCatAppInfo(): app.lawnchair.data.apps.AppInfo {
        return app.lawnchair.data.apps.AppInfo(
            packageName = this.componentName?.packageName ?: "",
            label = this.title.toString(),
            category = null, // Can't easily get from Launcher3 AppInfo, use null
            installedTime = 0L, // Can't easily get from Launcher3 AppInfo, use default
            description = null, // Can't easily get from Launcher3 AppInfo, use null
        )
    }

    init {
        context.launcher.deviceProfile.inv.addOnChangeListener(this)
        try {
            prefs2.hiddenApps.onEach(launchIn = context.launcher.lifecycleScope) {
                hiddenApps = it
                onAppsUpdated()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize hidden apps", t)
        }
        observeFolders()
    }

    override fun onAppsUpdated() {
        super.onAppsUpdated()
        cachedCategorizedApps = autoCatProvider.categorizeApps(appsStore.apps.map { it.toAutoCatAppInfo() })
    }

    private fun observeFolders() {
        viewModel.foldersLiveData.observe(context as LifecycleOwner) { folders ->
            folderList = folders
                .sortedBy { folderOrder.indexOf(it.id) }
                .toMutableList()
            updateAdapterItems()
        }
    }

    override fun updateItemFilter(itemFilter: Predicate<ItemInfo>?) {
        mItemFilter = Predicate { info ->
            require(info is AppInfo) { "`info` must be an instance of `AppInfo`." }
            val componentKey = info.toComponentKey().toString()
            (itemFilter?.test(info) != false) && !hiddenApps.contains(componentKey)
        }
        onAppsUpdated()
    }

    override fun addAppsWithSections(appList: List<AppInfo?>?, startPosition: Int): Int {
        if (appList.isNullOrEmpty()) return startPosition

        filteredList.clear()
        var position = startPosition

        // Check if category tabs are enabled
        val usingAppTabs = categoryTabsController.shouldShowTabs(context)
        val currentTabName = if (usingAppTabs) {
            categoryTabsController.getTabNameForTab(categoryTabsController.getCurrentTab())
        } else {
            null
        }

        // When using app tabs, handle work apps differently
        val isWorkProfile = isWorkOrPrivateSpace(appList)
        if (isWorkProfile && usingAppTabs) {
            // Check if this is the Work tab
            if (currentTabName == CategoryTabsController.TAB_WORK) {
                // Show only work apps on Work tab
                return super.addAppsWithSections(appList, position)
            }

            // Check if work apps should be hidden
            if (prefs.hideWorkApps.get()) {
                return position // Skip work apps
            }
            // Otherwise, fall through to mix work apps with personal apps
        } else if (isWorkProfile) {
            // Not using app tabs - use default work profile behavior
            return super.addAppsWithSections(appList, position)
        }

        // Use AutoCat database categorization for filtering
        val categorizedApps = cachedCategorizedApps ?: autoCatProvider.categorizeApps(appList.map { it?.toAutoCatAppInfo() })

        // Unified Folder System: Use persistent folders as primary source
        var folders = folderList.toList()

        // Fallback: If no folders exist yet and we are supposed to use tabs (e.g. first run), create temp ones
        if (folders.isEmpty() && (usingAppTabs || !prefs.drawerList.get())) {
            val tempFolders = mutableListOf<FolderInfo>()
            categorizedApps.forEach { (tabName, subCategories) ->
                val allAppsInTab = subCategories.values.flatten()
                if (allAppsInTab.size > 1) {
                    val folderInfo = FolderInfo().apply {
                        title = tabName
                        allAppsInTab.forEach { app -> app.toLauncherAppInfo()?.let { add(it) } }
                    }
                    tempFolders.add(folderInfo)
                }
            }
            folders = tempFolders
        }

        if (usingAppTabs && currentTabName != null) {
            // We are in a specific tab: Filter folders and apps

            // 1. Folders that belong to this tab
            val subCategories = categorizedApps[currentTabName]?.keys ?: emptySet()

            folders.forEach { folder ->
                val folderTitle = folder.title.toString()
                if (subCategories.contains(folderTitle)) {
                    // This folder is a subcategory (e.g. "Puzzle" in "Games")
                    // Create a copy for display
                    val displayFolder = FolderInfo().apply {
                        title = folder.title
                        icon = folder.icon
                        folder.getContents().forEach { add(it) }
                    }
                    mAdapterItems.add(AdapterItem.asFolder(displayFolder))

                    // Mark apps as shown
                    folder.getContents().forEach { item ->
                        if (item is AppInfo) filteredList.add(item)
                    }
                    position++
                } else if (folderTitle == currentTabName) {
                    // This folder IS the tab (e.g. "Games")
                    // "Explode" it: show its contents as apps
                    folder.getContents().forEach { item ->
                        if (item is AppInfo) {
                            mAdapterItems.add(AdapterItem.asApp(item))
                            filteredList.add(item)
                            position++
                        }
                    }
                }
            }

            // 2. Apps in this tab that were not in matched folders
            val allAppsInTab = categorizedApps[currentTabName]?.values?.flatten() ?: emptyList()
            allAppsInTab.forEach { app ->
                val launcherApp = app.toLauncherAppInfo()
                // Check if we haven't shown this app yet
                // Note: filteredList contains apps shown via folders.
                // We also need to check if we already added it via "Explode" above.
                if (launcherApp != null && !filteredList.contains(launcherApp)) {
                    mAdapterItems.add(AdapterItem.asApp(launcherApp))
                    position++
                }
            }
        } else {
            // All Apps Mode (Unified List)
            folders.forEach { folder ->
                if (folder.getContents().size > 1) {
                    val folderInfo = FolderInfo().apply {
                        title = folder.title
                        icon = folder.icon
                        folder.getContents().forEach { add(it) }
                    }
                    mAdapterItems.add(AdapterItem.asFolder(folderInfo))

                    folder.getContents().forEach { app ->
                        if (app is AppInfo) {
                            // If prefs.folderApps.get() is true (Hide apps in folders), add to filteredList
                            // Wait, if prefs.folderApps.get() is true (Show apps in folders?), logic was ambiguous.
                            // Default behavior: Apps in folders are NOT shown in list.
                            // If we want to hide them, we add to filteredList.
                            // Let's assume !prefs.folderApps.get() means "Hide apps".
                            // Checking previous code: "filterNot { ... && prefs.folderApps.get() }"
                            // If prefs.folderApps.get() is TRUE, filterNot removes it.
                            // So folderApps=TRUE means HIDE.
                            if (prefs.folderApps.get()) filteredList.add(app)
                        }
                    }
                    position++
                }
            }

            // Add remaining apps
            val remainingApps = appList.filterNot { app -> filteredList.contains(app) }
            position = super.addAppsWithSections(remainingApps as List<com.android.launcher3.model.data.AppInfo?>, position)
        }

        return position
    }

    override fun onIdpChanged(modelPropertiesChanged: Boolean) {
        onAppsUpdated()
    }
}
