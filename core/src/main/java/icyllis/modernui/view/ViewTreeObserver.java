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

package icyllis.modernui.view;

import javax.annotation.Nonnull;
import java.util.ArrayList;

/**
 * A view tree observer is used to register listeners that can be notified of global
 * changes in the view tree. Such global events include, but are not limited to,
 * layout of the whole tree, beginning of the drawing pass, touch mode change....
 * <p>
 * A ViewTreeObserver should never be instantiated by applications as it is provided
 * by the views hierarchy. Refer to {@link View#getViewTreeObserver()}
 * for more information.
 */
public final class ViewTreeObserver {

    private CopyOnWriteArray<OnGlobalLayoutListener> mOnGlobalLayoutListeners;
    private CopyOnWriteArray<OnPreDrawListener> mOnPreDrawListeners;
    private CopyOnWriteArray<OnScrollChangedListener> mOnScrollChangedListeners;

    private boolean mAlive = true;

    /**
     * Interface definition for a callback to be invoked when the global layout state
     * or the visibility of views within the view tree changes.
     */
    @FunctionalInterface
    public interface OnGlobalLayoutListener {

        /**
         * Callback method to be invoked when the global layout state or the visibility of views
         * within the view tree changes
         */
        void onGlobalLayout();
    }

    /**
     * Interface definition for a callback to be invoked when the view tree is about to be drawn.
     */
    @FunctionalInterface
    public interface OnPreDrawListener {

        /**
         * Callback method to be invoked when the view tree is about to be drawn. At this point, all
         * views in the tree have been measured and given a frame. Clients can use this to adjust
         * their scroll bounds or even to request a new layout before drawing occurs.
         *
         * @return Return true to proceed with the current drawing pass, or false to cancel.
         * @see View#onMeasure
         * @see View#onLayout
         * @see View#onDraw
         */
        boolean onPreDraw();
    }

    /**
     * Interface definition for a callback to be invoked when
     * something in the view tree has been scrolled.
     */
    @FunctionalInterface
    public interface OnScrollChangedListener {

        /**
         * Callback method to be invoked when something in the view tree
         * has been scrolled.
         */
        void onScrollChanged();
    }

    ViewTreeObserver() {
    }

    /**
     * Merges all the listeners registered on the specified observer with the listeners
     * registered on this object. After this method is invoked, the specified observer
     * will return false in {@link #isAlive()} and should not be used anymore.
     *
     * @param observer The ViewTreeObserver whose listeners must be added to this observer
     */
    void merge(@Nonnull ViewTreeObserver observer) {
        if (observer.mOnGlobalLayoutListeners != null) {
            if (mOnGlobalLayoutListeners != null) {
                mOnGlobalLayoutListeners.addAll(observer.mOnGlobalLayoutListeners);
            } else {
                mOnGlobalLayoutListeners = observer.mOnGlobalLayoutListeners;
            }
        }

        if (observer.mOnPreDrawListeners != null) {
            if (mOnPreDrawListeners != null) {
                mOnPreDrawListeners.addAll(observer.mOnPreDrawListeners);
            } else {
                mOnPreDrawListeners = observer.mOnPreDrawListeners;
            }
        }

        if (observer.mOnScrollChangedListeners != null) {
            if (mOnScrollChangedListeners != null) {
                mOnScrollChangedListeners.addAll(observer.mOnScrollChangedListeners);
            } else {
                mOnScrollChangedListeners = observer.mOnScrollChangedListeners;
            }
        }

        observer.kill();
    }

