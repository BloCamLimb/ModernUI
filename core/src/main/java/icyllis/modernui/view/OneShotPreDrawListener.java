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

package icyllis.modernui.view;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * An OnPreDrawListener that will remove itself after one OnPreDraw call. Typical
 * usage is:
 * <pre>{@code
 *     OneShotPreDrawListener.add(view, () -> view.doSomething())
 * }</pre>
 * <p>
 * The onPreDraw always returns true.
 * <p>
 * The listener will also remove itself from the ViewTreeObserver when the view
 * is detached from the view hierarchy. In that case, the Runnable will never be
 * executed.
 */
public final class OneShotPreDrawListener implements ViewTreeObserver.OnPreDrawListener,
        View.OnAttachStateChangeListener {

    private final View mView;
    private ViewTreeObserver mViewTreeObserver;
    private final Runnable mRunnable;

    private OneShotPreDrawListener(@Nonnull View view, @Nonnull Runnable runnable) {
        mView = view;
        mViewTreeObserver = view.getViewTreeObserver();
        mRunnable = runnable;
    }

    /**
     * Creates a OneShotPreDrawListener and adds it to view's ViewTreeObserver.
     *
     * @param view     The view whose ViewTreeObserver the OnPreDrawListener should listen.
     * @param runnable The Runnable to execute in the OnPreDraw (once)
     * @return The added OneShotPreDrawListener. It can be removed prior to
     * the onPreDraw by calling {@link #removeListener()}.
     */
    @Nonnull
    public static OneShotPreDrawListener add(@Nonnull View view, @Nonnull Runnable runnable) {
        Objects.requireNonNull(view);
        Objects.requireNonNull(runnable);

        OneShotPreDrawListener listener = new OneShotPreDrawListener(view, runnable);
        view.getViewTreeObserver().addOnPreDrawListener(listener);
        view.addOnAttachStateChangeListener(listener);
        return listener;
    }

    @Override
    public boolean onPreDraw() {
        removeListener();
        mRunnable.run();
        return true;
    }

    /**
     * Removes the listener from the ViewTreeObserver. This is useful to call if the
     * callback should be removed prior to {@link #onPreDraw()}.
     */
    public void removeListener() {
        if (mViewTreeObserver.isAlive()) {
            mViewTreeObserver.removeOnPreDrawListener(this);
        } else {
            mView.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        mView.removeOnAttachStateChangeListener(this);
    }

    @Override
    public void onViewAttachedToWindow(@Nonnull View v) {
        mViewTreeObserver = v.getViewTreeObserver();
    }

    @Override
    public void onViewDetachedFromWindow(@Nonnull View v) {
        removeListener();
    }
}
