package app.lawnchair.allapps

import android.content.Context
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

// ... other imports ...

// ... other imports ...

    private fun app.lawnchair.data.apps.AppInfo.toLauncherAppInfo(): com.android.launcher3.model.data.AppInfo {
        val launcherActivityInfo = context.packageManager.getLaunchIntentForPackage(this.packageName)
        val componentName = launcherActivityInfo?.component ?: ComponentName(this.packageName, "com.android.fallback.FallbackActivity") // Fallback
        val intent = launcherActivityInfo?.let { Intent(Intent.ACTION_MAIN).setComponent(it.component) } ?: Intent() // Fallback intent
        return com.android.launcher3.model.data.AppInfo(
            componentName,
            this.label as CharSequence,
            android.os.UserHandle.CURRENT,
            intent,
        )
    }

    private fun com.android.launcher3.model.data.AppInfo.toAutoCatAppInfo(): app.lawnchair.data.apps.AppInfo {
        return app.lawnchair.data.apps.AppInfo(
            packageName = this.componentName.packageName,
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
        cachedCategorizedApps = autoCatProvider.categorizeApps(appsStore.apps.toList())
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
        val drawerListDefault = prefs.drawerList.get()
        filteredList.clear()
        var position = startPosition

        // Check if category tabs are enabled
        val usingCategoryTabs = categoryTabsController.shouldShowTabs(context)
        val currentTabCategory = if (usingCategoryTabs) {
            categoryTabsController.getCategoryForTab(categoryTabsController.getCurrentTab())
        } else {
            null
        }

        // When using category tabs, handle work apps differently
        val isWorkProfile = isWorkOrPrivateSpace(appList)
        if (isWorkProfile && usingCategoryTabs) {
            // Check if this is the Work tab
            if (currentTabCategory == CategoryTabsController.TAB_WORK) {
                // Show only work apps on Work tab
                return super.addAppsWithSections(appList, position)
            }

            // Check if work apps should be hidden
            if (prefs.hideWorkApps.get()) {
                return position // Skip work apps
            }
            // Otherwise, fall through to mix work apps with personal apps
        } else if (isWorkProfile) {
            // Not using category tabs - use default work profile behavior
            return super.addAppsWithSections(appList, position)
        }

        if (!drawerListDefault) {
            // Use AutoCat database categorization
            val categorizedApps = cachedCategorizedApps ?: autoCatProvider.categorizeApps(appList.map { it?.toAutoCatAppInfo() })

            // If using tabs, filter to only show current tab's category
            val appsToShow = if (usingCategoryTabs && currentTabCategory != null) {
                // Show only apps from the current category tab
                categorizedApps.filter { it.key == currentTabCategory }
            } else {
                // Show all categories (either tabs disabled or "All Apps" tab selected)
                categorizedApps
            }

            appsToShow.forEach { (category, subCategories) ->
                if (usingCategoryTabs) {
                    // 1. Folders (Subcategories) first
                    subCategories.filterKeys { it.isNotEmpty() }.forEach { (subCategory, apps) ->
                        val folderInfo = FolderInfo()
                        folderInfo.title = subCategory
                        val iconPath = autoCatProvider.getSubCategoryIcon(category, subCategory)
                        if (iconPath != null) {
                            folderInfo.icon = iconPath
                        }
                        apps.forEach { app -> folderInfo.add(app.toLauncherAppInfo()) }
                        mAdapterItems.add(AdapterItem.asFolder(folderInfo))
                        position++
                    }

                    // 2. Apps (No subcategory) second
                    subCategories[""]?.forEach { app ->
                        mAdapterItems.add(AdapterItem.asApp(app.toLauncherAppInfo()))
                        position++
                    }
                } else {
                    // In folder mode, group all apps in this category into one folder (flatten subcategories)
                    val allAppsInCategory = subCategories.values.flatten()

                    if (allAppsInCategory.size == 1) {
                        mAdapterItems.add(AdapterItem.asApp(allAppsInCategory.first().toLauncherAppInfo()))
                    } else {
                        val folderInfo = FolderInfo().apply {
                            title = category
                            allAppsInCategory.forEach { add(it.toLauncherAppInfo()) }
                        }
                        mAdapterItems.add(AdapterItem.asFolder(folderInfo))
                    }
                    position++
                }
            }
        } else {
            folderList.forEach { folder ->
                if (folder.getContents().size > 1) {
                    val folderInfo = FolderInfo()
                    folderInfo.title = folder.title
                    mAdapterItems.add(AdapterItem.asFolder(folderInfo))
                    folder.getContents().forEach { app ->
                        (appsStore.getApp(app.componentKey) as? AppInfo)?.let {
                            folderInfo.add(it)
                            if (prefs.folderApps.get()) filteredList.add(it)
                        }
                    }
                }
                position++
            }
            val remainingApps = appList.filterNot { app -> filteredList.contains(app) && prefs.folderApps.get() }
            position = super.addAppsWithSections(remainingApps as List<com.android.launcher3.model.data.AppInfo?>, position)
        }

        return position
    }

    override fun onIdpChanged(modelPropertiesChanged: Boolean) {
        onAppsUpdated()
    }
}
