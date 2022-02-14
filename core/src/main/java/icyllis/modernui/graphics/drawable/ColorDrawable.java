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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.util.ColorStateList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A specialized Drawable that fills the Canvas with a specified color.
 * Note that a ColorDrawable ignores the ColorFilter.
 */
public class ColorDrawable extends Drawable {

    private final Paint mPaint = new Paint();

    private ColorState mColorState;
    private int mBlendColor;

    private boolean mMutated;

    /**
     * Creates a new black ColorDrawable.
     */
    public ColorDrawable() {
        mColorState = new ColorState();
    }

    /**
     * Creates a new ColorDrawable with the specified color.
     *
     * @param color The color to draw.
     */
    public ColorDrawable(int color) {
        mColorState = new ColorState();

        setColor(color);
    }

    private ColorDrawable(@Nonnull ColorState state) {
        mColorState = state;
        if (state.mTint != null) {
            mBlendColor = state.mTint.getColorForState(getState(), 0);
        }
    }

    /**
     * A mutable BitmapDrawable still shares its Bitmap with any other Drawable
     * that comes from the same resource.
     *
     * @return This drawable.
     */
    @Nonnull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mColorState = new ColorState(mColorState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        if (mColorState.mUseColor >>> 24 != 0) {
            if (mBlendColor >>> 24 == 0) {
                mPaint.setColor(mColorState.mUseColor);
            } else {
                //TODO change the blendFunc when pre-multiplied alpha pipeline available
                final int src = mBlendColor;
                final int srcA = src >>> 24;
                int srcR = ((src >> 16) & 0xFF) * srcA >> 8;
                int srcG = ((src >> 8) & 0xFF) * srcA >> 8;
                int srcB = (src & 0xFF) * srcA >> 8;

                final int dst = mColorState.mUseColor;
                int dstR = ((dst >> 16) & 0xFF) * (1 - srcA) >> 8;
                int dstG = ((dst >> 8) & 0xFF) * (1 - srcA) >> 8;
                int dstB = (dst & 0xFF) * (1 - srcA) >> 8;
                int dstA = (dst >>> 24) * (1 - srcA) >> 8;
                mPaint.setRGBA(srcR + dstR, srcG + dstG, srcB + dstB, srcA + dstA);
            }
            canvas.drawRect(getBounds(), mPaint);
        }
    }

    /**
     * Gets the drawable's color value.
     *
     * @return int The color to draw.
     */
    public int getColor() {
        return mColorState.mUseColor;
    }

    /**
     * Sets the drawable's color value. This action will clobber the results of
     * prior calls to {@link #setAlpha(int)} on this object, which side-affected
     * the underlying color.
     *
     * @param color The color to draw.
     */
    public void setColor(int color) {
        if (mColorState.mBaseColor != color || mColorState.mUseColor != color) {
            mColorState.mBaseColor = mColorState.mUseColor = color;
            invalidateSelf();
        }
    }

    /**
     * Returns the alpha value of this drawable's color. Note this may not be the same alpha value
     * provided in {@link Drawable#setAlpha(int)}. Instead, this will return the alpha of the color
     * combined with the alpha provided by setAlpha
     *
     * @return A value between 0 and 255.
     * @see ColorDrawable#setAlpha(int)
     */
    @Override
    public int getAlpha() {
        return mColorState.mUseColor >>> 24;
    }

    /**
     * Applies the given alpha to the underlying color. Note if the color already has
     * an alpha applied to it, this will apply this alpha to the existing value instead of
     * overwriting it.
     *
     * @param alpha The alpha value to set, between 0 and 255.
     */
    @Override
    public void setAlpha(int alpha) {
        alpha &= 0xFF;
        final int baseAlpha = mColorState.mBaseColor >>> 24;
        final int useAlpha = baseAlpha * alpha >> 8;
        final int useColor = (mColorState.mBaseColor << 8 >>> 8) | (useAlpha << 24);
        if (mColorState.mUseColor != useColor) {
            mColorState.mUseColor = useColor;
            invalidateSelf();
        }
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mColorState.mTint = tint;
        if (tint == null) {
            mBlendColor = 0;
        } else {
            mBlendColor = tint.getColorForState(getState(), 0);
        }
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] stateSet) {
        final ColorState state = mColorState;
        if (state.mTint != null) {
            mBlendColor = state.mTint.getColorForState(stateSet, 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return mColorState.mTint != null && mColorState.mTint.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mColorState.mTint != null && mColorState.mTint.hasFocusStateSpecified();
    }

    @Override
    public ConstantState getConstantState() {
        return mColorState;
    }

    static final class ColorState extends ConstantState {

        int mBaseColor; // base color, independent of setAlpha()
        int mUseColor;  // base color modulated by setAlpha()
        ColorStateList mTint = null;

        ColorState() {
            // Empty constructor.
        }

        ColorState(@Nonnull ColorState state) {
            mBaseColor = state.mBaseColor;
            mUseColor = state.mUseColor;
            mTint = state.mTint;
        }

        @Nonnull
        @Override
        public Drawable newDrawable() {
            return new ColorDrawable(this);
        }
    }
}
