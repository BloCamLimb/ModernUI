/*
 * Modern UI.
 * Copyright (C) 2022-2025 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

import icyllis.modernui.animation.AnimationUtils;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.view.View;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * Coordinates the timing of animations, input and drawing.
 * <p>
 * The choreographer receives timing pulses (such as vertical synchronization)
 * from the render thread then schedules work to occur as part of rendering
 * the next display frame.
 * </p><p>
 * Applications typically interact with the choreographer indirectly using
 * higher level abstractions in the animation framework or the view hierarchy.
 * Here are some examples of things you can do using the higher-level APIs.
 * </p>
 * <ul>
 * <li>To post an animation to be processed on a regular time basis synchronized with
 * display frame rendering, use {@link ValueAnimator#start()}.</li>
 * <li>To post a {@link Runnable} to be invoked once at the beginning of the next display
 * frame, use {@link View#postOnAnimation}.</li>
 * <li>To post a {@link Runnable} to be invoked once at the beginning of the next display
 * frame after a delay, use {@link View#postOnAnimationDelayed}.</li>
 * <li>To post a call to {@link View#invalidate()} to occur once at the beginning of the
 * next display frame, use {@link View#postInvalidateOnAnimation()}.</li>
 * <li>To ensure that the contents of a {@link View} scroll smoothly and are drawn in
 * sync with display frame rendering, do nothing.  This already happens automatically.
 * {@link View#draw(Canvas)} will be called at the appropriate time.</li>
 * </ul>
 * <p>
 * However, there are a few cases where you might want to use the functions of the
 * choreographer directly in your application.  Here are some examples.
 * </p>
 * <ul>
 * <li>If your application does its rendering in a different thread, possibly using GL,
 * or does not use the animation framework or view hierarchy at all
 * and you want to ensure that it is appropriately synchronized with the display, then use
 * {@link Choreographer#postFrameCallback}.</li>
 * <li>... and that's about it.</li>
 * </ul>
 * <p>
 * Each {@link Looper} thread has its own choreographer.  Other threads can
 * post callbacks to run on the choreographer but they will run on the {@link Looper}
 * to which the choreographer belongs.
 * </p>
 */
//TODO not stable, consider swap-chain? when to schedule?
public final class Choreographer {

    private static final Marker MARKER = MarkerManager.getMarker("Choreographer");

    // Prints debug messages about every frame and callback registered (high volume).
    private static final boolean DEBUG_FRAMES = false;

    // The number of milliseconds between animation frames.
    private static volatile long sFrameDelay = 1;

    // Thread local storage for the choreographer.
    private static final ThreadLocal<Choreographer> sThreadInstance = ThreadLocal.withInitial(() -> {
        final Looper looper = Looper.myLooper();
        if (looper == null) {
            throw new IllegalStateException("The current thread must have a looper!");
        }
        return new Choreographer(looper);
    });

    private static final int MSG_DO_FRAME = 0;
    private static final int MSG_DO_SCHEDULE_CALLBACK = 1;

    // All frame callbacks posted by applications have this token.
    private static final Object FRAME_CALLBACK_TOKEN = new Object() {
        @NonNull
        @Override
        public String toString() {
            return "FRAME_CALLBACK_TOKEN";
        }
    };

    /**
     * Callback type: Input callback.  Runs first.
     */
    @ApiStatus.Internal
    public static final int CALLBACK_INPUT = 0;

    /**
     * Callback type: Animation callback.  Runs before {@link #CALLBACK_TRAVERSAL}.
     */
    @ApiStatus.Internal
    public static final int CALLBACK_ANIMATION = 1;

    /**
     * Callback type: Traversal callback.  Handles layout and draw.  Runs
     * after all other asynchronous messages have been handled.
     */
    @ApiStatus.Internal
    public static final int CALLBACK_TRAVERSAL = 2;

    /**
     * Callback type: Commit callback.  Handles post-draw operations for the frame.
     * Runs after traversal completes.  The {@link #getFrameTime() frame time} reported
     * during this callback may be updated to reflect delays that occurred while
     * traversals were in progress in case heavy layout operations caused some frames
     * to be skipped.  The frame time reported during this callback provides a better
     * estimate of the start time of the frame in which animations (and other updates
     * to the view hierarchy state) actually took effect.
     */
    @ApiStatus.Internal
    public static final int CALLBACK_COMMIT = 3;

    private static final int CALLBACK_LAST = CALLBACK_COMMIT;

