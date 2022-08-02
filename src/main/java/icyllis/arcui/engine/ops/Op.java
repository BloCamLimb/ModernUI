/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine.ops;

import icyllis.arcui.core.RectF;
import icyllis.arcui.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Op is the base class for all Arc UI deferred GPU operations. To facilitate reordering and to
 * minimize draw calls, Arc UI does not generate geometry inline with draw calls. Instead, it
 * captures the arguments to the draw and then generates the geometry when flushing. This gives Op
 * subclasses complete freedom to decide how/when to combine in order to produce fewer draw calls
 * and minimize state changes.
 * <p>
 * Ops of the same subclass may be merged or chained using combineIfPossible. When two ops merge,
 * one takes on the union of the data and the other is left empty. The merged op becomes responsible
 * for drawing the data from both the original ops. When ops are chained each op maintains its own
 * data but they are linked in a list and the head op becomes responsible for executing the work for
 * the chain.
 * <p>
 * It is required that chain-ability is transitive. Moreover, if op A is able to merge with B then
 * it must be the case that any op that can chain with A will either merge or chain with any op
 * that can chain to B.
 * <p>
 * The bounds of the op must contain all the vertices in device space *irrespective* of the clip.
 * The bounds are used in determining which clip elements must be applied and thus the bounds cannot
 * in turn depend upon the clip.
 */
public abstract class Op {

    private static final int NULL_CLASS_ID = 0;

    private static final AtomicInteger sNextClassID = new AtomicInteger(NULL_CLASS_ID + 1);

    /**
     * The op that combineIfPossible was called on now represents its own work plus that of
     * the passed op. The passed op should be destroyed without being flushed. Currently it
     * is not legal to merge an op passed to combineIfPossible() the passed op is already in a
     * chain (though the op on which combineIfPossible() was called may be).
     */
    public static final int CombineResult_Merged = 0;
    /**
     * The caller *may* (but is not required) to chain these ops together. If they are chained
     * then prepare() and execute() will be called on the head op but not the other ops in the
     * chain. The head op will prepare and execute on behalf of all the ops in the chain.
     */
    public static final int CombineResult_MayChain = 1;
    /**
     * The ops cannot be combined.
     */
    public static final int CombineResult_CannotCombine = 2;

    /**
     * Indicates that the op will produce geometry that extends beyond its bounds for the
     * purpose of ensuring that the fragment shader runs on partially covered pixels for
     * non-MSAA antialiasing.
     */
    private static final int BoundsFlag_AABloat = 0x1 << 16;
    /**
     * Indicates that the geometry being drawn in a hairline stroke. A point that is drawn in device
     * space is also considered a hairline.
     */
    private static final int BoundsFlag_ZeroArea = 0x2 << 16;

    // we uniquely own this
    private Op mNextInChain;
    // we are uniquely owned by this
    private Op mPrevInChain;
    // lower 16 bits - class ID, higher 16 bits - bounds flags
    private int mFlags;

    private float mLeft;
    private float mTop;
    private float mRight;
    private float mBottom;

    protected Op(int classID) {
        assert classID != NULL_CLASS_ID && classID == (classID & 0xFFFF);
        mFlags = classID;
    }

    protected static int genClassID() {
        final int id = sNextClassID.getAndIncrement();
        assert id != NULL_CLASS_ID : "This should never wrap as it should only be called once for each Op " +
                "subclass.";
        return id;
    }

    public abstract String name();

    /**
     * @return CombineResult
     */
    public final int combineIfPossible(@Nonnull Op op, Caps caps) {
        assert op != this;
        if (classID() != op.classID()) {
            return CombineResult_CannotCombine;
        }
        var result = onCombineIfPossible(op, caps);
        if (result == CombineResult_Merged) {
            mFlags |= op.mFlags & ~0xFFFF;
            mLeft = Math.min(mLeft, op.mLeft);
            mTop = Math.min(mTop, op.mTop);
            mRight = Math.max(mRight, op.mRight);
            mBottom = Math.max(mBottom, op.mBottom);
        }
        return result;
    }

