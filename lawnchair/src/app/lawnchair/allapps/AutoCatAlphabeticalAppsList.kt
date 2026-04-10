package app.lawnchair.allapps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import app.lawnchair.autoCatLauncher
import app.lawnchair.categorization.AppTabsController
import app.lawnchair.categorization.AutoCatAppProvider
import app.lawnchair.data.folder.model.FolderOrderUtils
import app.lawnchair.data.folder.service.FolderService
import app.lawnchair.flowerpot.Flowerpot
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Suppress("SYNTHETIC_PROPERTY_WITHOUT_JAVA_ORIGIN")
class AutoCatAlphabeticalAppsList<T>(
    private val context: T,
    private val appsStore: AllAppsStore<T>,
    workProfileManager: WorkProfileManager?,
    privateProfileManager: PrivateProfileManager?,
) : AlphabeticalAppsList<T>(context, appsStore, workProfileManager, privateProfileManager),
    OnIDPChangeListener
    where T : Context, T : ActivityContext {

    companion object {
        private const val TAG = "AutoCatAlphabeticalAppsList"
    }

    private var hiddenApps: Set<String> = setOf()
    private val prefs2 = PreferenceManager2.getInstance(context)
    private val prefs = PreferenceManager.getInstance(context)

    private val folderService = FolderService.INSTANCE.get(context)
    private var folderList = mutableListOf<FolderInfo>()
    private val filteredList = mutableListOf<AppInfo>()

    private val folderOrder = FolderOrderUtils.stringToIntList(prefs.drawerListOrder.get())
    private val potsManager = Flowerpot.Manager.getInstance(context)
    private val autoCatProvider = AutoCatAppProvider.getInstance(context)
    private val appTabsController = AppTabsController.getInstance(context)
    private var cachedCategorizedApps: Map<String, Map<String, List<app.lawnchair.data.apps.AppInfo>>>? = null
    private var currentTabFilter: String? = null

    fun setTabFilter(tabName: String?) {
        if (currentTabFilter != tabName) {
            currentTabFilter = tabName
            onAppsUpdated()
        }
    }

    private fun com.android.launcher3.model.data.AppInfo.toAutoCatAppInfo(): app.lawnchair.data.apps.AppInfo {
        return try {
            app.lawnchair.data.apps.AppInfo(
                packageName = this.componentName?.packageName ?: "",
                label = this.title?.toString() ?: "",
                category = null,
                installedTime = 0L,
                description = null,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting AppInfo to AutoCatAppInfo", e)
            app.lawnchair.data.apps.AppInfo(
                packageName = "",
                label = "",
                category = null,
                installedTime = 0L,
                description = null,
            )
        }
    }

    /**
     * Creates a FolderInfo for app drawer display with properly loaded icons from icon packs.
     */
    private fun loadFolderWithIcons(
        sourceFolder: FolderInfo,
        appMap: Map<String?, List<AppInfo>>,
    ): FolderInfo {
        return FolderInfo().apply {
            container = ItemInfo.NO_ID
            title = sourceFolder.title

            sourceFolder.getContents().forEach { item ->
                when (item) {
                    is AppInfo -> {
                        val matchingApps = appMap[item.componentName?.packageName]
                        val matchingApp = matchingApps?.firstOrNull {
                            it.componentName == item.componentName
                        }
                        add(matchingApp ?: item)
                    }

                    else -> {
                        val packageName = item.targetComponent?.packageName
                        val matchingApps = appMap[packageName]
                        val matchingApp = matchingApps?.firstOrNull {
                            it.componentName == item.targetComponent
                        }
                        if (matchingApp != null) {
                            add(matchingApp)
                        } else {
                            add(item)
                        }
                    }
                }
            }
        }
    }

    init {
        (context as Context).autoCatLauncher.deviceProfile.inv.addOnChangeListener(this)
        try {
            prefs2.hiddenApps.onEach(launchIn = (context as Context).autoCatLauncher.lifecycleScope) {
                hiddenApps = it
                try {
                    onAppsUpdated()
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in hiddenApps observer", e)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialize hidden apps", t)
        }
        observeFolders()
    }

    override fun onAppsUpdated() {
        try {
            super.onAppsUpdated()
            val autoCatApps = try {
                appsStore.apps.mapNotNull { it?.toAutoCatAppInfo() }
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping apps for AutoCat", e)
                emptyList()
            }
            cachedCategorizedApps = autoCatProvider.categorizeApps(autoCatApps)
        } catch (e: Throwable) {
            Log.e(TAG, "Error in onAppsUpdated", e)
        }
    }

    private fun observeFolders() {
        (context as Context).autoCatLauncher.lifecycleScope.launch {
            folderService.getFoldersFlow()
                .distinctUntilChanged()
                .collect { folders ->
                    folderList = folders
                        .sortedBy { folderOrder.indexOf(it.id) }
                        .toMutableList()
                    updateAdapterItems()
                }
        }
    }

    override fun updateItemFilter(itemFilter: Predicate<ItemInfo>?) {
        mItemFilter = Predicate { info ->
            require(info is AppInfo) { "`info` must be an instance of `AppInfo`." }
            val componentKey = info.toComponentKey().toString()
            val packageName = info.componentName?.packageName

            // 1. Basic checks (User filter & Hidden apps)
            var visible = (itemFilter?.test(info) != false) && !hiddenApps.contains(componentKey)

            // 2. Archival / Vault filtering
            val wrapper = com.android.launcher3.util.ApplicationInfoWrapper(context, packageName ?: "", info.user)
            val isArchived = wrapper.isArchived()
            val isEnabled = wrapper.isEnabled()
            val isColdStorage = isArchived || !isEnabled

            if (currentTabFilter == AppTabsController.TAB_VAULT) {
                // Vault tab: show ONLY archived or disabled apps
                visible = visible && isColdStorage
            } else {
                // Other tabs: EXCLUDE archived or disabled apps
                visible = visible && !isColdStorage

                // 3. Tab Filtering (Original logic)
                if (visible && currentTabFilter != null && currentTabFilter != AppTabsController.TAB_ALL && packageName != null) {
                    if (currentTabFilter == "DISCOVERY") {
                        val isRecent = false // TODO: implement recent logic
                        visible = isRecent
                    } else if (currentTabFilter == AppTabsController.TAB_WORK) {
                        // Work profile handled by its own adapter
                    } else {
                        // Determine if app belongs to the current tab
                        val appTabInfo = cachedCategorizedApps?.get(currentTabFilter)?.values?.flatten()?.find {
                            it.packageName == packageName
                        }
                        visible = appTabInfo != null
                    }
                }
            }

            visible
        }
        onAppsUpdated()
    }

    override fun addAppsWithSections(appList: List<AppInfo?>?, startPosition: Int): Int {
        if (appList.isNullOrEmpty()) return startPosition

        try {
            filteredList.clear()
            var position = startPosition

            // Check if tabs are enabled
            val usingAppTabs = appTabsController.shouldShowTabs(context)
            val currentTabName = if (usingAppTabs) {
                appTabsController.getCurrentTabName()
            } else {
                null
            }

            val isWorkProfile = isWorkOrPrivateSpace(appList)

            // Special handling for Work Profile
            if (isWorkProfile && usingAppTabs) {
                // If we are on the "Work" tab, show work apps
                if (currentTabName == AppTabsController.TAB_WORK) {
                    return super.addAppsWithSections(appList, position)
                }
                // If we are NOT on the Work tab, hide work apps (unless "All Apps" includes them? Design decision: Work is usually separate)
                // For now, assume Work apps ONLY show on Work tab.
                // But wait, if currentTabName is "All Apps", maybe we should show them?
                // Standard behavior: Work apps are separate.

                if (currentTabName != AppTabsController.TAB_ALL) {
                    return position // Skip work apps for custom tabs
                }

                // If "All Apps" tab, maybe show them mixed?
                // Currently Lawnchair keeps them separate in PagedView.
                // Since we are using PagedView where Work is a separate page (index N+1),
                // this method will be called specifically for the Work AdapterHolder.

                // Note: ActivityAllAppsContainerView calls setup() for Work AdapterHolder with work matcher.
                // So this method is called with ONLY work apps.
                // We should just return super if we are indeed populating the work adapter.
                // But how do we know which adapter calls us?
                // We don't easily know. But appList contains work apps.

                // If we are here, it means we are populating a list of work apps.
                // If the current UI tab is NOT Work, we shouldn't display them?
                // Actually, the ViewPager handles visibility. Each AdapterHolder populates its own RV.
                // So if we are populating Work RV, we should just populate it.

                return super.addAppsWithSections(appList, position)
            } else if (isWorkProfile) {
                return super.addAppsWithSections(appList, position)
            }

            // --- PERSONAL APPS HANDLING ---

            // If tabs are enabled, we need to filter personal apps based on the *current tab for THIS adapter*.
            // Wait, ActivityAllAppsContainerView creates ONE AdapterHolder for MAIN (Personal).
            // AND dynamic AdapterHolders for Custom Tabs.
            // AND one for WORK.

            // Problem: This class (AutoCatAlphabeticalAppsList) doesn't know which AdapterHolder it belongs to.
            // It just knows "context".

            // However, ActivityAllAppsContainerView sets up the adapter with a specific Matcher.
            // But `addAppsWithSections` is about *sorting* and *grouping* (sections).
            // If we are in a Custom Tab adapter, we want ONLY apps for that tab.

            // We need a way to pass the "Target Tab" to this list.
            // Right now, we only have `currentTabName` from the *global* controller.
            // But `ActivityAllAppsContainerView` creates multiple instances of this list, one for each tab.
            // AND it sets them up.

            // CRITICAL: We need to inject the "Tab Name" into this class instance so it knows what to filter.
            // But I cannot easily change the constructor signature without breaking things or doing massive refactor.

            // Alternative: `ActivityAllAppsContainerView` calls `updateItemFilter` with a predicate.
            // I can use that predicate to filter apps by tab!
            // In `ActivityAllAppsContainerView.rebindAdapters`:
            // mAH.get(i).setup(rv, matcher);
            // I can wrap the matcher to also check for Tab membership.

            // Let's rely on `mItemFilter` which calls `updateItemFilter`.
            // Use that for tab filtering instead of doing it inside `addAppsWithSections`.
            // `addAppsWithSections` should just organize what remains.

            return super.addAppsWithSections(appList, position)
        } catch (e: Exception) {
            Log.e(TAG, "Error in AutoCat addAppsWithSections", e)
            return super.addAppsWithSections(appList, startPosition)
        }
    }

    override fun onIdpChanged(modelPropertiesChanged: Boolean) {
        onAppsUpdated()
    }
}