    private final Object mLock = new Object();

    private final Handler mHandler;

    private CallbackRecord mCallbackPool;

    private final CallbackQueue[] mCallbackQueues;

    private boolean mFrameScheduled;
    private boolean mCallbacksRunning;
    private long mLastFrameTimeNanos;

    private Choreographer(@NonNull Looper looper) {
        mHandler = new Handler(looper, this::handleMessage);
        mLastFrameTimeNanos = Long.MIN_VALUE;

        mCallbackQueues = new CallbackQueue[CALLBACK_LAST + 1];
        for (int i = 0; i <= CALLBACK_LAST; i++) {
            mCallbackQueues[i] = new CallbackQueue();
        }
    }

    /**
     * Gets the choreographer for the calling thread.  Must be called from
     * a thread that already has a {@link Looper} associated with it.
     * Must NOT be called from render thread.
     *
     * @return The choreographer for this thread.
     * @throws IllegalStateException if the thread does not have a looper
     */
    @NonNull
    public static Choreographer getInstance() {
        return sThreadInstance.get();
    }

    /**
     * The amount of time, in milliseconds, between each frame of the animation.
     * <p>
     * This is a requested time that the animation will attempt to honor, but the actual delay
     * between frames may be different, depending on system load and capabilities. This is a static
     * function because the same delay will be applied to all animations, since they are all
     * run off of a single timing loop.
     * </p>
     *
     * @return the requested time between frames, in milliseconds
     */
    @ApiStatus.Internal
    public static long getFrameDelay() {
        return sFrameDelay;
    }

    /**
     * The amount of time, in milliseconds, between each frame of the animation.
     * <p>
     * This is a requested time that the animation will attempt to honor, but the actual delay
     * between frames may be different, depending on system load and capabilities. This is a static
     * function because the same delay will be applied to all animations, since they are all
     * run off of a single timing loop.
     * </p>
     *
     * @param frameDelay the requested time between frames, in milliseconds
     */
    @ApiStatus.Internal
    public static void setFrameDelay(long frameDelay) {
        sFrameDelay = frameDelay;
    }

    /**
     * Subtracts typical frame delay time from a delay interval in milliseconds.
     * <p>
     * This method can be used to compensate for animation delay times that have baked
     * in assumptions about the frame delay.  For example, it's quite common for code to
     * assume a 60Hz frame time and bake in a 16ms delay.  When we call
     * {@link #postCallbackDelayed} on animation we want to know how long to wait before
     * posting the animation callback but let the animation timer take care of the remaining
     * frame delay time.
     * </p><p>
     * This method is somewhat conservative about how much of the frame delay it
     * subtracts.  It uses the same value returned by {@link #getFrameDelay} which by
     * default is 10ms even though many parts of the system assume 16ms.  Consequently,
     * we might still wait 6ms before posting an animation callback that we want to run
     * on the next frame, but this is much better than waiting a whole 16ms and likely
     * missing the deadline.
     * </p>
     *
     * @param delayMillis The original delay time including an assumed frame delay.
     * @return The adjusted delay time with the assumed frame delay subtracted out.
     */
    @ApiStatus.Internal
    public static long subtractFrameDelay(long delayMillis) {
        final long frameDelay = sFrameDelay;
        return delayMillis <= frameDelay ? 0 : delayMillis - frameDelay;
    }

    /**
     * Posts a callback to run on the next frame.
     * <p>
     * The callback runs once then is automatically removed.
     * </p>
     *
     * @param callbackType The callback type.
     * @param action       The callback action to run during the next frame.
     * @param token        The callback token, or null if none.
     * @see #removeCallbacks(int, Runnable, Object)
     */
    @ApiStatus.Internal
    public void postCallback(int callbackType, @NonNull Runnable action, @Nullable Object token) {
        postCallbackDelayed(callbackType, action, token, 0);
    }

    /**
     * Posts a callback to run on the next frame after the specified delay.
     * <p>
     * The callback runs once then is automatically removed.
     * </p>
     *
     * @param callbackType The callback type.
     * @param action       The callback action to run during the next frame after the specified delay.
     * @param token        The callback token, or null if none.
     * @param delayMillis  The delay time in milliseconds.
     * @see #removeCallbacks(int, Runnable, Object)
     */
    @ApiStatus.Internal
    public void postCallbackDelayed(int callbackType, @NonNull Runnable action, @Nullable Object token,
                                    long delayMillis) {
        Objects.requireNonNull(action);
        if (callbackType < 0 || callbackType > CALLBACK_LAST) {
            throw new AssertionError(callbackType);
        }
        postCallbackDelayedInternal(callbackType, action, token, delayMillis);
    }