    /**
     * @return CombineResult
     */
    protected int onCombineIfPossible(Op op, Caps caps) {
        return CombineResult_CannotCombine;
    }

    /**
     * Must be called at least once before use.
     */
    protected final void setBounds(float left, float top, float right, float bottom,
                                   boolean aaBloat, boolean zeroArea) {
        mLeft = left;
        mTop = top;
        mRight = right;
        mBottom = bottom;
        mFlags = (mFlags & 0xFFFF) |
                (aaBloat ? BoundsFlag_AABloat : 0) |
                (zeroArea ? BoundsFlag_ZeroArea : 0);
    }

    public final void getBounds(@Nonnull RectF bounds) {
        bounds.set(mLeft, mTop, mRight, mBottom);
    }

    public final float getLeft() {
        return mLeft;
    }

    public final float getTop() {
        return mTop;
    }

    public final float getRight() {
        return mRight;
    }

    public final float getBottom() {
        return mBottom;
    }

    /**
     * @return true if this has analytical anti-aliasing bloat when determining coverage (outset by 0.5)
     */
    public final boolean hasAABloat() {
        return (mFlags & BoundsFlag_AABloat) != 0;
    }

    /**
     * @return true if this draws a primitive that has zero area, we can also call hairline
     */
    public final boolean hasZeroArea() {
        return (mFlags & BoundsFlag_ZeroArea) != 0;
    }

    /**
     * @return unique ID to identify this class at runtime.
     */
    public final int classID() {
        return mFlags & 0xFFFF;
    }

    /**
     * This can optionally be called before 'prepare' (but after sorting). Each op that overrides
     * onPrePrepare must be prepared to handle both cases (when onPrePrepare has been called
     * ahead of time and when it has not been called).
     */
    //TODO more params
    public abstract void onPrePrepare(RecordingContext context);

    /**
     * Called prior to executing. The op should perform any resource creation or data transfers
     * necessary before execute() is called.
     */
    public abstract void onPrepare(OpFlushState state);

    /**
     * Issues the op's commands to {@link Server}.
     *
     * @param chainBounds If this op is chained then chainBounds is the union of the bounds of all ops in the chain.
     *                    Otherwise, this op's bounds.
     */
    public abstract void onExecute(OpFlushState state, RectF chainBounds);

    /**
     * Concatenates two op chains. This op must be a tail and the passed op must be a head. The ops
     * must be of the same subclass.
     */
    public final void mergeChain(@Nonnull Op next) {
        assert (classID() == next.classID());
        assert (isChainTail());
        assert (next.isChainHead());
        mNextInChain = next; // transfer ownership
        next.mPrevInChain = this;
    }

    /**
     * Returns true if this is the head of a chain (including a length 1 chain).
     */
    public final boolean isChainHead() {
        return mPrevInChain == null;
    }

    /**
     * Returns true if this is the tail of a chain (including a length 1 chain).
     */
    public final boolean isChainTail() {
        return mNextInChain == null;
    }

    /**
     * The next op in the chain.
     */
    public final Op nextInChain() {
        return mNextInChain;
    }

    /**
     * The previous op in the chain.
     */
    public final Op prevInChain() {
        return mPrevInChain;
    }

    /**
     * Cuts the chain after this op. The returned op is the op that was previously next in the
     * chain or null if this was already a tail.
     */
    @Nullable
    public final Op cutChain() {
        final Op next = mNextInChain;
        if (next != null) {
            next.mPrevInChain = null;
            mNextInChain = null;
            return next; // transfer ownership
        }
        return null;
    }

    /**
     * Debug tool.
     */
    public final boolean validateChain(Op expectedTail) {
        assert (isChainHead());
        int classID = classID();
        var op = this;
        while (op != null) {
            assert (op == this || (op.prevInChain() != null && op.prevInChain().nextInChain() == op));
            assert (classID == op.classID());
            if (op.nextInChain() != null) {
                assert (op.nextInChain().prevInChain() == op);
                assert (op != expectedTail);
            } else {
                assert (expectedTail == null || op == expectedTail);
            }
            op = op.nextInChain();
        }
        return true;
    }
}
