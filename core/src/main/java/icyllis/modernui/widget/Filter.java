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

package icyllis.modernui.widget;

import icyllis.modernui.core.Handler;
import icyllis.modernui.core.HandlerThread;
import icyllis.modernui.core.Looper;
import icyllis.modernui.core.Message;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;

import static icyllis.modernui.ModernUI.LOGGER;

/**
 * <p>A filter constrains data with a filtering pattern.</p>
 *
 * <p>Filters are usually created by {@link Filterable} classes.</p>
 *
 * <p>Filtering operations performed by calling {@link #filter(CharSequence)} or
 * {@link #filter(CharSequence, FilterListener)} are
 * performed asynchronously. When these methods are called, a filtering request
 * is posted in a request queue and processed later. Any call to one of these
 * methods will cancel any previous non-executed filtering request.</p>
 *
 * @see Filterable
 */
public abstract class Filter {

    private static final Marker MARKER = MarkerManager.getMarker("Filter");

    private static final String THREAD_NAME = "Filter";
    private static final int FILTER_TOKEN = 0xD0D0F00D;
    private static final int FINISH_TOKEN = 0xDEADBEEF;

    private Handler mThreadHandler;
    private HandlerThread mHandlerThread;
    private final Handler mResultHandler;

    private Delayer mDelayer;

    private final Object mLock = new Object();

    /**
     * <p>Creates a new asynchronous filter.</p>
     */
    public Filter() {
        mResultHandler = new ResultsHandler();
    }

    /**
     * Provide an interface that decides how long to delay the message for a given query.  Useful
     * for heuristics such as posting a delay for the delete key to avoid doing any work while the
     * user holds down the delete key.
     *
     * @param delayer The delayer.
     * @hide
     */
    public void setDelayer(Delayer delayer) {
        synchronized (mLock) {
            mDelayer = delayer;
        }
    }

    /**
     * <p>Starts an asynchronous filtering operation. Calling this method
     * cancels all previous non-executed filtering requests and posts a new
     * filtering request that will be executed later.</p>
     *
     * @param constraint the constraint used to filter the data
     * @see #filter(CharSequence, FilterListener)
     */
    public final void filter(CharSequence constraint) {
        filter(constraint, null);
    }

