/*
 * Copyright (C) 2026 AutoCat Launcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.anim;

import android.view.HapticFeedbackConstants;
import android.view.View;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Utility class to apply M3E "Squishy" spring animations to views.
 * Squashes X and bulges Y on press, then springs back on release.
 */
public class SquishAnimation {

    private static final float SQUASH_X = 0.92f;
    private static final float BULGE_Y = 1.06f;

    private final View mTarget;
    private final SpringAnimation mSpringX;
    private final SpringAnimation mSpringY;

    public SquishAnimation(View target) {
        mTarget = target;
        mSpringX = new SpringAnimation(target, DynamicAnimation.SCALE_X, 1f);
        mSpringY = new SpringAnimation(target, DynamicAnimation.SCALE_Y, 1f);

        setupSpring(mSpringX);
        setupSpring(mSpringY);
    }

    private void setupSpring(SpringAnimation anim) {
        anim.setSpring(new SpringForce()
                .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                .setStiffness(SpringForce.STIFFNESS_MEDIUM));
    }

    public void animatePressed(boolean isPressed) {
        if (isPressed) {
            mSpringX.animateToFinalPosition(SQUASH_X);
            mSpringY.animateToFinalPosition(BULGE_Y);
            mTarget.performHapticFeedback(HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE);
        } else {
            mSpringX.animateToFinalPosition(1f);
            mSpringY.animateToFinalPosition(1f);
        }
    }

    public void reset() {
        mSpringX.cancel();
        mSpringY.cancel();
        mSpringX.skipToEnd();
        mSpringY.skipToEnd();
    }
}
