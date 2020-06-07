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

package icyllis.modernui.ui.animation;

import com.google.common.collect.Lists;
import icyllis.modernui.ui.master.UIManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class Animation implements IAnimation {

    private final float duration;

    private float startTime;

    private float delayTime;

    @Nullable
    private List<Applier> appliers;

    @Nullable
    private List<IListener> listeners;

    private boolean waiting = true;

    private boolean started = false;

    private boolean reversed = false;

    /**
     * New animation with fixed duration
     *
     * @param duration milliseconds
     */
    public Animation(int duration) {
        // convert ms to tick
        this.duration = duration / 50.0f;
    }

    public Animation applyTo(@Nonnull Applier applier) {
        if (appliers == null) {
            appliers = Lists.newArrayList();
        }
        appliers.add(applier);
        return this;
    }

    public Animation applyTo(@Nonnull Applier... appliers) {
        if (this.appliers == null) {
            this.appliers = Lists.newArrayList();
        }
        Collections.addAll(this.appliers, appliers);
        return this;
    }

    /**
     * Milliseconds
     */
    public Animation withDelay(int delay) {
        delayTime = delay / 50.0f;
        return this;
    }

    public Animation listen(IListener listener) {
        if (listeners == null) {
            listeners = Lists.newArrayList();
        }
        listeners.add(listener);
        return this;
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
        startTime = UIManager.INSTANCE.getAnimationTime() + delayTime;
        waiting = false;
        reversed = false;
        started = false;
        if (appliers != null) {
            appliers.forEach(e -> e.record(reversed, isFull));
        }
        UIManager.INSTANCE.addAnimation(this);
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
        startTime = UIManager.INSTANCE.getAnimationTime() + delayTime;
        waiting = false;
        reversed = true;
        started = false;
        if (appliers != null) {
            appliers.forEach(e -> e.record(reversed, isFull));
        }
        UIManager.INSTANCE.addAnimation(this);
    }

    /**
     * Cancel the animation
     */
    public void cancel() {
        waiting = true;
        started = false;
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
    public void update(float time) {
        if (waiting || time <= startTime) {
            return;
        }
        if (!started) {
            started = true;
            if (listeners != null) {
                listeners.forEach(e -> e.onAnimationStart(this, reversed));
            }
        }
        float p = Math.min((time - startTime) / duration, 1);
        if (appliers != null) {
            appliers.forEach(e -> e.update(p));
        }
        if (p == 1) {
            waiting = true;
            started = false;
            if (listeners != null) {
                listeners.forEach(e -> e.onAnimationEnd(this, reversed));
            }
        }
    }

    public float getDuration() {
        return duration;
    }

    @Override
    public boolean shouldRemove() {
        return waiting;
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
