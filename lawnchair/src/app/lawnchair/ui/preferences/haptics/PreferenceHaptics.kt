package app.lawnchair.ui.preferences.haptics

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import com.android.launcher3.util.VibratorWrapper

/**
 * Enum defining all haptic feedback types used in preference screens.
 * Each type corresponds to a specific user interaction pattern.
 */
enum class PreferenceHapticType {
    // Basic interactions
    PREFERENCE_CLICK, // Light tap for standard clicks
    PREFERENCE_LONG_PRESS, // Heavier tap for long press
    SWITCH_TOGGLE_ON, // Smooth "click on" feedback
    SWITCH_TOGGLE_OFF, // Smooth "click off" feedback
    SLIDER_TICK, // Subtle tick per slider value change
    SLIDER_COMMIT, // Confirm final slider value

    // Navigation
    SCREEN_ENTER, // Entering new preference screen
    SCREEN_EXIT, // Exiting preference screen
    TAB_SWITCH, // Switching between tabs

    // Content
    EXPAND_SECTION, // Expanding accordion section
    COLLAPSE_SECTION, // Collapsing accordion section
    CATEGORY_SELECT, // Selecting category card

    // Search
    SEARCH_RESULT_FOUND, // Search results found
    SEARCH_NO_RESULTS, // No search results (different pattern)

    // Special feedback
    ERROR_FEEDBACK, // Error occurred
    SUCCESS_FEEDBACK, // Successful action
    PREVIEW_UPDATE, // Live preview changed
}

/**
 * Data class representing a haptic primitive for composition.
 *
 * @param type The primitive type (e.g., PRIMITIVE_TICK, PRIMITIVE_LOW_TICK)
 * @param scale The intensity scale (0.0 to 1.0)
 * @param delayMs Delay in milliseconds before this primitive
 */
data class HapticPrimitive(
    val type: Int,
    val scale: Float,
    val delayMs: Int = 0,
)

/**
 * Interface for preference haptic feedback.
 * Provides methods to perform haptic feedback with device-specific optimizations.
 */
interface PreferenceHaptics {
    /**
     * Performs haptic feedback for a specific interaction type.
     *
     * @param type The type of haptic feedback to perform
     */
    fun perform(type: PreferenceHapticType)

    /**
     * Performs custom haptic feedback using primitives.
     * Falls back to the provided effect if primitives are not supported.
     *
     * @param primitives List of haptic primitives to compose
     * @param fallback Fallback vibration effect for non-premium devices
     */
    fun performCustom(
        primitives: List<HapticPrimitive>,
        fallback: VibrationEffect,
    )

    /**
     * Whether the device supports premium haptic features (primitives).
     */
    val isPremiumDevice: Boolean
}

/**
 * Default implementation of PreferenceHaptics.
 * Integrates with device-specific haptic profiles for optimal feedback.
 */
private class PreferenceHapticsImpl(
    private val context: Context,
) : PreferenceHaptics {

    private val vibratorWrapper = VibratorWrapper.INSTANCE.get(context)
    private val vibrator = context.getSystemService<Vibrator>()
    private val hapticProfile = PremiumHapticProfiles.detectProfile(context)

    override val isPremiumDevice: Boolean
        get() = hapticProfile.supportsPrimitives

    override fun perform(type: PreferenceHapticType) {
        val effect = when (type) {
            PreferenceHapticType.PREFERENCE_CLICK ->
                VibratorWrapper.EFFECT_CLICK

            PreferenceHapticType.PREFERENCE_LONG_PRESS ->
                VibratorWrapper.EFFECT_CLICK

            PreferenceHapticType.SWITCH_TOGGLE_ON ->
                HapticEffects.switchToggleOn(hapticProfile)

            PreferenceHapticType.SWITCH_TOGGLE_OFF ->
                HapticEffects.switchToggleOff(hapticProfile)

            PreferenceHapticType.SLIDER_TICK -> {
                HapticEffects.sliderTexture(hapticProfile)?.let {
                    vibratorWrapper.vibrate(it)
                }
                return // Early return if texture feedback performed
            }

            PreferenceHapticType.SLIDER_COMMIT ->
                HapticEffects.commitFeedback(hapticProfile)

            PreferenceHapticType.SCREEN_ENTER,
            PreferenceHapticType.SCREEN_EXIT,
            -> VibratorWrapper.EFFECT_CLICK

            PreferenceHapticType.TAB_SWITCH ->
                VibratorWrapper.EFFECT_CLICK

            PreferenceHapticType.EXPAND_SECTION ->
                HapticEffects.sectionExpand(hapticProfile)

            PreferenceHapticType.COLLAPSE_SECTION ->
                HapticEffects.sectionCollapse(hapticProfile)

            PreferenceHapticType.CATEGORY_SELECT ->
                HapticEffects.categorySelect(hapticProfile)

            PreferenceHapticType.SEARCH_RESULT_FOUND ->
                VibratorWrapper.EFFECT_CLICK

            PreferenceHapticType.SEARCH_NO_RESULTS ->
                HapticEffects.errorFeedback(hapticProfile)

            PreferenceHapticType.ERROR_FEEDBACK ->
                HapticEffects.errorFeedback(hapticProfile)

            PreferenceHapticType.SUCCESS_FEEDBACK ->
                HapticEffects.successFeedback(hapticProfile)

            PreferenceHapticType.PREVIEW_UPDATE ->
                VibratorWrapper.EFFECT_CLICK
        }

        vibratorWrapper.vibrate(effect)
    }

    override fun performCustom(
        primitives: List<HapticPrimitive>,
        fallback: VibrationEffect,
    ) {
        if (!isPremiumDevice || primitives.isEmpty()) {
            vibratorWrapper.vibrate(fallback)
            return
        }

        // Build composition from primitives
        val composition = VibrationEffect.startComposition()
        val handler = Handler(Looper.getMainLooper())
        primitives.forEach { primitive ->
            if (primitive.delayMs > 0) {
                handler.postDelayed({
                    vibrator?.let {
                        if (it.areAllPrimitivesSupported(primitive.type)) {
                            it.vibrate(
                                VibrationEffect.startComposition()
                                    .addPrimitive(primitive.type, primitive.scale)
                                    .compose(),
                                VibratorWrapper.VIBRATION_ATTRS,
                            )
                        }
                    }
                }, primitive.delayMs.toLong())
            } else {
                composition.addPrimitive(primitive.type, primitive.scale)
            }
        }

        vibratorWrapper.vibrate(composition.compose())
    }
}

/**
 * Remembers a PreferenceHaptics instance for the current context.
 * The instance is cached and reused across recompositions.
 *
 * @return PreferenceHaptics instance for performing haptic feedback
 */
@Composable
fun rememberPreferenceHaptics(): PreferenceHaptics {
    val context = LocalContext.current

    return remember(context) {
        PreferenceHapticsImpl(context)
    }
}
