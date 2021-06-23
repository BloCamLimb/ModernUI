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
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import javax.annotation.Nonnull;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongConsumer;

public class AnimationHandler {

    private static AnimationHandler sInstance;

    private final CopyOnWriteArrayList<FrameCallback> mCallbacks = new CopyOnWriteArrayList<>();
    private final Object2LongMap<FrameCallback> mDelayedStartTime = new Object2LongOpenHashMap<>();

    @Nonnull
    public static synchronized LongConsumer init() {
        if (sInstance == null) {
            sInstance = new AnimationHandler();
            return sInstance::doAnimationFrame;
        } else {
            throw new IllegalStateException();
        }
    }

    public static AnimationHandler getInstance() {
        return sInstance;
    }

    private void doAnimationFrame(long frameTime) {
        long currentTime = RenderCore.timeMillis();
        for (FrameCallback callback : mCallbacks) {
            if (isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }
    }

    public void register(@Nonnull FrameCallback callback, long delay) {
        mCallbacks.addIfAbsent(callback);
        if (delay > 0) {
            mDelayedStartTime.put(callback, RenderCore.timeMillis() + delay);
        }
    }

    public void unregister(@Nonnull FrameCallback callback) {
        mCallbacks.remove(callback);
        mDelayedStartTime.removeLong(callback);
    }

    /**
     * Remove the callbacks from mDelayedCallbackStartTime once they have passed the initial delay
     * so that they can start getting frame callbacks.
     *
     * @return true if they have passed the initial delay or have no delay, false otherwise.
     */
    private boolean isCallbackDue(FrameCallback callback, long currentTime) {
        long startTime = mDelayedStartTime.getLong(callback);
        if (startTime == 0) {
            return true;
        }
        if (startTime < currentTime) {
            mDelayedStartTime.removeLong(callback);
            return true;
        }
        return false;
    }

    void autoCancelBasedOn(ObjectAnimator<?> animator) {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            FrameCallback cb = mCallbacks.get(i);
            if (animator.shouldAutoCancel(cb)) {
                ((Animator<?>) cb).cancel();
            }
        }
    }

    /**
     * Callbacks that receives notifications for animation timing and frame commit timing.
     */
    public interface FrameCallback {

        /**
         * Run animation based on the frame time.
         *
         * @param frameTime the frame start time, in the {@link RenderCore#timeMillis()} time base
         */
        void doAnimationFrame(long frameTime);
    }
}
