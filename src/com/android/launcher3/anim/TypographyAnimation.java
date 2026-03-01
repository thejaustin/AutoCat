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

import android.animation.ValueAnimator;
import android.widget.TextView;
import androidx.dynamicanimation.animation.SpringForce;

/**
 * Utility to animate variable font axes (like 'wght') for M3E expressive typography.
 */
public class TypographyAnimation {

    private final TextView mTarget;
    private ValueAnimator mWeightAnimator;
    private float mCurrentWeight = 400f;

    public TypographyAnimation(TextView target) {
        mTarget = target;
    }

    public void animateWeight(float targetWeight) {
        if (mWeightAnimator != null) {
            mWeightAnimator.cancel();
        }

        mWeightAnimator = ValueAnimator.ofFloat(mCurrentWeight, targetWeight);
        mWeightAnimator.setDuration(200);
        mWeightAnimator.addUpdateListener(animation -> {
            mCurrentWeight = (float) animation.getAnimatedValue();
            updateVariationSettings();
        });
        mWeightAnimator.start();
    }

    private void updateVariationSettings() {
        mTarget.setFontVariationSettings("'wght' " + mCurrentWeight);
    }

    public void reset() {
        if (mWeightAnimator != null) {
            mWeightAnimator.cancel();
        }
        mCurrentWeight = 400f;
        updateVariationSettings();
    }
}
