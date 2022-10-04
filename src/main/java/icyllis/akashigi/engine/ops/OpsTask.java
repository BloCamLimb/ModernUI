/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine.ops;

import icyllis.akashigi.core.Rect2f;
import icyllis.akashigi.core.Rect2i;
import icyllis.akashigi.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

//TODO
public class OpsTask extends RenderTask {

    private final ArrayList<OpChain> mOpChains = new ArrayList<>();

    private SurfaceProxyView mWriteView;

    public OpsTask(DrawingManager drawingManager, SurfaceProxyView targetView) {
        mWriteView = targetView;
    }

    @Override
    public void gatherProxyIntervals(ResourceAllocator alloc) {

        if (!mOpChains.isEmpty()) {
            int cur = alloc.curOp();
            alloc.addInterval(getTarget(), cur, cur + mOpChains.size() - 1, true);

            TextureProxyVisitor gather = (p, __) -> alloc.addInterval(p,
                    alloc.curOp(),
                    alloc.curOp(),
                    true);
            for (OpChain chain : mOpChains) {
                chain.visitTextures(gather);
                alloc.incOps();
            }
        } else {
            alloc.addInterval(getTarget(), alloc.curOp(), alloc.curOp(), true);
            alloc.incOps();
        }
    }

    @Override
    public void prepare(OpFlushState flushState) {
        for (OpChain chain : mOpChains) {
            if (chain.mHead != null) {
                int pipelineFlags = 0;
                if (chain.getAppliedClip() != null) {
                    if (chain.getAppliedClip().hasScissorClip()) {
                        pipelineFlags |= PipelineInfo.FLAG_HAS_SCISSOR_CLIP;
                    }
                    if (chain.getAppliedClip().hasStencilClip()) {
                        pipelineFlags |= PipelineInfo.FLAG_HAS_STENCIL_CLIP;
                    }
                }
                chain.mHead.onPrepare(flushState, mWriteView, pipelineFlags);
            }
        }
    }

    @Override
    public boolean execute(OpFlushState flushState) {
        assert (numTargets() == 1);
        SurfaceProxy target = getTarget();
        assert (target != null);
        RenderTarget renderTarget = target.peekRenderTarget();
        assert (renderTarget != null);

        OpsRenderPass opsRenderPass = flushState.beginOpsRenderPass(renderTarget,
                false,
                mWriteView.getOrigin(),
                new Rect2i(0, 0, renderTarget.getWidth(), renderTarget.getHeight()),
                (byte) Engine.LoadStoreOps_ClearStore,
                (byte) Engine.LoadStoreOps_DiscardStore,
                new float[]{0, 0, 0, 0});

        for (OpChain chain : mOpChains) {
            if (chain.mHead != null) {
                chain.mHead.onExecute(flushState,
                        new Rect2f(chain.mLeft, chain.mTop, chain.mRight, chain.mBottom));
            }
        }

        opsRenderPass.end();

        return true;
    }

    public void addOp(@Nonnull Op op) {
        recordOp(op, null, false);
    }

    public void recordOp(@Nonnull Op op, @Nullable AppliedClip appliedClip, boolean nonOverlapping) {
        int maxCandidates = Math.min(10, mOpChains.size());
        if (maxCandidates > 0) {
            int i = 0;
            while (true) {
                OpChain candidate = mOpChains.get(mOpChains.size() - 1 - i);
                op = candidate.appendOp(op, appliedClip, nonOverlapping);
                if (op == null) {
                    return;
                }
                // Check overlaps for painter's algorithm
                if (candidate.mRight > op.getLeft() &&
                        candidate.mBottom > op.getTop() &&
                        op.getRight() > candidate.mLeft &&
                        op.getBottom() > candidate.mTop) {
                    // Stop going backwards if we would cause a painter's order violation.
                    break;
                }
                if (++i == maxCandidates) {
                    // Reached max look-back
                    break;
                }
            }
        }
        if (appliedClip != null) {
            appliedClip = appliedClip.clone();
        }
        mOpChains.add(new OpChain(op, appliedClip, nonOverlapping));
    }

    private static class OpChain {

        private Op mHead;
        private Op mTail;

        @Nullable
        private final AppliedClip mAppliedClip;
        private final boolean mNonOverlapping;

        private float mLeft;
        private float mTop;
        private float mRight;
        private float mBottom;

        public OpChain(@Nonnull Op op, @Nullable AppliedClip appliedClip, boolean nonOverlapping) {
            mHead = op;
            mTail = op;
            mAppliedClip = appliedClip;
            mNonOverlapping = nonOverlapping;

            mLeft = op.getLeft();
            mTop = op.getTop();
            mRight = op.getRight();
            mBottom = op.getBottom();

            assert validate();
        }

        public void visitTextures(TextureProxyVisitor func) {
            if (mHead == null) {
                return;
            }
            for (Op op = mHead; op != null; op = op.nextInChain()) {
                op.visitTextures(func);
            }
        }

        public Op getHead() {
            return mHead;
        }

        @Nullable
        public AppliedClip getAppliedClip() {
            return mAppliedClip;
        }

        public void deleteOps() {
            while (mHead != null) {
                //TODO currently we assume there's no resource needs to clean in op instances
                popHead();
            }
        }

        public Op popHead() {
            assert (mHead != null);
            Op temp = mHead;
            mHead = mHead.cutChain();
            if (mHead == null) {
                assert (mTail == temp);
                mTail = null;
            }
            return temp;
        }

        public Op appendOp(@Nonnull Op op, @Nullable AppliedClip appliedClip, boolean nonOverlapping) {
            assert (op.isChainHead() && op.isChainTail());
            assert (op.validateChain(op));
            assert (mHead != null);

            if (mNonOverlapping != nonOverlapping ||
                    (mNonOverlapping &&
                            mRight >= op.getLeft() &&
                            mBottom >= op.getTop() &&
                            op.getRight() >= mLeft &&
                            op.getBottom() >= mTop) ||
                    !Objects.equals(mAppliedClip, appliedClip)) {
                return op;
            }

            if (mTail.combineIfPossible(op)) {
                mTail.mergeChain(op);
                mTail = mTail.nextInChain();
            } else {
                return op;
            }
            mLeft = Math.min(mLeft, op.getLeft());
            mTop = Math.min(mTop, op.getTop());
            mRight = Math.max(mRight, op.getRight());
            mBottom = Math.max(mBottom, op.getBottom());
            assert validate();
            return null;
        }

        private boolean validate() {
            if (mHead != null) {
                assert mTail != null;
                assert mHead.validateChain(mTail);
            }
            for (Op op = mHead; op != null; op = op.nextInChain()) {
                assert (mLeft <= op.getLeft() && mTop <= op.getTop() &&
                        mRight >= op.getRight() && mBottom >= op.getBottom());
            }
            return true;
        }
    }
}
