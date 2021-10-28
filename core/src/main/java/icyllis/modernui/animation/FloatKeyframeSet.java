/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.animation;

import icyllis.modernui.animation.Keyframe.FloatKeyframe;

import javax.annotation.Nonnull;

/**
 * This class holds a collection of FloatKeyframe objects and is called by ValueAnimator to calculate
 * values between those keyframes for a given animation. The class internal to the animation
 * package because it is an implementation detail of how Keyframes are stored and used.
 *
 * <p>This type-specific subclass of KeyframeSet, along with the other type-specific subclass for
 * int, exists to speed up the getValue() method when there is no custom
 * TypeEvaluator set for the animation, so that values can be calculated without autoboxing to the
 * Object equivalents of these primitive types.</p>
 */
class FloatKeyframeSet extends KeyframeSet<Float> implements Keyframes.FloatKeyframes {

    public FloatKeyframeSet(@Nonnull FloatKeyframe[] keyframes) {
        super(keyframes);
    }

    @Override
    public FloatKeyframeSet copy() {
        final int numKeyframes = mKeyframes.length;
        FloatKeyframe[] newKeyframes = new FloatKeyframe[numKeyframes];
        for (int i = 0; i < numKeyframes; ++i) {
            newKeyframes[i] = (FloatKeyframe) mKeyframes[i].copy();
        }
        return new FloatKeyframeSet(newKeyframes);
    }

    @Override
    public float getFloatValue(float fraction) {
        final Keyframe[] keyframes = mKeyframes;
        final Interpolator interpolator;
        final int length = keyframes.length;
        if (fraction <= 0f) {
            final FloatKeyframe prevKeyframe = (FloatKeyframe) keyframes[0];
            final FloatKeyframe nextKeyframe = (FloatKeyframe) keyframes[1];
            final float prevValue = prevKeyframe.getFloatValue();
            final float nextValue = nextKeyframe.getFloatValue();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) /
                    (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator == null ?
                    prevValue + intervalFraction * (nextValue - prevValue) :
                    mEvaluator.evaluate(intervalFraction, prevValue, nextValue);
        } else if (fraction >= 1f) {
            final FloatKeyframe prevKeyframe = (FloatKeyframe) keyframes[length - 2];
            final FloatKeyframe nextKeyframe = (FloatKeyframe) keyframes[length - 1];
            final float prevValue = prevKeyframe.getFloatValue();
            final float nextValue = nextKeyframe.getFloatValue();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) /
                    (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator == null ?
                    prevValue + intervalFraction * (nextValue - prevValue) :
                    mEvaluator.evaluate(intervalFraction, prevValue, nextValue);
        }
        FloatKeyframe prevKeyframe = (FloatKeyframe) keyframes[0];
        for (int i = 1; i < length; ++i) {
            final FloatKeyframe nextKeyframe = (FloatKeyframe) keyframes[i];
            if (fraction < nextKeyframe.getFraction()) {
                interpolator = nextKeyframe.getInterpolator();
                final float prevFraction = prevKeyframe.getFraction();
                float intervalFraction = (fraction - prevFraction) /
                        (nextKeyframe.getFraction() - prevFraction);
                final float prevValue = prevKeyframe.getFloatValue();
                final float nextValue = nextKeyframe.getFloatValue();
                if (interpolator != null) {
                    intervalFraction = interpolator.getInterpolation(intervalFraction);
                }
                return mEvaluator == null ?
                        prevValue + intervalFraction * (nextValue - prevValue) :
                        mEvaluator.evaluate(intervalFraction, prevValue, nextValue);
            }
            prevKeyframe = nextKeyframe;
        }
        return prevKeyframe.getFloatValue();
    }
}
