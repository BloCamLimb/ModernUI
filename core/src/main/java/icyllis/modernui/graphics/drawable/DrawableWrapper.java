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

import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.*;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DisplayMetrics;
import icyllis.modernui.view.View;

/**
 * Drawable container with only one child element.
 */
public abstract class DrawableWrapper extends Drawable implements Drawable.Callback {

    private DrawableWrapperState mState;
    private Drawable mDrawable;
    private boolean mMutated;

    DrawableWrapper(DrawableWrapperState state, Resources res) {
        mState = state;

        updateLocalState(res);
    }

    /**
     * Creates a new wrapper around the specified drawable.
     *
     * @param dr the drawable to wrap
     */
    public DrawableWrapper(@Nullable Drawable dr) {
        mState = null;
        setDrawable(dr);
    }

    /**
     * Initializes local dynamic properties from state. This should be called
     * after significant state changes, e.g. from the One True Constructor and
     * after inflating or applying a theme.
     */
    private void updateLocalState(Resources res) {
        if (mState != null && mState.mDrawableState != null) {
            final Drawable dr = mState.mDrawableState.newDrawable(res);
            setDrawable(dr);
        }
    }

    /**
     * Sets the wrapped drawable.
     *
     * @param dr the wrapped drawable
     */
    public void setDrawable(@Nullable Drawable dr) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }

        mDrawable = dr;

        if (dr != null) {
            dr.setCallback(this);

            // Only call setters for data that's stored in the base Drawable.
            dr.setVisible(isVisible(), true);
            dr.setState(getState());
            dr.setLevel(getLevel());
            dr.setBounds(getBounds());
            dr.setLayoutDirection(getLayoutDirection());

            if (mState != null) {
                mState.mDrawableState = dr.getConstantState();
            }
        }

        invalidateSelf();
    }

    /**
     * @return the wrapped drawable
     */
    @Nullable
    public Drawable getDrawable() {
        return mDrawable;
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        final Callback callback = getCallback();
        if (callback != null) {
            callback.unscheduleDrawable(this, what);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mDrawable != null) {
            mDrawable.draw(canvas);
        }
    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
        return mDrawable != null && mDrawable.getPadding(padding);
    }

    @Override
    public void setHotspot(float x, float y) {
        if (mDrawable != null) {
            mDrawable.setHotspot(x, y);
        }
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        if (mDrawable != null) {
            mDrawable.setHotspotBounds(left, top, right, bottom);
        }
    }

    @Override
    public void getHotspotBounds(@NonNull Rect outRect) {
        if (mDrawable != null) {
            mDrawable.getHotspotBounds(outRect);
        } else {
            outRect.set(getBounds());
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        final boolean superChanged = super.setVisible(visible, restart);
        final boolean changed = mDrawable != null && mDrawable.setVisible(visible, restart);
        return superChanged | changed;
    }

    @Override
    public void setAlpha(int alpha) {
        if (mDrawable != null) {
            mDrawable.setAlpha(alpha);
        }
    }

    @Override
    public int getAlpha() {
        return mDrawable != null ? mDrawable.getAlpha() : 255;
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (mDrawable != null) {
            mDrawable.setTintList(tint);
        }
    }

    @Override
    public void setTintBlendMode(@NonNull BlendMode blendMode) {
        if (mDrawable != null) {
            mDrawable.setTintBlendMode(blendMode);
        }
    }

    @Override
    public boolean onLayoutDirectionChanged(@View.ResolvedLayoutDir int layoutDirection) {
        return mDrawable != null && mDrawable.setLayoutDirection(layoutDirection);
    }

    @Override
    public boolean isStateful() {
        return mDrawable != null && mDrawable.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mDrawable != null && mDrawable.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(@NonNull int[] state) {
        if (mDrawable != null && mDrawable.isStateful()) {
            final boolean changed = mDrawable.setState(state);
            if (changed) {
                onBoundsChange(getBounds());
            }
            return changed;
        }
        return false;
    }

    @Override
    public void jumpToCurrentState() {
        if (mDrawable != null) {
            mDrawable.jumpToCurrentState();
        }
    }

    @Override
    protected boolean onLevelChange(int level) {
        return mDrawable != null && mDrawable.setLevel(level);
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        if (mDrawable != null) {
            mDrawable.setBounds(bounds);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return mDrawable != null ? mDrawable.getIntrinsicWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return mDrawable != null ? mDrawable.getIntrinsicHeight() : -1;
    }

    @Override
    @Nullable
    public ConstantState getConstantState() {
        if (mState != null && mState.canConstantState()) {
            return mState;
        }
        return null;
    }

    @Override
    @NonNull
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mState = mutateConstantState();
            if (mDrawable != null) {
                mDrawable.mutate();
            }
            if (mState != null) {
                mState.mDrawableState = mDrawable != null ? mDrawable.getConstantState() : null;
            }
            mMutated = true;
        }
        return this;
    }

    /**
     * Mutates the constant state and returns the new state. Responsible for
     * updating any local copy.
     * <p>
     * This method should never call the super implementation; it should always
     * mutate and return its own constant state.
     *
     * @return the new state
     */
    DrawableWrapperState mutateConstantState() {
        return mState;
    }

    /**
     * @hide Only used by the framework for pre-loading resources.
     */
    public void clearMutated() {
        super.clearMutated();
        if (mDrawable != null) {
            mDrawable.clearMutated();
        }
        mMutated = false;
    }

    abstract static class DrawableWrapperState extends Drawable.ConstantState {
        private int[] mThemeAttrs;

        int mChangingConfigurations;
        int mDensity = DisplayMetrics.DENSITY_DEFAULT;

        /**
         * The density to use when looking up resources from
         * {@link Resources#getDrawableForDensity(int, int, Theme)}.
         * A value of 0 means there is no override and the system density will be used.
         */
        int mSrcDensityOverride = 0;

        Drawable.ConstantState mDrawableState;

        DrawableWrapperState(@Nullable DrawableWrapperState orig, @Nullable Resources res) {
            if (orig != null) {
                mThemeAttrs = orig.mThemeAttrs;
                mChangingConfigurations = orig.mChangingConfigurations;
                mDrawableState = orig.mDrawableState;
                mSrcDensityOverride = orig.mSrcDensityOverride;
            }

            final int density;
            if (res != null) {
                density = res.getDisplayMetrics().densityDpi;
            } else if (orig != null) {
                density = orig.mDensity;
            } else {
                density = 0;
            }

            mDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
        }

        /**
         * Sets the constant state density.
         * <p>
         * If the density has been previously set, dispatches the change to
         * subclasses so that density-dependent properties may be scaled as
         * necessary.
         *
         * @param targetDensity the new constant state density
         */
        public final void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                final int sourceDensity = mDensity;
                mDensity = targetDensity;

                onDensityChanged(sourceDensity, targetDensity);
            }
        }

        /**
         * Called when the constant state density changes.
         * <p>
         * Subclasses with density-dependent constant state properties should
         * override this method and scale their properties as necessary.
         *
         * @param sourceDensity the previous constant state density
         * @param targetDensity the new constant state density
         */
        void onDensityChanged(int sourceDensity, int targetDensity) {
            // Stub method.
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return newDrawable(null);
        }

        @NonNull
        @Override
        public abstract Drawable newDrawable(@Nullable Resources res);

        public boolean canConstantState() {
            return mDrawableState != null;
        }
    }
}
