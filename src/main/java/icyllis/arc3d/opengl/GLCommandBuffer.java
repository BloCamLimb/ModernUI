/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Image;
import icyllis.arc3d.engine.*;
import org.lwjgl.system.MemoryStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_SWIZZLE_R;
import static org.lwjgl.opengl.GL45C.*;

/**
 * The main command buffer of OpenGL context. The commands executed on {@link GLCommandBuffer} are
 * mostly the same as that on {@link GLDevice}, but {@link GLCommandBuffer} assumes some values
 * and will not handle dirty context.
 */
public final class GLCommandBuffer extends CommandBuffer {

    private static final int
            TriState_Disabled = 0,
            TriState_Enabled = 1,
            TriState_Unknown = 2;

    private final GLDevice mDevice;
    private final GLResourceProvider mResourceProvider;

    // ThreadLocal has additional overhead, cache the instance here
    private final MemoryStack mStack = MemoryStack.stackGet();

    private int mHWViewportWidth;
    private int mHWViewportHeight;

    private int mHWScissorTest;
    private int mHWColorWrite;

    private int mHWScissorX;
    private int mHWScissorY;
    private int mHWScissorWidth;
    private int mHWScissorHeight;

    @SharedPtr
    private GLFramebuffer mHWFramebuffer;

    @SharedPtr
    private GLProgram mHWProgram;
    @SharedPtr
    private GLVertexArray mHWVertexArray;
    private boolean mHWVertexArrayInvalid;

    @SharedPtr
    private final GLUniformBuffer[] mBoundUniformBuffers;

    // Below OpenGL 4.5.
    private int mHWActiveTextureUnit;

    // [Unit][ImageType]
    private final UniqueID[][] mHWTextureStates;
    private final UniqueID[]   mHWSamplerStates;

    private GLGraphicsPipeline mGraphicsPipeline;

    private int mPrimitiveType;

    private int mIndexType;
    private long mIndexBufferOffset;

    //TODO shall we track refcnt here?
    @RawPtr
    private final GLBuffer[] mActiveVertexBuffers = new GLBuffer[Caps.MAX_VERTEX_BINDINGS];
    private final long[] mActiveVertexOffsets = new long[Caps.MAX_VERTEX_BINDINGS];

    private RenderPassDesc mRenderPassDesc;
    private final Rect2i mContentBounds = new Rect2i();

    private long mSubmitFence;

    GLCommandBuffer(GLDevice device, GLResourceProvider resourceProvider) {
        mDevice = device;
        mResourceProvider = resourceProvider;

        mBoundUniformBuffers = new GLUniformBuffer[4];

        int maxTextureUnits = device.getCaps().shaderCaps().mMaxFragmentSamplers;
        mHWTextureStates = new UniqueID[maxTextureUnits][Engine.ImageType.kCount];
        mHWSamplerStates = new UniqueID[maxTextureUnits];
    }

