package app.lawnchair.ui.preferences.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import app.lawnchair.ui.util.ExpressiveHaptics

enum class PreferenceHapticType {
    PREFERENCE_CLICK,
    CATEGORY_SELECT,
    EXPAND_SECTION,
    COLLAPSE_SECTION,
}

class PreferenceHaptics(private val haptics: ExpressiveHaptics) {
    fun perform(type: PreferenceHapticType) {
        when (type) {
            PreferenceHapticType.PREFERENCE_CLICK,
            PreferenceHapticType.CATEGORY_SELECT,
            -> haptics.click()

            PreferenceHapticType.EXPAND_SECTION,
            PreferenceHapticType.COLLAPSE_SECTION,
            -> haptics.tick()
        }
    }
}

@Composable
fun rememberPreferenceHaptics(): PreferenceHaptics {
    val view = LocalView.current
    return remember(view) { PreferenceHaptics(ExpressiveHaptics(view)) }
}
