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

import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.util.ColorStateList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A specialized Drawable that fills the Canvas with a specified color.
 */
public class ColorDrawable extends Drawable {

    private ColorState mColorState;
    private BlendMode mBlendMode;
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
    public ColorDrawable(@ColorInt int color) {
        mColorState = new ColorState();
        setColor(color);
    }

    private ColorDrawable(@Nonnull ColorState state) {
        mColorState = state;
        updateLocalState();
    }

    /**
     * Initializes local dynamic properties from state.
     */
    private void updateLocalState() {
        if (mColorState.mTint == null || mColorState.mBlendMode == null) {
            mBlendMode = null;
            return;
        }
        mBlendMode = mColorState.mBlendMode;
        mBlendColor = mColorState.mTint.getColorForState(getState(), Color.TRANSPARENT);
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
        int color = mColorState.mUseColor;
        if (Color.alpha(color) != 0 || mBlendMode != null) {
            if (mBlendMode != null) {
                color = Color.blend(mBlendMode, mBlendColor, color);
                if (Color.alpha(color) == 0) {
                    return;
                }
            }
            Paint paint = Paint.get();
            paint.setColor(color);
            canvas.drawRect(getBounds(), paint);
        }
    }

    /**
     * Gets the drawable's color value.
     *
     * @return int The color to draw.
     */
    @ColorInt
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
    public void setColor(@ColorInt int color) {
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
        alpha += alpha >> 7;   // make it 0..256
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
        updateLocalState();
        invalidateSelf();
    }

    @Override
    public void setTintBlendMode(@Nonnull BlendMode blendMode) {
        mColorState.mBlendMode = blendMode;
        updateLocalState();
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] stateSet) {
        final ColorState state = mColorState;
        if (state.mTint != null && state.mBlendMode != null) {
            mBlendMode = mColorState.mBlendMode;
            mBlendColor = mColorState.mTint.getColorForState(getState(), Color.TRANSPARENT);
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
        int mUseColor;  // base color, modulated by setAlpha()
        ColorStateList mTint = null;
        BlendMode mBlendMode = DEFAULT_BLEND_MODE;

        ColorState() {
            // Empty constructor.
        }

        ColorState(@Nonnull ColorState state) {
            mBaseColor = state.mBaseColor;
            mUseColor = state.mUseColor;
            mTint = state.mTint;
            mBlendMode = state.mBlendMode;
        }

        @Nonnull
        @Override
        public Drawable newDrawable() {
            return new ColorDrawable(this);
        }
    }
}
