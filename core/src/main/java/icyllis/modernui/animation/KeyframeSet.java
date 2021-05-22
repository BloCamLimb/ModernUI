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

import javax.annotation.Nonnull;

public class KeyframeSet implements Keyframes {

    final Keyframe[] mKeyframes;
    TypeEvaluator<Object> mEvaluator;

    public KeyframeSet(@Nonnull Keyframe[] keyframes) {
        if (keyframes.length < 2)
            throw new IllegalArgumentException();
        mKeyframes = keyframes;
    }

    @Override
    public void setEvaluator(TypeEvaluator<Object> evaluator) {
        mEvaluator = evaluator;
    }

    @Override
    public Object getValue(float fraction) {
        final Keyframe[] keyframes = mKeyframes;
        final Interpolator interpolator;
        final int length = keyframes.length;
        if (length == 2) {
            final Keyframe nextKeyframe = keyframes[1];
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            return mEvaluator.evaluate(fraction, keyframes[0].getValue(),
                    nextKeyframe.getValue());
        }
        if (fraction <= 0f) {
            final Keyframe firstKeyframe = keyframes[0];
            final Keyframe nextKeyframe = keyframes[1];
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = firstKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) /
                    (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator.evaluate(intervalFraction, firstKeyframe.getValue(),
                    nextKeyframe.getValue());
        }
        if (fraction >= 1f) {
            final Keyframe prevKeyframe = keyframes[length - 2];
            final Keyframe lastKeyframe = keyframes[length - 1];
            interpolator = lastKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) /
                    (lastKeyframe.getFraction() - prevFraction);
            return mEvaluator.evaluate(intervalFraction, prevKeyframe.getValue(),
                    lastKeyframe.getValue());
        }
        Keyframe prevKeyframe = keyframes[0];
        for (int i = 1; i < length; ++i) {
            final Keyframe nextKeyframe = keyframes[i];
            if (fraction < nextKeyframe.getFraction()) {
                interpolator = nextKeyframe.getInterpolator();
                final float prevFraction = prevKeyframe.getFraction();
                float intervalFraction = (fraction - prevFraction) /
                        (nextKeyframe.getFraction() - prevFraction);
                if (interpolator != null) {
                    intervalFraction = interpolator.getInterpolation(intervalFraction);
                }
                return mEvaluator.evaluate(intervalFraction, prevKeyframe.getValue(),
                        nextKeyframe.getValue());
            }
            prevKeyframe = nextKeyframe;
        }
        return prevKeyframe.getValue();
    }

    @Override
    public Keyframe[] getKeyframes() {
        return mKeyframes;
    }
}

class IntKeyframeSet extends KeyframeSet implements Keyframes.IntKeyframes {

    public IntKeyframeSet(@Nonnull IntKeyframe[] keyframes) {
        super(keyframes);
    }

    @Override
    public Object getValue(float fraction) {
        return getIntValue(fraction);
    }

    @Override
    public int getIntValue(float fraction) {
        final Keyframe[] keyframes = mKeyframes;
        final Interpolator interpolator;
        final int length = keyframes.length;
        if (fraction <= 0f) {
            final IntKeyframe prevKeyframe = (IntKeyframe) keyframes[0];
            final IntKeyframe nextKeyframe = (IntKeyframe) keyframes[1];
            final int prevValue = prevKeyframe.getIntValue();
            final int nextValue = nextKeyframe.getIntValue();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) /
                    (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator == null ?
                    prevValue + (int) (intervalFraction * (nextValue - prevValue)) :
                    ((Number) mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).intValue();
        } else if (fraction >= 1f) {
            final IntKeyframe prevKeyframe = (IntKeyframe) keyframes[length - 2];
            final IntKeyframe nextKeyframe = (IntKeyframe) keyframes[length - 1];
            final int prevValue = prevKeyframe.getIntValue();
            final int nextValue = nextKeyframe.getIntValue();
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction) /
                    (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator == null ?
                    prevValue + (int) (intervalFraction * (nextValue - prevValue)) :
                    ((Number) mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).intValue();
        }
        IntKeyframe prevKeyframe = (IntKeyframe) keyframes[0];
        for (int i = 1; i < length; ++i) {
            final IntKeyframe nextKeyframe = (IntKeyframe) mKeyframes[i];
            if (fraction < nextKeyframe.getFraction()) {
                interpolator = nextKeyframe.getInterpolator();
                final float prevFraction = prevKeyframe.getFraction();
                float intervalFraction = (fraction - prevFraction) /
                        (nextKeyframe.getFraction() - prevFraction);
                final int prevValue = prevKeyframe.getIntValue();
                final int nextValue = nextKeyframe.getIntValue();
                if (interpolator != null) {
                    intervalFraction = interpolator.getInterpolation(intervalFraction);
                }
                return mEvaluator == null ?
                        prevValue + (int) (intervalFraction * (nextValue - prevValue)) :
                        ((Number) mEvaluator.evaluate(intervalFraction, prevValue, nextValue)).intValue();
            }
            prevKeyframe = nextKeyframe;
        }
        return prevKeyframe.getIntValue();
    }
}
