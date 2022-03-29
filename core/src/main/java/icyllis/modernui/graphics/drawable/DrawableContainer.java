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

package icyllis.modernui.graphics.drawable;

import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.BlendMode;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.math.Rect;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.LayoutDirection;
import icyllis.modernui.util.SparseArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A helper class that contains several {@link Drawable}s and selects which one to use.
 * <p>
 * You can extend it to create your own DrawableContainers or directly use one its child classes.
 */
public class DrawableContainer extends Drawable implements Drawable.Callback {

    private DrawableContainerState mDrawableContainerState;
    private Rect mHotspotBounds;
    private Drawable mCurrDrawable;
    private Drawable mLastDrawable;
    private int mAlpha = 0xFF;

    /**
     * Whether setAlpha() has been called at least once.
     */
    private boolean mHasAlpha;

    private int mCurIndex = -1;
    private boolean mMutated;

    // Animations.
    private Runnable mAnimationRunnable;
    private long mEnterAnimationEnd;
    private long mExitAnimationEnd;

    /**
     * Callback that blocks invalidation. Used for drawable initialization.
     */
    private BlockInvalidateCallback mBlockInvalidateCallback;

    public DrawableContainer() {
    }

    @Override
    public void draw(@Nonnull Canvas canvas) {
        if (mCurrDrawable != null) {
            mCurrDrawable.draw(canvas);
        }
        if (mLastDrawable != null) {
            mLastDrawable.draw(canvas);
        }
    }

    private boolean needsMirroring() {
        return isAutoMirrored() && getLayoutDirection() == LayoutDirection.RTL;
    }

    @Override
    public boolean getPadding(@Nonnull Rect padding) {
        final Rect r = mDrawableContainerState.getConstantPadding();
        boolean result;
        if (r != null) {
            padding.set(r);
            result = (r.left | r.top | r.bottom | r.right) != 0;
        } else {
            if (mCurrDrawable != null) {
                result = mCurrDrawable.getPadding(padding);
            } else {
                result = super.getPadding(padding);
            }
        }
        if (needsMirroring()) {
            final int left = padding.left;
            padding.left = padding.right;
            padding.right = left;
        }
        return result;
    }

