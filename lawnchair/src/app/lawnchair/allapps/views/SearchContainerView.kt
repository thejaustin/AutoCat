package app.lawnchair.allapps.views

import android.content.Context
import android.util.AttributeSet
import app.lawnchair.search.AutoCatSearchUiDelegate
import com.android.launcher3.allapps.LauncherAllAppsContainerView

class SearchContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LauncherAllAppsContainerView(context, attrs, defStyleAttr) {

    override fun createSearchUiDelegate() = AutoCatSearchUiDelegate(this)
}
