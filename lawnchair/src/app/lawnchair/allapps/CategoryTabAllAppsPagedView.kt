package app.lawnchair.allapps

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.android.launcher3.allapps.AllAppsPagedView

/**
 * Extended AllAppsPagedView that supports dynamic app tabs.
 * Unlike the standard Personal/Work tabs which are fixed at 2 pages,
 * this supports N pages based on user-defined tabs.
 */
class CategoryTabAllAppsPagedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AllAppsPagedView(context, attrs, defStyle) {

    companion object {
        private const val TAG = "CategoryTabAllAppsPagedView"
    }

    private var tabCount = 0

    /**
     * Set the number of app tabs. This should be called before
     * setting up the view pager.
     */
    fun setTabCount(count: Int) {
        if (count != tabCount) {
            tabCount = count
            Log.d(TAG, "Tab count set to: $count")
        }
    }

    /**
     * Get the total number of pages (tabs).
     */
    fun getTabCount(): Int = tabCount

    override fun getChildCount(): Int {
        return if (tabCount > 0) {
            tabCount
        } else {
            super.getChildCount()
        }
    }
}
