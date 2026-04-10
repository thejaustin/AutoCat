package com.android.launcher3.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.android.launcher3.model.data.WidgetStackInfo
import kotlin.math.abs

/**
 * A view that holds a stack of widgets and allows swiping between them.
 */
class WidgetStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var stackInfo: WidgetStackInfo? = null
    private var currentIndex = 0
    
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var lastY = 0f
    private var isSwiping = false

    fun setStackInfo(info: WidgetStackInfo) {
        this.stackInfo = info
        updateVisibleWidget()
    }

    fun addWidgetView(view: View) {
        addView(view)
        updateVisibleWidget()
    }

    private fun updateVisibleWidget() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            // Skip the indicator view itself if we add one later, 
            // but for now let's just use children as widgets.
            child.visibility = if (i == currentIndex) View.VISIBLE else View.GONE
        }
    }
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // Ensure all widgets take up the full space
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, measuredWidth, measuredHeight)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = ev.y
                isSwiping = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = ev.y - lastY
                if (abs(deltaY) > touchSlop) {
                    isSwiping = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isSwiping) {
            if (ev.action == MotionEvent.ACTION_UP) {
                val deltaY = ev.y - lastY
                if (abs(deltaY) > touchSlop * 2) {
                    if (deltaY > 0) {
                        showPrevious()
                    } else {
                        showNext()
                    }
                }
                isSwiping = false
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    private fun showNext() {
        if (childCount > 1) {
            currentIndex = (currentIndex + 1) % childCount
            updateVisibleWidget()
        }
    }

    private fun showPrevious() {
        if (childCount > 1) {
            currentIndex = (currentIndex - 1 + childCount) % childCount
            updateVisibleWidget()
        }
    }
}
