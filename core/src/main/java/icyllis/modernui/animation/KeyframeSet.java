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

import icyllis.modernui.ModernUI;
import icyllis.modernui.animation.Keyframe.FloatKeyframe;
import icyllis.modernui.animation.Keyframe.IntKeyframe;
import icyllis.modernui.animation.Keyframe.ObjectKeyframe;

import javax.annotation.Nonnull;

/**
 * This class holds a collection of Keyframe objects and is called by ValueAnimator to calculate
 * values between those keyframes for a given animation. The class internal to the animation
 * package because it is an implementation detail of how Keyframes are stored and used.
 */
public class KeyframeSet<T> implements Keyframes<T> {

    final Keyframe[] mKeyframes;
    TypeEvaluator<T> mEvaluator;

    public KeyframeSet(@Nonnull Keyframe[] keyframes) {
        if (keyframes.length < 2)
            throw new IllegalArgumentException();
        mKeyframes = keyframes;
    }

    @Nonnull
    public static KeyframeSet<Integer> ofInt(@Nonnull int[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException();
        final int length = values.length;
        final Keyframe[] keyframes = new IntKeyframe[Math.max(length, 2)];
        if (length == 1) {
            keyframes[0] = Keyframe.ofInt(0f);
            keyframes[1] = Keyframe.ofInt(1f, values[0]);
        } else {
            for (int i = 0; i < length; i++) {
                keyframes[i] = Keyframe.ofInt((float) i / (length - 1), values[i]);
            }
        }
        return new IntKeyframeSet((IntKeyframe[]) keyframes);
    }

    @Nonnull
    public static KeyframeSet<Float> ofFloat(@Nonnull float[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException();
        boolean badValue = false;
        final int length = values.length;
        final Keyframe[] keyframes = new FloatKeyframe[Math.max(length, 2)];
        if (length == 1) {
            keyframes[0] = Keyframe.ofFloat(0f);
            keyframes[1] = Keyframe.ofFloat(1f, values[0]);
            if (Float.isNaN(values[0])) {
                badValue = true;
            }
        } else {
            for (int i = 0; i < length; i++) {
                keyframes[i] = Keyframe.ofFloat((float) i / (length - 1), values[i]);
                if (Float.isNaN(values[i])) {
                    badValue = true;
                }
            }
        }
        if (badValue) {
            ModernUI.LOGGER.warn(Animator.MARKER, "Bad value (NaN) in float animator");
        }
        return new FloatKeyframeSet((FloatKeyframe[]) keyframes);
    }

    @Nonnull
    public static <T> KeyframeSet<T> ofObject(@Nonnull T[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException();
        final int length = values.length;
        final Keyframe[] keyframes = new ObjectKeyframe[Math.max(length, 2)];
        if (length == 1) {
            keyframes[0] = Keyframe.ofObject(0f);
            keyframes[1] = Keyframe.ofObject(1f, values[0]);
        } else {
            for (int i = 0; i < length; i++) {
                keyframes[i] = Keyframe.ofObject((float) i / (length - 1), values[i]);
            }
        }
        return new KeyframeSet<>(keyframes);
    }

    @Override
    public void setEvaluator(TypeEvaluator<T> evaluator) {
        mEvaluator = evaluator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getValue(float fraction) {
        final Keyframe[] keyframes = mKeyframes;
        final Interpolator interpolator;
        final int length = keyframes.length;
        if (length == 2) {
            final Keyframe nextKeyframe = keyframes[1];
            interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            return mEvaluator.evaluate(fraction, (T) keyframes[0].getValue(),
                    (T) nextKeyframe.getValue());
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
            return mEvaluator.evaluate(intervalFraction, (T) firstKeyframe.getValue(),
                    (T) nextKeyframe.getValue());
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
            return mEvaluator.evaluate(intervalFraction, (T) prevKeyframe.getValue(),
                    (T) lastKeyframe.getValue());
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
                return mEvaluator.evaluate(intervalFraction, (T) prevKeyframe.getValue(),
                        (T) nextKeyframe.getValue());
            }
            prevKeyframe = nextKeyframe;
        }
        return (T) prevKeyframe.getValue();
    }

    @Override
    public Keyframe[] getKeyframes() {
        return mKeyframes;
    }
}
