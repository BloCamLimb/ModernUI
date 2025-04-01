/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.view.Gravity;

/**
 * A Drawable that clips another Drawable based on this Drawable's current
 * level value.  You can control how much the child Drawable gets clipped in width
 * and height based on the level, as well as a gravity to control where it is
 * placed in its overall container.  Most often used to implement things like
 * progress bars, by increasing the drawable's level with {@link
 * Drawable#setLevel(int) setLevel()}.
 * <p class="note"><strong>Note:</strong> The drawable is clipped completely and not visible when
 * the level is 0 and fully revealed when the level is 10,000.</p>
 */
public class ClipDrawable extends DrawableWrapper {
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    private final Rect mTmpRect = new Rect();

    private ClipState mState;

    ClipDrawable() {
        this(new ClipState(null, null), null);
    }

    /**
     * Creates a new clip drawable with the specified gravity and orientation.
     *
     * @param drawable    the drawable to clip
     * @param gravity     gravity constant (see {@link Gravity} used to position
     *                    the clipped drawable within the parent container
     * @param orientation bitwise-or of {@link #HORIZONTAL} and/or
     *                    {@link #VERTICAL}
     */
    public ClipDrawable(Drawable drawable, int gravity, int orientation) {
        this(new ClipState(null, null), null);

        mState.mGravity = gravity;
        mState.mOrientation = orientation;

        setDrawable(drawable);
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        invalidateSelf();
        return true;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Drawable dr = getDrawable();
        if (dr == null || dr.getLevel() == 0) {
            return;
        }

        final Rect r = mTmpRect;
        final Rect bounds = getBounds();
        final int level = getLevel();

        int w = bounds.width();
        final int iw = 0; //mState.mDrawable.getIntrinsicWidth();
        if ((mState.mOrientation & HORIZONTAL) != 0) {
            w -= (w - iw) * (MAX_LEVEL - level) / MAX_LEVEL;
        }

        int h = bounds.height();
        final int ih = 0; //mState.mDrawable.getIntrinsicHeight();
        if ((mState.mOrientation & VERTICAL) != 0) {
            h -= (h - ih) * (MAX_LEVEL - level) / MAX_LEVEL;
        }

        final int layoutDirection = getLayoutDirection();
        Gravity.apply(mState.mGravity, w, h, bounds, r, layoutDirection);

        if (w > 0 && h > 0) {
            canvas.save();
            canvas.clipRect(r);
            dr.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    DrawableWrapperState mutateConstantState() {
        mState = new ClipState(mState, null);
        return mState;
    }

    static final class ClipState extends DrawableWrapperState {
        int mOrientation = HORIZONTAL;
        int mGravity = Gravity.LEFT;

        ClipState(ClipState orig, Resources res) {
            super(orig, res);

            if (orig != null) {
                mOrientation = orig.mOrientation;
                mGravity = orig.mGravity;
            }
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            return new ClipDrawable(this, res);
        }
    }

    private ClipDrawable(ClipState state, Resources res) {
        super(state, res);

        mState = state;
    }
}