    void submit() {

        mSubmitFence = mDevice.getGL().glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    void checkFinishedAndReset() {
        if (mSubmitFence == 0) {
            return;
        }
        int status = mDevice.getGL().glClientWaitSync(mSubmitFence, 0, 0);
        if (status == GL_CONDITION_SATISFIED ||
                status == GL_ALREADY_SIGNALED) {

            mDevice.getGL().glDeleteSync(mSubmitFence);
        }
    }

    public void resetStates(int states) {
        if ((states & Engine.GLBackendState.kRenderTarget) != 0) {
            mHWFramebuffer = RefCnt.move(mHWFramebuffer);
        }

        if ((states & Engine.GLBackendState.kPipeline) != 0) {
            mHWProgram = RefCnt.move(mHWProgram);
            mHWVertexArray = RefCnt.move(mHWVertexArray);
            mHWVertexArrayInvalid = true;
            for (int i = 0; i < mBoundUniformBuffers.length; i++) {
                mBoundUniformBuffers[i] = RefCnt.move(mBoundUniformBuffers[i]);
            }
        }

        if ((states & Engine.GLBackendState.kView) != 0) {
            mHWScissorTest = TriState_Unknown;
            mHWScissorX = -1;
            mHWScissorY = -1;
            mHWScissorWidth = -1;
            mHWScissorHeight = -1;
            mHWViewportWidth = -1;
            mHWViewportHeight = -1;
        }

        if ((states & Engine.GLBackendState.kMisc) != 0) {
            mHWColorWrite = TriState_Unknown;
        }
    }

    @Override
    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   FramebufferDesc framebufferDesc,
                                   Rect2ic renderPassBounds,
                                   float[] clearColors,
                                   float clearDepth,
                                   int clearStencil) {
        if ((framebufferDesc.mFramebufferFlags & ISurface.FLAG_GL_WRAP_DEFAULT_FB) != 0) {
            mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, 0);
            mHWFramebuffer = RefCnt.move(mHWFramebuffer);
        } else {
            GLFramebuffer framebuffer;
            framebuffer = mResourceProvider.findOrCreateFramebuffer(framebufferDesc);
            if (framebuffer == null) {
                return false;
            }
            if (mHWFramebuffer != framebuffer) {
                mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, framebuffer.getRenderFramebuffer());
                mHWFramebuffer = framebuffer;
            } else {
                framebuffer.unref();
            }
        }
        flushViewport(framebufferDesc.mWidth, framebufferDesc.mHeight);

        try (MemoryStack stack = mStack.push()) {
            var color = stack.mallocFloat(4);
            for (int i = 0; i < renderPassDesc.mNumColorAttachments; i++) {
                var attachmentDesc = renderPassDesc.mColorAttachments[i];
                boolean colorLoadClear = attachmentDesc.mLoadOp == Engine.LoadOp.Clear;
                if (colorLoadClear) {
                    flushScissorTest(false);
                    flushColorWrite(true);
                    color.put(0, clearColors, i << 2, 4);
                    glClearBufferfv(GL_COLOR,
                            i,
                            color);
                }
            }
            boolean stencilLoadClear = renderPassDesc.mDepthStencilAttachment.mDesc != null &&
                    renderPassDesc.mDepthStencilAttachment.mLoadOp == Engine.LoadOp.Clear;
            if (stencilLoadClear) {
                flushScissorTest(false);
                glStencilMask(0xFFFFFFFF); // stencil will be flushed later
                glClearBufferfi(GL_DEPTH_STENCIL,
                        0,
                        clearDepth, clearStencil);
            }
        }
        mRenderPassDesc = renderPassDesc;
        mContentBounds.set(renderPassBounds);

        return true;
    }

    @Override
    public void endRenderPass() {
        /*mActiveIndexBuffer = RefCnt.move(mActiveIndexBuffer);
        mActiveVertexBuffer = RefCnt.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = RefCnt.move(mActiveInstanceBuffer);*/
        GLFramebuffer framebuffer = mHWFramebuffer;

        RenderPassDesc renderPassDesc = mRenderPassDesc;
        try (MemoryStack stack = mStack.push()) {
            var attachmentsToDiscard = stack.mallocInt(renderPassDesc.mNumColorAttachments + 1);
            for (int i = 0; i < renderPassDesc.mNumColorAttachments; i++) {
                var attachmentDesc = renderPassDesc.mColorAttachments[i];
                boolean colorLoadClear = attachmentDesc.mStoreOp == Engine.StoreOp.DontCare;
                if (colorLoadClear) {
                    attachmentsToDiscard.put(framebuffer == null
                            ? GL_COLOR
                            : GL_COLOR_ATTACHMENT0 + i);
                }
            }
            boolean stencilStoreDiscard = renderPassDesc.mDepthStencilAttachment.mDesc != null &&
                    renderPassDesc.mDepthStencilAttachment.mStoreOp == Engine.StoreOp.DontCare;
            if (stencilStoreDiscard) {
                attachmentsToDiscard.put(framebuffer == null
                        ? GL_STENCIL
                        : GL_DEPTH_STENCIL_ATTACHMENT);
            }
            attachmentsToDiscard.flip();
            if (attachmentsToDiscard.hasRemaining()) {
                glInvalidateFramebuffer(GL_FRAMEBUFFER, attachmentsToDiscard);
            }
        }

        if (framebuffer != null) {
            if (framebuffer.getRenderFramebuffer() != framebuffer.getResolveFramebuffer()) {
                // BlitFramebuffer respects the scissor, so disable it.
                flushScissorTest(false);
                // MSAA to single
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer.getResolveFramebuffer());
                glBlitFramebuffer(
                        mContentBounds.mLeft, mContentBounds.mTop, mContentBounds.mRight, mContentBounds.mBottom, //
                        // src rect
                        mContentBounds.mLeft, mContentBounds.mTop, mContentBounds.mRight, mContentBounds.mBottom, //
                        // dst rect
                        GL_COLOR_BUFFER_BIT, GL_NEAREST);
            }
        }
    }

    /**
     * Flush viewport.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     */
    public void flushViewport(int width, int height) {
        assert (width >= 0 && height >= 0);
        if (width != mHWViewportWidth || height != mHWViewportHeight) {
            /*glViewportIndexedf(0, 0.0f, 0.0f, width, height);
            glDepthRangeIndexed(0, 0.0f, 1.0f);*/
            mDevice.getGL().glViewport(0, 0, width, height);
            mHWViewportWidth = width;
            mHWViewportHeight = height;
        }
    }

    /**
     * Flush scissor.
     *
     * @param width  the effective width of color attachment
     * @param height the effective height of color attachment
     * @param origin the surface origin
     * @see Engine.SurfaceOrigin
     */
    public void flushScissorRect(int width, int height, int origin,
                                 int scissorLeft, int scissorTop,
                                 int scissorRight, int scissorBottom) {
        assert (width >= 0 && height >= 0);
        final int scissorWidth = scissorRight - scissorLeft;
        final int scissorHeight = scissorBottom - scissorTop;
        assert (scissorLeft >= 0 && scissorTop >= 0 &&
                scissorWidth >= 0 && scissorWidth <= width &&
                scissorHeight >= 0 && scissorHeight <= height);
        final int scissorY;
        if (origin == Engine.SurfaceOrigin.kUpperLeft) {
            scissorY = scissorTop;
        } else {
            assert (origin == Engine.SurfaceOrigin.kLowerLeft);
            scissorY = height - scissorBottom;
        }
        assert (scissorY >= 0);
        if (scissorLeft != mHWScissorX ||
                scissorY != mHWScissorY ||
                scissorWidth != mHWScissorWidth ||
                scissorHeight != mHWScissorHeight) {
            mDevice.getGL().glScissor(scissorLeft, scissorY,
                    scissorWidth, scissorHeight);
            mHWScissorX = scissorLeft;
            mHWScissorY = scissorY;
            mHWScissorWidth = scissorWidth;
            mHWScissorHeight = scissorHeight;
        }
    }

    /**
     * Flush scissor test.
     *
     * @param enable whether to enable scissor test
     */
    public void flushScissorTest(boolean enable) {
        if (enable) {
            if (mHWScissorTest != TriState_Enabled) {
                mDevice.getGL().glEnable(GL_SCISSOR_TEST);
                mHWScissorTest = TriState_Enabled;
            }
        } else {
            if (mHWScissorTest != TriState_Disabled) {
                mDevice.getGL().glDisable(GL_SCISSOR_TEST);
                mHWScissorTest = TriState_Disabled;
            }
        }
    }

    /**
     * Flush color mask for draw buffer 0.
     *
     * @param enable whether to write color
     */
    public void flushColorWrite(boolean enable) {
        if (enable) {
            if (mHWColorWrite != TriState_Enabled) {
                mDevice.getGL().glColorMask(true, true, true, true);
                mHWColorWrite = TriState_Enabled;
            }
        } else {
            if (mHWColorWrite != TriState_Disabled) {
                mDevice.getGL().glColorMask(false, false, false, false);
                mHWColorWrite = TriState_Disabled;
            }
        }
    }

    /**
     * Bind raw framebuffer and flush render target to be invalid.
     */
    public void bindFramebuffer(int framebuffer) {
        mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    }

    public void bindPipeline(@Nonnull @RawPtr GLProgram program,
                             @Nonnull @RawPtr GLVertexArray vertexArray) {
        if (mHWProgram != program) {
            // active program will not be deleted, so no collision
            mDevice.getGL().glUseProgram(program.getProgram());
            bindVertexArray(vertexArray);
            mHWProgram = RefCnt.create(mHWProgram, program);
        }
    }

    public void bindVertexArray(int vertexArray) {
        mDevice.getGL().glBindVertexArray(vertexArray);
        mHWVertexArray = RefCnt.move(mHWVertexArray);
        mHWVertexArrayInvalid = true;
    }

    public void bindVertexArray(@Nullable @RawPtr GLVertexArray vertexArray) {
        if (mHWVertexArrayInvalid ||
                mHWVertexArray != vertexArray) {
            // active vertex array will not be deleted, so no collision
            mDevice.getGL().glBindVertexArray(vertexArray == null ? 0 : vertexArray.getHandle());
            mHWVertexArray = RefCnt.create(mHWVertexArray, vertexArray);
            mHWVertexArrayInvalid = false;
        }
    }

    public void bindUniformBuffer(@Nonnull @RawPtr GLUniformBuffer uniformBuffer) {
        int index = uniformBuffer.getBinding();
        if (mBoundUniformBuffers[index] != uniformBuffer) {
            mDevice.getGL().glBindBufferBase(GL_UNIFORM_BUFFER, index, uniformBuffer.getHandle());
            mBoundUniformBuffers[index] = RefCnt.create(mBoundUniformBuffers[index], uniformBuffer);
        }
    }

    @Override
    public boolean bindGraphicsPipeline(GraphicsPipeline graphicsPipeline) {
        /*mActiveIndexBuffer = RefCnt.move(mActiveIndexBuffer);
        mActiveVertexBuffer = RefCnt.move(mActiveVertexBuffer);
        mActiveInstanceBuffer = RefCnt.move(mActiveInstanceBuffer);*/

        mGraphicsPipeline = (GLGraphicsPipeline) graphicsPipeline;
        if (mGraphicsPipeline == null) {
            return false;
        }
        mPrimitiveType = switch (graphicsPipeline.getPrimitiveType()) {
            case Engine.PrimitiveType.PointList -> GL_POINTS;
            case Engine.PrimitiveType.LineList -> GL_LINES;
            case Engine.PrimitiveType.LineStrip -> GL_LINE_STRIP;
            case Engine.PrimitiveType.TriangleList -> GL_TRIANGLES;
            case Engine.PrimitiveType.TriangleStrip -> GL_TRIANGLE_STRIP;
            default -> throw new AssertionError();
        };

        //TODO flush RT again?
        if (!mGraphicsPipeline.bindPipeline(this)) {
            return false;
        }

        /*return mPipelineState.bindUniforms(mCmdBuffer, pipelineInfo,
                mRenderTarget.getWidth(), mRenderTarget.getHeight());*/
        return true;
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {

    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        //TODO
    }

    @Override
    public void bindIndexBuffer(int indexType, @RawPtr Buffer buffer, long offset) {
        assert (mGraphicsPipeline != null);
        mIndexType = switch (indexType) {
            case Engine.IndexType.kUByte -> GL_UNSIGNED_BYTE;
            case Engine.IndexType.kUShort -> GL_UNSIGNED_SHORT;
            case Engine.IndexType.kUInt -> GL_UNSIGNED_INT;
            default -> throw new AssertionError();
        };
        mGraphicsPipeline.bindIndexBuffer((GLBuffer) buffer);
        mIndexBufferOffset = offset;
    }

    @Override
    public void bindVertexBuffer(int binding, @RawPtr Buffer buffer, long offset) {
        assert (mGraphicsPipeline != null);
        GLBuffer glBuffer = (GLBuffer) buffer;
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            // base instance support covers all cases
            mGraphicsPipeline.bindVertexBuffer(binding, glBuffer, offset);
        } else {
            // bind instance buffer on drawInstanced(), we may rebind vertex buffer on drawIndexed()
            if (mGraphicsPipeline.getVertexInputRate(binding) == VertexInputLayout.INPUT_RATE_VERTEX) {
                mGraphicsPipeline.bindVertexBuffer(binding, glBuffer, offset);
            }
            mActiveVertexBuffers[binding] = glBuffer;
            mActiveVertexOffsets[binding] = offset;
        }
    }

    @Override
    public void bindUniformBuffer(int binding, @RawPtr Buffer buffer, long offset, long size) {
        assert (mGraphicsPipeline != null);
        GLBuffer glBuffer = (GLBuffer) buffer;
        mDevice.getGL().glBindBufferRange(GL_UNIFORM_BUFFER, binding, glBuffer.getHandle(),
                offset, size);
    }

    @Override
    public void bindTextureSampler(int binding, @RawPtr Image texture,
                                   @RawPtr Sampler sampler, short readSwizzle) {
        assert (texture != null && texture.isSampledImage());
        GLTexture glTexture = (GLTexture) texture;
        GLSampler glSampler = (GLSampler) sampler;
        boolean dsa = mDevice.getCaps().hasDSASupport();
        int target = glTexture.getTarget();
        int imageType = glTexture.getImageType();
        if (mHWTextureStates[binding][imageType] != glTexture.getUniqueID()) {
            if (dsa) {
                mDevice.getGL().glBindTextureUnit(binding, glTexture.getHandle());
            } else {
                setTextureUnit(binding);
                mDevice.getGL().glBindTexture(target, glTexture.getHandle());
            }
            mHWTextureStates[binding][imageType] = glTexture.getUniqueID();
        }
        if (mHWSamplerStates[binding] != glSampler.getUniqueID()) {
            mDevice.getGL().glBindSampler(binding, glSampler.getHandle());
            mHWSamplerStates[binding] = glSampler.getUniqueID();
        }
        GLTextureMutableState mutableState = glTexture.getGLMutableState();
        if (mutableState.baseMipmapLevel != 0) {
            if (dsa) {
                glTextureParameteri(glTexture.getHandle(), GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            }
            mutableState.baseMipmapLevel = 0;
        }
        int maxLevel = texture.getMipLevelCount() - 1; // minus base level
        if (mutableState.maxMipmapLevel != maxLevel) {
            if (dsa) {
                glTextureParameteri(glTexture.getHandle(), GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            mutableState.maxMipmapLevel = maxLevel;
        }
        //TODO texture view
        // texture view is available since 4.3, but less used in OpenGL
        // in case of some driver bugs, we don't use GL_TEXTURE_SWIZZLE_RGBA
        // and OpenGL ES does not support GL_TEXTURE_SWIZZLE_RGBA at all
        for (int i = 0; i < 4; ++i) {
            int swiz = switch (readSwizzle & 0xF) {
                case 0 -> GL_RED;
                case 1 -> GL_GREEN;
                case 2 -> GL_BLUE;
                case 3 -> GL_ALPHA;
                case 4 -> GL_ZERO;
                case 5 -> GL_ONE;
                default -> throw new AssertionError(readSwizzle);
            };
            if (mutableState.getSwizzle(i) != swiz) {
                mutableState.setSwizzle(i, swiz);
                // swizzle enums are sequential
                int channel = GL_TEXTURE_SWIZZLE_R + i;
                if (dsa) {
                    glTextureParameteri(glTexture.getHandle(), channel, swiz);
                } else {
                    glTexParameteri(GL_TEXTURE_2D, channel, swiz);
                }
            }
            readSwizzle >>= 4;
        }
    }

    /**
     * Binds texture unit in context. OpenGL 3 only.
     *
     * @param unit 0-based texture unit index
     */
    private void setTextureUnit(int unit) {
        assert (unit >= 0 && unit < mHWTextureStates.length);
        if (unit != mHWActiveTextureUnit) {
            mDevice.getGL().glActiveTexture(GL_TEXTURE0 + unit);
            mHWActiveTextureUnit = unit;
        }
    }

    private long getIndexOffset(int baseIndex) {
        int indexSize = Engine.IndexType.size(mIndexType);
        return (long) baseIndex * indexSize + mIndexBufferOffset;
    }

    @Override
    public void draw(int vertexCount, int baseVertex) {
        mDevice.getGL().glDrawArrays(mPrimitiveType, baseVertex, vertexCount);
    }

    @Override
    public void drawIndexed(int indexCount, int baseIndex,
                            int baseVertex) {
        long indicesOffset = getIndexOffset(baseIndex);
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset, 1, baseVertex, 0);
        } else if (mDevice.getCaps().hasDrawElementsBaseVertexSupport()) {
            mDevice.getGL().glDrawElementsBaseVertex(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset, baseVertex);
        } else {
            int bindings = mGraphicsPipeline.getVertexBindingCount();
            for (int i = 0; i < bindings; i++) {
                assert (mGraphicsPipeline.getVertexInputRate(i) == VertexInputLayout.INPUT_RATE_VERTEX);
                long vertexOffset = (long) baseVertex * mGraphicsPipeline.getVertexStride(i) +
                        mActiveVertexOffsets[i];
                mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], vertexOffset);
            }
            mDevice.getGL().glDrawElements(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset);
        }
    }

    @Override
    public void drawInstanced(int instanceCount, int baseInstance,
                              int vertexCount, int baseVertex) {
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawArraysInstancedBaseInstance(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount, baseInstance);
        } else {
            int bindings = mGraphicsPipeline.getVertexBindingCount();
            for (int i = 0; i < bindings; i++) {
                if (mGraphicsPipeline.getVertexInputRate(i) > VertexInputLayout.INPUT_RATE_VERTEX) {
                    long instanceOffset = (long) baseInstance * mGraphicsPipeline.getVertexStride(i) +
                            mActiveVertexOffsets[i];
                    mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], instanceOffset);
                }
            }
            mDevice.getGL().glDrawArraysInstanced(mPrimitiveType, baseVertex, vertexCount,
                    instanceCount);
        }
    }

    @Override
    public void drawIndexedInstanced(int indexCount, int baseIndex,
                                     int instanceCount, int baseInstance,
                                     int baseVertex) {
        long indicesOffset = getIndexOffset(baseIndex);
        if (mDevice.getCaps().hasBaseInstanceSupport()) {
            mDevice.getGL().glDrawElementsInstancedBaseVertexBaseInstance(mPrimitiveType, indexCount,
                    mIndexType, indicesOffset, instanceCount, baseVertex, baseInstance);
        } else {
            boolean hasBaseVertexSupport = mDevice.getCaps().hasDrawElementsBaseVertexSupport();
            int bindings = mGraphicsPipeline.getVertexBindingCount();
            for (int i = 0; i < bindings; i++) {
                if (mGraphicsPipeline.getVertexInputRate(i) > VertexInputLayout.INPUT_RATE_VERTEX) {
                    long instanceOffset = (long) baseInstance * mGraphicsPipeline.getVertexStride(i) +
                            mActiveVertexOffsets[i];
                    mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], instanceOffset);
                } else if (!hasBaseVertexSupport) {
                    long vertexOffset = (long) baseVertex * mGraphicsPipeline.getVertexStride(i) +
                            mActiveVertexOffsets[i];
                    mGraphicsPipeline.bindVertexBuffer(i, mActiveVertexBuffers[i], vertexOffset);
                }
            }
            if (hasBaseVertexSupport) {
                mDevice.getGL().glDrawElementsInstancedBaseVertex(mPrimitiveType, indexCount,
                        mIndexType, indicesOffset, instanceCount, baseVertex);
            } else {
                mDevice.getGL().glDrawElementsInstanced(mPrimitiveType, indexCount,
                        mIndexType, indicesOffset, instanceCount);
            }
        }
    }
}
