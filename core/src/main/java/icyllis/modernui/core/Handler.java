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

package icyllis.modernui.core;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

import java.util.Objects;

/**
 * A Handler allows you to send and process {@link Message} and Runnable
 * objects associated with a thread's {@link MessageQueue}.  Each Handler
 * instance is associated with a single thread and that thread's message
 * queue. When you create a new Handler it is bound to a {@link Looper}.
 * It will deliver messages and runnables to that Looper's message
 * queue and execute them on that Looper's thread.
 *
 * <p>There are two main uses for a Handler: (1) to schedule messages and
 * runnables to be executed at some point in the future; and (2) to enqueue
 * an action to be performed on a different thread than your own.
 *
 * <p>Scheduling messages is accomplished with the
 * {@link #post}, {@link #postAtTime(Runnable, long)},
 * {@link #postDelayed}, {@link #sendEmptyMessage},
 * {@link #sendMessage}, {@link #sendMessageAtTime}, and
 * {@link #sendMessageDelayed} methods.  The <em>post</em> versions allow
 * you to enqueue Runnable objects to be called by the message queue when
 * they are received; the <em>sendMessage</em> versions allow you to enqueue
 * a {@link Message} object containing a bundle of data that will be
 * processed by the Handler's {@link #handleMessage} method (requiring that
 * you implement a subclass of Handler).
 *
 * <p>When posting or sending to a Handler, you can either
 * allow the item to be processed as soon as the message queue is ready
 * to do so, or specify a delay before it gets processed or absolute time for
 * it to be processed.  The latter two allow you to implement timeouts,
 * ticks, and other timing-based behavior.
 *
 * <p>When a
 * process is created for your application, its main thread is dedicated to
 * running a message queue that takes care of managing the top-level
 * application objects (activities, broadcast receivers, etc.) and any windows
 * they create.  You can create your own threads, and communicate back with
 * the main application thread through a Handler.  This is done by calling
 * the same <em>post</em> or <em>sendMessage</em> methods as before, but from
 * your new thread.  The given Runnable or Message will then be scheduled
 * in the Handler's message queue and processed when appropriate.
 */
@SuppressWarnings("unused")
public class Handler {

    /**
     * Callback interface you can use when instantiating a Handler to avoid
     * having to implement your own subclass of Handler.
     */
    @FunctionalInterface
    public interface Callback {

        /**
         * @param msg A {@link Message Message} object
         * @return True if no further handling is desired
         */
        boolean handleMessage(@NonNull Message msg);
    }

    final MessageQueue mQueue;
    final Callback mCallback;
    final boolean mAsynchronous;

    /**
     * Use the provided {@link Looper} instead of the default one.
     *
     * @param looper The looper, must not be null.
     */
    public Handler(Looper looper) {
        this(looper, null, false);
    }

    /**
     * Use the provided {@link Looper} instead of the default one and take a callback
     * interface in which to handle messages.
     *
     * @param looper   The looper, must not be null.
     * @param callback The callback interface in which to handle messages.
     */
    public Handler(Looper looper, Callback callback) {
        this(looper, callback, false);
    }

    /**
     * Use the provided {@link Looper} instead of the default one and take a callback
     * interface in which to handle messages.  Also set whether the handler
     * should be asynchronous.
     * <p>
     * Handlers are synchronous by default unless this constructor is used to make
     * one that is strictly asynchronous.
     * <p>
     * Asynchronous messages represent interrupts or events that do not require global ordering
     * with respect to synchronous messages.  Asynchronous messages are not subject to
     * the synchronization barriers introduced by conditions such as display vsync.
     *
     * @param looper   The looper, must not be null.
     * @param callback The callback interface in which to handle messages, or null.
     * @param async    If true, the handler calls {@link Message#setAsynchronous(boolean)} for
     *                 each {@link Message} that is sent to it or {@link Runnable} that is posted to it.
     */
    private Handler(Looper looper, Callback callback, boolean async) {
        mQueue = Objects.requireNonNull(looper, "No Looper").mQueue;
        mCallback = callback;
        mAsynchronous = async;
    }

    /**
     * Create a new Handler whose posted messages and runnables are not subject to
     * synchronization barriers such as display vsync.
     *
     * <p>Messages sent to an async handler are guaranteed to be ordered with respect to one another,
     * but not necessarily with respect to messages from other Handlers.</p>
     *
     * @param looper the Looper that the new Handler should be bound to
     * @return a new async Handler instance
     */
    @NonNull
    public static Handler createAsync(Looper looper) {
        return new Handler(looper, null, true);
    }

