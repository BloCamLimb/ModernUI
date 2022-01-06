/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icyllis.modernui.core;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.MainThread;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class used to run a message loop for a thread.  Threads by default do
 * not have a message loop associated with them; to create one, call
 * {@link #prepare} in the thread that is to run the loop, and then
 * {@link #loop} to have it process messages until the loop is stopped.
 *
 * <p>Most interaction with a message loop is through the
 * {@link Handler} class.
 *
 * <p>Modified from Android Open Source Project.
 */
public final class Looper {

    private static final Marker MARKER = MarkerManager.getMarker("Looper");

    // sThreadLocal.get() will return null unless you've called prepare().
    static final ThreadLocal<Looper> sThreadLocal = new ThreadLocal<>();
    private static volatile Looper sMainLooper;
    private static volatile Observer sObserver;

    final MessageQueue mQueue;
    final Thread mThread;
    private boolean mInLoop;

    /**
     * If set, the looper will show a warning log if a message dispatch takes longer than this.
     */
    private long mSlowDispatchThresholdMs;

    /**
     * If set, the looper will show a warning log if a message delivery (actual delivery time -
     * post time) takes longer than this.
     */
    private long mSlowDeliveryThresholdMs;

    /**
     * True if a message delivery takes longer than {@link #mSlowDeliveryThresholdMs}.
     */
    private boolean mSlowDeliveryDetected;

    /**
     * Initialize the current thread as a looper.
     * <p>
     * This gives you a chance to create handlers that then reference
     * this looper, before actually starting the loop. Be sure to call
     * {@link #loop()} after calling this method, and end it by calling
     * {@link #quit()}.
     *
     * @throws RuntimeException initializes twice
     */
    public static void prepare() {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(0));
    }

    /**
     * Returns the application's main looper, which lives in the main thread of the application.
     */
    public static Looper getMainLooper() {
        return sMainLooper;
    }

    /**
     * Set the transaction observer for all Loopers in this process.
     */
    public static void setObserver(@Nullable Observer observer) {
        sObserver = observer;
    }

    /**
     * Poll and deliver single message, return true if the outer loop should continue.
     */
    @ApiStatus.Internal
    public static boolean poll(@Nonnull final Looper me) {
        Message msg = me.mQueue.next(); // might block
        if (msg == null) {
            // No message indicates that the message queue is quitting.
            return false;
        }

        // Make sure the observer won't change while processing a transaction.
        final Observer observer = sObserver;

        final long slowDispatchThresholdMs = me.mSlowDispatchThresholdMs;
        final long slowDeliveryThresholdMs = me.mSlowDeliveryThresholdMs;
        final boolean logSlowDelivery = (slowDeliveryThresholdMs > 0) && (msg.when > 0);
        final boolean logSlowDispatch = (slowDispatchThresholdMs > 0);

        final long dispatchStart = logSlowDelivery || logSlowDispatch ? ArchCore.timeMillis() : 0;
        final long dispatchEnd;
        final Object token = observer == null ? null : observer.messageDispatchStarting();
        try {
            msg.target.dispatchMessage(msg);
            if (observer != null) {
                observer.messageDispatched(token, msg);
            }
            dispatchEnd = logSlowDispatch ? ArchCore.timeMillis() : 0;
        } catch (Exception exception) {
            if (observer != null) {
                observer.dispatchingThrewException(token, msg, exception);
            }
            throw exception;
        }
        if (logSlowDelivery) {
            if (me.mSlowDeliveryDetected) {
                if (dispatchStart - msg.when <= 10) {
                    ModernUI.LOGGER.warn(MARKER, "Drained");
                    me.mSlowDeliveryDetected = false;
                }
            } else {
                if (showSlowLog(slowDeliveryThresholdMs, msg.when, dispatchStart, "delivery", msg)) {
                    // Once we write a slow delivery log, suppress until the queue drains.
                    me.mSlowDeliveryDetected = true;
                }
            }
        }
        if (logSlowDispatch) {
            showSlowLog(slowDispatchThresholdMs, dispatchStart, dispatchEnd, "dispatch", msg);
        }

        msg.recycleUnchecked();
        return true;
    }

    /**
     * Enter the looper in this thread.
     */
    @ApiStatus.Internal
    @Nonnull
    public static Looper enter() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
        if (me.mInLoop) {
            ModernUI.LOGGER.warn(MARKER, "Loop again would have the queued messages be executed"
                    + " before this one completed.");
        }
        me.mInLoop = true;
        me.mSlowDeliveryDetected = false;
        return me;
    }

    /**
     * Run the message queue in this thread. Be sure to call
     * {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = enter();
        //noinspection StatementWithEmptyBody
        while (poll(me));
    }

    /**
     * Run the main event loop. This must be called from the entry point of the application.
     * <p>
     * Main looper cannot quit via {@link #quit()}, but it will quit right after the main window
     * is marked closed.
     *
     * @param w the main window.
     */
    @ApiStatus.Internal
    @MainThread
    public static void loop(@Nonnull Window w) {
        final Looper me = new Looper(w.getHandle());
        sThreadLocal.set(me);
        sMainLooper = me;
        me.mInLoop = true;
        me.mSlowDeliveryDetected = false;
        //noinspection StatementWithEmptyBody
        while (poll(me));
    }

    private static boolean showSlowLog(long threshold, long measureStart, long measureEnd,
                                       String what, Message msg) {
        final long actualTime = measureEnd - measureStart;
        if (actualTime < threshold) {
            return false;
        }
        // For slow delivery, the current message isn't really important, but log it anyway.
        ModernUI.LOGGER.warn(MARKER, "Slow {} took {}ms {} h={} c={} m={}",
                what, actualTime, Thread.currentThread().getName(),
                msg.target.getClass().getName(), msg.callback, msg.what);
        return true;
    }

    /**
     * Return the Looper object associated with the current thread.  Returns
     * null if the calling thread is not associated with a Looper.
     */
    @Nullable
    public static Looper myLooper() {
        return sThreadLocal.get();
    }

    /**
     * Return the {@link MessageQueue} object associated with the current
     * thread.
     *
     * @throws NullPointerException not called from a thread running a Looper
     */
    @Nonnull
    public static MessageQueue myQueue() {
        return sThreadLocal.get().mQueue;
    }

    private Looper(long w) {
        mQueue = new MessageQueue(w);
        mThread = Thread.currentThread();
    }

    /**
     * Returns true if the current thread is this looper's thread.
     */
    public boolean isCurrentThread() {
        return Thread.currentThread() == mThread;
    }

    /**
     * Set a thresholds for slow dispatch/delivery log.
     */
    public void setSlowLogThresholdMs(long slowDispatchThresholdMs, long slowDeliveryThresholdMs) {
        mSlowDispatchThresholdMs = slowDispatchThresholdMs;
        mSlowDeliveryThresholdMs = slowDeliveryThresholdMs;
    }

    /**
     * Quits the looper.
     * <p>
     * Causes the {@link #loop} method to terminate without processing any
     * more messages in the message queue.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p><p class="note">
     * Using this method may be unsafe because some messages may not be delivered
     * before the looper terminates.  Consider using {@link #quitSafely} instead to ensure
     * that all pending work is completed in an orderly manner.
     * </p>
     *
     * @see #quitSafely
     */
    public void quit() {
        mQueue.quit(false);
    }

    /**
     * Quits the looper safely.
     * <p>
     * Causes the {@link #loop} method to terminate as soon as all remaining messages
     * in the message queue that are already due to be delivered have been handled.
     * However pending delayed messages with due times in the future will not be
     * delivered before the loop terminates.
     * </p><p>
     * Any attempt to post messages to the queue after the looper is asked to quit will fail.
     * For example, the {@link Handler#sendMessage(Message)} method will return false.
     * </p>
     */
    public void quitSafely() {
        mQueue.quit(true);
    }

    /**
     * Gets the Thread associated with this Looper.
     *
     * @return The looper's thread.
     */
    @Nonnull
    public Thread getThread() {
        return mThread;
    }

    /**
     * Gets this looper's message queue.
     *
     * @return The looper's message queue.
     */
    @Nonnull
    public MessageQueue getQueue() {
        return mQueue;
    }

    @Nonnull
    @Override
    public String toString() {
        return "Looper (" + mThread.getName() + ", tid " + mThread.getId()
                + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }

    public interface Observer {

        /**
         * Called right before a message is dispatched.
         * <p>
         * The token type is not specified to allow the implementation to specify its own type.
         * The token must be passed back exactly once to either
         * {@link Observer#messageDispatched} or {@link Observer#dispatchingThrewException}
         * and must not be reused again.
         *
         * @return a token used for collecting telemetry when dispatching a single message.
         */
        Object messageDispatchStarting();

        /**
         * Called when a message was processed by a Handler.
         *
         * @param token Token obtained by previously calling
         *              {@link Observer#messageDispatchStarting} on the same Observer instance.
         * @param msg   The message that was dispatched.
         */
        void messageDispatched(Object token, Message msg);

        /**
         * Called when an exception was thrown while processing a message.
         *
         * @param token     Token obtained by previously calling
         *                  {@link Observer#messageDispatchStarting} on the same Observer instance.
         * @param msg       The message that was dispatched and caused an exception.
         * @param exception The exception that was thrown.
         */
        void dispatchingThrewException(Object token, Message msg, Exception exception);
    }
}