    /**
     * Register a callback to be invoked when the global layout state or the visibility of views
     * within the view tree changes
     *
     * @param listener The callback to add
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnGlobalLayoutListener(@Nonnull OnGlobalLayoutListener listener) {
        checkIsAlive();
        if (mOnGlobalLayoutListeners == null) {
            mOnGlobalLayoutListeners = new CopyOnWriteArray<>();
        }
        mOnGlobalLayoutListeners.add(listener);
    }

    /**
     * Remove a previously installed global layout callback
     *
     * @param victim The callback to remove
     * @throws IllegalStateException If {@link #isAlive()} returns false
     * @see #addOnGlobalLayoutListener(OnGlobalLayoutListener)
     */
    public void removeOnGlobalLayoutListener(@Nonnull OnGlobalLayoutListener victim) {
        checkIsAlive();
        if (mOnGlobalLayoutListeners == null) {
            return;
        }
        mOnGlobalLayoutListeners.remove(victim);
    }

    /**
     * Register a callback to be invoked when the view tree is about to be drawn
     *
     * @param listener The callback to add
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnPreDrawListener(@Nonnull OnPreDrawListener listener) {
        checkIsAlive();
        if (mOnPreDrawListeners == null) {
            mOnPreDrawListeners = new CopyOnWriteArray<>();
        }
        mOnPreDrawListeners.add(listener);
    }

    /**
     * Remove a previously installed pre-draw callback
     *
     * @param victim The callback to remove
     * @throws IllegalStateException If {@link #isAlive()} returns false
     * @see #addOnPreDrawListener(OnPreDrawListener)
     */
    public void removeOnPreDrawListener(@Nonnull OnPreDrawListener victim) {
        checkIsAlive();
        if (mOnPreDrawListeners == null) {
            return;
        }
        mOnPreDrawListeners.remove(victim);
    }

    /**
     * Register a callback to be invoked when a view has been scrolled.
     *
     * @param listener The callback to add
     * @throws IllegalStateException If {@link #isAlive()} returns false
     */
    public void addOnScrollChangedListener(@Nonnull OnScrollChangedListener listener) {
        checkIsAlive();
        if (mOnScrollChangedListeners == null) {
            mOnScrollChangedListeners = new CopyOnWriteArray<>();
        }
        mOnScrollChangedListeners.add(listener);
    }

    /**
     * Remove a previously installed scroll-changed callback
     *
     * @param victim The callback to remove
     * @throws IllegalStateException If {@link #isAlive()} returns false
     * @see #addOnScrollChangedListener(OnScrollChangedListener)
     */
    public void removeOnScrollChangedListener(@Nonnull OnScrollChangedListener victim) {
        checkIsAlive();
        if (mOnScrollChangedListeners == null) {
            return;
        }
        mOnScrollChangedListeners.remove(victim);
    }

    private void checkIsAlive() {
        if (!mAlive) {
            throw new IllegalStateException("This ViewTreeObserver is not alive, call "
                    + "getViewTreeObserver() again");
        }
    }

    /**
     * Indicates whether this ViewTreeObserver is alive. When an observer is not alive,
     * any call to a method (except this one) will throw an exception.
     * <p>
     * If an application keeps a long-lived reference to this ViewTreeObserver, it should
     * always check for the result of this method before calling any other method.
     *
     * @return True if this object is alive and be used, false otherwise.
     */
    public boolean isAlive() {
        return mAlive;
    }

    /**
     * Marks this ViewTreeObserver as not alive. After invoking this method, invoking
     * any other method but {@link #isAlive()} and this will throw an Exception.
     */
    private void kill() {
        mAlive = false;
    }

    /**
     * Notifies registered listeners that a global layout happened. This can be called
     * manually if you are forcing a layout on a View or a hierarchy of Views that are
     * not attached to a Window or in the GONE state.
     */
    public void dispatchOnGlobalLayout() {
        // NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
        // perform the dispatching. The iterator is a safeguard against listeners that
        // could mutate the list by calling the various add/remove methods. This prevents
        // the array from being modified while we iterate it.
        final CopyOnWriteArray<OnGlobalLayoutListener> listeners = mOnGlobalLayoutListeners;
        if (listeners != null && listeners.size() > 0) {
            CopyOnWriteArray.Access<OnGlobalLayoutListener> access = listeners.start();
            try {
                int count = access.size();
                for (int i = 0; i < count; i++) {
                    access.get(i).onGlobalLayout();
                }
            } finally {
                listeners.end();
            }
        }
    }

