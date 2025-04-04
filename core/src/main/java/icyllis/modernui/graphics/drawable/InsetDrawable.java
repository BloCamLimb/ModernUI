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
 *   Copyright (C) 2008 The Android Open Source Project
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
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.Rect;
import icyllis.modernui.resources.Resources;
import icyllis.modernui.util.DisplayMetrics;

/**
 * A Drawable that insets another Drawable by a specified distance or fraction of the content bounds.
 * This is used when a View needs a background that is smaller than
 * the View's actual bounds.
 */
public class InsetDrawable extends DrawableWrapper {
    private final Rect mTmpRect = new Rect();
    private final Rect mTmpInsetRect = new Rect();

    private InsetState mState;

    /**
     * No-arg constructor used by drawable inflation.
     */
    InsetDrawable() {
        this(new InsetState(null, null), null);
    }

    /**
     * Creates a new inset drawable with the specified inset.
     *
     * @param drawable The drawable to inset.
     * @param inset    Inset in pixels around the drawable.
     */
    public InsetDrawable(@Nullable Drawable drawable, int inset) {
        this(drawable, inset, inset, inset, inset);
    }

    /**
     * Creates a new inset drawable with the specified inset.
     *
     * @param drawable The drawable to inset.
     * @param inset    Inset in fraction (range: [0, 1)) of the inset content bounds.
     */
    public InsetDrawable(@Nullable Drawable drawable, float inset) {
        this(drawable, inset, inset, inset, inset);
    }

    /**
     * Creates a new inset drawable with the specified insets in pixels.
     *
     * @param drawable    The drawable to inset.
     * @param insetLeft   Left inset in pixels.
     * @param insetTop    Top inset in pixels.
     * @param insetRight  Right inset in pixels.
     * @param insetBottom Bottom inset in pixels.
     */
    public InsetDrawable(@Nullable Drawable drawable, int insetLeft, int insetTop,
                         int insetRight, int insetBottom) {
        this(new InsetState(null, null), null);

        mState.mInsetLeft = new InsetValue(0f, insetLeft);
        mState.mInsetTop = new InsetValue(0f, insetTop);
        mState.mInsetRight = new InsetValue(0f, insetRight);
        mState.mInsetBottom = new InsetValue(0f, insetBottom);

        setDrawable(drawable);
    }

    /**
     * Creates a new inset drawable with the specified insets in fraction of the view bounds.
     *
     * @param drawable            The drawable to inset.
     * @param insetLeftFraction   Left inset in fraction (range: [0, 1)) of the inset content bounds.
     * @param insetTopFraction    Top inset in fraction (range: [0, 1)) of the inset content bounds.
     * @param insetRightFraction  Right inset in fraction (range: [0, 1)) of the inset content bounds.
     * @param insetBottomFraction Bottom inset in fraction (range: [0, 1)) of the inset content bounds.
     */
    public InsetDrawable(@Nullable Drawable drawable, float insetLeftFraction,
                         float insetTopFraction, float insetRightFraction, float insetBottomFraction) {
        this(new InsetState(null, null), null);

        mState.mInsetLeft = new InsetValue(insetLeftFraction, 0);
        mState.mInsetTop = new InsetValue(insetTopFraction, 0);
        mState.mInsetRight = new InsetValue(insetRightFraction, 0);
        mState.mInsetBottom = new InsetValue(insetBottomFraction, 0);

        setDrawable(drawable);
    }

    private void getInsets(Rect out) {
        final Rect b = getBounds();
        out.left = mState.mInsetLeft.getDimension(b.width());
        out.right = mState.mInsetRight.getDimension(b.width());
        out.top = mState.mInsetTop.getDimension(b.height());
        out.bottom = mState.mInsetBottom.getDimension(b.height());
    }

    @Override
    public boolean getPadding(@NonNull Rect padding) {
        final boolean pad = super.getPadding(padding);
        getInsets(mTmpInsetRect);
        padding.left += mTmpInsetRect.left;
        padding.right += mTmpInsetRect.right;
        padding.top += mTmpInsetRect.top;
        padding.bottom += mTmpInsetRect.bottom;

        return pad || (mTmpInsetRect.left | mTmpInsetRect.right
                | mTmpInsetRect.top | mTmpInsetRect.bottom) != 0;
    }

    /*@Override
    public Insets getOpticalInsets() {
        final Insets contentInsets = super.getOpticalInsets();
        getInsets(mTmpInsetRect);
        return Insets.of(
                contentInsets.left + mTmpInsetRect.left,
                contentInsets.top + mTmpInsetRect.top,
                contentInsets.right + mTmpInsetRect.right,
                contentInsets.bottom + mTmpInsetRect.bottom);
    }*/

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        final Rect r = mTmpRect;
        r.set(bounds);

