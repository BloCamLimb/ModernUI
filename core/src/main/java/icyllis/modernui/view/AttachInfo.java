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

import icyllis.modernui.core.Handler;
import icyllis.modernui.graphics.*;

/**
 * A set of information given to a view when it is attached to its parent
 * window.
 */
final class AttachInfo {

    interface Callbacks {

        void playSoundEffect(int effectId);

        boolean performHapticFeedback(int effectId, boolean always);
    }

    final Callbacks mRootCallbacks;

    /**
     * The top view of the hierarchy.
     */
    View mRootView;

    final KeyEvent.DispatcherState mKeyDispatchState = new KeyEvent.DispatcherState();

    /**
     * Indicates whether the view's window currently has the focus.
     */
    boolean mHasWindowFocus = true;

    /**
     * The current visibility of the window.
     */
    int mWindowVisibility;

    /**
     * Indicates whether the view's window is currently in touch mode.
     */
    boolean mInTouchMode = true;

    /**
     * Set if the visibility of any views has changed.
     */
    boolean mViewVisibilityChanged;

    /**
     * Set to true if a view has been scrolled.
     */
    boolean mViewScrollChanged;

    /**
     * The view tree observer used to dispatch global events like
     * layout, pre-draw, touch mode change, etc.
     */
    final ViewTreeObserver mTreeObserver;

    /**
     * The view root.
     */
    final ViewRoot mViewRoot;

    /**
     * A Handler supplied by a view's {@link ViewRoot}. This
     * handler can be used to pump events in the UI events queue.
     */
    final Handler mHandler;

    /**
     * Global to the view hierarchy used as a temporary for dealing with
     * x/y points in the ViewGroup.invalidateChild implementation.
     */
    final int[] mInvalidateChildLocation = new int[2];

    /**
     * Global to the view hierarchy used as a temporary for dealing with
     * computing absolute on-screen location.
     */
    final int[] mTmpLocation = new int[2];

    /**
     * Global to the view hierarchy used as a temporary for dealing with
     * x/y location when view is transformed.
     */
    final float[] mTmpTransformLocation = new float[2];

    /**
     * Temporary for use in computing invalidate rectangles while
     * calling up the hierarchy.
     */
    final Rect mTmpInvalRect = new Rect();

    /**
     * Temporary for use in computing hit areas with transformed views
     */
    final RectF mTmpTransformRect = new RectF();

    /**
     * Temporary for use in transforming invalidation rect
     */
    final Matrix mTmpMatrix = new Matrix();

    /**
     * Temporary for use in querying outlines from OutlineProviders
     */
    final Outline mTmpOutline = new Outline();

    /**
     * Show where the margins, bounds and layout bounds are for each view.
     */
    boolean mDebugLayout = false;

    /**
     * Point used to compute visible regions.
     */
    final Point mPoint = new Point();

    /**
     * Used to track which View originated a requestLayout() call, used when
     * requestLayout() is called during layout.
     */
    View mViewRequestingLayout;

    /**
     * The view that currently has a tooltip displayed.
     */
    View mTooltipHost;

    AttachInfo(ViewRoot viewRoot, Handler handler, Callbacks callbacks) {
        mViewRoot = viewRoot;
        mHandler = handler;
        mRootCallbacks = callbacks;
        mTreeObserver = new ViewTreeObserver();
    }
}
