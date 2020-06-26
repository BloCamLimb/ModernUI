/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.widget;

import icyllis.modernui.ui.animation.ITimeInterpolator;
import icyllis.modernui.ui.master.UIManager;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Make scrollable view to scroll smoothly in one orientation
 *
 * @since 1.6
 */
@SuppressWarnings("unused")
public class Scroller {

    private float startValue;
    private float targetValue;

    private float maxValue;

    private float currValue;

    private int startTime;
    private int duration;

    /**
     * Use lambda
     */
    @Nullable
    private final ICallback callback;

    @Nonnull
    private final ITimeInterpolator interpolator;

    public Scroller() {
        this(ITimeInterpolator.VISCOUS_FLUID);
    }

    public Scroller(@Nonnull ICallback callback) {
        this(callback, ITimeInterpolator.VISCOUS_FLUID);
    }

    public Scroller(@Nonnull ITimeInterpolator interpolator) {
        this(null, interpolator);
    }

    public Scroller(@Nullable ICallback callback, @Nonnull ITimeInterpolator interpolator) {
        this.callback = callback;
        this.interpolator = interpolator;
    }

    /**
     * Compute and update the scroll offset
     *
     * @param time current drawing time
     */
    public void update(int time) {
        if (currValue != targetValue) {
            float p = Math.min((float) (time - startTime) / duration, 1);
            p = interpolator.getInterpolation(p);
            currValue = MathHelper.lerp(p, startValue, targetValue);
            if (callback != null) {
                callback.applyScrollAmount(currValue);
            }
        }
    }

    /**
     * Set max value can scroll
     *
     * @param max max value
     */
    public void setMaxValue(float max) {
        maxValue = max;
    }

    /**
     * Set start value
     *
     * @param start start value
     */
    public void setStartValue(float start) {
        startValue = start;
    }

    /**
     * Get current scroll amount
     *
     * @return current scroll amount
     */
    public float getCurrValue() {
        return currValue;
    }

    /**
     * Returns how long the scroll event will take, in milliseconds.
     *
     * @return the duration of the scroll in milliseconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Start a scroll with changes
     *
     * @param delta    the change from previous target to new target
     * @param duration scroll duration in milliseconds
     */
    public void scrollBy(float delta, int duration) {
        scrollTo(targetValue + delta, duration);
    }

    /**
     * Start a scroll with changes
     *
     * @param delta the change from previous target to new target
     */
    public void scrollBy(float delta) {
        scrollTo(targetValue + delta);
    }

    /**
     * Start a scroll with targets
     *
     * @param target   the target scroll amount
     * @param duration scroll duration in milliseconds
     */
    public void scrollTo(float target, int duration) {
        startTime = UIManager.INSTANCE.getDrawingTime();
        startValue = currValue;
        float endX = MathHelper.clamp(target, 0, maxValue) * 2.0f;
        targetValue = Math.round(endX) / 2.0f;
        this.duration = duration;
    }

    /**
     * Start a scroll with targets
     *
     * @param target the target scroll amount
     */
    public void scrollTo(float target) {
        startTime = UIManager.INSTANCE.getDrawingTime();
        startValue = currValue;
        float end = MathHelper.clamp(target, 0, maxValue) * 2.0f;
        targetValue = Math.round(end) / 2.0f;
        float dis = Math.abs(targetValue - currValue);
        if (dis > 300.0f) {
            duration = (int) (dis * 1.6f);
        } else if (dis > 66.0f) {
            duration = (int) (Math.sqrt(dis * 4.0f) * 16.0f);
        } else {
            duration = 300;
        }
    }

    /**
     * Stops the animation. Aborting the animating cause the scroller to move
     * to the target position
     */
    public void abortAnimation() {
        currValue = targetValue;
        if (callback != null) {
            callback.applyScrollAmount(currValue);
        }
    }

    /**
     * Set target value dynamically
     *
     * @param target tart value
     * @see #extendDuration(int)
     */
    public void setTargetValue(float target) {
        targetValue = target;
    }

    /**
     * Extend the scroll animation. This allows a running animation to scroll
     * further and longer, when used with {@link #setTargetValue(float)}.
     *
     * @param extend additional time to scroll in milliseconds
     */
    public void extendDuration(int extend) {
        int passed = UIManager.INSTANCE.getDrawingTime() - startTime;
        duration = passed + extend;
    }

    @FunctionalInterface
    public interface ICallback {

        /**
         * Apply the scroll value to somewhere
         *
         * @param scrollAmount current scroll amount
         */
        void applyScrollAmount(float scrollAmount);
    }
}