    @Override
    public void setAlpha(int alpha) {
        if (!mHasAlpha || mAlpha != alpha) {
            mHasAlpha = true;
            mAlpha = alpha;
            if (mCurrDrawable != null) {
                if (mEnterAnimationEnd == 0) {
                    mCurrDrawable.setAlpha(alpha);
                } else {
                    animate(false);
                }
            }
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setTintList(ColorStateList tint) {
        mDrawableContainerState.mHasTintList = true;

        if (mDrawableContainerState.mTintList != tint) {
            mDrawableContainerState.mTintList = tint;

            if (mCurrDrawable != null) {
                mCurrDrawable.setTintList(tint);
            }
        }
    }

    @Override
    public void setTintBlendMode(@Nonnull BlendMode blendMode) {
        mDrawableContainerState.mHasTintMode = true;

        if (mDrawableContainerState.mBlendMode != blendMode) {
            mDrawableContainerState.mBlendMode = blendMode;

            if (mCurrDrawable != null) {
                mCurrDrawable.setTintBlendMode(blendMode);
            }
        }
    }

    /**
     * Change the global fade duration when a new drawable is entering
     * the scene.
     *
     * @param ms The amount of time to fade in milliseconds.
     */
    public void setEnterFadeDuration(int ms) {
        mDrawableContainerState.mEnterFadeDuration = ms;
    }

    /**
     * Change the global fade duration when a new drawable is leaving
     * the scene.
     *
     * @param ms The amount of time to fade in milliseconds.
     */
    public void setExitFadeDuration(int ms) {
        mDrawableContainerState.mExitFadeDuration = ms;
    }

    @Override
    protected void onBoundsChange(@Nonnull Rect bounds) {
        if (mLastDrawable != null) {
            mLastDrawable.setBounds(bounds);
        }
        if (mCurrDrawable != null) {
            mCurrDrawable.setBounds(bounds);
        }
    }

    @Override
    public boolean isStateful() {
        return mDrawableContainerState.isStateful();
    }

    @Override
    public boolean hasFocusStateSpecified() {
        if (mCurrDrawable != null) {
            return mCurrDrawable.hasFocusStateSpecified();
        }
        if (mLastDrawable != null) {
            return mLastDrawable.hasFocusStateSpecified();
        }
        return false;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (mDrawableContainerState.mAutoMirrored != mirrored) {
            mDrawableContainerState.mAutoMirrored = mirrored;
            if (mCurrDrawable != null) {
                mCurrDrawable.setAutoMirrored(mDrawableContainerState.mAutoMirrored);
            }
        }
    }

    @Override
    public boolean isAutoMirrored() {
        return mDrawableContainerState.mAutoMirrored;
    }

    @Override
    public void jumpToCurrentState() {
        boolean changed = false;
        if (mLastDrawable != null) {
            mLastDrawable.jumpToCurrentState();
            mLastDrawable = null;
            changed = true;
        }
        if (mCurrDrawable != null) {
            mCurrDrawable.jumpToCurrentState();
            if (mHasAlpha) {
                mCurrDrawable.setAlpha(mAlpha);
            }
        }
        if (mExitAnimationEnd != 0) {
            mExitAnimationEnd = 0;
            changed = true;
        }
        if (mEnterAnimationEnd != 0) {
            mEnterAnimationEnd = 0;
            changed = true;
        }
        if (changed) {
            invalidateSelf();
        }
    }

    @Override
    public void setHotspot(float x, float y) {
        if (mCurrDrawable != null) {
            mCurrDrawable.setHotspot(x, y);
        }
    }

    @Override
    public void setHotspotBounds(int left, int top, int right, int bottom) {
        if (mHotspotBounds == null) {
            mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            mHotspotBounds.set(left, top, right, bottom);
        }

        if (mCurrDrawable != null) {
            mCurrDrawable.setHotspotBounds(left, top, right, bottom);
        }
    }

    @Override
    public void getHotspotBounds(@Nonnull Rect outRect) {
        if (mHotspotBounds != null) {
            outRect.set(mHotspotBounds);
        } else {
            super.getHotspotBounds(outRect);
        }
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] state) {
        if (mLastDrawable != null) {
            return mLastDrawable.setState(state);
        }
        if (mCurrDrawable != null) {
            return mCurrDrawable.setState(state);
        }
        return false;
    }

    @Override
    protected boolean onLevelChange(int level) {
        if (mLastDrawable != null) {
            return mLastDrawable.setLevel(level);
        }
        if (mCurrDrawable != null) {
            return mCurrDrawable.setLevel(level);
        }
        return false;
    }

    @Override
    protected boolean onLayoutDirectionChanged(int layoutDirection) {
        // Let the container handle setting its own layout direction. Otherwise,
        // we're accessing potentially unused states.
        return mDrawableContainerState.setLayoutDirection(layoutDirection, getCurrentIndex());
    }

    @Override
    public int getIntrinsicWidth() {
        if (mDrawableContainerState.isConstantSize()) {
            return mDrawableContainerState.getConstantWidth();
        }
        return mCurrDrawable != null ? mCurrDrawable.getIntrinsicWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        if (mDrawableContainerState.isConstantSize()) {
            return mDrawableContainerState.getConstantHeight();
        }
        return mCurrDrawable != null ? mCurrDrawable.getIntrinsicHeight() : -1;
    }

    @Override
    public int getMinimumWidth() {
        if (mDrawableContainerState.isConstantSize()) {
            return mDrawableContainerState.getConstantMinimumWidth();
        }
        return mCurrDrawable != null ? mCurrDrawable.getMinimumWidth() : 0;
    }

    @Override
    public int getMinimumHeight() {
        if (mDrawableContainerState.isConstantSize()) {
            return mDrawableContainerState.getConstantMinimumHeight();
        }
        return mCurrDrawable != null ? mCurrDrawable.getMinimumHeight() : 0;
    }

    @Override
    public void invalidateDrawable(@Nonnull Drawable who) {
        // This may have been called as the result of a tint changing, in
        // which case we may need to refresh the cached statefulness
        if (mDrawableContainerState != null) {
            mDrawableContainerState.invalidateCache();
        }

        if (who == mCurrDrawable && getCallback() != null) {
            getCallback().invalidateDrawable(this);
        }
    }

    @Override
    public void scheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what, long when) {
        if (who == mCurrDrawable && getCallback() != null) {
            getCallback().scheduleDrawable(this, what, when);
        }
    }

