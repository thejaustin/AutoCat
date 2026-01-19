package app.lawnchair.ui.preferences.components.reorderable

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.android.launcher3.Utilities

enum class ReorderHapticFeedbackType {
    START,
    MOVE,
    END,
}

class ReorderHapticFeedback(private val view: android.view.View) {
    fun performHapticFeedback(type: ReorderHapticFeedbackType) {
        when (type) {
            ReorderHapticFeedbackType.START -> {
                if (Utilities.ATLEAST_U) {
                    view.performHapticFeedback(HapticFeedbackConstants.DRAG_START)
                }
            }

            ReorderHapticFeedbackType.MOVE -> {
                if (Utilities.ATLEAST_U) {
                    view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
                }
            }

            ReorderHapticFeedbackType.END -> {
                if (Utilities.ATLEAST_R) {
                    view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                }
            }
        }
    }
}

@Composable
fun rememberReorderHapticFeedback(): ReorderHapticFeedback {
    val view = LocalView.current
    return remember(view) { ReorderHapticFeedback(view) }
}
