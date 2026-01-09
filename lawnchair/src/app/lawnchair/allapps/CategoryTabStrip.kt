package app.lawnchair.allapps

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import app.lawnchair.categorization.CategoryTabsController
import app.lawnchair.font.FontManager
import app.lawnchair.theme.color.tokens.ColorStateListTokens
import app.lawnchair.theme.drawable.DrawableTokens
import com.android.launcher3.BaseActivity
import com.android.launcher3.DeviceProfile
import com.android.launcher3.R
import com.android.launcher3.pageindicators.PageIndicator
import com.android.launcher3.util.Themes
import com.android.launcher3.views.ActivityContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Scrollable tab strip for app tabs in app drawer.
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
    private var job: Job? = null

    init {
        // Fill viewport to allow centering when content is smaller than width
        isFillViewport = true

        // Create the container for tabs
        tabContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            // Center tabs if they don't fill the screen
            gravity = Gravity.CENTER
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Emulate PersonalWorkSlidingTabStrip alignment logic
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val activityContext = ActivityContext.lookupContext<BaseActivity>(context)
        val grid = activityContext.deviceProfile
        val iconPadding = size / grid.numShownAllAppsColumns - grid.allAppsIconSizePx
        val newWidth = size - iconPadding

        val newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)
        super.onMeasure(newWidthMeasureSpec, heightMeasureSpec)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        job = CoroutineScope(Dispatchers.Main).launch {
            launch {
                categoryController.tabNames.collect {
                    setupTabs()
                }
            }
            launch {
                categoryController.currentTabIndex.collect { index ->
                    setActiveMarker(index)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        job?.cancel()
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
        return Button(context, null, android.R.attr.borderlessButtonStyle).apply {
            text = label
            isAllCaps = false
            setAllCaps(false)
            textSize = 14f

            // Apply theming
            background = DrawableTokens.AllAppsTabsBackground.resolve(context)
            (background as? RippleDrawable)?.setDrawableByLayerId(
                android.R.id.mask,
                DrawableTokens.AllAppsTabsMaskDrawable.resolve(context),
            )
            setTextColor(ColorStateListTokens.AllAppsTabText.resolve(context))
            fontManager.setCustomFont(this, R.id.font_body_medium)

            // Tab dimensions
            val buttonMargin = resources.getDimensionPixelSize(R.dimen.all_apps_tabs_button_horizontal_padding)
            val horizontalPadding = resources.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                marginStart = if (index == 0) 0 else buttonMargin
                marginEnd = 0
            }

            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            minWidth = resources.getDimensionPixelSize(R.dimen.all_apps_header_pill_height) * 2

            // Click listener
            setOnClickListener {
                setActiveMarker(index)
            }

            // Long click listener for Edit/Delete
            setOnLongClickListener {
                showTabOptions(this, label)
                true
            }
        }
    }

    private fun showTabOptions(view: View, tabName: String) {
        if (tabName == CategoryTabsController.TAB_ALL || tabName == CategoryTabsController.TAB_WORK) {
            return
        }

        val popup = PopupMenu(context, view)
        popup.menu.add(Menu.NONE, 1, 1, "Rename")
        popup.menu.add(Menu.NONE, 2, 2, "Delete")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showRenameDialog(tabName)
                    true
                }

                2 -> {
                    showDeleteDialog(tabName)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showRenameDialog(oldTabName: String) {
        val input = EditText(context)
        input.setText(oldTabName)
        input.setSelectAllOnFocus(true)

        AlertDialog.Builder(context)
            .setTitle("Rename Tab")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newTabName = input.text.toString().trim()
                if (newTabName.isNotEmpty() && newTabName != oldTabName) {
                    categoryController.renameTab(oldTabName, newTabName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(tabName: String) {
        AlertDialog.Builder(context)
            .setTitle("Delete Tab")
            .setMessage("Are you sure you want to delete '$tabName'? Apps will be moved to 'Other'.")
            .setPositiveButton("Delete") { _, _ ->
                categoryController.deleteTab(tabName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun setActiveMarker(activePage: Int) {
        if (activePage < 0 || activePage >= tabs.size) return

        // Update tab selection state with expressive spring animation
        tabs.forEachIndexed { index, tab ->
            val wasSelected = tab.isSelected
            tab.isSelected = index == activePage

            // Material 3 Expressive: Spring-based scale animation on selection
            if (index == activePage && !wasSelected) {
                animateTabSelection(tab)
            } else if (wasSelected && index != activePage) {
                animateTabDeselection(tab)
            }
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

    /**
     * Material 3 Expressive: Animate tab selection with spring physics
     */
    private fun animateTabSelection(tab: Button) {
        // Spring-based scale animation with overshoot for expressive feel
        val scaleX = SpringAnimation(tab, DynamicAnimation.SCALE_X, 1.08f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY // Creates expressive bounce
        }

        val scaleY = SpringAnimation(tab, DynamicAnimation.SCALE_Y, 1.08f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
        }

        scaleX.start()
        scaleY.start()

        // Subtle elevation animation for depth
        tab.elevation = resources.getDimensionPixelSize(R.dimen.all_apps_header_pill_height) * 0.08f
    }

    /**
     * Material 3 Expressive: Animate tab deselection
     */
    private fun animateTabDeselection(tab: Button) {
        val scaleX = SpringAnimation(tab, DynamicAnimation.SCALE_X, 1.0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }

        val scaleY = SpringAnimation(tab, DynamicAnimation.SCALE_Y, 1.0f).apply {
            spring.stiffness = SpringForce.STIFFNESS_MEDIUM
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
        }

        scaleX.start()
        scaleY.start()

        // Reset elevation
        tab.elevation = 0f
    }

    fun setOnActivePageChangedListener(listener: OnActivePageChangedListener?) {
        onActivePageChangedListener = listener
    }

    override fun setScroll(currentScroll: Int, totalScroll: Int) {
        // Not used for app tabs
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