    private void postCallbackDelayedInternal(int callbackType, @NonNull Object action, @Nullable Object token,
                                             long delayMillis) {
        if (DEBUG_FRAMES) {
            LOGGER.debug(MARKER, "PostCallback: type=" + callbackType
                    + ", action=" + action + ", token=" + token
                    + ", delayMillis=" + delayMillis);
        }

        synchronized (mLock) {
            final long now = Core.timeMillis();
            final long dueTime = now + delayMillis;
            mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);

            if (dueTime <= now) {
                scheduleFrameLocked(now);
            } else {
                Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
                msg.arg1 = callbackType;
                msg.setAsynchronous(true);
                mHandler.sendMessageAtTime(msg, dueTime);
            }
        }
    }

    /**
     * Removes callbacks that have the specified action and token.
     *
     * @param callbackType The callback type.
     * @param action       The action property of the callbacks to remove, or null to remove
     *                     callbacks with any action.
     * @param token        The token property of the callbacks to remove, or null to remove
     *                     callbacks with any token.
     * @see #postCallback
     * @see #postCallbackDelayed
     */
    @ApiStatus.Internal
    public void removeCallbacks(int callbackType, @Nullable Runnable action, @Nullable Object token) {
        if (callbackType < 0 || callbackType > CALLBACK_LAST) {
            throw new AssertionError(callbackType);
        }
        removeCallbacksInternal(callbackType, action, token);
    }

    private void removeCallbacksInternal(int callbackType, @Nullable Object action, @Nullable Object token) {
        if (DEBUG_FRAMES) {
            LOGGER.debug(MARKER, "RemoveCallbacks: type=" + callbackType
                    + ", action=" + action + ", token=" + token);
        }

        synchronized (mLock) {
            mCallbackQueues[callbackType].removeCallbacksLocked(action, token);
            if (action != null && token == null) {
                mHandler.removeMessages(MSG_DO_SCHEDULE_CALLBACK, action);
            }
        }
    }

    /**
     * Posts a frame callback to run on the next frame.
     * <p>
     * The callback runs once then is automatically removed.
     * </p>
     *
     * @param callback The frame callback to run during the next frame.
     * @see #postFrameCallbackDelayed
     * @see #removeFrameCallback
     */
    public void postFrameCallback(@NonNull FrameCallback callback) {
        postFrameCallbackDelayed(callback, 0);
    }

    /**
     * Posts a frame callback to run on the next frame after the specified delay.
     * <p>
     * The callback runs once then is automatically removed.
     * </p>
     *
     * @param callback    The frame callback to run during the next frame.
     * @param delayMillis The delay time in milliseconds.
     * @see #postFrameCallback
     * @see #removeFrameCallback
     */
    public void postFrameCallbackDelayed(@NonNull FrameCallback callback, long delayMillis) {
        Objects.requireNonNull(callback);
        postCallbackDelayedInternal(CALLBACK_ANIMATION, callback, FRAME_CALLBACK_TOKEN, delayMillis);
    }

    /**
     * Removes a previously posted frame callback.
     *
     * @param callback The frame callback to remove.
     * @see #postFrameCallback
     * @see #postFrameCallbackDelayed
     */
    public void removeFrameCallback(@NonNull FrameCallback callback) {
        Objects.requireNonNull(callback);
        removeCallbacksInternal(CALLBACK_ANIMATION, callback, FRAME_CALLBACK_TOKEN);
    }

    /**
     * Gets the time when the current frame started.
     * <p>
     * This method provides the time in milliseconds when the frame started being rendered.
     * The frame time provides a stable time base for synchronizing animations
     * and drawing.  It should be used instead of {@link Core#timeMillis()}
     * or {@link Core#timeNanos()} for animations and drawing in the UI.  Using the frame
     * time helps to reduce inter-frame jitter because the frame time is fixed at the time
     * the frame was scheduled to start, regardless of when the animations or drawing
     * callback actually runs.  All callbacks that run as part of rendering a frame will
     * observe the same frame time so using the frame time also helps to synchronize effects
     * that are performed by different callbacks.
     * </p><p>
     * Please note that the framework already takes care to process animations and
     * drawing using the frame time as a stable time base.  Most applications should
     * not need to use the frame time information directly.
     * </p><p>
     * This method should only be called from within a callback.
     * </p>
     *
     * @return The frame start time, in the {@link Core#timeMillis()} time base.
     * @throws IllegalStateException if no frame is in progress.
     */
    @ApiStatus.Internal
    public long getFrameTime() {
        return getFrameTimeNanos() / 1000000;
    }

    /**
     * Same as {@link #getFrameTime()} but with nanosecond precision.
     *
     * @return The frame start time, in the {@link Core#timeNanos()} time base.
     * @throws IllegalStateException if no frame is in progress.
     */
    @ApiStatus.Internal
    public long getFrameTimeNanos() {
        synchronized (mLock) {
            if (!mCallbacksRunning) {
                throw new IllegalStateException("This method must only be called as "
                        + "part of a callback while a frame is in progress.");
            }
            return mLastFrameTimeNanos;
        }
    }

    /**
     * Like {@link #getFrameTimeNanos}, but always returns the last frame time, not matter
     * whether callbacks are currently running.
     *
     * @return The frame start time of the last frame, in the {@link Core#timeNanos()} time base.
     */
    @ApiStatus.Internal
    public long getLastFrameTimeNanos() {
        synchronized (mLock) {
            return mLastFrameTimeNanos;
        }
    }

    private void scheduleFrameLocked(long now) {
        if (!mFrameScheduled) {
            mFrameScheduled = true;
            final long nextFrameTime = Math.max(
                    mLastFrameTimeNanos / 1000000 + sFrameDelay, now);
            if (DEBUG_FRAMES) {
                LOGGER.debug(MARKER, "Scheduling next frame in " + (nextFrameTime - now) + " ms.");
            }
            Message msg = mHandler.obtainMessage(MSG_DO_FRAME);
            msg.setAsynchronous(true);
            mHandler.sendMessageAtTime(msg, nextFrameTime);
        }
    }

    void doFrame() {
        final long frameTimeNanos = Core.timeNanos();
        try {
            synchronized (mLock) {
                if (!mFrameScheduled) {
                    // nothing to do
                    return;
                }

                if (frameTimeNanos < mLastFrameTimeNanos) {
                    // should not happen
                    return;
                }

                mFrameScheduled = false;
                mLastFrameTimeNanos = frameTimeNanos;
            }

            AnimationUtils.lockAnimationClock(frameTimeNanos / 1000000);

            doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);

            doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);

            doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);

            doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);
        } finally {
            AnimationUtils.unlockAnimationClock();
        }

        if (DEBUG_FRAMES) {
            final long endNanos = Core.timeNanos();
            LOGGER.info(MARKER, "Frame : Finished, took "
                    + (endNanos - frameTimeNanos) * 0.000001f + " ms.");
        }
    }

    void doCallbacks(int callbackType, long frameTimeNanos) {
        CallbackRecord callbacks;
        synchronized (mLock) {
            // We use "now" to determine when callbacks become due because it's possible
            // for earlier processing phases in a frame to post callbacks that should run
            // in a following phase, such as an input event that causes an animation to start.
            final long now = Core.timeMillis();
            callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(now);
            if (callbacks == null) {
                return;
            }
            mCallbacksRunning = true;
        }
        try {
            for (CallbackRecord c = callbacks; c != null; c = c.next) {
                if (DEBUG_FRAMES) {
                    LOGGER.info(MARKER, "RunCallback: type=" + callbackType
                            + ", action=" + c.action + ", token=" + c.token
                            + ", latencyMillis=" + (Core.timeMillis() - c.dueTime));
                }
                c.run(frameTimeNanos);
            }
        } finally {
            synchronized (mLock) {
                mCallbacksRunning = false;
                do {
                    final CallbackRecord next = callbacks.next;
                    recycleCallbackLocked(callbacks);
                    callbacks = next;
                } while (callbacks != null);
            }
        }
    }

    void doScheduleCallback(int callbackType) {
        synchronized (mLock) {
            if (!mFrameScheduled) {
                final long now = Core.timeMillis();
                if (mCallbackQueues[callbackType].hasDueCallbacksLocked(now)) {
                    scheduleFrameLocked(now);
                }
            }
        }
    }

    @NonNull
    private CallbackRecord obtainCallbackLocked(long dueTime, @NonNull Object action, @Nullable Object token) {
        CallbackRecord callback = mCallbackPool;
        if (callback == null) {
            callback = new CallbackRecord();
        } else {
            mCallbackPool = callback.next;
            callback.next = null;
        }
        callback.dueTime = dueTime;
        callback.action = action;
        callback.token = token;
        return callback;
    }

    private void recycleCallbackLocked(@NonNull CallbackRecord callback) {
        callback.action = null;
        callback.token = null;
        callback.next = mCallbackPool;
        mCallbackPool = callback;
    }

    private boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_DO_FRAME -> doFrame();
            case MSG_DO_SCHEDULE_CALLBACK -> doScheduleCallback(msg.arg1);
        }
        return true;
    }

    /**
     * Implement this interface to receive a callback when a new display frame is
     * being rendered.  The callback is invoked on the {@link Looper} thread to
     * which the {@link Choreographer} is attached.
     */
    @FunctionalInterface
    public interface FrameCallback {

        /**
         * Called when a new display frame is being rendered.
         * <p>
         * This method provides the time in nanoseconds when the frame started being rendered.
         * The frame time provides a stable time base for synchronizing animations
         * and drawing.  It should be used instead of {@link Core#timeMillis()}
         * or {@link Core#timeNanos()} for animations and drawing in the UI.  Using the frame
         * time helps to reduce inter-frame jitter because the frame time is fixed at the time
         * the frame was scheduled to start, regardless of when the animations or drawing
         * callback actually runs.  All callbacks that run as part of rendering a frame will
         * observe the same frame time so using the frame time also helps to synchronize effects
         * that are performed by different callbacks.
         * </p><p>
         * Please note that the framework already takes care to process animations and
         * drawing using the frame time as a stable time base.  Most applications should
         * not need to use the frame time information directly.
         * </p>
         *
         * @param choreographer  the choreographer called this method
         * @param frameTimeNanos The time in nanoseconds when the frame started being rendered,
         *                       in the {@link Core#timeNanos()} timebase.  Divide this value by {@code 1000000}
         *                       to convert it to the {@link Core#timeMillis()} time base.
         */
        void doFrame(@NonNull Choreographer choreographer, long frameTimeNanos);
    }

    private final class CallbackRecord {

        public CallbackRecord next;
        public long dueTime;
        public Object action; // Runnable or FrameCallback
        public Object token;

        public void run(long frameTimeNanos) {
            if (token == FRAME_CALLBACK_TOKEN) {
                ((FrameCallback) action).doFrame(Choreographer.this, frameTimeNanos);
            } else {
                ((Runnable) action).run();
            }
        }
    }

    private final class CallbackQueue {

        @Nullable
        private CallbackRecord mHead;

        public boolean hasDueCallbacksLocked(long now) {
            return mHead != null && mHead.dueTime <= now;
        }

        @Nullable
        public CallbackRecord extractDueCallbacksLocked(long now) {
            CallbackRecord callbacks = mHead;
            if (callbacks == null || callbacks.dueTime > now) {
                return null;
            }

            CallbackRecord last = callbacks;
            CallbackRecord next = last.next;
            while (next != null) {
                if (next.dueTime > now) {
                    last.next = null;
                    break;
                }
                last = next;
                next = next.next;
            }
            mHead = next;
            return callbacks;
        }

        public void addCallbackLocked(long dueTime, @NonNull Object action, @Nullable Object token) {
            CallbackRecord callback = obtainCallbackLocked(dueTime, action, token);
            CallbackRecord entry = mHead;
            if (entry == null) {
                mHead = callback;
                return;
            }
            if (dueTime < entry.dueTime) {
                callback.next = entry;
                mHead = callback;
                return;
            }
            while (entry.next != null) {
                if (dueTime < entry.next.dueTime) {
                    callback.next = entry.next;
                    break;
                }
                entry = entry.next;
            }
            entry.next = callback;
        }

        public void removeCallbacksLocked(@Nullable Object action, @Nullable Object token) {
            CallbackRecord predecessor = null;
            for (CallbackRecord callback = mHead; callback != null; ) {
                final CallbackRecord next = callback.next;
                if ((action == null || callback.action == action)
                        && (token == null || callback.token == token)) {
                    if (predecessor != null) {
                        predecessor.next = next;
                    } else {
                        mHead = next;
                    }
                    recycleCallbackLocked(callback);
                } else {
                    predecessor = callback;
                }
                callback = next;
            }
        }
    }
}
