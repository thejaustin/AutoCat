package app.lawnchair.ui.preferences.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK
import android.os.Vibrator
import androidx.core.content.getSystemService
import com.android.launcher3.Utilities

/**
 * Haptic profile for a specific device or device category.
 * Contains tuned intensity scales for different types of haptic feedback.
 *
 * @param clickScale Intensity for button clicks and taps (0.0-1.0)
 * @param tickScale Intensity for subtle ticks and incremental feedback (0.0-1.0)
 * @param textureScale Intensity for continuous texture feedback like sliders (0.0-1.0)
 * @param commitScale Intensity for commit/confirmation feedback (0.0-1.0)
 * @param supportsPrimitives Whether the device supports haptic primitives (Android S+)
 */
data class HapticProfile(
    val clickScale: Float,
    val tickScale: Float,
    val textureScale: Float,
    val commitScale: Float,
    val supportsPrimitives: Boolean,
)

/**
 * Collection of haptic profiles for different device types.
 * Each profile is tuned for the specific haptic engine characteristics of the device.
 */
object PremiumHapticProfiles {

    /**
     * Samsung Galaxy S22 Ultra, S23 Ultra, S24 Ultra - Strong linear motor.
     * Tuned for sharp, precise haptic feedback that leverages Samsung's excellent haptic engine.
     */
    val SAMSUNG_FLAGSHIP = HapticProfile(
        clickScale = 0.7f,      // Sharp, responsive clicks
        tickScale = 0.5f,       // Subtle but noticeable ticks
        textureScale = 0.04f,   // Fine-grained texture for sliders
        commitScale = 0.6f,     // Satisfying confirmation feedback
        supportsPrimitives = true,
    )

    /**
     * Samsung Galaxy Z Fold 3+, Z Flip 3+ - Premium foldables.
     * Slightly lighter feedback than Ultra series for device portability.
     */
    val SAMSUNG_FOLDABLE = HapticProfile(
        clickScale = 0.65f,
        tickScale = 0.45f,
        textureScale = 0.035f,
        commitScale = 0.55f,
        supportsPrimitives = true,
    )

    /**
     * Google Pixel 6+, 7+, 8+ - Excellent haptic engine.
     * Tuned for Google's vibration motor characteristics - crisp and clear.
     */
    val PIXEL_PREMIUM = HapticProfile(
        clickScale = 0.8f,      // Slightly stronger for Pixel's motor
        tickScale = 0.6f,       // Clear, distinct ticks
        textureScale = 0.05f,   // Rich texture feedback
        commitScale = 0.7f,     // Strong confirmation
        supportsPrimitives = true,
    )

    /**
     * OnePlus 9+, 10+, 11+ - Linear motor.
     * Tuned for OnePlus X-axis linear motor.
     */
    val ONEPLUS_FLAGSHIP = HapticProfile(
        clickScale = 0.6f,      // Lighter for OnePlus motor
        tickScale = 0.4f,       // Subtle ticks
        textureScale = 0.03f,   // Fine texture
        commitScale = 0.5f,     // Balanced commit
        supportsPrimitives = true,
    )

    /**
     * Xiaomi 12+, 13+, 14+ flagship series.
     * Tuned for Xiaomi's premium haptic motors.
     */
    val XIAOMI_FLAGSHIP = HapticProfile(
        clickScale = 0.75f,
        tickScale = 0.55f,
        textureScale = 0.045f,
        commitScale = 0.65f,
        supportsPrimitives = true,
    )

    /**
     * OPPO Find X5+, X6+, X7+ and other premium OPPO devices.
     */
    val OPPO_FLAGSHIP = HapticProfile(
        clickScale = 0.65f,
        tickScale = 0.45f,
        textureScale = 0.035f,
        commitScale = 0.55f,
        supportsPrimitives = true,
    )

    /**
     * Standard profile for devices without advanced haptic capabilities.
     * Falls back to basic vibration patterns.
     */
    val STANDARD = HapticProfile(
        clickScale = 1.0f,      // Full strength for basic motors
        tickScale = 0.9f,       // Near-full strength
        textureScale = 0f,      // No texture support on basic devices
        commitScale = 1.0f,     // Full confirmation
        supportsPrimitives = false,
    )

