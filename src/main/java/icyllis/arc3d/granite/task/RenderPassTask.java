/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite.task;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.DrawPass;
import icyllis.arc3d.granite.RecordingContext;

import java.util.Arrays;
import java.util.Objects;

public final class RenderPassTask extends Task {

    DrawPass mDrawPass;
    RenderPassDesc mRenderPassDesc;
    @SharedPtr
    ImageViewProxy mColorTarget;
    @SharedPtr
    ImageViewProxy mResolveTarget;
    float[] mClearColor;

    private RenderPassTask(DrawPass drawPass,
                           RenderPassDesc renderPassDesc,
                           @SharedPtr ImageViewProxy colorTarget,
                           @SharedPtr ImageViewProxy resolveTarget,
                           float[] clearColor) {
        mDrawPass = drawPass;
        mRenderPassDesc = renderPassDesc;
        mColorTarget = colorTarget;
        mResolveTarget = resolveTarget;
        mClearColor = clearColor;
    }

    /**
     * All arguments must be immutable, except for clearColor.
     * DrawPass is owned by this object.
     */
    @SharedPtr
    public static RenderPassTask make(RecordingContext context,
                                      DrawPass drawPass,
                                      @SharedPtr ImageViewProxy colorTarget,
                                      @SharedPtr ImageViewProxy resolveTarget,
                                      byte loadOp, byte storeOp,
                                      float[] clearColor) {
        Objects.requireNonNull(drawPass);
        Objects.requireNonNull(colorTarget);
        assert clearColor.length >= 4;
        var renderPassDesc = new RenderPassDesc();
        renderPassDesc.mNumColorAttachments = 1;
        var colorDesc = renderPassDesc.mColorAttachments[0] = new RenderPassDesc.ColorAttachmentDesc();
        colorDesc.mDesc = colorTarget.getDesc();
        colorDesc.mLoadOp = loadOp;
        colorDesc.mStoreOp = storeOp;
        int depthStencilFlags = drawPass.getDepthStencilFlags();
        if (depthStencilFlags != Engine.DepthStencilFlags.kNone) {
            int depthBits = (depthStencilFlags & Engine.DepthStencilFlags.kDepth) != 0
                    ? 16 : 0;
            int stencilBits = (depthStencilFlags & Engine.DepthStencilFlags.kStencil) != 0
                    ? 8 : 0;
            var depthStencilAttachment = renderPassDesc.mDepthStencilAttachment;
            depthStencilAttachment.mDesc =
                    context.getCaps().getDefaultDepthStencilImageDesc(
                            depthBits, stencilBits,
                            colorTarget.getWidth(), colorTarget.getHeight(),
                            colorTarget.getSampleCount(),
                            ISurface.FLAG_RENDERABLE | ISurface.FLAG_MEMORYLESS
                    );
            assert depthStencilAttachment.mDesc != null;
            // Always clear the depth and stencil to 0 at the start of a DrawPass, but discard at the
            // end since their contents do not affect the next frame.
            depthStencilAttachment.mLoadOp = Engine.LoadOp.kClear;
            depthStencilAttachment.mStoreOp = Engine.StoreOp.kDiscard;
        }
        //TODO MSAA attachment
        return new RenderPassTask(drawPass,
                renderPassDesc,
                colorTarget,
                resolveTarget,
                Arrays.copyOf(clearColor, 4));
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        mDrawPass.close();
        mDrawPass = null;
        mColorTarget = RefCnt.move(mColorTarget);
        mResolveTarget = RefCnt.move(mResolveTarget);
    }

    @Override
    public int prepare(RecordingContext context) {
        ResourceProvider resourceProvider = context.getResourceProvider();
        if (!mColorTarget.instantiateIfNonLazy(resourceProvider)) {
            return RESULT_FAILURE;
        }
        if (mResolveTarget != null &&
                !mResolveTarget.instantiateIfNonLazy(resourceProvider)) {
            return RESULT_FAILURE;
        }
        if (!mDrawPass.prepare(resourceProvider, mRenderPassDesc)) {
            return RESULT_FAILURE;
        }
        return RESULT_SUCCESS;
    }

    @Override
    public int execute(ImmediateContext context, CommandBuffer commandBuffer) {
        assert mColorTarget.isInstantiated();
        assert mResolveTarget == null || mResolveTarget.isInstantiated();

        Image colorAttachment = mColorTarget.getImage();

        @SharedPtr
        Image depthStencilAttachment = null;
        if (mRenderPassDesc.mDepthStencilAttachment.mDesc != null) {
            depthStencilAttachment = context.getResourceProvider().findOrCreateImage(
                    mRenderPassDesc.mDepthStencilAttachment.mDesc,
                    true,
                    "SharedDSAttachment"
            );
            if (depthStencilAttachment == null) {
                return RESULT_FAILURE;
            }
        }

        // here all attachments are ref-ed
        colorAttachment.ref();
        @SharedPtr
        Image resolveAttachment = mResolveTarget != null ? mResolveTarget.refImage() : null;

        var framebufferDesc = new FramebufferDesc(
                colorAttachment.getWidth(), colorAttachment.getHeight(), colorAttachment.getSampleCount(),
                new FramebufferDesc.ColorAttachmentDesc(
                        colorAttachment, resolveAttachment, 0, 0
                ),
                new FramebufferDesc.DepthStencilAttachmentDesc(
                        depthStencilAttachment
                )
        );

        if (commandBuffer.beginRenderPass(
                mRenderPassDesc,
                framebufferDesc,
                mDrawPass.getBounds(),
                mClearColor,
                0.0f, 0
        )) {
            // matches 2D projection vector
            commandBuffer.setViewport(0, 0, framebufferDesc.mWidth, framebufferDesc.mHeight);
            boolean success = mDrawPass.execute(commandBuffer);
            commandBuffer.endRenderPass();
            if (success) {
                commandBuffer.trackCommandBufferResource(colorAttachment);
                commandBuffer.trackCommandBufferResource(resolveAttachment);
                commandBuffer.trackCommandBufferResource(depthStencilAttachment);
                return RESULT_SUCCESS;
            }
        }
        RefCnt.move(colorAttachment);
        RefCnt.move(resolveAttachment);
        RefCnt.move(depthStencilAttachment);
        return RESULT_FAILURE;
    }
}