        r.left += mState.mInsetLeft.getDimension(bounds.width());
        r.top += mState.mInsetTop.getDimension(bounds.height());
        r.right -= mState.mInsetRight.getDimension(bounds.width());
        r.bottom -= mState.mInsetBottom.getDimension(bounds.height());

        // Apply inset bounds to the wrapped drawable.
        super.onBoundsChange(r);
    }

    @Override
    public int getIntrinsicWidth() {
        // Modern UI fixed: NPE
        final int childWidth = super.getIntrinsicWidth();
        final float fraction = mState.mInsetLeft.mFraction + mState.mInsetRight.mFraction;
        if (childWidth < 0 || fraction >= 1) {
            return -1;
        }
        return (int) (childWidth / (1 - fraction)) + mState.mInsetLeft.mDimension
                + mState.mInsetRight.mDimension;
    }

    @Override
    public int getIntrinsicHeight() {
        final int childHeight = super.getIntrinsicHeight();
        final float fraction = mState.mInsetTop.mFraction + mState.mInsetBottom.mFraction;
        if (childHeight < 0 || fraction >= 1) {
            return -1;
        }
        return (int) (childHeight / (1 - fraction)) + mState.mInsetTop.mDimension
                + mState.mInsetBottom.mDimension;
    }

    /*@Override
    public void getOutline(@NonNull Outline outline) {
        getDrawable().getOutline(outline);
    }*/

    @Override
    DrawableWrapperState mutateConstantState() {
        mState = new InsetState(mState, null);
        return mState;
    }

    static final class InsetState extends DrawableWrapper.DrawableWrapperState {
        private int[] mThemeAttrs;

        InsetValue mInsetLeft;
        InsetValue mInsetTop;
        InsetValue mInsetRight;
        InsetValue mInsetBottom;

        InsetState(@Nullable InsetState orig, @Nullable Resources res) {
            super(orig, res);

            if (orig != null) {
                mInsetLeft = orig.mInsetLeft.copy();
                mInsetTop = orig.mInsetTop.copy();
                mInsetRight = orig.mInsetRight.copy();
                mInsetBottom = orig.mInsetBottom.copy();

                if (orig.mDensity != mDensity) {
                    applyDensityScaling(orig.mDensity, mDensity);
                }
            } else {
                mInsetLeft = new InsetValue();
                mInsetTop = new InsetValue();
                mInsetRight = new InsetValue();
                mInsetBottom = new InsetValue();
            }
        }

        @Override
        void onDensityChanged(int sourceDensity, int targetDensity) {
            super.onDensityChanged(sourceDensity, targetDensity);

            applyDensityScaling(sourceDensity, targetDensity);
        }

        /**
         * Called when the constant state density changes to scale
         * density-dependent properties specific to insets.
         *
         * @param sourceDensity the previous constant state density
         * @param targetDensity the new constant state density
         */
        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            mInsetLeft.scaleFromDensity(sourceDensity, targetDensity);
            mInsetTop.scaleFromDensity(sourceDensity, targetDensity);
            mInsetRight.scaleFromDensity(sourceDensity, targetDensity);
            mInsetBottom.scaleFromDensity(sourceDensity, targetDensity);
        }

        @NonNull
        @Override
        public Drawable newDrawable(@Nullable Resources res) {
            // If this drawable is being created for a different density,
            // just create a new constant state and call it a day.
            final InsetState state;
            if (res != null) {
                final int densityDpi = res.getDisplayMetrics().densityDpi;
                final int density = densityDpi == 0 ? DisplayMetrics.DENSITY_DEFAULT : densityDpi;
                if (density != mDensity) {
                    state = new InsetState(this, res);
                } else {
                    state = this;
                }
            } else {
                state = this;
            }

            return new InsetDrawable(state, res);
        }
    }

    static final class InsetValue {
        final float mFraction;
        int mDimension;

        public InsetValue() {
            this(0f, 0);
        }

        public InsetValue(float fraction, int dimension) {
            mFraction = fraction;
            mDimension = dimension;
        }

        int getDimension(int boundSize) {
            return (int) (boundSize * mFraction) + mDimension;
        }

        void scaleFromDensity(int sourceDensity, int targetDensity) {
            if (mDimension != 0) {
                mDimension = Image.scaleFromDensity(mDimension, sourceDensity, targetDensity);
            }
        }

        @NonNull
        public InsetValue copy() {
            return new InsetValue(mFraction, mDimension);
        }
    }

    /**
     * The one constructor to rule them all. This is called by all public
     * constructors to set the state and initialize local properties.
     */
    private InsetDrawable(@NonNull InsetState state, @Nullable Resources res) {
        super(state, res);

        mState = state;
    }
}