    /**
     * Detects the appropriate haptic profile for the current device.
     * Uses manufacturer and model information to select the best tuned profile.
     *
     * @param context Application context for accessing system services
     * @return HapticProfile tailored to the device or STANDARD fallback
     */
    fun detectProfile(context: Context): HapticProfile {
        // Check if device supports primitives (Android S+)
        if (!Utilities.ATLEAST_S) {
            return STANDARD
        }

        val vibrator = context.getSystemService<Vibrator>() ?: return STANDARD
        val supportsPrimitives = vibrator.areAllPrimitivesSupported(PRIMITIVE_LOW_TICK)

        if (!supportsPrimitives) {
            return STANDARD
        }

        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()

        return when {
            // Samsung devices
            manufacturer == "samsung" -> when {
                // S22/S23/S24 Ultra series
                model.contains("s22") && model.contains("ultra") -> SAMSUNG_FLAGSHIP
                model.contains("s23") && model.contains("ultra") -> SAMSUNG_FLAGSHIP
                model.contains("s24") && model.contains("ultra") -> SAMSUNG_FLAGSHIP

                // S22/S23/S24 standard and Plus
                model.contains("s22") -> SAMSUNG_FLAGSHIP
                model.contains("s23") -> SAMSUNG_FLAGSHIP
                model.contains("s24") -> SAMSUNG_FLAGSHIP

                // Foldables
                model.contains("fold") -> SAMSUNG_FOLDABLE
                model.contains("flip") -> SAMSUNG_FOLDABLE

                // Other premium Samsung devices
                model.contains("note") -> SAMSUNG_FLAGSHIP

                else -> STANDARD.copy(supportsPrimitives = true)
            }

            // Google Pixel devices
            manufacturer == "google" -> when {
                model.contains("pixel") && (
                    model.contains("6") ||
                    model.contains("7") ||
                    model.contains("8") ||
                    model.contains("9")
                ) -> PIXEL_PREMIUM

                else -> STANDARD.copy(supportsPrimitives = true)
            }

            // OnePlus devices
            manufacturer == "oneplus" -> when {
                model.contains("9") ||
                model.contains("10") ||
                model.contains("11") ||
                model.contains("12")
                -> ONEPLUS_FLAGSHIP

                else -> STANDARD.copy(supportsPrimitives = true)
            }

            // Xiaomi devices
            manufacturer == "xiaomi" || manufacturer == "redmi" -> when {
                model.contains("12") ||
                model.contains("13") ||
                model.contains("14") ||
                model.contains("mi 11")
                -> XIAOMI_FLAGSHIP

                else -> STANDARD.copy(supportsPrimitives = true)
            }

            // OPPO devices
            manufacturer == "oppo" -> when {
                model.contains("find x") ||
                model.contains("reno")
                -> OPPO_FLAGSHIP

                else -> STANDARD.copy(supportsPrimitives = true)
            }

            // Default fallback with primitive support
            else -> STANDARD.copy(supportsPrimitives = true)
        }
    }

    /**
     * Gets a human-readable description of the detected profile.
     *
     * @param profile The haptic profile
     * @return String description of the profile
     */
    fun getProfileDescription(profile: HapticProfile): String {
        return when (profile) {
            SAMSUNG_FLAGSHIP -> "Samsung Flagship (Optimized)"
            SAMSUNG_FOLDABLE -> "Samsung Foldable (Optimized)"
            PIXEL_PREMIUM -> "Google Pixel (Optimized)"
            ONEPLUS_FLAGSHIP -> "OnePlus Flagship (Optimized)"
            XIAOMI_FLAGSHIP -> "Xiaomi Flagship (Optimized)"
            OPPO_FLAGSHIP -> "OPPO Flagship (Optimized)"
            else -> if (profile.supportsPrimitives) "Premium Device" else "Standard Device"
        }
    }
}
