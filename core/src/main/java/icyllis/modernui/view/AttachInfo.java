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

import icyllis.modernui.math.PointF;
import icyllis.modernui.util.TimedAction;

import javax.annotation.Nonnull;

/**
 * A set of information given to a view when it is attached to its parent
 * window.
 */
public final class AttachInfo {

    /**
     * The view root impl.
     */
    final ViewRootImpl mViewRootImpl;

    /**
     * This handler can be used to pump events in the UI events queue.
     */
    final Handler mHandler;

    /**
     * The current visibility of the window.
     */
    public int mWindowVisibility;

    /**
     * The top view of the hierarchy.
     */
    View mRootView;

    PointF mTmpPointF = new PointF();

    public AttachInfo(ViewRootImpl viewRootImpl, Handler handler) {
        mViewRootImpl = viewRootImpl;
        mHandler = handler;
    }

    public interface Handler {

        default boolean post(@Nonnull Runnable r) {
            return postDelayed(r, 0);
        }

        boolean postDelayed(@Nonnull Runnable r, long delayMillis);

        default void postOnAnimation(@Nonnull Runnable r) {
            postOnAnimationDelayed(r, 0);
        }

        void postOnAnimationDelayed(@Nonnull Runnable r, long delayMillis);

        void transfer(@Nonnull TimedAction action, long delayMillis);

        void removeCallbacks(@Nonnull Runnable r);
    }
}
