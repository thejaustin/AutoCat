package app.lawnchair.categorization

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
class CategoryTabsController private constructor(private val context: Context) : SafeCloseable {

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject<CategoryTabsController>(::CategoryTabsController)

        const val TAB_ALL = "All Apps"
        const val TAB_WORK = "Work"
        const val TAB_ALL_INDEX = 0

        @JvmStatic
        fun getInstance(context: Context): CategoryTabsController {
            return INSTANCE.get(context)
        }
    }

    private val database by lazy { TabDatabase.getInstance(context) }
    private val categoryDao by lazy { database.categoryDao() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _categories = MutableStateFlow<List<CustomTab>>(emptyList())
    val categories: StateFlow<List<CustomTab>> = _categories.asStateFlow()

    private val _currentTabIndex = MutableStateFlow(TAB_ALL_INDEX)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()

    private val _tabNames = MutableStateFlow<List<String>>(listOf(TAB_ALL))
    val tabNames: StateFlow<List<String>> = _tabNames.asStateFlow()

    @Volatile
    private var categoriesLoaded = false

    private fun ensureCategoriesLoaded() {
        if (!categoriesLoaded) {
            synchronized(this) {
                if (!categoriesLoaded) {
                    loadCategories()
                    categoriesLoaded = true
                }
            }
        }
    }

    private fun loadCategories() {
        scope.launch {
            try {
                val cats: List<CustomTab> = withContext(Dispatchers.IO) {
                    categoryDao.getAllCustomCategories()
                        .filter { it.isVisible }
                        .sortedBy { it.sortOrder }
                }
                _categories.value = cats
                updateTabNames(cats)
            } catch (e: Exception) {
                // Fallback to just "All Apps" tab on error
                _categories.value = emptyList()
                _tabNames.value = listOf(TAB_ALL)
            }
        }
    }

    private fun updateTabNames(cats: List<CustomTab>) {
        val names = mutableListOf(TAB_ALL)
        names.addAll(cats.map { it.name })

        // Add Work tab at the end if enabled
        val prefs = app.lawnchair.preferences.PreferenceManager.getInstance(context)
        if (prefs.showWorkTab.get()) {
            names.add(TAB_WORK)
        }

        _tabNames.value = names
    }

    /**
     * Refresh tabs from database. Call this after tab changes.
     */
    fun refresh() {
        loadCategories()
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
        ensureCategoriesLoaded()
        return _currentTabIndex.value
    }

    /**
     * Get the name of the current tab.
     */
    fun getCurrentTabName(): String {
        ensureCategoriesLoaded()
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
     */
    fun getTabNameForTab(tabIndex: Int): String? {
        return if (tabIndex == TAB_ALL_INDEX) {
            null // "All Apps" shows everything
        } else if (isWorkTab(tabIndex)) {
            TAB_WORK // Special marker for work tab
        } else if (tabIndex > 0 && tabIndex <= _categories.value.size) {
            _categories.value[tabIndex - 1].name
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

        // Work tab is always the last tab
        val workTabIndex = _tabNames.value.size - 1
        return tabIndex == workTabIndex && _tabNames.value.getOrNull(workTabIndex) == TAB_WORK
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
        ensureCategoriesLoaded()
        return _tabNames.value.size
    }

    /**
     * Check if we should show tabs (tabs are enabled in settings).
     * Always shows at minimum "All Apps" tab when enabled, even with no categories.
     */
    fun shouldShowTabs(context: Context): Boolean {
        ensureCategoriesLoaded()
        return areTabsEnabled(context)
    }

    /**
     * Rename a custom tab.
     */
    fun renameTab(oldTabName: String, newTabName: String) {
        if (oldTabName == TAB_ALL || oldTabName == TAB_WORK) return

        scope.launch(Dispatchers.IO) {
            val tab = categoryDao.getCustomCategoryByName(oldTabName)
            if (tab != null) {
                categoryDao.updateCustomCategory(tab.copy(name = newTabName))
                categoryDao.updateAppTabName(oldTabName, newTabName)
                loadCategories()
                AutoCatAppProvider.getInstance(context).refreshCache()
            }
        }
    }

    /**
     * Delete a custom tab.
     */
    fun deleteTab(tabName: String) {
        if (tabName == TAB_ALL || tabName == TAB_WORK) return

        scope.launch(Dispatchers.IO) {
            val tab = categoryDao.getCustomCategoryByName(tabName)
            if (tab != null) {
                categoryDao.deleteCustomCategory(tab)
                categoryDao.resetAppTabsForDeletedTab(tabName)
                loadCategories()
                AutoCatAppProvider.getInstance(context).refreshCache()
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
