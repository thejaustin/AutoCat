package app.lawnchair.ui.preferences.haptics

import android.os.VibrationEffect
import android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK
import android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
import android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
import android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
import android.os.VibrationEffect.Composition.PRIMITIVE_SPIN
import android.os.VibrationEffect.Composition.PRIMITIVE_THUD
import android.os.VibrationEffect.Composition.PRIMITIVE_TICK
import android.os.VibrationEffect.createPredefined
import android.os.VibrationEffect.EFFECT_CLICK
import android.os.VibrationEffect.EFFECT_DOUBLE_CLICK
import android.os.VibrationEffect.EFFECT_HEAVY_CLICK
import android.os.VibrationEffect.EFFECT_TICK

/**
 * Collection of predefined haptic effects optimized for preference interactions.
 * Each effect is tuned based on the device's haptic profile.
 */
object HapticEffects {

    /**
     * Creates a haptic effect for switch toggle ON.
     * Sharp, precise click with subtle follow-up for premium devices.
     *
     * S22 Ultra: Sharp initial click (0.7) + soft tick (0.15) = satisfying toggle-on feel
     */
    fun switchToggleOn(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_TICK, profile.clickScale)
                .addPrimitive(PRIMITIVE_LOW_TICK, profile.tickScale * 0.3f, 10)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }

    /**
     * Creates a haptic effect for switch toggle OFF.
     * Softer, more subtle than toggle ON to differentiate the actions.
     *
     * S22 Ultra: Single soft tick (0.4) = gentle toggle-off feel
     */
    fun switchToggleOff(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_LOW_TICK, profile.tickScale * 0.8f)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }

    /**
     * Creates a haptic effect for slider texture feedback.
     * Very subtle, continuous feedback as the slider moves.
     * Returns null for devices without texture support.
     *
     * S22 Ultra: Ultra-fine tick (0.04) = smooth slider scrubbing
     */
    fun sliderTexture(profile: HapticProfile): VibrationEffect? {
        if (!profile.supportsPrimitives || profile.textureScale == 0f) {
            return null
        }

        return VibrationEffect.startComposition()
            .addPrimitive(PRIMITIVE_LOW_TICK, profile.textureScale)
            .compose()
    }

    /**
     * Creates a haptic effect for committing a slider value.
     * Stronger feedback to confirm the final selection.
     *
     * S22 Ultra: Medium tick (0.6) = clear confirmation
     */
    fun commitFeedback(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_TICK, profile.commitScale)
                .compose()
        } else {
            createPredefined(EFFECT_CLICK)
        }
    }

    /**
     * Creates a haptic effect for selecting a category card.
     * Double-tap pattern for premium feel.
     *
     * S22 Ultra: Sharp tick (0.63) + softer tick (0.35) = engaging selection
     */
    fun categorySelect(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_TICK, profile.clickScale * 0.9f)
                .addPrimitive(PRIMITIVE_TICK, profile.clickScale * 0.5f, 30)
                .compose()
        } else {
            createPredefined(EFFECT_CLICK)
        }
    }

    /**
     * Creates a haptic effect for expanding a section.
     * Rising intensity pattern to match the expansion animation.
     *
     * S22 Ultra: Gentle rise (0.3 -> 0.2 -> 0.15) = smooth expansion
     */
    fun sectionExpand(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_TICK, profile.tickScale * 0.6f)
                .addPrimitive(PRIMITIVE_LOW_TICK, profile.tickScale * 0.4f, 20)
                .addPrimitive(PRIMITIVE_LOW_TICK, profile.tickScale * 0.3f, 15)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }

    /**
     * Creates a haptic effect for collapsing a section.
     * Falling intensity pattern to match the collapse animation.
     *
     * S22 Ultra: Gentle fall (0.15 -> 0.25) = smooth collapse
     */
    fun sectionCollapse(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_LOW_TICK, profile.tickScale * 0.3f)
                .addPrimitive(PRIMITIVE_TICK, profile.tickScale * 0.5f, 15)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }

    /**
     * Creates a haptic effect for successful actions.
     * Uplifting double-tap pattern.
     *
     * S22 Ultra: Rising double-tap (0.42 -> 0.54) = positive confirmation
     */
    fun successFeedback(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_TICK, profile.commitScale * 0.7f)
                .addPrimitive(PRIMITIVE_TICK, profile.commitScale * 0.9f, 50)
                .compose()
        } else {
            createPredefined(EFFECT_DOUBLE_CLICK)
        }
    }

    /**
     * Creates a haptic effect for errors or negative feedback.
     * Heavier, more pronounced pattern to grab attention.
     *
     * S22 Ultra: Double thud (0.48 -> 0.36) = clear error indication
     */
    fun errorFeedback(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_THUD, profile.commitScale * 0.8f)
                .addPrimitive(PRIMITIVE_THUD, profile.commitScale * 0.6f, 100)
                .compose()
        } else {
            createPredefined(EFFECT_HEAVY_CLICK)
        }
    }

    /**
     * Creates a haptic effect for drag start.
     * Quick rise to indicate lift-off.
     */
    fun dragStart(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_QUICK_RISE, profile.clickScale * 0.8f)
                .compose()
        } else {
            createPredefined(EFFECT_CLICK)
        }
    }

    /**
     * Creates a haptic effect for drag drop/commit.
     * Quick fall to indicate placement.
     */
    fun dragDrop(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_QUICK_FALL, profile.commitScale * 0.7f)
                .compose()
        } else {
            createPredefined(EFFECT_CLICK)
        }
    }

    /**
     * Creates a haptic effect for loading or processing.
     * Spinning pattern for ongoing activity.
     */
    fun processing(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_SPIN, profile.tickScale * 0.5f)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }

    /**
     * Creates a haptic effect for a boundary hit (e.g., scroll to edge).
     * Subtle bump to indicate limit reached.
     */
    fun boundaryHit(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_THUD, profile.tickScale * 0.4f)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }

    /**
     * Creates a haptic effect for revealing content.
     * Slow rise for anticipation.
     */
    fun contentReveal(profile: HapticProfile): VibrationEffect {
        return if (profile.supportsPrimitives) {
            VibrationEffect.startComposition()
                .addPrimitive(PRIMITIVE_SLOW_RISE, profile.tickScale * 0.6f)
                .compose()
        } else {
            createPredefined(EFFECT_TICK)
        }
    }
}
