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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.resources.Resources;

/**
 * A resource that manages a number of alternate Drawables, each assigned a maximum numerical value.
 * Setting the level value of the object with {@link #setLevel(int)} will load the image with the next
 * greater or equal value assigned to its max attribute.
 * <p>
 * A good example use of a LevelListDrawable would be a Wi-Fi signal strength indicator icon, with
 * different images to indicate the current signal level.
 */
public class LevelListDrawable extends DrawableContainer {

    private LevelListState mLevelListState;
    private boolean mMutated;

    public LevelListDrawable() {
        this(null, null);
    }

    public void addLevel(int low, int high, Drawable drawable) {
        if (drawable != null) {
            mLevelListState.addLevel(low, high, drawable);
            // in case the new state matches our current state...
            onLevelChange(getLevel());
        }
    }

    // overrides from Drawable

    @Override
    protected boolean onLevelChange(int level) {
        int idx = mLevelListState.indexOfLevel(level);
        if (selectDrawable(idx)) {
            return true;
        }
        return super.onLevelChange(level);
    }

    @NonNull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mLevelListState.mutate();
            mMutated = true;
        }
        return this;
    }

    @Override
    LevelListState cloneConstantState() {
        return new LevelListState(mLevelListState, this, null);
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    @Override
    protected void setConstantState(@NonNull DrawableContainerState state) {
        super.setConstantState(state);

        if (state instanceof LevelListState) {
            mLevelListState = (LevelListState) state;
        }
    }

    private final static class LevelListState extends DrawableContainerState {

        private int[] mLows;
        private int[] mHighs;

        LevelListState(LevelListState orig, LevelListDrawable owner, Resources res) {
            super(orig, owner, res);

            if (orig != null) {
                // Perform a shallow copy and rely on mutate() to deep-copy.
                mLows = orig.mLows;
                mHighs = orig.mHighs;
            } else {
                mLows = new int[getCapacity()];
                mHighs = new int[getCapacity()];
            }
        }

        private void mutate() {
            mLows = mLows.clone();
            mHighs = mHighs.clone();
        }

        public void addLevel(int low, int high, Drawable drawable) {
            int pos = addChild(drawable);
            mLows[pos] = low;
            mHighs[pos] = high;
        }

        public int indexOfLevel(int level) {
            final int[] lows = mLows;
            final int[] highs = mHighs;
            final int N = getChildCount();
            for (int i = 0; i < N; i++) {
                if (level >= lows[i] && level <= highs[i]) {
                    return i;
                }
            }
            return -1;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new LevelListDrawable(this, null);
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            return new LevelListDrawable(this, res);
        }

        @Override
        public void growArray(int oldSize, int newSize) {
            super.growArray(oldSize, newSize);
            int[] newInts = new int[newSize];
            System.arraycopy(mLows, 0, newInts, 0, oldSize);
            mLows = newInts;
            newInts = new int[newSize];
            System.arraycopy(mHighs, 0, newInts, 0, oldSize);
            mHighs = newInts;
        }
    }

    private LevelListDrawable(@Nullable LevelListState state, @Nullable Resources res) {
        final LevelListState as = new LevelListState(state, this, res);
        setConstantState(as);
        onLevelChange(getLevel());
    }
}
