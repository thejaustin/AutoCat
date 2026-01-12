/*
 * Copyright 2026 The AutoCat Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.lawnchair.animation

/**
 * Material 3 Expressive spring configurations following Google's official specifications.
 *
 * Material 3 Expressive (M3E) introduces research-backed spring animations that make
 * interactions feel more alive, fluid, and natural. These configurations follow the
 * exact parameters specified in Google's M3E Motion Theming guidelines.
 *
 * Reference: https://m3.material.io/blog/m3-expressive-motion-theming
 *
 * ## Usage
 *
 * ### Expressive Spatial Springs
 * Use for animating position, size, orientation, or shape properties. These springs
 * have lower damping ratios (0.6-0.8) which creates noticeable overshoot and bounce
 * for an expressive feel.
 *
 * ```kotlin
 * // Quick, bouncy response
 * SpringAnimation(view, DynamicAnimation.SCALE_X, targetScale).apply {
 *     spring.stiffness = M3ESpringConfig.ExpressiveSpatial.FAST.stiffness
 *     spring.dampingRatio = M3ESpringConfig.ExpressiveSpatial.FAST.dampingRatio
 * }.start()
 * ```
 *
 * ### Standard Effects Springs
 * Use for animating color or opacity properties. These springs have high damping (1.0)
 * to resolve quickly without bouncing, which is more appropriate for non-spatial changes.
 *
 * ```kotlin
 * // Smooth fade without bounce
 * view.physicsAnimator.spring(
 *     DynamicAnimation.ALPHA,
 *     targetAlpha,
 *     M3ESpringConfig.StandardEffects.FAST
 * ).start()
 * ```
 *
 * @see PhysicsAnimator
 */
object M3ESpringConfig {
    /**
     * Expressive Spatial springs for position, size, orientation, and shape animations.
     *
     * These springs have lower damping ratios (0.6-0.8) which allows for noticeable
     * overshoot and bounce, creating the "expressive" feel that characterizes M3E.
     *
     * **When to use:**
     * - View position (translation X/Y/Z)
     * - View size (scale X/Y)
     * - View rotation
     * - Shape morphing
     * - Layout changes
     */
    object ExpressiveSpatial {
        /**
         * Fast expressive spatial spring.
         *
         * Creates quick, bouncy animations with the most pronounced overshoot.
         * Best for immediate UI responses like button presses or tab selections.
         *
         * **Parameters:**
         * - Stiffness: 800 (higher stiffness = faster response)
         * - Damping: 0.6 (lower damping = more bounce)
         *
         * **Typical duration:** ~300-400ms with 1-2 bounces
         */
        val FAST = SpringConfig(stiffness = 800f, dampingRatio = 0.6f)

        /**
         * Default expressive spatial spring.
         *
         * Creates moderate-speed animations with noticeable but controlled bounce.
         * This is the recommended spring for most spatial animations in M3E.
         *
         * **Parameters:**
         * - Stiffness: 380 (balanced response time)
         * - Damping: 0.8 (moderate bounce)
         *
         * **Typical duration:** ~400-600ms with 1 bounce
         */
        val DEFAULT = SpringConfig(stiffness = 380f, dampingRatio = 0.8f)

        /**
         * Slow expressive spatial spring.
         *
         * Creates smooth, graceful animations with subtle bounce.
         * Best for large transitions like drawer opening or page changes.
         *
         * **Parameters:**
         * - Stiffness: 200 (slower, more gentle response)
         * - Damping: 0.8 (moderate bounce)
         *
         * **Typical duration:** ~600-800ms with 1 bounce
         */
        val SLOW = SpringConfig(stiffness = 200f, dampingRatio = 0.8f)
    }

    /**
     * Standard Effects springs for color and opacity animations.
     *
     * These springs have high damping (1.0) which eliminates bounce entirely.
     * This is appropriate for non-spatial properties where overshooting would
     * be visually jarring (e.g., an alpha value going above 1.0 or below 0.0).
     *
     * **When to use:**
     * - Color transitions
     * - Opacity (alpha) fades
     * - Blur amount
     * - Brightness/contrast
     * - Any property where overshoot is undesirable
     */
    object StandardEffects {
        /**
         * Fast standard effects spring.
         *
         * Creates very quick, smooth transitions without any bounce.
         * Best for fading elements in/out or quick color changes.
         *
         * **Parameters:**
         * - Stiffness: 3800 (very high for instant response)
         * - Damping: 1.0 (critically damped, no bounce)
         *
         * **Typical duration:** ~150-250ms with no overshoot
         */
        val FAST = SpringConfig(stiffness = 3800f, dampingRatio = 1.0f)
    }

    /**
     * Spring configuration data class.
     *
     * @param stiffness The spring stiffness constant. Higher values create faster,
     *                  more energetic motion. Range: 1-5000 (typical: 200-800)
     * @param dampingRatio The spring damping ratio. Values < 1.0 create bounce,
     *                     1.0 is critically damped (no bounce), > 1.0 is overdamped.
     *                     Range: 0.0-1.0+ (M3E uses 0.6-1.0)
     */
    data class SpringConfig(
        val stiffness: Float,
        val dampingRatio: Float,
    )
}
