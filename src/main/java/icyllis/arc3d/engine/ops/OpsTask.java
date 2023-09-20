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

package icyllis.arc3d.engine.ops;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Objects;

import static icyllis.arc3d.engine.Engine.*;

//TODO
public class OpsTask extends RenderTask {

    public static final int
            STENCIL_CONTENT_DONT_CARE = 0,
            STENCIL_CONTENT_USER_BITS_CLEARED = 1,
            STENCIL_CONTENT_PRESERVED = 2;

    private final ArrayList<OpChain> mOpChains = new ArrayList<>(25);

    private final ObjectOpenHashSet<TextureProxy> mSampledTextures = new ObjectOpenHashSet<>();

    private final SurfaceProxyView mWriteView;
    private int mPipelineFlags;

    private final Rect2f mTotalBounds = new Rect2f();
    private final Rect2i mContentBounds = new Rect2i();

    private byte mColorLoadOp = LoadOp.Load;
    private int mInitialStencilContent = STENCIL_CONTENT_DONT_CARE;
    private final float[] mLoadClearColor = new float[4];

    /**
     * @param writeView the reference to the owner's write view
     */
    public OpsTask(@Nonnull DrawingManager drawingMgr,
                   @Nonnull SurfaceProxyView writeView) {
        super(drawingMgr);
        mWriteView = writeView;             // move
        addTarget(writeView.refProxy());    // inc
    }

    public void setColorLoadOp(byte loadOp, float red, float green, float blue, float alpha) {
        mColorLoadOp = loadOp;
        mLoadClearColor[0] = red;
        mLoadClearColor[1] = green;
        mLoadClearColor[2] = blue;
        mLoadClearColor[3] = alpha;
        Swizzle.apply(mWriteView.getSwizzle(), mLoadClearColor);
        if (loadOp == LoadOp.Clear) {
            SurfaceProxy target = getTarget();
            mTotalBounds.set(0, 0,
                    target.getBackingWidth(), target.getBackingHeight());
        }
    }

    public void setInitialStencilContent(int stencilContent) {
        mInitialStencilContent = stencilContent;
    }

    @Override
    public void gatherProxyIntervals(ResourceAllocator alloc) {
        if (!mOpChains.isEmpty()) {
            int cur = alloc.curOp();
            alloc.addInterval(getTarget(), cur, cur + mOpChains.size() - 1, true);

            var gather = (TextureProxyVisitor) (p, __) -> alloc.addInterval(p,
                    alloc.curOp(),
                    alloc.curOp(),
                    true);
            for (OpChain chain : mOpChains) {
                chain.visitProxies(gather);
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
                int pipelineFlags = mPipelineFlags;
                if (chain.getAppliedClip() != null) {
                    if (chain.getAppliedClip().hasScissorClip()) {
                        pipelineFlags |= PipelineInfo.kHasScissorClip_Flag;
                    }
                    if (chain.getAppliedClip().hasStencilClip()) {
                        pipelineFlags |= PipelineInfo.kHasStencilClip_Flag;
                    }
                }
                chain.mHead.onPrepare(flushState, mWriteView, pipelineFlags);
            }
        }
    }

    @Override
    public boolean execute(OpFlushState flushState) {
        assert (getNumTargets() == 1);
        SurfaceProxy target = getTarget();
        assert (target != null && target == mWriteView.getProxy());

        OpsRenderPass opsRenderPass = flushState.beginOpsRenderPass(mWriteView,
                mContentBounds,
                LoadStoreOps.make(mColorLoadOp, StoreOp.Store),
                LoadStoreOps.DontLoad_Store,
                mLoadClearColor,
                mSampledTextures,
                mPipelineFlags);

        for (OpChain chain : mOpChains) {
            if (chain.mHead != null) {
                chain.mHead.onExecute(flushState,
                        chain.mBounds);
            }
        }

        opsRenderPass.end();

        return true;
    }

    @Override
    protected void onMakeClosed(RecordingContext context) {
        if (mOpChains.isEmpty() && mColorLoadOp == LoadOp.Load) {
            return;
        }
        SurfaceProxy target = getTarget();
        int rtHeight = target.getBackingHeight();
        Rect2f clippedContentBounds = new Rect2f(0, 0, target.getBackingWidth(), rtHeight);
        boolean result = clippedContentBounds.intersect(mTotalBounds);
        assert result;
        clippedContentBounds.roundOut(mContentBounds);
        TextureProxy textureProxy = target.asTextureProxy();
        if (textureProxy != null) {
            if (textureProxy.isManualMSAAResolve()) {
                int msaaTop;
                int msaaBottom;
                if (mWriteView.getOrigin() == SurfaceOrigin.kLowerLeft) {
                    msaaTop = rtHeight - mContentBounds.mBottom;
                    msaaBottom = rtHeight - mContentBounds.mTop;
                } else {
                    msaaTop = mContentBounds.mTop;
                    msaaBottom = mContentBounds.mBottom;
                }
                textureProxy.setMSAADirty(mContentBounds.mLeft, msaaTop,
                        mContentBounds.mRight, msaaBottom);
            }
            if (textureProxy.isMipmapped()) {
                textureProxy.setMipmapsDirty(true);
            }
        }
    }

