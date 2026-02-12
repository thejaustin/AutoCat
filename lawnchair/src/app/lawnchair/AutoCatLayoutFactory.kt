package app.lawnchair

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import app.lawnchair.font.FontManager
import com.android.launcher3.BubbleTextView
import com.android.launcher3.util.SafeCloseable
import com.android.launcher3.views.DoubleShadowBubbleTextView

class AutoCatLayoutFactory(context: Context) :
    LayoutInflater.Factory2,
    SafeCloseable {

    private val fontManager by lazy { FontManager.INSTANCE.get(context) }
    private val constructorMap = mapOf<String, (Context, AttributeSet) -> View>(
        "Button" to ::Button,
        "TextView" to ::TextView,
        BubbleTextView::class.java.name to ::BubbleTextView,
        DoubleShadowBubbleTextView::class.java.name to ::DoubleShadowBubbleTextView,
    )

    // Cache to avoid repeated styling attribute lookups
    private val fontOverrideCache = mutableMapOf<Int, Boolean>()

    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet,
    ): View? {
        val view = constructorMap[name]?.let { it(context, attrs) }
        if (view is TextView) {
            val attrHash = attrs.hashCode()
            if (fontOverrideCache[attrHash] != false) {
                val success = runCatching { fontManager.overrideFont(view, attrs) }.isSuccess
                if (!success) {
                    fontOverrideCache[attrHash] = false
                }
            }
        }
        return view
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return onCreateView(null, name, context, attrs)
    }

    override fun close() {
        // No resources to release
    }
}
