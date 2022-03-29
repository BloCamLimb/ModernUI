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

import icyllis.modernui.core.Core;
import icyllis.modernui.core.Choreographer;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * This custom, static handler handles the timing pulse that is shared by all active
 * ValueAnimators. This approach ensures that the setting of animation values will happen on the
 * same thread that animations start on, and that all animations will share the same times for
 * calculating their values, which makes synchronizing animations possible.
 */
@ApiStatus.Internal
public class AnimationHandler {

    private final static ThreadLocal<AnimationHandler> sAnimatorHandler = new ThreadLocal<>();

    private final ArrayList<FrameCallback> mAnimationCallbacks = new ArrayList<>();
    private final Object2LongOpenHashMap<FrameCallback> mDelayedStartTime = new Object2LongOpenHashMap<>();

    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(@Nonnull Choreographer choreographer, long frameTimeNanos) {
            doAnimationFrame(frameTimeNanos / 1000000);
            if (mAnimationCallbacks.size() > 0) {
                choreographer.postFrameCallback(this);
            }
        }
    };

    private boolean mListDirty = false;

    private AnimationHandler() {
    }

    @Nonnull
    public static AnimationHandler getInstance() {
        if (sAnimatorHandler.get() == null) {
            sAnimatorHandler.set(new AnimationHandler());
        }
        return sAnimatorHandler.get();
    }

    /**
     * Return the number of callbacks that have registered for frame callbacks.
     */
    public static int getAnimationCount() {
        AnimationHandler handler = sAnimatorHandler.get();
        if (handler == null) {
            return 0;
        }
        return handler.getCallbackSize();
    }

    /**
     * Register to get a callback on the next frame after the delay.
     * If the callback is ever registered, the delay will be overridden.
     *
     * @param callback the callback to register
     * @param delay    delayed time in milliseconds, if > 0
     */
    public void addFrameCallback(@Nonnull FrameCallback callback, long delay) {
        if (mAnimationCallbacks.isEmpty()) {
            Choreographer.getInstance().postFrameCallback(mFrameCallback);
        }
        boolean newlyAdded;
        if (!mAnimationCallbacks.contains(callback)) {
            mAnimationCallbacks.add(callback);
            newlyAdded = true;
        } else {
            newlyAdded = false;
        }
        if (delay > 0) {
            mDelayedStartTime.put(callback, Core.timeMillis() + delay);
        } else if (!newlyAdded) {
            // remove it if any
            mDelayedStartTime.removeLong(callback);
        }
    }

    /**
     * Removes the given callback from the list, so it will no longer be called for frame related
     * timing.
     *
     * @param callback the callback to unregister
     */
    public void removeCallback(@Nonnull FrameCallback callback) {
        int id = mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            // mark it was removed
            mAnimationCallbacks.set(id, null);
            mDelayedStartTime.removeLong(callback);
            mListDirty = true;
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void doAnimationFrame(long frameTime) {
        long currentTime = Core.timeMillis();
        // take a snapshot on currently
        // we don't accept newly added callbacks during handle these callbacks
        final int size = mAnimationCallbacks.size();
        for (int i = 0; i < size; i++) {
            final FrameCallback callback = mAnimationCallbacks.get(i);
            if (callback == null) {
                // mark removed
                continue;
            }
            if (isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }
        cleanUpList();
    }

    /**
     * Remove the callbacks from mDelayedStartTime once they have passed the initial delay
     * so that they can start getting frame callbacks.
     *
     * @return true if they have passed the initial delay or have no delay, false otherwise.
     */
    private boolean isCallbackDue(@Nonnull FrameCallback callback, long currentTime) {
        long startTime = mDelayedStartTime.getLong(callback);
        if (startTime == 0) {
            return true;
        }
        if (currentTime >= startTime) {
            mDelayedStartTime.removeLong(callback);
            return true;
        }
        return false;
    }

    void autoCancelBasedOn(@Nonnull ObjectAnimator animator) {
        for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
            FrameCallback cb = mAnimationCallbacks.get(i);
            if (cb == null) {
                continue;
            }
            if (animator.shouldAutoCancel(cb)) {
                ((Animator) cb).cancel();
            }
        }
    }

    private void cleanUpList() {
        if (mListDirty) {
            for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
                if (mAnimationCallbacks.get(i) == null) {
                    mAnimationCallbacks.remove(i);
                }
            }
            mListDirty = false;
        }
    }

    private int getCallbackSize() {
        int count = 0;
        for (int i = mAnimationCallbacks.size() - 1; i >= 0; i--) {
            if (mAnimationCallbacks.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Callbacks that receives notifications for animation timing.
     */
    public interface FrameCallback {

        /**
         * Run animation based on the frame time.
         *
         * @param frameTime the frame start time, in the {@link Core#timeMillis()} time base
         * @return if the animation has finished.
         */
        boolean doAnimationFrame(long frameTime);
    }
}
