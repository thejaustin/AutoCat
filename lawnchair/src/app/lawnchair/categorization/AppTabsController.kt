package app.lawnchair.categorization

import android.content.Context
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
import com.android.launcher3.util.MainThreadInitializedObject
import com.android.launcher3.util.SafeCloseable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controller for managing app tabs in the app drawer.
 * Provides dynamic tabs based on user-defined tabs.
 */
class AppTabsController private constructor(private val context: Context) : SafeCloseable {

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject<AppTabsController>(::AppTabsController)

        const val TAB_ALL = "All Apps"
        const val TAB_WORK = "Work"
        const val TAB_VAULT = "Vault"
        const val TAB_ALL_INDEX = 0

        @JvmStatic
        fun getInstance(context: Context): AppTabsController {
            return INSTANCE.get(context)
        }
    }

    private val database by lazy { TabDatabase.getInstance(context) }
    private val tabDao by lazy { database.tabDao() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _tabs = MutableStateFlow<List<CustomTab>>(emptyList())
    val tabs: StateFlow<List<CustomTab>> = _tabs.asStateFlow()

    private val _currentTabIndex = MutableStateFlow(TAB_ALL_INDEX)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()

    private val _tabNames = MutableStateFlow<List<String>>(listOf(TAB_ALL))
    val tabNames: StateFlow<List<String>> = _tabNames.asStateFlow()

    @Volatile
    private var tabsLoaded = false

    private fun ensureTabsLoaded() {
        if (!tabsLoaded) {
            synchronized(this) {
                if (!tabsLoaded) {
                    loadTabs()
                    tabsLoaded = true
                }
            }
        }
    }

    private fun loadTabs() {
        scope.launch {
            try {
                val loadedTabs: List<CustomTab> = withContext(Dispatchers.IO) {
                    tabDao.getAllCustomTabs()
                        .filter { it.isVisible }
                        .sortedBy { it.sortOrder }
                }
                _tabs.value = loadedTabs
                updateTabNames(loadedTabs)
            } catch (e: Exception) {
                // Fallback to just "All Apps" tab on error
                _tabs.value = emptyList()
                _tabNames.value = listOf(TAB_ALL)
            }
        }
    }

    private fun updateTabNames(loadedTabs: List<CustomTab>) {
        val names = mutableListOf(TAB_ALL)
        names.addAll(loadedTabs.map { it.name })

        // Add Work tab at the end if enabled
        val prefs = app.lawnchair.preferences.PreferenceManager.getInstance(context)
        if (prefs.showWorkTab.get()) {
            names.add(TAB_WORK)
        }

        // Add Vault tab (Archived/Frozen apps)
        names.add(TAB_VAULT)

        _tabNames.value = names
    }

    /**
     * Refresh tabs from database. Call this after tab changes.
     */
    fun refresh() {
        loadTabs()
    }

    /**
     * Set the currently active tab index.
     */
    fun setCurrentTab(index: Int) {
        if (index >= 0 && index < _tabNames.value.size) {
            _currentTabIndex.value = index
        }
    }

    /**
     * Get the current tab index.
     */
    fun getCurrentTab(): Int {
        ensureTabsLoaded()
        return _currentTabIndex.value
    }

    /**
     * Get the name of the current tab.
     */
    fun getCurrentTabName(): String {
        ensureTabsLoaded()
        val index = _currentTabIndex.value
        return if (index >= 0 && index < _tabNames.value.size) {
            _tabNames.value[index]
        } else {
            TAB_ALL
        }
    }

    /**
     * Get the tab name for a given tab index.
     * Returns null for "All Apps" tab.
     * Returns TAB_WORK constant for Work tab.
     * Returns TAB_VAULT constant for Vault tab.
     */
    fun getTabNameForTab(tabIndex: Int): String? {
        return if (tabIndex == TAB_ALL_INDEX) {
            null // "All Apps" shows everything
        } else if (isWorkTab(tabIndex)) {
            TAB_WORK // Special marker for work tab
        } else if (isVaultTab(tabIndex)) {
            TAB_VAULT // Special marker for vault tab
        } else if (tabIndex > 0 && tabIndex <= _tabs.value.size) {
            _tabs.value[tabIndex - 1].name
        } else {
            null
        }
    }

    /**
     * Check if the given tab index is the Work tab.
     */
    fun isWorkTab(tabIndex: Int): Boolean {
        val prefs = app.lawnchair.preferences.PreferenceManager.getInstance(context)
        if (!prefs.showWorkTab.get()) return false

        // Work tab is typically the second to last if Vault is enabled
        return _tabNames.value.getOrNull(tabIndex) == TAB_WORK
    }

    /**
     * Check if the given tab index is the Vault tab.
     */
    fun isVaultTab(tabIndex: Int): Boolean {
        // Vault tab is currently always the last tab
        val vaultIndex = _tabNames.value.size - 1
        return tabIndex == vaultIndex && _tabNames.value.getOrNull(vaultIndex) == TAB_VAULT
    }

    /**
     * Check if tabs are currently enabled based on preference.
     */
    fun areTabsEnabled(context: Context): Boolean {
        val prefs = app.lawnchair.preferences.PreferenceManager.getInstance(context)
        return prefs.autoCatUseTabs.get()
    }

    /**
     * Get the number of tabs.
     */
    fun getTabCount(): Int {
        ensureTabsLoaded()
        return _tabNames.value.size
    }

    /**
     * Check if we should show tabs (tabs are enabled in settings).
     * Always shows at minimum "All Apps" tab when enabled, even with no categories.
     */
    fun shouldShowTabs(context: Context): Boolean {
        ensureTabsLoaded()
        return areTabsEnabled(context)
    }

    /**
     * Rename a custom tab.
     */
    fun renameTab(oldTabName: String, newTabName: String) {
        if (oldTabName == TAB_ALL || oldTabName == TAB_WORK) return

        scope.launch(Dispatchers.IO) {
            val tab = tabDao.getCustomTabByName(oldTabName)
            if (tab != null) {
                tabDao.updateCustomTab(tab.copy(name = newTabName))
                tabDao.updateAppTabName(oldTabName, newTabName)
                loadTabs()
                app.lawnchair.categorization.AutoCatAppProvider.getInstance(context).refreshCache()
            }
        }
    }

    /**
     * Delete a custom tab.
     */
    fun deleteTab(tabName: String) {
        if (tabName == TAB_ALL || tabName == TAB_WORK) return

        scope.launch(Dispatchers.IO) {
            val tab = tabDao.getCustomTabByName(tabName)
            if (tab != null) {
                tabDao.deleteCustomTab(tab)
                tabDao.resetAppTabsForDeletedTab(tabName)
                loadTabs()
                app.lawnchair.categorization.AutoCatAppProvider.getInstance(context).refreshCache()
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
