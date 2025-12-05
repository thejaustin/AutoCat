package app.lawnchair.allapps

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.android.launcher3.allapps.AllAppsPagedView

/**
 * Extended AllAppsPagedView that supports dynamic category tabs.
 * Unlike the standard Personal/Work tabs which are fixed at 2 pages,
 * this supports N pages based on user-defined categories.
 */
class CategoryAllAppsPagedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AllAppsPagedView(context, attrs, defStyle) {

    companion object {
        private const val TAG = "CategoryAllAppsPagedView"
    }

    private var categoryCount = 0

    /**
     * Set the number of category tabs. This should be called before
     * setting up the view pager.
     */
    fun setCategoryCount(count: Int) {
        if (count != categoryCount) {
            categoryCount = count
            Log.d(TAG, "Category count set to: $count")
        }
    }

    /**
     * Get the total number of pages (categories).
     */
    fun getCategoryCount(): Int = categoryCount

    override fun getChildCount(): Int {
        return if (categoryCount > 0) {
            categoryCount
        } else {
            super.getChildCount()
        }
    }
}
