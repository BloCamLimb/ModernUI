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
import icyllis.modernui.animation.Keyframe.IntKeyframe;
import icyllis.modernui.animation.Keyframe.ObjectKeyframe;
import icyllis.modernui.util.Log;

import javax.annotation.Nonnull;

/**
 * This class holds a collection of Keyframe objects and is called by ValueAnimator to calculate
 * values between those keyframes for a given animation. The class internal to the animation
 * package because it is an implementation detail of how Keyframes are stored and used.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class KeyframeSet implements Keyframes {

    final Keyframe[] mKeyframes;
    TypeEvaluator mEvaluator;

    KeyframeSet(@Nonnull Keyframe... keyframes) {
        if (keyframes.length < 2) {
            throw new IllegalArgumentException("Keyframes < 2");
        }
        mKeyframes = keyframes;
    }

    @Nonnull
    public static IntKeyframeSet ofInt(@Nonnull int... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Length == 0");
        }
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
        return new IntKeyframeSet(keyframes);
    }

    @Nonnull
    public static FloatKeyframeSet ofFloat(@Nonnull float... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Length == 0");
        }
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
            Log.LOGGER.warn(Animator.MARKER, "Bad value (NaN) in float animator");
        }
        return new FloatKeyframeSet(keyframes);
    }

    @Nonnull
    public static Keyframes ofObject(@Nonnull Object... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Length == 0");
        }
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
        return new KeyframeSet(keyframes);
    }

    @Nonnull
    public static KeyframeSet ofKeyframe(@Nonnull Keyframe... keyframes) {
        if (keyframes.length < 2) {
            throw new IllegalArgumentException("Keyframes < 2");
        }
        boolean hasFloat = false;
        boolean hasInt = false;
        boolean hasOther = false;
        for (Keyframe keyframe : keyframes) {
            if (keyframe instanceof FloatKeyframe) {
                hasFloat = true;
            } else if (keyframe instanceof IntKeyframe) {
                hasInt = true;
            } else {
                hasOther = true;
            }
        }
        if (hasFloat && !hasInt && !hasOther) {
            return new FloatKeyframeSet(keyframes);
        } else if (hasInt && !hasFloat && !hasOther) {
            return new IntKeyframeSet(keyframes);
        } else {
            return new KeyframeSet(keyframes);
        }
    }

    @Override
    public void setEvaluator(TypeEvaluator<?> evaluator) {
        mEvaluator = evaluator;
    }

    @Override
    public KeyframeSet copy() {
        Keyframe[] keyframes = mKeyframes;
        int length = keyframes.length;
        final Keyframe[] newKeyframes = new Keyframe[length];
        for (int i = 0; i < length; ++i) {
            newKeyframes[i] = keyframes[i].copy();
        }
        return new KeyframeSet(newKeyframes);
    }

    @Override
    public Object getValue(float fraction) {
        final Keyframe[] keyframes = mKeyframes;
        final int length = keyframes.length;
        final TimeInterpolator interpolator;
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
                // Apply interpolator on the proportional duration.
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(" ");
        for (Keyframe keyframe : mKeyframes) {
            sb.append(keyframe.getValue()).append("  ");
        }
        return sb.toString();
    }
}
