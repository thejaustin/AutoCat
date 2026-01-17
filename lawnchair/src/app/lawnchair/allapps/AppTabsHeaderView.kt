package app.lawnchair.allapps

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.AbstractComposeView
import app.lawnchair.categorization.AppTabsController
import app.lawnchair.ui.theme.LawnchairTheme
import com.android.launcher3.pageindicators.PageIndicator
import com.android.launcher3.workprofile.PersonalWorkSlidingTabStrip.OnActivePageChangedListener

class AppTabsHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr),
    PageIndicator {

    private var onActivePageChangedListener: OnActivePageChangedListener? = null
    private val controller = AppTabsController.getInstance(context)

    @Composable
    override fun Content() {
        LawnchairTheme {
            AppTabsView(
                onTabSelect = { page ->
                    // Notify listener (ActivityAllAppsContainerView) to switch page
                    onActivePageChangedListener?.onActivePageChanged(page)
                },
            )
        }
    }

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        // Optional: Implement scroll progress for sliding indicator
    }

    override fun setActiveMarker(activePage: Int) {
        // Update the controller's state, which the UI observes
        controller.setCurrentTab(activePage)
    }

    override fun setMarkersCount(numMarkers: Int) {
        // No-op, count is managed by controller
    }

    fun setOnActivePageChangedListener(listener: OnActivePageChangedListener?) {
        this.onActivePageChangedListener = listener
    }
}
