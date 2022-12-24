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

import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.util.ColorStateList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A Drawable that manages a {@link ColorDrawable} to make it stateful and backed by a
 * {@link ColorStateList}.
 */
public class ColorStateListDrawable extends Drawable implements Drawable.Callback {

    private ColorDrawable mColorDrawable;
    private ColorStateListDrawableState mState;
    private boolean mMutated = false;

    public ColorStateListDrawable() {
        mState = new ColorStateListDrawableState();
        initializeColorDrawable();
    }

    public ColorStateListDrawable(@Nonnull ColorStateList colorStateList) {
        mState = new ColorStateListDrawableState();
        initializeColorDrawable();
        setColorStateList(colorStateList);
    }

    private ColorStateListDrawable(@Nonnull ColorStateListDrawableState state) {
        mState = state;
        initializeColorDrawable();
        onStateChange(getState());
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        mColorDrawable.draw(canvas);
    }

    @Override
    public int getAlpha() {
        return mColorDrawable.getAlpha();
    }

    @Override
    public boolean isStateful() {
        return mState.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mState.hasFocusStateSpecified();
    }

    @Nonnull
    @Override
    public Drawable getCurrent() {
        return mColorDrawable;
    }

    @Override
    public void setAlpha(int alpha) {
        mState.mAlpha = alpha;
        onStateChange(getState());
    }

    /**
     * Remove the alpha override, reverting to the alpha defined on each color in the
     * {@link ColorStateList}.
     */
    public void clearAlpha() {
        mState.mAlpha = -1;
        onStateChange(getState());
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        mState.mTint = tint;
        mColorDrawable.setTintList(tint);
        onStateChange(getState());
    }

    @Override
    public void setTintBlendMode(@Nonnull BlendMode blendMode) {
        mState.mBlendMode = blendMode;
        mColorDrawable.setTintBlendMode(blendMode);
        onStateChange(getState());
    }

    @Override
    protected void onBoundsChange(@Nonnull Rect bounds) {
        super.onBoundsChange(bounds);
        mColorDrawable.setBounds(bounds);
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] state) {
        if (mState.mColor != null) {
            int color = mState.mColor.getColorForState(state, mState.mColor.getDefaultColor());

            if (mState.mAlpha != -1) {
                color = (color & 0xFFFFFF) | mState.mAlpha << 24;
            }

            if (color != mColorDrawable.getColor()) {
                mColorDrawable.setColor(color);
                mColorDrawable.setState(state);
                return true;
            } else {
                return mColorDrawable.setState(state);
            }
        } else {
            return false;
        }
    }

    @Override
    public void invalidateDrawable(@Nonnull Drawable who) {
        if (who == mColorDrawable && getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what, long when) {
        if (who == mColorDrawable && getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what) {
        if (who == mColorDrawable && getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
    }

    @Nonnull
    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    /**
     * Returns the ColorStateList backing this Drawable, or a new ColorStateList of the default
     * ColorDrawable color if one hasn't been defined yet.
     *
     * @return a ColorStateList
     */
    @Nonnull
    public ColorStateList getColorStateList() {
        return Objects.requireNonNullElseGet(mState.mColor, () -> ColorStateList.valueOf(mColorDrawable.getColor()));
    }

    @Nonnull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mState = new ColorStateListDrawableState(mState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    /**
     * Replace this Drawable's ColorStateList. It is not copied, so changes will propagate on the
     * next call to {@link #setState(int[])}.
     *
     * @param colorStateList A color state list to attach.
     */
    public void setColorStateList(@Nonnull ColorStateList colorStateList) {
        mState.mColor = colorStateList;
        onStateChange(getState());
    }

    static final class ColorStateListDrawableState extends ConstantState {

        ColorStateList mColor = null;
        ColorStateList mTint = null;
        int mAlpha = -1;
        BlendMode mBlendMode = DEFAULT_BLEND_MODE;

        ColorStateListDrawableState() {
        }

        ColorStateListDrawableState(ColorStateListDrawableState state) {
            mColor = state.mColor;
            mTint = state.mTint;
            mAlpha = state.mAlpha;
            mBlendMode = state.mBlendMode;
        }

        @Nonnull
        @Override
        public Drawable newDrawable() {
            return new ColorStateListDrawable(this);
        }

        public boolean isStateful() {
            return (mColor != null && mColor.isStateful())
                    || (mTint != null && mTint.isStateful());
        }

        public boolean hasFocusStateSpecified() {
            return (mColor != null && mColor.hasFocusStateSpecified())
                    || (mTint != null && mTint.hasFocusStateSpecified());
        }
    }

    private void initializeColorDrawable() {
        mColorDrawable = new ColorDrawable();
        mColorDrawable.setCallback(this);

        if (mState.mTint != null) {
            mColorDrawable.setTintList(mState.mTint);
        }

        if (mState.mBlendMode != DEFAULT_BLEND_MODE) {
            mColorDrawable.setTintBlendMode(mState.mBlendMode);
        }
    }
}
