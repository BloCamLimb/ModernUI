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

import icyllis.modernui.R;
import icyllis.modernui.util.StateSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Lets you assign a number of graphic images to a single Drawable and swap out the visible item by a string
 * ID value.
 */
public class StateListDrawable extends DrawableContainer {

    private StateListState mStateListState;
    private boolean mMutated;

    public StateListDrawable() {
        this(null, null);
    }

    private StateListDrawable(@Nullable StateListState state, @Nullable Object res) {
        // Every state list drawable has its own constant state.
        final StateListState newState = new StateListState(state, this);
        setConstantState(newState);
        onStateChange(getState());
    }

    /**
     * This constructor exists so subclasses can avoid calling the default
     * constructor and setting up a StateListDrawable-specific constant state.
     */
    StateListDrawable(@Nullable StateListState state) {
        if (state != null) {
            setConstantState(state);
        }
    }

    /**
     * Add a new image/string ID to the set of images.
     *
     * @param stateSet An array of resource Ids to associate with the image.
     *                 Switch to this image by calling setState().
     * @param drawable The image to show. Note this must be a unique Drawable that is not shared
     *                 between any other View or Drawable otherwise the results are
     *                 undefined and can lead to unexpected rendering behavior
     */
    public void addState(int[] stateSet, Drawable drawable) {
        if (drawable != null) {
            mStateListState.addStateSet(stateSet, drawable);
            // in case the new state matches our current state...
            onStateChange(getState());
        }
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public boolean hasFocusStateSpecified() {
        return mStateListState.hasFocusStateSpecified();
    }

    @Override
    protected boolean onStateChange(@Nonnull int[] stateSet) {
        final boolean changed = super.onStateChange(stateSet);

        int idx = mStateListState.indexOfStateSet(stateSet);
        if (idx < 0) {
            idx = mStateListState.indexOfStateSet(StateSet.WILD_CARD);
        }

        return selectDrawable(idx) || changed;
    }

    StateListState getStateListState() {
        return mStateListState;
    }

    /**
     * Gets the number of states contained in this drawable.
     *
     * @return The number of states contained in this drawable.
     * @see #getStateSet(int)
     * @see #getStateDrawable(int)
     */
    public int getStateCount() {
        return mStateListState.getChildCount();
    }

    /**
     * Gets the state set at an index.
     *
     * @param index The index of the state set.
     * @return The state set at the index.
     * @see #getStateCount()
     * @see #getStateDrawable(int)
     */
    @Nonnull
    public int[] getStateSet(int index) {
        return mStateListState.mStateSets[index];
    }

    /**
     * Gets the drawable at an index.
     *
     * @param index The index of the drawable.
     * @return The drawable at the index.
     * @see #getStateCount()
     * @see #getStateSet(int)
     */
    @Nullable
    public Drawable getStateDrawable(int index) {
        return mStateListState.getChild(index);
    }

    /**
     * Gets the index of the drawable with the provided state set.
     *
     * @param stateSet the state set to look up
     * @return the index of the provided state set, or -1 if not found
     * @see #getStateDrawable(int)
     * @see #getStateSet(int)
     */
    public int findStateDrawableIndex(@Nonnull int[] stateSet) {
        return mStateListState.indexOfStateSet(stateSet);
    }

    @Nonnull
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mStateListState.mutate();
            mMutated = true;
        }
        return this;
    }

    @Override
    StateListState cloneConstantState() {
        return new StateListState(mStateListState, this);
    }

    @Override
    public void clearMutated() {
        super.clearMutated();
        mMutated = false;
    }

    @Override
    protected void setConstantState(@Nonnull DrawableContainerState state) {
        super.setConstantState(state);

        if (state instanceof StateListState) {
            mStateListState = (StateListState) state;
        }
    }

    static class StateListState extends DrawableContainerState {

        int[][] mStateSets;

        StateListState(@Nullable StateListState orig, StateListDrawable owner) {
            super(orig, owner);

            if (orig != null) {
                // Perform a shallow copy and rely on mutate() to deep-copy.
                mStateSets = orig.mStateSets;
            } else {
                mStateSets = new int[getCapacity()][];
            }
        }

        void mutate() {
            final int[][] stateSets = new int[mStateSets.length][];
            for (int i = mStateSets.length - 1; i >= 0; i--) {
                stateSets[i] = mStateSets[i] != null ? mStateSets[i].clone() : null;
            }
            mStateSets = stateSets;
        }

        int addStateSet(int[] stateSet, Drawable drawable) {
            final int pos = addChild(drawable);
            mStateSets[pos] = stateSet;
            return pos;
        }

        int indexOfStateSet(int[] stateSet) {
            final int[][] stateSets = mStateSets;
            final int N = getChildCount();
            for (int i = 0; i < N; i++) {
                if (StateSet.stateSetMatches(stateSets[i], stateSet)) {
                    return i;
                }
            }
            return -1;
        }

        boolean hasFocusStateSpecified() {
            return StateSet.containsAttribute(mStateSets, R.attr.state_focused);
        }

        @Nonnull
        @Override
        public Drawable newDrawable() {
            return new StateListDrawable(this, null);
        }

        @Override
        public void growArray(int oldSize, int newSize) {
            super.growArray(oldSize, newSize);
            final int[][] newStateSets = new int[newSize][];
            System.arraycopy(mStateSets, 0, newStateSets, 0, oldSize);
            mStateSets = newStateSets;
        }
    }
}