    /**
     * Create a new Handler whose posted messages and runnables are not subject to
     * synchronization barriers such as display vsync.
     *
     * <p>Messages sent to an async handler are guaranteed to be ordered with respect to one another,
     * but not necessarily with respect to messages from other Handlers.</p>
     *
     * @param looper   the Looper that the new Handler should be bound to
     * @param callback the callback interface in which to handle messages.
     * @return a new async Handler instance
     */
    @NonNull
    public static Handler createAsync(Looper looper, Callback callback) {
        return new Handler(looper, callback, true);
    }

    /**
     * Returns a string representing the name of the specified message.
     * The default implementation will either return the class name of the
     * message callback if any, or the hexadecimal representation of the
     * message "what" field.
     *
     * @param message The message whose name is being queried
     */
    @NonNull
    public String getMessageName(@NonNull Message message) {
        if (message.callback != null) {
            return message.callback.getClass().getName();
        }
        return "0x" + Integer.toHexString(message.what);
    }

    /**
     * Returns a new {@link Message Message} from the global message pool. More efficient than
     * creating and allocating new instances. The retrieved message has its handler set to this
     * instance (Message.target == this).
     *
     * @return A Message from the global message pool.
     */
    @NonNull
    public final Message obtainMessage() {
        return Message.obtain(this);
    }

    /**
     * Same as {@link #obtainMessage()}, except that it also sets the what member of the returned Message.
     *
     * @param what Value to assign to the returned Message.what field.
     * @return A Message from the global message pool.
     */
    @NonNull
    public final Message obtainMessage(int what) {
        return Message.obtain(this, what);
    }

    /**
     * Same as {@link #obtainMessage()}, except that it also sets the what and obj members
     * of the returned Message.
     *
     * @param what Value to assign to the returned Message.what field.
     * @param obj  Value to assign to the returned Message.obj field.
     * @return A Message from the global message pool.
     */
    @NonNull
    public final Message obtainMessage(int what, @Nullable Object obj) {
        return Message.obtain(this, what, obj);
    }

    /**
     * Same as {@link #obtainMessage()}, except that it also sets the what, arg1 and arg2 members of the returned
     * Message.
     *
     * @param what Value to assign to the returned Message.what field.
     * @param arg1 Value to assign to the returned Message.arg1 field.
     * @param arg2 Value to assign to the returned Message.arg2 field.
     * @return A Message from the global message pool.
     */
    @NonNull
    public final Message obtainMessage(int what, int arg1, int arg2) {
        return Message.obtain(this, what, arg1, arg2);
    }

    /**
     * Same as {@link #obtainMessage()}, except that it also sets the what, obj, arg1,and arg2 values on the
     * returned Message.
     *
     * @param what Value to assign to the returned Message.what field.
     * @param arg1 Value to assign to the returned Message.arg1 field.
     * @param arg2 Value to assign to the returned Message.arg2 field.
     * @param obj  Value to assign to the returned Message.obj field.
     * @return A Message from the global message pool.
     */
    @NonNull
    public final Message obtainMessage(int what, int arg1, int arg2, @Nullable Object obj) {
        return Message.obtain(this, what, arg1, arg2, obj);
    }

    /**
     * Causes the Runnable r to be added to the message queue.
     * The runnable will be run on the thread to which this handler is
     * attached.
     *
     * @param r The Runnable that will be executed.
     * @return Returns true if the Runnable was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     */
    public final boolean post(@NonNull Runnable r) {
        return sendMessageDelayed(getPostMessage(r), 0);
    }

    /**
     * Causes the Runnable r to be added to the message queue, to be run
     * at a specific time given by <var>timeMillis</var>.
     * <b>The time-base is {@link Core#timeMillis}.</b>
     * Time spent in deep sleep will add a delay to execution.
     * The runnable will be run on the thread to which this handler is attached.
     *
     * @param r          The Runnable that will be executed.
     * @param timeMillis The absolute time at which the callback should run,
     *                   using the {@link Core#timeMillis} time-base.
     * @return Returns true if the Runnable was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.  Note that a
     * result of true does not mean the Runnable will be processed -- if
     * the looper is quit before the delivery time of the message
     * occurs then the message will be dropped.
     */
    public final boolean postAtTime(@NonNull Runnable r, long timeMillis) {
        return sendMessageAtTime(getPostMessage(r), timeMillis);
    }

