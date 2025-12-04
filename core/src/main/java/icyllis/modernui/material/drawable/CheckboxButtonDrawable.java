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
 */

package icyllis.modernui.material.drawable;

import icyllis.modernui.R;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.material.MaterialDrawable;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.ColorStateList;
import org.jetbrains.annotations.ApiStatus;

/**
 * A button drawable for CheckBox, similar to Material Design.
 */
@ApiStatus.Internal
public class CheckboxButtonDrawable extends MaterialDrawable {

    private static final float SIZE = 24;
    private final int mSize; // 24dp
    private boolean mAnimated;
    private boolean mHasStateLayer;

    private static final float MAX_HOLE_RADIUS = 7;

    private float mScale = 1.0f;
    // Punch a square hole in the box:
    // if is 0, we fill the box; if is max radius, we stroke the box;
    // otherwise clip the difference of the hole
    private float mHoleRadius = MAX_HOLE_RADIUS;

    // We will draw two quads
    private final float[] mLeftQuad = new float[8];
    private final float[] mRightQuad = new float[8];

    private ColorStateList mIconTint;
    private int mIconColor = ~0;

    // -1: init, 0: unchecked, 1: checked, 2: indeterminate
    private int mCurState = -1;
    private int mTransitionToState = -1;

    // current transition, if any
    private Animator mTransition;

    public CheckboxButtonDrawable(@NonNull Resources res, boolean animated, boolean hasStateLayer) {
        mSize = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, SIZE, res.getDisplayMetrics()));
        mAnimated = animated;
        mHasStateLayer = hasStateLayer;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mAlpha <= 0) {
            return;
        }
        Paint paint = Paint.obtain();
        paint.setColor(mColor);
        paint.setAlphaF(paint.getAlphaF() * mAlpha * (1 / 255f));
        final Rect r = getBounds();
        canvas.save();
        canvas.translate(r.exactCenterX(), r.exactCenterY());
        float scale = mScale * mSize * (1 / SIZE);
        canvas.scale(scale, scale);

        if (mHoleRadius >= MAX_HOLE_RADIUS) {
            paint.setStroke(true);
            paint.setStrokeWidth(2);
            canvas.drawRoundRect(-8, -8, 8, 8, 1, paint);
        } else {
            if (mHoleRadius > 0) {
                canvas.clipOutRect(-mHoleRadius, -mHoleRadius, mHoleRadius, mHoleRadius);
            }
            canvas.drawRoundRect(-9, -9, 9, 9, 2, paint);
        }

        paint.setColor(mIconColor);
        paint.setAlphaF(mAlpha * (1 / 255f) * paint.getAlphaF());

        if (mHoleRadius <= 0) {
            canvas.drawEdgeAAQuad(null, mLeftQuad, 0,
                    Canvas.EDGE_AA_FLAG_TOP | Canvas.EDGE_AA_FLAG_RIGHT | Canvas.EDGE_AA_FLAG_BOTTOM, paint);
            canvas.drawEdgeAAQuad(null, mRightQuad, 0,
                    Canvas.EDGE_AA_FLAG_TOP | Canvas.EDGE_AA_FLAG_RIGHT | Canvas.EDGE_AA_FLAG_BOTTOM, paint);
        }

        canvas.restore();
        paint.recycle();
    }

    public void setIconTint(@Nullable ColorStateList iconTint) {
        if (mIconTint != iconTint) {
            mIconTint = iconTint;
            if (iconTint != null) {
                mIconColor = iconTint.getColorForState(getState(), ~0);
            } else {
                mIconColor = ~0;
            }
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        boolean changed = super.onStateChange(stateSet);

        if (mIconTint != null) {
            mIconColor = mIconTint.getColorForState(stateSet, ~0);
            changed = true;
        }

        int toState = 0;
        for (int state : stateSet) {
            if (state == R.attr.state_indeterminate) {
                toState = 2;
                break;
            }
            if (state == R.attr.state_checked) {
                toState = 1;
            }
        }
        changed |= (mAnimated && selectTransition(toState)) || selectDrawable(toState);

        return changed;
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return super.hasFocusStateSpecified() ||
                (mIconTint != null && mIconTint.hasFocusStateSpecified());
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);

        if (mTransition != null && (changed || restart)) {
            if (visible) {
                mTransition.start();
            } else {
                jumpToCurrentState();
            }
        }

        return changed;
    }

    private boolean selectTransition(int toState) {
        return false;
    }

    private boolean selectDrawable(int toState) {
        if (mCurState == toState) {
            return false;
        }

        mCurState = toState;

        if (toState == 2) {
            // indeterminate
            var clip = mLeftQuad;
            clip[0] = -2.6f; clip[1] = 1f;
            clip[2] = -5f; clip[3] = 1f;
            clip[4] = -5f; clip[5] = -1f;
            clip[6] = -2.6f; clip[7] = -1f;
            clip = mRightQuad;
            clip[0] = -2.6f; clip[1] = 1f;
            clip[2] = 5f; clip[3] = 1f;
            clip[4] = 5f; clip[5] = -1f;
            clip[6] = -2.6f; clip[7] = -1f;
            mHoleRadius = 0;
        } else if (toState == 1) {
            // checked, this is y+0.6 off BuiltinIconDrawable
            var clip = mLeftQuad;
            clip[0] = -2f; clip[1] = 5f;
            clip[2] = -6f; clip[3] = 1f;
            clip[4] = -4.6f; clip[5] = -0.4f;
            clip[6] = -2f; clip[7] = 2.2f;
            clip = mRightQuad;
            clip[0] = -2f; clip[1] = 5f;
            clip[2] = 6f; clip[3] = -3f;
            clip[4] = 4.6f; clip[5] = -4.4f;
            clip[6] = -2f; clip[7] = 2.2f;
            mHoleRadius = 0;
        } else {
            // unchecked
            mHoleRadius = MAX_HOLE_RADIUS;
        }

        mScale = 1.0f;

        invalidateSelf();

        return true;
    }

    @Override
    public void jumpToCurrentState() {
        super.jumpToCurrentState();

        if (mTransition != null) {
            mTransition.cancel();
            mTransition = null;

            selectDrawable(mTransitionToState);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        if (mHasStateLayer) {
            return mSize * 3 / 2;
        }
        return mSize;
    }

    @Override
    public int getIntrinsicHeight() {
        if (mHasStateLayer) {
            return mSize * 3 / 2;
        }
        return mSize;
    }
}
