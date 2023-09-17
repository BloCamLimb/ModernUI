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

package icyllis.arc3d.engine.ops;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Op is the base class for all deferred GPU operations. To facilitate reordering and to
 * minimize draw calls, Engine does not generate geometry inline with draw calls. Instead, it
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

    /**
     * Indicates that the op will produce geometry that extends beyond its bounds for the
     * purpose of ensuring that the fragment shader runs on partially covered pixels for
     * non-MSAA antialiasing.
     */
    private static final int BoundsFlag_AABloat = 0x1;
    /**
     * Indicates that the geometry being drawn in a hairline stroke. A point that is drawn in device
     * space is also considered a hairline.
     */
    private static final int BoundsFlag_ZeroArea = 0x2;

    // we own this (uniquely)
    private Op mNextInChain;
    // we are owned by this (uniquely)
    private Op mPrevInChain;

    private int mFlags;

    private float mLeft;
    private float mTop;
    private float mRight;
    private float mBottom;

    public Op() {
    }

    public void visitProxies(TextureProxyVisitor func) {
        // This default implementation assumes the op has no proxies
    }

    /**
     * The op that combineIfPossible was called on now represents its own work plus that of
     * the passed op. The passed op should be destroyed without being flushed. Currently it
     * is not legal to merge an op passed to combineIfPossible() the passed op is already in a
     * chain (though the op on which combineIfPossible() was called may be).
     */
    public final boolean combineIfPossible(@Nonnull Op op) {
        assert (op != this);
        if (getClass() != op.getClass()) {
            return false;
        }
        boolean result = onCombineIfPossible(op);
        if (result) {
            mFlags |= op.mFlags;
            mLeft = Math.min(mLeft, op.mLeft);
            mTop = Math.min(mTop, op.mTop);
            mRight = Math.max(mRight, op.mRight);
            mBottom = Math.max(mBottom, op.mBottom);
        }
        return result;
    }

    protected boolean onCombineIfPossible(@Nonnull Op op) {
        return false;
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
        mFlags = (aaBloat ? BoundsFlag_AABloat : 0) |
                (zeroArea ? BoundsFlag_ZeroArea : 0);
    }

    public final void getBounds(@Nonnull Rect2f bounds) {
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
     * This can optionally be called before 'prepare' (but after sorting). Each op that overrides
     * onPrePrepare must be prepared to handle both cases (when onPrePrepare has been called
     * ahead of time and when it has not been called).
     */
    //TODO more params
    public abstract void onPrePrepare(RecordingContext context,
                                      SurfaceProxyView writeView,
                                      int pipelineFlags);

    /**
     * Called prior to executing. The op should perform any resource creation or data transfers
     * necessary before execute() is called.
     */
    public abstract void onPrepare(OpFlushState state,
                                   SurfaceProxyView writeView,
                                   int pipelineFlags);

    /**
     * Issues the op's commands to {@link Server}.
     *
     * @param chainBounds If this op is chained then chainBounds is the union of the bounds of all ops in the chain.
     *                    Otherwise, this op's bounds.
     */
    public abstract void onExecute(OpFlushState state, Rect2f chainBounds);

    /**
     * Concatenates two op chains. This op must be a tail and the passed op must be a head. The ops
     * must be of the same subclass.
     */
    public final void mergeChain(@Nonnull Op next) {
        assert (getClass() == next.getClass());
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
        Op op = this;
        while (op != null) {
            assert (op == this || (op.prevInChain() != null && op.prevInChain().nextInChain() == op));
            assert (getClass() == op.getClass());
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
