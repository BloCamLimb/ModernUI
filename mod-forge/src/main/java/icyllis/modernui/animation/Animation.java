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

import icyllis.modernui.platform.RenderCore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// the old animation API and not encouraged
public class Animation implements AnimationHandler.FrameCallback {

    private final int duration;

    private long startTime;

    private int delayTime;

    @Nullable
    private List<Applier> appliers;

    @Nullable
    private List<IListener> listeners;

    private boolean started = false;

    private boolean reversed = false;

    /**
     * New animation with fixed duration
     *
     * @param duration in milliseconds
     */
    public Animation(int duration) {
        this.duration = duration;
    }

    public Animation applyTo(@Nonnull Applier applier) {
        if (appliers == null) {
            appliers = new ArrayList<>();
        }
        appliers.add(applier);
        return this;
    }

    public Animation applyTo(@Nonnull Applier... appliers) {
        if (this.appliers == null) {
            this.appliers = new ArrayList<>();
        }
        Collections.addAll(this.appliers, appliers);
        return this;
    }

    /**
     * Milliseconds
     */
    public Animation withDelay(int delay) {
        delayTime = delay;
        return this;
    }

    public void listen(IListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        listeners.add(listener);
    }

    /**
     * Play the animation depends on current value with full duration
     */
    public void start() {
        start0(false);
    }

    /**
     * Play the animation depends on applier original properties
     */
    public void startFull() {
        start0(true);
    }

    private void start0(boolean isFull) {
        startTime = RenderCore.timeMillis() + delayTime;
        reversed = false;
        started = false;
        if (appliers != null) {
            appliers.forEach(e -> e.record(reversed, isFull));
        }
        AnimationHandler.getInstance().register(this, 0);
    }

    /**
     * Play the inverted animation whose appliers depending on current value with full duration
     * For example, an applier with
     * 0 -> 1, duration: 400ms with standard accelerate interpolator (slow -> fast),
     * call invert() at 200ms after start(), it will change to 0.25 -> 0,
     * duration: 400ms (slow -> fast, as well. not fast -> slow because it's not reversed)
     */
    public void invert() {
        invert0(false);
    }

    public void invertFull() {
        invert0(true);
    }

    private void invert0(boolean isFull) {
        startTime = RenderCore.timeMillis() + delayTime;
        reversed = true;
        started = false;
        if (appliers != null) {
            appliers.forEach(e -> e.record(reversed, isFull));
        }
        AnimationHandler.getInstance().register(this, 0);
    }

    /**
     * Cancel the animation
     */
    public void cancel() {
        started = false;
        AnimationHandler.getInstance().unregister(this);
    }

    public void skipToStart() {
        cancel();
        if (appliers != null) {
            appliers.forEach(e -> {
                e.record(true, false);
                e.update(1);
            });
        }
    }

    public void skipToEnd() {
        cancel();
        if (appliers != null) {
            appliers.forEach(e -> {
                e.record(false, false);
                e.update(1);
            });
        }
    }

    @Override
    public void doAnimationFrame(long frameTime) {
        if (frameTime <= startTime) {
            return;
        }
        if (!started) {
            started = true;
            if (listeners != null) {
                listeners.forEach(e -> e.onAnimationStart(this, reversed));
            }
        }
        float p = Math.min((float) (frameTime - startTime) / duration, 1);
        if (appliers != null) {
            appliers.forEach(e -> e.update(p));
        }
        if (p == 1) {
            started = false;
            if (listeners != null) {
                listeners.forEach(e -> e.onAnimationEnd(this, reversed));
            }
            AnimationHandler.getInstance().unregister(this);
        }
    }

    public int getDuration() {
        return duration;
    }

    public interface IListener {

        /**
         * Called when animation started by user
         *
         * @param animation started animation
         * @param isReverse whether to play reverse animation
         */
        default void onAnimationStart(@Nonnull Animation animation, boolean isReverse) {

        }

        /**
         * Called at the end of the animation
         *
         * @param animation ended animation
         * @param isReverse whether to play reverse animation
         */
        default void onAnimationEnd(@Nonnull Animation animation, boolean isReverse) {

        }
    }
}
