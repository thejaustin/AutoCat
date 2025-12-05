package app.lawnchair.allapps

import android.content.Context
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import app.lawnchair.categorization.CategoryTabsController
import app.lawnchair.font.FontManager
import app.lawnchair.theme.color.tokens.ColorStateListTokens
import app.lawnchair.theme.drawable.DrawableTokens
import com.android.launcher3.R
import com.android.launcher3.pageindicators.PageIndicator
import com.android.launcher3.util.Themes

/**
 * Scrollable tab strip for category tabs in app drawer.
 * Supports N dynamic tabs unlike PersonalWorkSlidingTabStrip which only supports 2.
 */
class CategoryTabStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : HorizontalScrollView(context, attrs, defStyleAttr),
    PageIndicator {

    interface OnActivePageChangedListener {
        fun onActivePageChanged(activePage: Int)
    }

    private val tabContainer: LinearLayout
    private val categoryController = CategoryTabsController.getInstance(context)
    private val fontManager = FontManager.INSTANCE.get(context)

    private var onActivePageChangedListener: OnActivePageChangedListener? = null
    private var lastActivePage = 0
    private val tabs = mutableListOf<Button>()

    init {
        // Create the container for tabs
        tabContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.all_apps_header_pill_height) / 4
            setPadding(padding, 0, padding, 0)
        }
        addView(
            tabContainer,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT,
            ),
        )

        // Smooth scrolling
        isSmoothScrollingEnabled = true
        isHorizontalScrollBarEnabled = false
    }

    /**
     * Initialize tabs from CategoryTabsController.
     */
    fun setupTabs() {
        tabContainer.removeAllViews()
        tabs.clear()

        val tabNames = categoryController.tabNames.value
        tabNames.forEachIndexed { index, name ->
            val tab = createTab(name, index)
            tabs.add(tab)
            tabContainer.addView(tab)
        }

        // Select the current tab
        setActiveMarker(categoryController.getCurrentTab())
    }

    private fun createTab(label: String, index: Int): Button {
        return Button(context).apply {
            text = label
            isAllCaps = false
            setAllCaps(false)

            // Apply theming
            background = DrawableTokens.AllAppsTabsBackground.resolve(context)
            (background as? RippleDrawable)?.setDrawableByLayerId(
                android.R.id.mask,
                DrawableTokens.AllAppsTabsMaskDrawable.resolve(context),
            )
            setTextColor(ColorStateListTokens.AllAppsTabText.resolve(context))
            fontManager.setCustomFont(this, R.id.font_body_medium)

            // Tab dimensions
            val height = resources.getDimensionPixelSize(R.dimen.all_apps_header_pill_height)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin)
            val verticalPadding = height / 4

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                height,
            ).apply {
                marginStart = if (index == 0) 0 else horizontalPadding / 2
                marginEnd = horizontalPadding / 2
            }

            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            minWidth = resources.getDimensionPixelSize(R.dimen.all_apps_header_pill_height) * 2

            // Click listener
            setOnClickListener {
                setActiveMarker(index)
            }
        }
    }

    override fun setActiveMarker(activePage: Int) {
        if (activePage < 0 || activePage >= tabs.size) return

        // Update tab selection state
        tabs.forEachIndexed { index, tab ->
            tab.isSelected = index == activePage
        }

        // Scroll to show active tab
        val activeTab = tabs.getOrNull(activePage)
        if (activeTab != null) {
            post {
                val scrollX = activeTab.left - (width - activeTab.width) / 2
                smoothScrollTo(scrollX.coerceAtLeast(0), 0)
            }
        }

        // Notify listener
        if (onActivePageChangedListener != null && lastActivePage != activePage) {
            onActivePageChangedListener?.onActivePageChanged(activePage)
        }
        lastActivePage = activePage
    }

    fun setOnActivePageChangedListener(listener: OnActivePageChangedListener?) {
        onActivePageChangedListener = listener
    }

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        // Not used for category tabs
    }

    override fun setMarkersCount(numMarkers: Int) {
        // Tabs are set up via setupTabs() instead
    }

    /**
     * Refresh tabs when categories change.
     */
    fun refreshTabs() {
        categoryController.refresh()
        setupTabs()
    }

    /**
     * Get the number of tabs currently shown.
     */
    fun getTabCount(): Int = tabs.size
}