    @Override
    public void unscheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what) {
        if (who == mCurrDrawable && getCallback() != null) {
            getCallback().unscheduleDrawable(this, what);
        }
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (mLastDrawable != null) {
            mLastDrawable.setVisible(visible, restart);
        }
        if (mCurrDrawable != null) {
            mCurrDrawable.setVisible(visible, restart);
        }
        return changed;
    }

    public int getCurrentIndex() {
        return mCurIndex;
    }

    /**
     * Sets the currently displayed drawable by index.
     * <p>
     * If an invalid index is specified, the current drawable will be set to
     * {@code null} and the index will be set to {@code -1}.
     *
     * @param index the index of the drawable to display
     * @return {@code true} if the drawable changed, {@code false} otherwise
     */
    public boolean selectDrawable(int index) {
        if (index == mCurIndex) {
            return false;
        }

        final long now = Core.timeMillis();

        if (mDrawableContainerState.mExitFadeDuration > 0) {
            if (mLastDrawable != null) {
                mLastDrawable.setVisible(false, false);
            }
            if (mCurrDrawable != null) {
                mLastDrawable = mCurrDrawable;
                mExitAnimationEnd = now + mDrawableContainerState.mExitFadeDuration;
            } else {
                mLastDrawable = null;
                mExitAnimationEnd = 0;
            }
        } else if (mCurrDrawable != null) {
            mCurrDrawable.setVisible(false, false);
        }

        if (index >= 0 && index < mDrawableContainerState.mNumChildren) {
            final Drawable d = mDrawableContainerState.getChild(index);
            mCurrDrawable = d;
            mCurIndex = index;
            if (d != null) {
                if (mDrawableContainerState.mEnterFadeDuration > 0) {
                    mEnterAnimationEnd = now + mDrawableContainerState.mEnterFadeDuration;
                }
                initializeDrawableForDisplay(d);
            }
        } else {
            mCurrDrawable = null;
            mCurIndex = -1;
        }

        if (mEnterAnimationEnd != 0 || mExitAnimationEnd != 0) {
            if (mAnimationRunnable == null) {
                mAnimationRunnable = () -> {
                    animate(true);
                    invalidateSelf();
                };
            } else {
                unscheduleSelf(mAnimationRunnable);
            }
            // Compute first frame and schedule next animation.
            animate(true);
        }

        invalidateSelf();

        return true;
    }

    /**
     * Initializes a drawable for display in this container.
     *
     * @param d The drawable to initialize.
     */
    private void initializeDrawableForDisplay(@Nonnull Drawable d) {
        if (mBlockInvalidateCallback == null) {
            mBlockInvalidateCallback = new BlockInvalidateCallback();
        }

        // Temporary fix for suspending callbacks during initialization. We
        // don't want any of these setters causing an invalidate() since that
        // may call back into DrawableContainer.
        d.setCallback(mBlockInvalidateCallback.wrap(d.getCallback()));

        try {
            if (mDrawableContainerState.mEnterFadeDuration <= 0 && mHasAlpha) {
                d.setAlpha(mAlpha);
            }

            if (mDrawableContainerState.mHasTintList) {
                d.setTintList(mDrawableContainerState.mTintList);
            }
            if (mDrawableContainerState.mHasTintMode) {
                d.setTintBlendMode(mDrawableContainerState.mBlendMode);
            }

            d.setVisible(isVisible(), true);
            d.setState(getState());
            d.setLevel(getLevel());
            d.setBounds(getBounds());
            d.setLayoutDirection(getLayoutDirection());
            d.setAutoMirrored(mDrawableContainerState.mAutoMirrored);

            final Rect hotspotBounds = mHotspotBounds;
            if (hotspotBounds != null) {
                d.setHotspotBounds(hotspotBounds.left, hotspotBounds.top,
                        hotspotBounds.right, hotspotBounds.bottom);
            }
        } finally {
            d.setCallback(mBlockInvalidateCallback.unwrap());
        }
    }

    void animate(boolean schedule) {
        mHasAlpha = true;

        final long now = Core.timeMillis();
        boolean animating = false;
        if (mCurrDrawable != null) {
            if (mEnterAnimationEnd != 0) {
                if (mEnterAnimationEnd <= now) {
                    mCurrDrawable.setAlpha(mAlpha);
                    mEnterAnimationEnd = 0;
                } else {
                    int animAlpha = (int) ((mEnterAnimationEnd - now) * 255)
                            / mDrawableContainerState.mEnterFadeDuration;
                    mCurrDrawable.setAlpha(((255 - animAlpha) * mAlpha) / 255);
                    animating = true;
                }
            }
        } else {
            mEnterAnimationEnd = 0;
        }
        if (mLastDrawable != null) {
            if (mExitAnimationEnd != 0) {
                if (mExitAnimationEnd <= now) {
                    mLastDrawable.setVisible(false, false);
                    mLastDrawable = null;
                    mExitAnimationEnd = 0;
                } else {
                    int animAlpha = (int) ((mExitAnimationEnd - now) * 255)
                            / mDrawableContainerState.mExitFadeDuration;
                    mLastDrawable.setAlpha((animAlpha * mAlpha) / 255);
                    animating = true;
                }
            }
        } else {
            mExitAnimationEnd = 0;
        }

        if (schedule && animating) {
            scheduleSelf(mAnimationRunnable, now + 1000 / 60);
        }
    }

    @Nullable
    @Override
    public Drawable getCurrent() {
        return mCurrDrawable;
    }

    @Override
    public ConstantState getConstantState() {
        if (mDrawableContainerState.canConstantState()) {
            return mDrawableContainerState;
        }
        return null;
    }

    @Nonnull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            final DrawableContainerState clone = cloneConstantState();
            clone.mutate();
            setConstantState(clone);
            mMutated = true;
        }
        return this;
    }

    /**
     * Returns a shallow copy of the container's constant state to be used as
     * the base state for {@link #mutate()}.
     *
     * @return a shallow copy of the constant state
     */
    DrawableContainerState cloneConstantState() {
        return mDrawableContainerState;
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mDrawableContainerState.clearMutated();
        mMutated = false;
    }

    protected void setConstantState(DrawableContainerState state) {
        mDrawableContainerState = state;

        // The locally cached drawables may have changed.
        if (mCurIndex >= 0) {
            mCurrDrawable = state.getChild(mCurIndex);
            if (mCurrDrawable != null) {
                initializeDrawableForDisplay(mCurrDrawable);
            }
        }

        // Clear out the last drawable. We don't have enough information to
        // propagate local state from the past.
        mLastDrawable = null;
    }

    /**
     * A ConstantState that can contain several {@link Drawable}s.
     */
    protected static abstract class DrawableContainerState extends ConstantState {

        final DrawableContainer mOwner;

        SparseArray<ConstantState> mDrawableFutures;
        Drawable[] mDrawables;
        int mNumChildren;

        boolean mVariablePadding = false;
        boolean mCheckedPadding;
        Rect mConstantPadding;

        boolean mConstantSize = false;
        boolean mCheckedConstantSize;
        int mConstantWidth;
        int mConstantHeight;
        int mConstantMinimumWidth;
        int mConstantMinimumHeight;

        boolean mCheckedStateful;
        boolean mStateful;

        boolean mCheckedConstantState;
        boolean mCanConstantState;

        boolean mMutated;
        int mLayoutDirection;

        int mEnterFadeDuration = 0;
        int mExitFadeDuration = 0;

        boolean mAutoMirrored;

        ColorStateList mTintList;
        BlendMode mBlendMode;
        boolean mHasTintList;
        boolean mHasTintMode;

        protected DrawableContainerState(@Nullable DrawableContainerState orig, DrawableContainer owner) {
            mOwner = owner;

            if (orig != null) {
                mCheckedConstantState = true;
                mCanConstantState = true;

                mVariablePadding = orig.mVariablePadding;
                mConstantSize = orig.mConstantSize;
                mMutated = orig.mMutated;
                mLayoutDirection = orig.mLayoutDirection;
                mEnterFadeDuration = orig.mEnterFadeDuration;
                mExitFadeDuration = orig.mExitFadeDuration;
                mAutoMirrored = orig.mAutoMirrored;
                mTintList = orig.mTintList;
                mBlendMode = orig.mBlendMode;
                mHasTintList = orig.mHasTintList;
                mHasTintMode = orig.mHasTintMode;

                if (orig.mCheckedStateful) {
                    mStateful = orig.mStateful;
                    mCheckedStateful = true;
                }

                // Postpone cloning children and futures until we're absolutely
                // sure that we're done computing values for the original state.
                final Drawable[] origDr = orig.mDrawables;
                mDrawables = new Drawable[origDr.length];
                mNumChildren = orig.mNumChildren;

                final SparseArray<ConstantState> origDf = orig.mDrawableFutures;
                if (origDf != null) {
                    mDrawableFutures = origDf.clone();
                } else {
                    mDrawableFutures = new SparseArray<>(mNumChildren);
                }

                // Create futures for drawables with constant states. If a
                // drawable doesn't have a constant state, then we can't clone
                // it and we'll have to reference the original.
                final int N = mNumChildren;
                for (int i = 0; i < N; i++) {
                    if (origDr[i] != null) {
                        final ConstantState cs = origDr[i].getConstantState();
                        if (cs != null) {
                            mDrawableFutures.put(i, cs);
                        } else {
                            mDrawables[i] = origDr[i];
                        }
                    }
                }
            } else {
                mDrawables = new Drawable[10];
                mNumChildren = 0;
            }
        }

        /**
         * Adds the drawable to the end of the list of contained drawables.
         *
         * @param dr the drawable to add
         * @return the position of the drawable within the container
         */
        public final int addChild(Drawable dr) {
            final int pos = mNumChildren;
            if (pos >= mDrawables.length) {
                growArray(pos, pos + 10);
            }

            dr.mutate();
            dr.setVisible(false, true);
            dr.setCallback(mOwner);

            mDrawables[pos] = dr;
            mNumChildren++;

            invalidateCache();

            mConstantPadding = null;
            mCheckedPadding = false;
            mCheckedConstantSize = false;
            mCheckedConstantState = false;

            return pos;
        }

        /**
         * Invalidates the cached statefulness.
         */
        void invalidateCache() {
            mCheckedStateful = false;
        }

        final int getCapacity() {
            return mDrawables.length;
        }

        private void createAllFutures() {
            if (mDrawableFutures != null) {
                final int futureCount = mDrawableFutures.size();
                for (int keyIndex = 0; keyIndex < futureCount; keyIndex++) {
                    final int index = mDrawableFutures.keyAt(keyIndex);
                    final ConstantState cs = mDrawableFutures.valueAt(keyIndex);
                    mDrawables[index] = prepareDrawable(cs.newDrawable());
                }

                mDrawableFutures = null;
            }
        }

        @Nonnull
        private Drawable prepareDrawable(@Nonnull Drawable child) {
            child.setLayoutDirection(mLayoutDirection);
            child = child.mutate();
            child.setCallback(mOwner);
            return child;
        }

        public final int getChildCount() {
            return mNumChildren;
        }

        /*
         * @deprecated Use {@link #getChild} instead.
         */
        public final Drawable[] getChildren() {
            // Create all futures for backwards compatibility.
            createAllFutures();

            return mDrawables;
        }

        @Nullable
        public final Drawable getChild(int index) {
            final Drawable result = mDrawables[index];
            if (result != null) {
                return result;
            }

            // Prepare future drawable if necessary.
            if (mDrawableFutures != null) {
                final int keyIndex = mDrawableFutures.indexOfKey(index);
                if (keyIndex >= 0) {
                    final ConstantState cs = mDrawableFutures.valueAt(keyIndex);
                    final Drawable prepared = prepareDrawable(cs.newDrawable());
                    mDrawables[index] = prepared;
                    mDrawableFutures.removeAt(keyIndex);
                    if (mDrawableFutures.size() == 0) {
                        mDrawableFutures = null;
                    }
                    return prepared;
                }
            }

            return null;
        }

        final boolean setLayoutDirection(int layoutDirection, int currentIndex) {
            boolean changed = false;

            // No need to call createAllFutures, since future drawables will
            // change layout direction when they are prepared.
            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            for (int i = 0; i < N; i++) {
                if (drawables[i] != null) {
                    final boolean childChanged = drawables[i].setLayoutDirection(layoutDirection);
                    if (i == currentIndex) {
                        changed = childChanged;
                    }
                }
            }

            mLayoutDirection = layoutDirection;

            return changed;
        }

        private void mutate() {
            // No need to call createAllFutures, since future drawables will
            // mutate when they are prepared.
            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            for (int i = 0; i < N; i++) {
                if (drawables[i] != null) {
                    drawables[i].mutate();
                }
            }

            mMutated = true;
        }

        final void clearMutated() {
            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            for (int i = 0; i < N; i++) {
                if (drawables[i] != null) {
                    drawables[i].clearMutated();
                }
            }

            mMutated = false;
        }

        /**
         * A boolean value indicating whether to use the maximum padding value
         * of all frames in the set (false), or to use the padding value of the
         * frame being shown (true). Default value is false.
         */
        public final void setVariablePadding(boolean variable) {
            mVariablePadding = variable;
        }

        @Nullable
        public final Rect getConstantPadding() {
            if (mVariablePadding) {
                return null;
            }

            if ((mConstantPadding != null) || mCheckedPadding) {
                return mConstantPadding;
            }

            createAllFutures();

            Rect r = null;
            final Rect t = new Rect();
            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            for (int i = 0; i < N; i++) {
                if (drawables[i].getPadding(t)) {
                    if (r == null) r = new Rect(0, 0, 0, 0);
                    if (t.left > r.left) r.left = t.left;
                    if (t.top > r.top) r.top = t.top;
                    if (t.right > r.right) r.right = t.right;
                    if (t.bottom > r.bottom) r.bottom = t.bottom;
                }
            }

            mCheckedPadding = true;
            return (mConstantPadding = r);
        }

        public final void setConstantSize(boolean constant) {
            mConstantSize = constant;
        }

        public final boolean isConstantSize() {
            return mConstantSize;
        }

        public final int getConstantWidth() {
            if (!mCheckedConstantSize) {
                computeConstantSize();
            }

            return mConstantWidth;
        }

        public final int getConstantHeight() {
            if (!mCheckedConstantSize) {
                computeConstantSize();
            }

            return mConstantHeight;
        }

        public final int getConstantMinimumWidth() {
            if (!mCheckedConstantSize) {
                computeConstantSize();
            }

            return mConstantMinimumWidth;
        }

        public final int getConstantMinimumHeight() {
            if (!mCheckedConstantSize) {
                computeConstantSize();
            }

            return mConstantMinimumHeight;
        }

        protected void computeConstantSize() {
            mCheckedConstantSize = true;

            createAllFutures();

            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            mConstantWidth = mConstantHeight = -1;
            mConstantMinimumWidth = mConstantMinimumHeight = 0;
            for (int i = 0; i < N; i++) {
                final Drawable dr = drawables[i];
                int s = dr.getIntrinsicWidth();
                if (s > mConstantWidth) mConstantWidth = s;
                s = dr.getIntrinsicHeight();
                if (s > mConstantHeight) mConstantHeight = s;
                s = dr.getMinimumWidth();
                if (s > mConstantMinimumWidth) mConstantMinimumWidth = s;
                s = dr.getMinimumHeight();
                if (s > mConstantMinimumHeight) mConstantMinimumHeight = s;
            }
        }

        public final void setEnterFadeDuration(int duration) {
            mEnterFadeDuration = duration;
        }

        public final int getEnterFadeDuration() {
            return mEnterFadeDuration;
        }

        public final void setExitFadeDuration(int duration) {
            mExitFadeDuration = duration;
        }

        public final int getExitFadeDuration() {
            return mExitFadeDuration;
        }

        public final boolean isStateful() {
            if (mCheckedStateful) {
                return mStateful;
            }

            createAllFutures();

            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            boolean isStateful = false;
            for (int i = 0; i < N; i++) {
                if (drawables[i].isStateful()) {
                    isStateful = true;
                    break;
                }
            }

            mStateful = isStateful;
            mCheckedStateful = true;
            return isStateful;
        }

        public void growArray(int oldSize, int newSize) {
            Drawable[] newDrawables = new Drawable[newSize];
            System.arraycopy(mDrawables, 0, newDrawables, 0, oldSize);
            mDrawables = newDrawables;
        }

        public synchronized boolean canConstantState() {
            if (mCheckedConstantState) {
                return mCanConstantState;
            }

            createAllFutures();

            mCheckedConstantState = true;

            final int N = mNumChildren;
            final Drawable[] drawables = mDrawables;
            for (int i = 0; i < N; i++) {
                if (drawables[i].getConstantState() == null) {
                    mCanConstantState = false;
                    return false;
                }
            }

            mCanConstantState = true;
            return true;
        }
    }

    /**
     * Callback that blocks drawable invalidation.
     */
    private static class BlockInvalidateCallback implements Callback {

        private Callback mCallback;

        public BlockInvalidateCallback wrap(Callback callback) {
            mCallback = callback;
            return this;
        }

        public Callback unwrap() {
            final Callback callback = mCallback;
            mCallback = null;
            return callback;
        }

        @Override
        public void invalidateDrawable(@Nonnull Drawable who) {
            // Ignore invalidation.
        }

        @Override
        public void scheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what, long when) {
            if (mCallback != null) {
                mCallback.scheduleDrawable(who, what, when);
            }
        }

        @Override
        public void unscheduleDrawable(@Nonnull Drawable who, @Nonnull Runnable what) {
            if (mCallback != null) {
                mCallback.unscheduleDrawable(who, what);
            }
        }
    }
}