    /**
     * <p>Starts an asynchronous filtering operation. Calling this method
     * cancels all previous non-executed filtering requests and posts a new
     * filtering request that will be executed later.</p>
     *
     * <p>Upon completion, the listener is notified.</p>
     *
     * @param constraint the constraint used to filter the data
     * @param listener   a listener notified upon completion of the operation
     * @see #filter(CharSequence)
     * @see #performFiltering(CharSequence)
     * @see #publishResults(CharSequence, FilterResults)
     */
    public final void filter(CharSequence constraint, FilterListener listener) {
        synchronized (mLock) {
            if (mThreadHandler == null) {
                mHandlerThread = new HandlerThread(THREAD_NAME);
                mHandlerThread.start();
                mThreadHandler = new RequestHandler(mHandlerThread.getLooper());
            }

            final long delay = (mDelayer == null) ? 0 : mDelayer.getPostingDelay(constraint);

            Message message = mThreadHandler.obtainMessage(FILTER_TOKEN);

            RequestArguments args = new RequestArguments();
            // make sure we use an immutable copy of the constraint, so that
            // it doesn't change while the filter operation is in progress
            args.constraint = constraint != null ? constraint.toString() : null;
            args.listener = listener;
            message.obj = args;

            mThreadHandler.removeMessages(FILTER_TOKEN);
            mThreadHandler.removeMessages(FINISH_TOKEN);
            mThreadHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * <p>Invoked in a worker thread to filter the data according to the
     * constraint. Subclasses must implement this method to perform the
     * filtering operation. Results computed by the filtering operation
     * must be returned as a {@link FilterResults} that
     * will then be published in the UI thread through
     * {@link #publishResults(CharSequence, FilterResults)}.</p>
     *
     * <p><strong>Contract:</strong> When the constraint is null, the original
     * data must be restored.</p>
     *
     * @param constraint the constraint used to filter the data
     * @return the results of the filtering operation
     * @see #filter(CharSequence, FilterListener)
     * @see #publishResults(CharSequence, FilterResults)
     * @see FilterResults
     */
    protected abstract FilterResults performFiltering(CharSequence constraint);

    /**
     * <p>Invoked in the UI thread to publish the filtering results in the
     * user interface. Subclasses must implement this method to display the
     * results computed in {@link #performFiltering}.</p>
     *
     * @param constraint the constraint used to filter the data
     * @param results    the results of the filtering operation
     * @see #filter(CharSequence, FilterListener)
     * @see #performFiltering(CharSequence)
     * @see FilterResults
     */
    protected abstract void publishResults(CharSequence constraint,
                                           FilterResults results);

    /**
     * <p>Converts a value from the filtered set into a CharSequence. Subclasses
     * should override this method to convert their results. The default
     * implementation returns an empty String for null values or the default
     * String representation of the value.</p>
     *
     * @param resultValue the value to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    public CharSequence convertResultToString(Object resultValue) {
        return resultValue == null ? "" : resultValue.toString();
    }

    /**
     * <p>Holds the results of a filtering operation. The results are the values
     * computed by the filtering operation and the number of these values.</p>
     */
    protected static class FilterResults {

        public FilterResults() {
        }

        /**
         * <p>Contains all the values computed by the filtering operation.</p>
         */
        public Object values;

        /**
         * <p>Contains the number of values computed by the filtering
         * operation.</p>
         */
        public int count;
    }

    /**
     * <p>Listener used to receive a notification upon completion of a filtering
     * operation.</p>
     */
    @FunctionalInterface
    public interface FilterListener {

        /**
         * <p>Notifies the end of a filtering operation.</p>
         *
         * @param count the number of values computed by the filter
         */
        void onFilterComplete(int count);
    }

    /**
     * <p>Worker thread handler. When a new filtering request is posted from
     * {@link Filter#filter(CharSequence, FilterListener)},
     * it is sent to this handler.</p>
     */
    private class RequestHandler extends Handler {

        public RequestHandler(Looper looper) {
            super(looper);
        }

        /**
         * <p>Handles filtering requests by calling
         * {@link Filter#performFiltering} and then sending a message
         * with the results to the results handler.</p>
         *
         * @param msg the filtering request
         */
        public void handleMessage(@Nonnull Message msg) {
            int what = msg.what;
            Message message;
            switch (what) {
                case FILTER_TOKEN:
                    RequestArguments args = (RequestArguments) msg.obj;
                    try {
                        args.results = performFiltering(args.constraint);
                    } catch (Exception e) {
                        args.results = new FilterResults();
                        LOGGER.warn(MARKER, "An exception occurred during performFiltering()!", e);
                    } finally {
                        message = mResultHandler.obtainMessage(what);
                        message.obj = args;
                        message.sendToTarget();
                    }

                    synchronized (mLock) {
                        if (mThreadHandler != null) {
                            Message finishMessage = mThreadHandler.obtainMessage(FINISH_TOKEN);
                            mThreadHandler.sendMessageDelayed(finishMessage, 3000);
                        }
                    }
                    break;
                case FINISH_TOKEN:
                    synchronized (mLock) {
                        if (mThreadHandler != null) {
                            mHandlerThread.quit();
                            mThreadHandler = null;
                            mHandlerThread = null;
                        }
                    }
                    break;
            }
        }
    }

    /**
     * <p>Handles the results of a filtering operation. The results are
     * handled in the UI thread.</p>
     */
    private class ResultsHandler extends Handler {

        public ResultsHandler() {
            super(Looper.myLooper());
        }

        /**
         * <p>Messages received from the request handler are processed in the
         * UI thread. The processing involves calling
         * {@link Filter#publishResults(CharSequence,
         * FilterResults)}
         * to post the results back in the UI and then notifying the listener,
         * if any.</p>
         *
         * @param msg the filtering results
         */
        @Override
        public void handleMessage(@Nonnull Message msg) {
            RequestArguments args = (RequestArguments) msg.obj;

            publishResults(args.constraint, args.results);
            if (args.listener != null) {
                int count = args.results != null ? args.results.count : -1;
                args.listener.onFilterComplete(count);
            }
        }
    }

    /**
     * <p>Holds the arguments of a filtering request as well as the results
     * of the request.</p>
     */
    private static class RequestArguments {

        /**
         * <p>The constraint used to filter the data.</p>
         */
        CharSequence constraint;

        /**
         * <p>The listener to notify upon completion. Can be null.</p>
         */
        FilterListener listener;

        /**
         * <p>The results of the filtering operation.</p>
         */
        FilterResults results;
    }

    /**
     * @hide
     */
    public interface Delayer {

        /**
         * @param constraint The constraint passed to {@link Filter#filter(CharSequence)}
         * @return The delay that should be used for
         * {@link Handler#sendMessageDelayed(Message, long)}
         */
        long getPostingDelay(CharSequence constraint);
    }
}