    public void addOp(@Nonnull Op op) {
        recordOp(op, null, ProcessorAnalyzer.EMPTY_ANALYSIS);
    }

    public void addDrawOp(@Nonnull DrawOp op, @Nullable AppliedClip clip, int processorAnalysis) {
        TextureProxyVisitor addDependency = (p, ss) -> {
            mSampledTextures.add(p);
            addDependency(p, ss);
        };

        op.visitProxies(addDependency);

        if ((processorAnalysis & ProcessorAnalyzer.NON_COHERENT_BLENDING) != 0) {
            mPipelineFlags |= PipelineInfo.kRenderPassBlendBarrier_Flag;
        }

        recordOp(op, clip != null && clip.hasClip() ? clip : null, processorAnalysis);
    }

    void recordOp(@Nonnull Op op, @Nullable AppliedClip clip, int processorAnalysis) {
        // A closed OpsTask should never receive new/more ops
        assert (!isClosed());

        if (!Rect2f.isFinite(op.getLeft(), op.getTop(),
                op.getRight(), op.getBottom())) {
            return;
        }

        mTotalBounds.join(op.getLeft(), op.getTop(),
                op.getRight(), op.getBottom());

        int maxCandidates = Math.min(10, mOpChains.size());
        if (maxCandidates > 0) {
            int i = 0;
            while (true) {
                OpChain candidate = mOpChains.get(mOpChains.size() - 1 - i);
                op = candidate.appendOp(op, clip, processorAnalysis);
                if (op == null) {
                    return;
                }
                // Check overlaps for painter's algorithm
                if (Rect2f.rectsOverlap(candidate.mBounds,
                        op.getLeft(), op.getTop(),
                        op.getRight(), op.getBottom())) {
                    // Stop going backwards if we would cause a painter's order violation.
                    break;
                }
                if (++i == maxCandidates) {
                    // Reached max look-back
                    break;
                }
            }
        }
        if (clip != null) {
            clip = clip.clone();
        }
        mOpChains.add(new OpChain(op, clip, processorAnalysis));
    }

    private static class OpChain {

        private Op mHead;
        private Op mTail;

        @Nullable
        private final AppliedClip mAppliedClip;
        private final int mProcessorAnalysis;

        private final Rect2f mBounds;

        public OpChain(@Nonnull Op op, @Nullable AppliedClip appliedClip, int processorAnalysis) {
            mHead = op;
            mTail = op;

            mAppliedClip = appliedClip;
            mProcessorAnalysis = processorAnalysis;

            mBounds = new Rect2f(op.getLeft(), op.getTop(),
                    op.getRight(), op.getBottom());

            assert (validate());
        }

        public void visitProxies(TextureProxyVisitor func) {
            for (Op op = mHead; op != null; op = op.nextInChain()) {
                op.visitProxies(func);
            }
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

        public Op appendOp(@Nonnull Op op, @Nullable AppliedClip appliedClip, int processorAnalysis) {
            assert (op.isChainHead() && op.isChainTail());
            assert (op.validateChain(op));
            assert (mHead != null);

            if (((mProcessorAnalysis & ProcessorAnalyzer.NON_OVERLAPPING) !=
                    (processorAnalysis & ProcessorAnalyzer.NON_OVERLAPPING)) ||
                    ((mProcessorAnalysis & ProcessorAnalyzer.NON_OVERLAPPING) != 0 &&
                            Rect2f.rectsTouchOrOverlap(mBounds,
                                    op.getLeft(), op.getTop(),
                                    op.getRight(), op.getBottom())) ||
                    !Objects.equals(mAppliedClip, appliedClip)) {
                return op;
            }

            if (mTail.combineIfPossible(op)) {
                mTail.mergeChain(op);
                mTail = mTail.nextInChain();
            } else {
                return op;
            }
            mBounds.joinNoCheck(op.getLeft(), op.getTop(),
                    op.getRight(), op.getBottom());
            assert validate();
            return null;
        }

        private boolean validate() {
            if (mHead != null) {
                assert mTail != null;
                assert mHead.validateChain(mTail);
            }
            for (Op op = mHead; op != null; op = op.nextInChain()) {
                assert (mBounds.mLeft <= op.getLeft() && mBounds.mTop <= op.getTop() &&
                        mBounds.mRight >= op.getRight() && mBounds.mBottom >= op.getBottom());
            }
            return true;
        }
    }
}
