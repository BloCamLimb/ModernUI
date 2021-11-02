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

package icyllis.modernui.mcgui;

import icyllis.modernui.animation.TimeInterpolator;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;

/**
 * Scroll smoothly in one orientation. Mostly deprecated.
 *
 * @since 1.6
 */
public class ScrollController {

    private float startValue;
    private float targetValue;

    private float maxValue;

    private float currValue;

    private long startTime;
    private int duration;

    @Nonnull
    private final IListener listener;

    public ScrollController(@Nonnull IListener listener) {
        this.listener = listener;
    }

    /**
     * Compute and update the scroll offset
     *
     * @param time current drawing time
     */
    public void update(long time) {
        if (currValue != targetValue) {
            float p = Math.min((float) (time - startTime) / duration, 1);
            p = TimeInterpolator.DECELERATE.getInterpolation(p);
            currValue = Mth.lerp(p, startValue, targetValue);
            listener.onScrollAmountUpdated(this, currValue);
            //UIManager.getInstance().repostCursorEvent();
        }
    }

    /**
     * Set max value can scroll
     *
     * @param max scroll range
     */
    public void setMaxScroll(float max) {
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
    public boolean scrollBy(float delta) {
        return scrollTo(targetValue + delta);
    }

    /**
     * Start a scroll with targets
     *
     * @param target   the target scroll amount
     * @param duration scroll duration in milliseconds
     */
    public void scrollTo(float target, int duration) {
        startTime = UIManager.sInstance.getElapsedTime();
        startValue = currValue;
        targetValue = Mth.clamp(target, 0, maxValue);
        this.duration = duration;
    }

    /**
     * Start a scroll with targets
     *
     * @param target the target scroll amount
     */
    public boolean scrollTo(float target) {
        float lastTime = startTime;
        startTime = UIManager.sInstance.getElapsedTime();
        startValue = currValue;
        targetValue = Mth.clamp(target, 0, maxValue);
        if (startValue == targetValue) {
            return false;
        }

        // smooth
        float dis = Math.abs(targetValue - currValue);
        if (dis > 120.0) {
            duration = (int) (Math.sqrt(dis / 120.0) * 200.0);
        } else {
            duration = 200;
        }
        // fast scroll
        float det = startTime - lastTime;
        if (det < 120.0) {
            duration *= (det / 600.0f) + 0.8f;
        }
        return true;
    }

    /**
     * Stops the animation. Aborting the animating cause the scroller to move
     * to the target position
     */
    public void abortAnimation() {
        currValue = targetValue;
        listener.onScrollAmountUpdated(this, currValue);
        //UIManager.getInstance().repostCursorEvent();
    }

    public boolean isScrolling() {
        return currValue != targetValue;
    }

    @FunctionalInterface
    public interface IListener {

        /**
         * Apply the scroll value to listener
         *
         * @param controller scroller to call the method
         * @param amount     current scroll amount
         */
        void onScrollAmountUpdated(ScrollController controller, float amount);
    }
}
