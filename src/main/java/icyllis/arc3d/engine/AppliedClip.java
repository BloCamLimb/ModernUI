/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.Rect2f;

/**
 * Produced by {@link Clip}. It provides a set of modifications to the drawing state that
 * implements the clip.
 */
public class AppliedClip implements Cloneable {

    private int mScreenWidth;
    private int mScreenHeight;

    private int mScissorLeft;
    private int mScissorTop;
    private int mScissorRight;
    private int mScissorBottom;

    private int mStencilStackID;

    public AppliedClip() {
    }

    public AppliedClip init(int logicalScreenWidth, int logicalScreenHeight,
                            int backingStoreWidth, int backingStoreHeight) {
        assert (logicalScreenWidth > 0 && logicalScreenHeight > 0);
        assert (backingStoreWidth > 0 && backingStoreHeight > 0);
        // the backing store dim should be equal or greater than logical screen dim
        assert (logicalScreenWidth <= backingStoreWidth && logicalScreenHeight <= backingStoreHeight);
        mScreenWidth = backingStoreWidth;
        mScreenHeight = backingStoreHeight;
        setScissor(0, 0, logicalScreenWidth, logicalScreenHeight);
        mStencilStackID = 0;
        return this;
    }

    /**
     * Intersects the applied clip with the provided rect. Returns false if the draw became empty.
     * 'clippedDrawBounds' will be intersected with 'irect'. This returns false if the clip becomes
     * empty or the draw no longer intersects the clip. In either case the draw can be skipped.
     */
    public boolean addScissor(int left, int top, int right, int bottom,
                              Rect2f clippedDrawBounds) {
        return intersect(left, top, right, bottom) &&
                clippedDrawBounds.intersect(left, top, right, bottom);
    }

    public void setScissor(int left, int top, int right, int bottom) {
        mScissorLeft = 0;
        mScissorTop = 0;
        mScissorRight = mScreenWidth;
        mScissorBottom = mScreenHeight;
        intersect(left, top, right, bottom);
    }

    private boolean intersect(int left, int top, int right, int bottom) {
        int tmpL = Math.max(mScissorLeft, left);
        int tmpT = Math.max(mScissorTop, top);
        int tmpR = Math.min(mScissorRight, right);
        int tmpB = Math.min(mScissorBottom, bottom);
        if (tmpR <= tmpL || tmpB <= tmpT) {
            mScissorLeft = 0;
            mScissorTop = 0;
            mScissorRight = 0;
            mScissorBottom = 0;
            return false;
        } else {
            mScissorLeft = tmpL;
            mScissorTop = tmpT;
            mScissorRight = tmpR;
            mScissorBottom = tmpB;
            return true;
        }
    }

    public void addStencilClip(int stencilStackID) {
        assert (mStencilStackID == 0);
        mStencilStackID = stencilStackID;
    }

    public boolean hasScissorClip() {
        assert ((mScissorLeft == 0 && mScissorTop == 0 && mScissorRight == 0 && mScissorBottom == 0) ||
                (mScissorLeft >= 0 && mScissorTop >= 0 && mScissorRight <= mScreenWidth && mScissorBottom <= mScreenHeight));
        return mScissorLeft > 0 || mScissorTop > 0 ||
                mScissorRight < mScreenWidth || mScissorBottom < mScreenHeight;
    }

    public int getScissorLeft() {
        return mScissorLeft;
    }

    public int getScissorTop() {
        return mScissorTop;
    }

    public int getScissorRight() {
        return mScissorRight;
    }

    public int getScissorBottom() {
        return mScissorBottom;
    }

    public boolean hasStencilClip() {
        return mStencilStackID != 0;
    }

    public int getStencilStackID() {
        return mStencilStackID;
    }

    public boolean hasClip() {
        return hasScissorClip() || hasStencilClip();
    }

    @Override
    public int hashCode() {
        int result = mScreenWidth;
        result = 31 * result + mScreenHeight;
        result = 31 * result + mScissorLeft;
        result = 31 * result + mScissorTop;
        result = 31 * result + mScissorRight;
        result = 31 * result + mScissorBottom;
        result = 31 * result + mStencilStackID;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppliedClip that = (AppliedClip) o;
        if (mScreenWidth != that.mScreenWidth) return false;
        if (mScreenHeight != that.mScreenHeight) return false;
        if (mScissorLeft != that.mScissorLeft) return false;
        if (mScissorTop != that.mScissorTop) return false;
        if (mScissorRight != that.mScissorRight) return false;
        if (mScissorBottom != that.mScissorBottom) return false;
        return mStencilStackID == that.mStencilStackID;
    }

    @Override
    public AppliedClip clone() {
        try {
            return (AppliedClip) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
