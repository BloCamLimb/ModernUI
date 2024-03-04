/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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
import icyllis.arc3d.core.Rect2ic;

/**
 * Produced by {@link Clip}. It provides a set of modifications to the drawing state that
 * implements the clip.
 */
public final class ClipResult implements Cloneable {

    private int mStencilSeq;

    private int mScissorX0;
    private int mScissorY0;
    private int mScissorX1;
    private int mScissorY1;

    private int mScreenWidth;
    private int mScreenHeight;

    /**
     * Call this method before use.
     * <p>
     * There are two kinds of sizes. Sometimes we create a larger texture but only
     * use a subset of the area, thus it must be scissored.
     */
    public ClipResult init(int logicalWidth, int logicalHeight,
                           int physicalWidth, int physicalHeight) {
        assert (logicalWidth > 0 && logicalHeight > 0);
        assert (physicalWidth > 0 && physicalHeight > 0);
        assert (logicalWidth <= physicalWidth &&
                logicalHeight <= physicalHeight);
        mScreenWidth = physicalWidth;
        mScreenHeight = physicalHeight;
        setScissor(0, 0, logicalWidth, logicalHeight);
        mStencilSeq = 0;
        return this;
    }

    //// scissor clip

    /**
     * Intersects the clip state with the provided rect. Returns false if the draw became empty.
     * 'clippedBounds' will be intersected with 'rect'. This returns false if the clip becomes
     * empty or the draw no longer intersects the clip. In either case the draw can be skipped.
     */
    public boolean addScissor(Rect2ic rect,
                              Rect2f clippedBounds) {
        return intersect(rect.left(), rect.top(), rect.right(), rect.bottom()) &&
                clippedBounds.intersect(rect);
    }

    public void setScissor(int l, int t, int r, int b) {
        mScissorX0 = 0;
        mScissorY0 = 0;
        mScissorX1 = mScreenWidth;
        mScissorY1 = mScreenHeight;
        intersect(l, t, r, b);
    }

    private boolean intersect(int l, int t, int r, int b) {
        int tmpL = Math.max(mScissorX0, l);
        int tmpT = Math.max(mScissorY0, t);
        int tmpR = Math.min(mScissorX1, r);
        int tmpB = Math.min(mScissorY1, b);
        if (tmpR <= tmpL || tmpB <= tmpT) {
            mScissorX0 = 0;
            mScissorY0 = 0;
            mScissorX1 = 0;
            mScissorY1 = 0;
            return false;
        } else {
            mScissorX0 = tmpL;
            mScissorY0 = tmpT;
            mScissorX1 = tmpR;
            mScissorY1 = tmpB;
            return true;
        }
    }

    /**
     * Should scissor clip be applied?
     */
    public boolean hasScissorClip() {
        return mScissorX0 > 0 || mScissorY0 > 0 ||
                mScissorX1 < mScreenWidth || mScissorY1 < mScreenHeight;
    }

    public int getScissorX0() {
        return mScissorX0;
    }

    public int getScissorY0() {
        return mScissorY0;
    }

    public int getScissorX1() {
        return mScissorX1;
    }

    public int getScissorY1() {
        return mScissorY1;
    }

    //// stencil clip

    /**
     * Sets the sequence number of the stencil mask.
     */
    public void setStencil(int seq) {
        assert (mStencilSeq == 0);
        mStencilSeq = seq;
    }

    /**
     * Should stencil clip be applied?
     */
    public boolean hasStencilClip() {
        return mStencilSeq != 0;
    }

    public int getStencilSeq() {
        return mStencilSeq;
    }

    public boolean hasClip() {
        return hasScissorClip() || hasStencilClip();
    }

    @Override
    public int hashCode() {
        int h = mStencilSeq;
        h = 31 * h + mScissorX0;
        h = 31 * h + mScissorY0;
        h = 31 * h + mScissorX1;
        h = 31 * h + mScissorY1;
        h = 31 * h + mScreenWidth;
        h = 31 * h + mScreenHeight;
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClipResult other = (ClipResult) o;
        return mStencilSeq == other.mStencilSeq &&
                mScissorX0 == other.mScissorX0 &&
                mScissorY0 == other.mScissorY0 &&
                mScissorX1 == other.mScissorX1 &&
                mScissorY1 == other.mScissorY1 &&
                mScreenWidth == other.mScreenWidth &&
                mScreenHeight == other.mScreenHeight;
    }

    @Override
    public ClipResult clone() {
        try {
            return (ClipResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