    /**
     * Notifies registered listeners that the drawing pass is about to start. If a
     * listener returns true, then the drawing pass is canceled and rescheduled. This can
     * be called manually if you are forcing the drawing on a View or a hierarchy of Views
     * that are not attached to a Window or in the GONE state.
     *
     * @return True if the current draw should be canceled and rescheduled, false otherwise.
     */
    public boolean dispatchOnPreDraw() {
        boolean cancelDraw = false;
        final CopyOnWriteArray<OnPreDrawListener> listeners = mOnPreDrawListeners;
        if (listeners != null && listeners.size() > 0) {
            CopyOnWriteArray.Access<OnPreDrawListener> access = listeners.start();
            try {
                int count = access.size();
                for (int i = 0; i < count; i++) {
                    cancelDraw |= !(access.get(i).onPreDraw());
                }
            } finally {
                listeners.end();
            }
        }
        return cancelDraw;
    }

    /**
     * Notifies registered listeners that something has scrolled.
     */
    public void dispatchOnScrollChanged() {
        // NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
        // perform the dispatching. The iterator is a safeguard against listeners that
        // could mutate the list by calling the various add/remove methods. This prevents
        // the array from being modified while we iterate it.
        final CopyOnWriteArray<OnScrollChangedListener> listeners = mOnScrollChangedListeners;
        if (listeners != null && listeners.size() > 0) {
            CopyOnWriteArray.Access<OnScrollChangedListener> access = listeners.start();
            try {
                int count = access.size();
                for (int i = 0; i < count; i++) {
                    access.get(i).onScrollChanged();
                }
            } finally {
                listeners.end();
            }
        }
    }

    /**
     * Copy on write array. This array is not thread safe, and only one loop can
     * iterate over this array at any given time. This class avoids allocations
     * until a concurrent modification happens.
     * <p>
     * Usage:
     * <pre>
     *  CopyOnWriteArray.Access&lt;MyData&gt; access = array.start();
     *  try {
     *      for (int i = 0; i < access.size(); i++) {
     *          MyData d = access.get(i);
     *      }
     *  } finally {
     *      access.end();
     *  }
     * </pre>
     */
    static class CopyOnWriteArray<T> {

        private ArrayList<T> mData = new ArrayList<>();
        private ArrayList<T> mDataCopy;

        private final Access<T> mAccess = new Access<>();

        private boolean mStart;

        static class Access<T> {
            private ArrayList<T> mData;
            private int mSize;

            T get(int index) {
                return mData.get(index);
            }

            int size() {
                return mSize;
            }
        }

        CopyOnWriteArray() {
        }

        private ArrayList<T> getArray() {
            if (mStart) {
                if (mDataCopy == null) mDataCopy = new ArrayList<>(mData);
                return mDataCopy;
            }
            return mData;
        }

        Access<T> start() {
            if (mStart) throw new IllegalStateException("Iteration already started");
            mStart = true;
            mDataCopy = null;
            mAccess.mData = mData;
            mAccess.mSize = mData.size();
            return mAccess;
        }

        void end() {
            if (!mStart) throw new IllegalStateException("Iteration not started");
            mStart = false;
            if (mDataCopy != null) {
                mData = mDataCopy;
                mAccess.mData.clear();
                mAccess.mSize = 0;
            }
            mDataCopy = null;
        }

        int size() {
            return getArray().size();
        }

        void add(T item) {
            getArray().add(item);
        }

        void addAll(@Nonnull CopyOnWriteArray<T> array) {
            getArray().addAll(array.mData);
        }

        void remove(T item) {
            getArray().remove(item);
        }

        void clear() {
            getArray().clear();
        }
    }
}
