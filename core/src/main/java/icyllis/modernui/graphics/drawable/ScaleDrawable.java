/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.view.Gravity;

public class ScaleDrawable extends DrawableWrapper {

    private final Rect mTmpRect = new Rect();

    private ScaleState mState;

    ScaleDrawable() {
        this(new ScaleState(null, null), null);
    }

    /**
     * Creates a new scale drawable with the specified gravity and scale
     * properties.
     *
     * @param drawable    the drawable to scale
     * @param gravity     gravity constant (see {@link Gravity} used to position
     *                    the scaled drawable within the parent container
     * @param scaleWidth  width scaling factor [0...1] to use then the level is
     *                    at the maximum value, or -1 to not scale width
     * @param scaleHeight height scaling factor [0...1] to use then the level
     *                    is at the maximum value, or -1 to not scale height
     */
    public ScaleDrawable(Drawable drawable, int gravity, float scaleWidth, float scaleHeight) {
        this(new ScaleState(null, null), null);

        mState.mGravity = gravity;
        mState.mScaleWidth = scaleWidth;
        mState.mScaleHeight = scaleHeight;

        setDrawable(drawable);
    }

    public void setGravity(int gravity) {
        mState.mGravity = gravity;
        invalidateSelf();
    }

    public void setScaleWidth(float scaleWidth) {
        mState.mScaleWidth = scaleWidth;
        invalidateSelf();
    }

    public void setScaleHeight(float scaleHeight) {
        mState.mScaleHeight = scaleHeight;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Drawable d = getDrawable();
        if (d != null && d.getLevel() != 0) {
            d.draw(canvas);
        }
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        onBoundsChange(getBounds());
        invalidateSelf();
        return true;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        final Drawable d = getDrawable();
        if (d == null) {
            return;
        }
        final Rect r = mTmpRect;
        final boolean min = mState.mUseIntrinsicSizeAsMin;
        final int level = getLevel();

        int w = bounds.width();
        if (mState.mScaleWidth > 0) {
            final int iw = min ? d.getIntrinsicWidth() : 0;
            w -= (int) ((w - iw) * (MAX_LEVEL - level) * mState.mScaleWidth / MAX_LEVEL);
        }

        int h = bounds.height();
        if (mState.mScaleHeight > 0) {
            final int ih = min ? d.getIntrinsicHeight() : 0;
            h -= (int) ((h - ih) * (MAX_LEVEL - level) * mState.mScaleHeight / MAX_LEVEL);
        }

        final int layoutDirection = getLayoutDirection();
        Gravity.apply(mState.mGravity, w, h, bounds, r, layoutDirection);

        if (w > 0 && h > 0) {
            d.setBounds(r.left, r.top, r.right, r.bottom);
        }
    }

    @Override
    DrawableWrapperState mutateConstantState() {
        mState = new ScaleState(mState, null);
        return mState;
    }

    static final class ScaleState extends DrawableWrapperState {
        /**
         * Constant used to disable scaling for a particular dimension.
         */
        private static final float DO_NOT_SCALE = -1.0f;

        float mScaleWidth = DO_NOT_SCALE;
        float mScaleHeight = DO_NOT_SCALE;
        int mGravity = Gravity.LEFT;
        boolean mUseIntrinsicSizeAsMin = false;
        int mInitialLevel = 0;

        ScaleState(ScaleState orig, Resources res) {
            super(orig, res);

            if (orig != null) {
                mScaleWidth = orig.mScaleWidth;
                mScaleHeight = orig.mScaleHeight;
                mGravity = orig.mGravity;
                mUseIntrinsicSizeAsMin = orig.mUseIntrinsicSizeAsMin;
                mInitialLevel = orig.mInitialLevel;
            }
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            return new ScaleDrawable(this, res);
        }
    }

    /**
     * Creates a new ScaleDrawable based on the specified constant state.
     * <p>
     * The resulting drawable is guaranteed to have a new constant state.
     *
     * @param state constant state from which the drawable inherits
     */
    private ScaleDrawable(ScaleState state, Resources res) {
        super(state, res);

        mState = state;

        updateLocalState();
    }

    private void updateLocalState() {
        setLevel(mState.mInitialLevel);
    }
}