    /**
     * Causes the Runnable r to be added to the message queue, to be run
     * at a specific time given by <var>timeMillis</var>.
     * <b>The time-base is {@link Core#timeMillis}.</b>
     * Time spent in deep sleep will add a delay to execution.
     * The runnable will be run on the thread to which this handler is attached.
     *
     * @param r          The Runnable that will be executed.
     * @param token      An instance which can be used to cancel {@code r} via
     *                   {@link #removeCallbacksAndMessages}.
     * @param timeMillis The absolute time at which the callback should run,
     *                   using the {@link Core#timeMillis} time-base.
     * @return Returns true if the Runnable was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.  Note that a
     * result of true does not mean the Runnable will be processed -- if
     * the looper is quit before the delivery time of the message
     * occurs then the message will be dropped.
     * @see Core#timeMillis
     */
    public final boolean postAtTime(@NonNull Runnable r, @Nullable Object token, long timeMillis) {
        return sendMessageAtTime(getPostMessage(r, token), timeMillis);
    }

    /**
     * Causes the Runnable r to be added to the message queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the thread to which this handler
     * is attached.
     * <b>The time-base is {@link Core#timeMillis}.</b>
     * Time spent in deep sleep will add a delay to execution.
     *
     * @param r           The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *                    will be executed.
     * @return Returns true if the Runnable was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.  Note that a
     * result of true does not mean the Runnable will be processed --
     * if the looper is quit before the delivery time of the message
     * occurs then the message will be dropped.
     */
    public final boolean postDelayed(@NonNull Runnable r, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r), delayMillis);
    }

    /**
     * Causes the Runnable r to be added to the message queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the thread to which this handler
     * is attached.
     * <b>The time-base is {@link Core#timeMillis}.</b>
     * Time spent in deep sleep will add a delay to execution.
     *
     * @param r           The Runnable that will be executed.
     * @param token       An instance which can be used to cancel {@code r} via
     *                    {@link #removeCallbacksAndMessages}.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *                    will be executed.
     * @return Returns true if the Runnable was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.  Note that a
     * result of true does not mean the Runnable will be processed --
     * if the looper is quit before the delivery time of the message
     * occurs then the message will be dropped.
     */
    public final boolean postDelayed(@NonNull Runnable r, @Nullable Object token, long delayMillis) {
        return sendMessageDelayed(getPostMessage(r, token), delayMillis);
    }

    /**
     * Posts a message to an object that implements Runnable.
     * Causes the Runnable r to executed on the next iteration through the
     * message queue. The runnable will be run on the thread to which this
     * handler is attached.
     * <b>This method is only for use in very special circumstances -- it
     * can easily starve the message queue, cause ordering problems, or have
     * other unexpected side-effects.</b>
     *
     * @param r The Runnable that will be executed.
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     */
    public final boolean postAtFrontOfQueue(@NonNull Runnable r) {
        return sendMessageAtFrontOfQueue(getPostMessage(r));
    }

    /**
     * Remove any pending posts of Runnable r that are in the message queue.
     */
    public final void removeCallbacks(@NonNull Runnable r) {
        mQueue.removeMessages(this, r, null);
    }

    /**
     * Remove any pending posts of Runnable <var>r</var> with Object
     * <var>token</var> that are in the message queue.  If <var>token</var> is null,
     * all callbacks will be removed.
     */
    public final void removeCallbacks(@NonNull Runnable r, @Nullable Object token) {
        mQueue.removeMessages(this, r, token);
    }

    /**
     * Pushes a message onto the end of the message queue after all pending messages
     * before the current time. It will be received in {@link #handleMessage},
     * in the thread attached to this handler.
     *
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     */
    public final boolean sendMessage(@NonNull Message msg) {
        return sendMessageDelayed(msg, 0);
    }

    /**
     * Sends a Message containing only the what value.
     *
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     */
    public final boolean sendEmptyMessage(int what) {
        return sendEmptyMessageDelayed(what, 0);
    }

    /**
     * Sends a Message containing only the what value, to be delivered
     * after the specified amount of time elapses.
     *
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     * @see #sendMessageDelayed(Message, long)
     */
    public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
        Message msg = Message.obtain();
        msg.what = what;
        return sendMessageDelayed(msg, delayMillis);
    }

    /**
     * Sends a Message containing only the what value, to be delivered
     * at a specific time.
     *
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     * @see #sendMessageAtTime(Message, long)
     */
    public final boolean sendEmptyMessageAtTime(int what, long timeMillis) {
        Message msg = Message.obtain();
        msg.what = what;
        return sendMessageAtTime(msg, timeMillis);
    }

    /**
     * Enqueue a message into the message queue after all pending messages
     * before (current time + delayMillis). You will receive it in
     * {@link #handleMessage}, in the thread attached to this handler.
     *
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.  Note that a
     * result of true does not mean the message will be processed -- if
     * the looper is quit before the delivery time of the message
     * occurs then the message will be dropped.
     */
    public final boolean sendMessageDelayed(@NonNull Message msg, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return sendMessageAtTime(msg, Core.timeMillis() + delayMillis);
    }

    /**
     * Enqueue a message into the message queue after all pending messages
     * before the absolute time (in milliseconds) <var>timeMillis</var>.
     * <b>The time-base is {@link Core#timeMillis}.</b>
     * Time spent in deep sleep will add a delay to execution.
     * You will receive it in {@link #handleMessage}, in the thread attached
     * to this handler.
     *
     * @param timeMillis The absolute time at which the message should be
     *                   delivered, using the
     *                   {@link Core#timeMillis} time-base.
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.  Note that a
     * result of true does not mean the message will be processed -- if
     * the looper is quit before the delivery time of the message
     * occurs then the message will be dropped.
     */
    public final boolean sendMessageAtTime(@NonNull Message msg, long timeMillis) {
        return enqueueMessage(msg, timeMillis);
    }

    /**
     * Enqueue a message at the front of the message queue, to be processed on
     * the next iteration of the message loop.  You will receive it in
     * {@link #handleMessage}, in the thread attached to this handler.
     * <b>This method is only for use in very special circumstances -- it
     * can easily starve the message queue, cause ordering problems, or have
     * other unexpected side-effects.</b>
     *
     * @return Returns true if the message was successfully placed in to the
     * message queue.  Returns false on failure, usually because the
     * looper processing the message queue is exiting.
     */
    public final boolean sendMessageAtFrontOfQueue(@NonNull Message msg) {
        return enqueueMessage(msg, 0);
    }

    private boolean enqueueMessage(@NonNull Message msg, long timeMillis) {
        msg.target = this;
        if (mAsynchronous) {
            msg.setAsynchronous(true);
        }
        return mQueue.enqueueMessage(msg, timeMillis);
    }

    /**
     * Remove any pending posts of messages with code 'what' that are in the
     * message queue.
     */
    public final void removeMessages(int what) {
        mQueue.removeMessages(this, what, null);
    }

    /**
     * Remove any pending posts of messages with code 'what' and whose obj is
     * 'object' that are in the message queue.  If <var>object</var> is null,
     * all messages will be removed.
     */
    public final void removeMessages(int what, @Nullable Object object) {
        mQueue.removeMessages(this, what, object);
    }

    /**
     * Remove any pending posts of callbacks and sent messages whose
     * <var>obj</var> is <var>token</var>.  If <var>token</var> is null,
     * all callbacks and messages will be removed.
     */
    public final void removeCallbacksAndMessages(@Nullable Object token) {
        mQueue.removeCallbacksAndMessages(this, token);
    }

    /**
     * Check if there are any pending posts of messages with code 'what' in
     * the message queue.
     */
    public final boolean hasMessages(int what) {
        return mQueue.hasMessages(this, what, null);
    }

    /**
     * Return whether there are any messages or callbacks currently scheduled on this handler.
     */
    public final boolean hasMessages() {
        return mQueue.hasMessages(this);
    }

    /**
     * Check if there are any pending posts of messages with code 'what' and
     * whose obj is 'object' in the message queue.
     */
    public final boolean hasMessages(int what, @Nullable Object object) {
        return mQueue.hasMessages(this, what, object);
    }

    /**
     * Check if there are any pending posts of messages with callback r in
     * the message queue.
     */
    public final boolean hasCallbacks(@NonNull Runnable r) {
        return mQueue.hasMessages(this, r);
    }

    /**
     * Subclasses can implement this to receive messages.
     */
    public void handleMessage(@NonNull Message msg) {
    }

    /**
     * Handle system messages here.
     */
    public void dispatchMessage(@NonNull Message msg) {
        if (msg.callback != null) {
            msg.callback.run();
        } else {
            if (mCallback != null) {
                if (mCallback.handleMessage(msg)) {
                    return;
                }
            }
            handleMessage(msg);
        }
    }

    /**
     * Returns the {@link MessageQueue} object associated with this Handler.
     */
    @NonNull
    public final MessageQueue getQueue() {
        return mQueue;
    }

    /**
     * Returns whether this handler was queued from the current thread.
     */
    public final boolean isCurrentThread() {
        Looper looper = Looper.myLooper();
        return looper != null && looper.mQueue == mQueue;
    }

    @Override
    public String toString() {
        return "Handler (" + getClass().getName() + ") {"
                + Integer.toHexString(System.identityHashCode(this))
                + "}";
    }

    @NonNull
    private static Message getPostMessage(Runnable r) {
        Message m = Message.obtain();
        m.callback = r;
        return m;
    }

    @NonNull
    private static Message getPostMessage(Runnable r, Object token) {
        Message m = Message.obtain();
        m.obj = token;
        m.callback = r;
        return m;
    }
}
