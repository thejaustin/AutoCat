package app.lawnchair.categorization

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.lawnchair.data.category.CategoryDatabase
import app.lawnchair.data.category.entities.CustomCategory
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
 * Controller for managing category tabs in the app drawer.
 * Provides dynamic tabs based on user-defined categories.
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

    private val categoryDao = CategoryDatabase.getInstance(context).categoryDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _categories = MutableStateFlow<List<CustomCategory>>(emptyList())
    val categories: StateFlow<List<CustomCategory>> = _categories.asStateFlow()

    private val _currentTabIndex = MutableStateFlow(TAB_ALL_INDEX)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()

    private val _tabNames = MutableStateFlow<List<String>>(listOf(TAB_ALL))
    val tabNames: StateFlow<List<String>> = _tabNames.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        scope.launch {
            try {
                val cats = withContext(Dispatchers.IO) {
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

    private fun updateTabNames(cats: List<CustomCategory>) {
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
     * Refresh categories from database. Call this after category changes.
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
    fun getCurrentTab(): Int = _currentTabIndex.value

    /**
     * Get the name of the current tab.
     */
    fun getCurrentTabName(): String {
        val index = _currentTabIndex.value
        return if (index >= 0 && index < _tabNames.value.size) {
            _tabNames.value[index]
        } else {
            TAB_ALL
        }
    }

    /**
     * Get the category name for a given tab index.
     * Returns null for "All Apps" tab.
     * Returns TAB_WORK constant for Work tab.
     */
    fun getCategoryForTab(tabIndex: Int): String? {
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
    fun getTabCount(): Int = _tabNames.value.size

    /**
     * Check if we should show tabs (tabs are enabled in settings).
     * Always shows at minimum "All Apps" tab when enabled, even with no categories.
     */
    fun shouldShowTabs(context: Context): Boolean {
        return areTabsEnabled(context)
    }

    /**
     * Rename a custom category.
     */
    fun renameCategory(oldName: String, newName: String) {
        if (oldName == TAB_ALL || oldName == TAB_WORK) return

        scope.launch(Dispatchers.IO) {
            val category = categoryDao.getCustomCategoryByName(oldName)
            if (category != null) {
                categoryDao.updateCustomCategory(category.copy(name = newName))
                categoryDao.updateAppCategoryName(oldName, newName)
                loadCategories()
                AutoCatAppProvider.getInstance(context).refreshCache()
            }
        }
    }

    /**
     * Delete a custom category.
     */
    fun deleteCategory(categoryName: String) {
        if (categoryName == TAB_ALL || categoryName == TAB_WORK) return

        scope.launch(Dispatchers.IO) {
            val category = categoryDao.getCustomCategoryByName(categoryName)
            if (category != null) {
                categoryDao.deleteCustomCategory(category)
                categoryDao.resetAppCategoriesForDeletedCategory(categoryName)
                loadCategories()
                AutoCatAppProvider.getInstance(context).refreshCache()
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
