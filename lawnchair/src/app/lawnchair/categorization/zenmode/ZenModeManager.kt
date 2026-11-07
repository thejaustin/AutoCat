package app.lawnchair.categorization.zenmode

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import app.lawnchair.categorization.CategoryTabsManager
import app.lawnchair.data.tab.TabDatabase
import app.lawnchair.data.tab.entities.CustomTab
import app.lawnchair.preferences.PreferenceManager

/**
 * Zen Mode - links AutoCat tabs to Do Not Disturb.
 *
 * While Do Not Disturb (or any Android mode that silences interruptions) is on,
 * tabs marked "hide in Zen" are left out of the app drawer, or only tabs marked
 * "show only in Zen" are shown when there are any. The filter is applied when tabs
 * are read, so the saved visibility of each tab is never changed.
 */
class ZenModeManager private constructor(
    private val context: Context,
) {
    private val tabDao by lazy { TabDatabase.getInstance(context).categoryDao() }
    private val prefs by lazy { PreferenceManager.getInstance(context) }
    private val notificationManager by lazy {
        context.getSystemService(NotificationManager::class.java)
    }

    private var receiverRegistered = false
    private val interruptionFilterReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (prefs.autoCatEnableZenMode.get()) {
                CategoryTabsManager.getInstance(context).refreshTabs()
            }
        }
    }

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
     * Whether Zen Mode is enabled in settings and the system is currently silencing interruptions.
     */
    fun isZenActive(): Boolean {
        if (!prefs.autoCatEnableZenMode.get()) return false
        return when (notificationManager?.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_NONE,
            -> true

            else -> false
        }
    }

    /**
     * Applies the Zen rules to [tabs] when Zen Mode is active, otherwise returns them unchanged.
     */
    fun filterTabs(tabs: List<CustomTab>): List<CustomTab> {
        if (!isZenActive()) return tabs
        val showOnly = tabs.filter { it.showOnlyInZenMode }
        return showOnly.ifEmpty { tabs.filterNot { it.hideInZenMode } }
    }

    /**
     * Refreshes the drawer tabs whenever Do Not Disturb turns on or off.
     * Call from the launcher's onCreate, paired with [unregister].
     */
    fun register() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            interruptionFilterReceiver,
            IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    fun unregister() {
        if (!receiverRegistered) return
        context.unregisterReceiver(interruptionFilterReceiver)
        receiverRegistered = false
    }

    /**
     * Gets Focus Mode rules for all categories.
     */
    suspend fun getFocusRules(): List<FocusRule> {
        return tabDao.getAllCustomCategories().map { tab ->
            FocusRule(
                categoryName = tab.name,
                hideDuringFocus = tab.hideInZenMode,
                showOnlyDuringFocus = tab.showOnlyInZenMode,
            )
        }
    }

    /**
     * Updates Focus Mode rule for a category.
     */
    suspend fun updateFocusRule(rule: FocusRule) {
        val tab = tabDao.getCustomCategoryByName(rule.categoryName) ?: return

        tabDao.updateCustomCategory(
            tab.copy(
                hideInZenMode = rule.hideDuringFocus,
                showOnlyInZenMode = rule.showOnlyDuringFocus,
            ),
        )
        Log.d(TAG, "Updated Focus rule for '${rule.categoryName}': hide=${rule.hideDuringFocus}, showOnly=${rule.showOnlyDuringFocus}")
        CategoryTabsManager.getInstance(context).refreshTabs()
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

        return tabDao.getAllCustomCategories()
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

        return tabDao.getAllCustomCategories()
            .filter { tab ->
                essentialKeywords.any { keyword ->
                    tab.name.contains(keyword, ignoreCase = true)
                }
            }
            .map { it.name }
    }
}
