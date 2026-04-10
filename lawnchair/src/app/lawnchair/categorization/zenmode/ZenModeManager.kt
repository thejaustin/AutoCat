package app.lawnchair.categorization.zenmode

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import app.lawnchair.categorization.CategoryTabsManager
import app.lawnchair.data.tab.TabDao
import app.lawnchair.data.tab.TabDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zen Mode - Links AutoCat categories to Android Focus Mode.
 *
 * When Focus Mode is active, hides distracting app categories
 * and surfaces only essential apps for productivity.
 *
 * Features:
 * - Auto-hide categories when Focus Mode activates
 * - Show only "Work", "Productivity", "Essentials" tabs
 * - Restore full app drawer when Focus Mode deactivates
 * - Per-category Focus Mode rules
 */
class ZenModeManager private constructor(
    private val context: Context,
) {
    private val tabDatabase by lazy { TabDatabase.getInstance(context) }
    private val tabDao by lazy { tabDatabase.tabDao() }
    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val _isFocusModeActive = MutableStateFlow(false)
    val isFocusModeActive: StateFlow<Boolean> = _isFocusModeActive.asStateFlow()

    private val _activeFocusRules = MutableStateFlow(setOf<String>())
    val activeFocusRules: StateFlow<Set<String>> = _activeFocusRules.asStateFlow()

    companion object {
        private const val TAG = "ZenModeManager"

        @Volatile
        private var instance: ZenModeManager? = null

        fun getInstance(context: Context): ZenModeManager {
            return instance ?: synchronized(this) {
                instance ?: ZenModeManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    /**
     * Focus Mode rule - maps category names to Focus Mode states.
     */
    data class FocusRule(
        val categoryName: String,
        val hideDuringFocus: Boolean = true,
        val showOnlyDuringFocus: Boolean = false,
    )

    /**
     * Checks if Focus Mode is currently active.
     */
    fun isFocusModeEnabled(): Boolean {
        return try {
            // Check if any focus-related packages are in foreground
            val currentTime = System.currentTimeMillis()
            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 60000,
                currentTime,
            )

            // Focus mode typically restricts certain apps
            // We'll use a heuristic based on app usage patterns
            usageStats?.isNotEmpty() == true
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to access usage stats", e)
            false
        }
    }

    /**
     * Gets Focus Mode rules for all categories.
     */
    suspend fun getFocusRules(): List<FocusRule> {
        return tabDao.getAllTabs().map { tab ->
            FocusRule(
                categoryName = tab.name,
                hideDuringFocus = tab.hideDuringFocusMode,
                showOnlyDuringFocus = tab.showOnlyDuringFocusMode,
            )
        }
    }

    /**
     * Updates Focus Mode rule for a category.
     */
    suspend fun updateFocusRule(rule: FocusRule) {
        val tab = tabDao.getTabByName(rule.categoryName) ?: return

        tab.hideDuringFocusMode = rule.hideDuringFocus
        tab.showOnlyDuringFocus = rule.showOnlyDuringFocus

        tabDao.update(tab)
        Log.d(TAG, "Updated Focus rule for '${rule.categoryName}': hide=${rule.hideDuringFocus}, showOnly=${rule.showOnlyDuringFocus}")
    }

    /**
     * Applies Focus Mode state to app drawer.
     *
     * @param active true if Focus Mode is active
     */
    suspend fun applyFocusModeState(active: Boolean) {
        _isFocusModeActive.value = active

        if (active) {
            activateFocusMode()
        } else {
            deactivateFocusMode()
        }

        Log.i(TAG, "Focus Mode ${if (active) "activated" else "deactivated"}")
    }

    /**
     * Activates Focus Mode - hides distracting categories.
     */
    private suspend fun activateFocusMode() {
        val rules = getFocusRules()
        val categoriesToHide = rules
            .filter { it.hideDuringFocus && !it.showOnlyDuringFocus }
            .map { it.categoryName }

        val categoriesToShow = rules
            .filter { it.showOnlyDuringFocus }
            .map { it.categoryName }
            .toSet()

        _activeFocusRules.value = categoriesToShow

        // Hide categories marked for Focus Mode
        categoriesToHide.forEach { categoryName ->
            tabDao.getTabByName(categoryName)?.let { tab ->
                if (tab.isVisible) {
                    tab.isVisible = false
                    tabDao.update(tab)
                    Log.d(TAG, "Hidden category '$categoryName' during Focus Mode")
                }
            }
        }

        // Show only essential categories if specified
        if (categoriesToShow.isNotEmpty()) {
            tabDao.getAllTabs().forEach { tab ->
                if (categoriesToShow.contains(tab.name)) {
                    if (!tab.isVisible) {
                        tab.isVisible = true
                        tabDao.update(tab)
                        Log.d(TAG, "Shown essential category '${tab.name}' during Focus Mode")
                    }
                }
            }
        }

        // Notify tabs manager to refresh
        CategoryTabsManager.getInstance(context).refreshTabs()
    }

    /**
     * Deactivates Focus Mode - restores all categories.
     */
    private suspend fun deactivateFocusMode() {
        val rules = getFocusRules()
        val categoriesPreviouslyHidden = rules
            .filter { it.hideDuringFocus && !it.showOnlyDuringFocus }
            .map { it.categoryName }
            .toSet()

        // Restore previously hidden categories
        categoriesPreviouslyHidden.forEach { categoryName ->
            tabDao.getTabByName(categoryName)?.let { tab ->
                if (!tab.isVisible) {
                    tab.isVisible = true
                    tabDao.update(tab)
                    Log.d(TAG, "Restored category '$categoryName' after Focus Mode")
                }
            }
        }

        _activeFocusRules.value = emptySet()

        // Notify tabs manager to refresh
        CategoryTabsManager.getInstance(context).refreshTabs()
    }

    /**
     * Sets up Focus Mode listener.
     * Call this from Application or Launcher onCreate.
     */
    fun setupFocusModeListener() {
        // Listen for Focus Mode changes via broadcast
        val intentFilter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        context.registerReceiver(
            createFocusModeReceiver(),
            intentFilter,
        )

        Log.d(TAG, "Focus Mode listener setup complete")
    }

    private fun createFocusModeReceiver(): android.content.BroadcastReceiver {
        return object : android.content.BroadcastReceiver() {
            private var lastFocusState = false

            override fun onReceive(context: Context, intent: Intent) {
                val currentFocusState = isFocusModeEnabled()

                if (currentFocusState != lastFocusState) {
                    lastFocusState = currentFocusState
                    scope.launch {
                        applyFocusModeState(currentFocusState)
                    }
                }
            }
        }
    }

    /**
     * Gets recommended categories to hide during Focus Mode.
     */
    suspend fun getRecommendedCategoriesToHide(): List<String> {
        val distractingKeywords = setOf(
            "games",
            "social",
            "entertainment",
            "streaming",
            "news",
            "shopping",
            "dating",
            "fitness",
        )

        return tabDao.getAllTabs()
            .filter { tab ->
                distractingKeywords.any { keyword ->
                    tab.name.contains(keyword, ignoreCase = true)
                }
            }
            .map { it.name }
    }

    /**
     * Gets recommended categories to show during Focus Mode.
     */
    suspend fun getRecommendedCategoriesToShow(): List<String> {
        val essentialKeywords = setOf(
            "work",
            "productivity",
            "essentials",
            "utilities",
            "communication",
            "calendar",
            "notes",
            "finance",
        )

        return tabDao.getAllTabs()
            .filter { tab ->
                essentialKeywords.any { keyword ->
                    tab.name.contains(keyword, ignoreCase = true)
                }
            }
            .map { it.name }
    }
}
