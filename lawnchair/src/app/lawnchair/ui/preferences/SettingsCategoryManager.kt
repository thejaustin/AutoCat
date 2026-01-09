package app.lawnchair.ui.preferences

import android.content.Context
import app.lawnchair.preferences.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the order and visibility of settings categories
 */
class SettingsCategoryManager(private val context: Context) {

    private val prefs = PreferenceManager.getInstance(context)
    private val _categories = MutableStateFlow<List<SettingsCategory>>(emptyList())
    val categories: StateFlow<List<SettingsCategory>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * Load categories from preferences or use defaults
     */
    private fun loadCategories() {
        val defaults = DefaultSettingsCategories.getDefaults()
        val savedOrder = prefs.settingsCategoryOrder.get()
        val savedVisibility = prefs.settingsCategoryVisibility.get()

        val categoryMap = defaults.associateBy { it.id }.toMutableMap()

        // Apply saved order
        val orderedCategories = if (savedOrder.isNotEmpty()) {
            val orderList = savedOrder.split(",")
            orderList.mapIndexedNotNull { index, id ->
                categoryMap[id]?.copy(order = index)
            }
        } else {
            defaults
        }

        // Apply saved visibility
        val finalCategories = if (savedVisibility.isNotEmpty()) {
            val visibilityMap = savedVisibility.split(",").associate {
                val parts = it.split(":")
                parts[0] to (parts.getOrNull(1) == "true")
            }
            orderedCategories.map { category ->
                val isVisible = visibilityMap[category.id] ?: category.isVisible
                category.copy(isVisible = isVisible)
            }
        } else {
            orderedCategories
        }

        _categories.value = finalCategories.sortedBy { it.order }
    }

    /**
     * Save the current category configuration
     */
    fun saveCategories() {
        val categories = _categories.value
        val orderString = categories.joinToString(",") { it.id }
        val visibilityString = categories.joinToString(",") { "${it.id}:${it.isVisible}" }

        prefs.settingsCategoryOrder.set(orderString)
        prefs.settingsCategoryVisibility.set(visibilityString)
    }

    /**
     * Reorder categories by moving an item from one position to another
     */
    fun reorderCategories(fromIndex: Int, toIndex: Int) {
        val currentList = _categories.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _categories.value = currentList.mapIndexed { index, category ->
                category.copy(order = index)
            }
            saveCategories()
        }
    }

    /**
     * Toggle visibility of a category
     */
    fun toggleCategoryVisibility(categoryId: String) {
        _categories.value = _categories.value.map { category ->
            if (category.id == categoryId) {
                category.copy(isVisible = !category.isVisible)
            } else {
                category
            }
        }
        saveCategories()
    }

    /**
     * Reset to default configuration
     */
    fun resetToDefaults() {
        prefs.settingsCategoryOrder.set("")
        prefs.settingsCategoryVisibility.set("")
        loadCategories()
    }

    companion object {
        @Volatile
        private var instance: SettingsCategoryManager? = null

        fun getInstance(context: Context): SettingsCategoryManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsCategoryManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
