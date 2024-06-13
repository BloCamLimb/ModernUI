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

import java.util.Arrays;

import static org.lwjgl.opengl.GL32C.*;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_SWIZZLE_R;
import static org.lwjgl.opengl.GL45C.*;

/**
 * The OpenGL command buffer. The commands executed on {@link GLCommandBuffer} are
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

    private int mHWViewportX;
    private int mHWViewportY;
    private int mHWViewportWidth;
    private int mHWViewportHeight;

    private int mHWScissorX;
    private int mHWScissorY;
    private int mHWScissorWidth;
    private int mHWScissorHeight;

    private int mHWScissorTest;
    private int mHWColorWrite;
    private int mHWBlendState;
    private int mHWBlendSrcFactor;
    private int mHWBlendDstFactor;

    @SharedPtr
    private GLFramebuffer mHWFramebuffer;

    @RawPtr
    private GLProgram mHWProgram;
    @RawPtr
    private GLVertexArray mHWVertexArray;

    // Below OpenGL 4.5.
    private int mHWActiveTextureUnit;

    // [Unit][ImageType]
    private final UniqueID[][] mHWTextureStates;
    private final UniqueID[] mHWSamplerStates;

    @RawPtr
    private GLGraphicsPipeline mGraphicsPipeline;

    private int mPrimitiveType;

    private int mIndexType;
    private long mIndexBufferOffset;

    @RawPtr
    private final GLBuffer[] mActiveVertexBuffers = new GLBuffer[Caps.MAX_VERTEX_BINDINGS];
    private final long[] mActiveVertexOffsets = new long[Caps.MAX_VERTEX_BINDINGS];

    private RenderPassDesc mRenderPassDesc;
    private final Rect2i mContentBounds = new Rect2i();

    private long mSubmitFence;

    GLCommandBuffer(GLDevice device, GLResourceProvider resourceProvider) {
        mDevice = device;
        mResourceProvider = resourceProvider;

        int maxTextureUnits = device.getCaps().shaderCaps().mMaxFragmentSamplers;
        mHWTextureStates = new UniqueID[maxTextureUnits][Engine.ImageType.kCount];
        mHWSamplerStates = new UniqueID[maxTextureUnits];

        resetStates();
    }

    public void resetStates() {
        mHWFramebuffer = RefCnt.move(mHWFramebuffer);

        mHWProgram = null;
        mHWVertexArray = null;

        mHWScissorTest = TriState_Unknown;
        mHWScissorX = -1;
        mHWScissorY = -1;
        mHWScissorWidth = -1;
        mHWScissorHeight = -1;
        mHWViewportX = -1;
        mHWViewportY = -1;
        mHWViewportWidth = -1;
        mHWViewportHeight = -1;

        mHWColorWrite = TriState_Unknown;
        mHWBlendState = TriState_Unknown;
        mHWBlendSrcFactor = BlendInfo.FACTOR_UNKNOWN;
        mHWBlendDstFactor = BlendInfo.FACTOR_UNKNOWN;

        for (UniqueID[] textures : mHWTextureStates) {
            Arrays.fill(textures, null);
        }
        Arrays.fill(mHWSamplerStates, null);

        Arrays.fill(mActiveVertexBuffers, null);
    }

    @Override
    public boolean beginRenderPass(RenderPassDesc renderPassDesc,
                                   FramebufferDesc framebufferDesc,
                                   Rect2ic renderPassBounds,
                                   float[] clearColors,
                                   float clearDepth,
                                   int clearStencil) {
        mDevice.flushRenderCalls();
        if ((framebufferDesc.mFramebufferFlags & FramebufferDesc.FLAG_GL_WRAP_DEFAULT_FB) != 0) {
            mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, 0);
            mHWFramebuffer = RefCnt.move(mHWFramebuffer);
        } else {
            @SharedPtr
            GLFramebuffer framebuffer = mResourceProvider.findOrCreateFramebuffer(framebufferDesc);
            if (framebuffer == null) {
                return false;
            }
            if (mHWFramebuffer != framebuffer) {
                mDevice.getGL().glBindFramebuffer(GL_FRAMEBUFFER, framebuffer.getRenderFramebuffer());
                mHWFramebuffer = RefCnt.move(mHWFramebuffer, framebuffer);
            } else {
                framebuffer.unref();
            }
        }

        // disable scissor test at the beginning of RenderPass
        // ClearBuffer also respects it
        flushScissorTest(false);

        try (MemoryStack stack = mStack.push()) {
            var color = stack.mallocFloat(4);
            for (int i = 0; i < renderPassDesc.mNumColorAttachments; i++) {
                var attachmentDesc = renderPassDesc.mColorAttachments[i];
                boolean colorLoadClear = attachmentDesc.mLoadOp == Engine.LoadOp.kClear;
                if (colorLoadClear) {
                    flushColorWrite(true);
                    color.put(0, clearColors, i << 2, 4);
                    glClearBufferfv(GL_COLOR,
                            i,
                            color);
                }
            }
            boolean stencilLoadClear = renderPassDesc.mDepthStencilAttachment.mDesc != null &&
                    renderPassDesc.mDepthStencilAttachment.mLoadOp == Engine.LoadOp.kClear;
            if (stencilLoadClear) {
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
        Arrays.fill(mActiveVertexBuffers, null);

        // BlitFramebuffer respects the scissor, so disable it.
        flushScissorTest(false);

        GLFramebuffer framebuffer = mHWFramebuffer;
        if (framebuffer != null) {
            if (framebuffer.getRenderFramebuffer() != framebuffer.getResolveFramebuffer()) {
                // MSAA to single
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer.getResolveFramebuffer());
                var b = mContentBounds;
                glBlitFramebuffer(
                        b.mLeft, b.mTop, b.mRight, b.mBottom, // src rect
                        b.mLeft, b.mTop, b.mRight, b.mBottom, // dst rect
                        GL_COLOR_BUFFER_BIT, GL_NEAREST);
            }
        }

        RenderPassDesc renderPassDesc = mRenderPassDesc;
        try (MemoryStack stack = mStack.push()) {
            var attachmentsToDiscard = stack.mallocInt(renderPassDesc.mNumColorAttachments + 1);
            for (int i = 0; i < renderPassDesc.mNumColorAttachments; i++) {
                var attachmentDesc = renderPassDesc.mColorAttachments[i];
                boolean colorLoadClear = attachmentDesc.mStoreOp == Engine.StoreOp.kDiscard;
                if (colorLoadClear) {
                    attachmentsToDiscard.put(framebuffer == null
                            ? GL_COLOR
                            : GL_COLOR_ATTACHMENT0 + i);
                }
            }
            boolean stencilStoreDiscard = renderPassDesc.mDepthStencilAttachment.mDesc != null &&
                    renderPassDesc.mDepthStencilAttachment.mStoreOp == Engine.StoreOp.kDiscard;
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

        // We only track the Framebuffer within RenderPass, no need to track CommandBuffer usage
        mHWFramebuffer = RefCnt.move(framebuffer);
        mRenderPassDesc = null;
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

    @Override
    public boolean bindGraphicsPipeline(@RawPtr GraphicsPipeline graphicsPipeline) {
        Arrays.fill(mActiveVertexBuffers, null);

        mGraphicsPipeline = (GLGraphicsPipeline) graphicsPipeline;
        mPrimitiveType = switch (mGraphicsPipeline.getPrimitiveType()) {
            case Engine.PrimitiveType.PointList -> GL_POINTS;
            case Engine.PrimitiveType.LineList -> GL_LINES;
            case Engine.PrimitiveType.LineStrip -> GL_LINE_STRIP;
            case Engine.PrimitiveType.TriangleList -> GL_TRIANGLES;
            case Engine.PrimitiveType.TriangleStrip -> GL_TRIANGLE_STRIP;
            default -> throw new AssertionError();
        };

        GLProgram program = mGraphicsPipeline.getProgram();
        if (program == null) {
            return false;
        }
        if (mHWProgram != program) {
            // active program will not be deleted, so no collision
            mDevice.getGL().glUseProgram(program.getProgram());
            mHWProgram = program;
        }
        GLVertexArray vertexArray = mGraphicsPipeline.getVertexArray();
        assert vertexArray != null;
        if (mHWVertexArray != vertexArray) {
            // active vertex array will not be deleted, so no collision
            mDevice.getGL().glBindVertexArray(vertexArray.getHandle());
            mHWVertexArray = vertexArray;
        }

        BlendInfo blendInfo = mGraphicsPipeline.getBlendInfo();
        boolean blendOff = blendInfo.shouldDisableBlend() || !blendInfo.mColorWrite;
        if (blendOff) {
            if (mHWBlendState != TriState_Disabled) {
                mDevice.getGL().glDisable(GL_BLEND);
                mHWBlendState = TriState_Disabled;
            }
        } else {
            if (mHWBlendState != TriState_Enabled) {
                mDevice.getGL().glEnable(GL_BLEND);
                mHWBlendState = TriState_Enabled;
            }
        }
        if (mHWBlendSrcFactor != blendInfo.mSrcFactor ||
                mHWBlendDstFactor != blendInfo.mDstFactor) {
            mDevice.getGL().glBlendFunc(
                    GLUtil.getGLBlendFactor(blendInfo.mSrcFactor),
                    GLUtil.getGLBlendFactor(blendInfo.mDstFactor)
            );
            mHWBlendSrcFactor = blendInfo.mSrcFactor;
            mHWBlendDstFactor = blendInfo.mDstFactor;
        }
        flushColorWrite(blendInfo.mColorWrite);

        /*return mPipelineState.bindUniforms(mCmdBuffer, pipelineInfo,
                mRenderTarget.getWidth(), mRenderTarget.getHeight());*/
        return true;
    }

    @Override
    public void setViewport(int x, int y, int width, int height) {
        assert (width >= 0 && height >= 0);
        if (x != mHWViewportX || y != mHWViewportY ||
                width != mHWViewportWidth || height != mHWViewportHeight) {
            /*glViewportIndexedf(0, 0.0f, 0.0f, width, height);
            glDepthRangeIndexed(0, 0.0f, 1.0f);*/
            mDevice.getGL().glViewport(x, y, width, height);
            mHWViewportX = x;
            mHWViewportY = y;
            mHWViewportWidth = width;
            mHWViewportHeight = height;
        }
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        flushScissorTest(true);
        if (x != mHWScissorX || y != mHWScissorY ||
                width != mHWScissorWidth || height != mHWScissorHeight) {
            mDevice.getGL().glScissor(x, y, width, height);
            mHWScissorX = x;
            mHWScissorY = y;
            mHWScissorWidth = width;
            mHWScissorHeight = height;
        }
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

    @Override
    protected void begin() {
        mDevice.flushRenderCalls();
    }

    @Override
    protected boolean submit(QueueManager queueManager) {
        resetStates();
        mSubmitFence = mDevice.getGL().glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        // glFlush is required after fence creation
        glFlush();
        if (!mDevice.getCaps().skipErrorChecks()) {
            mDevice.clearErrors();
        }
        return true;
    }

    @Override
    protected boolean checkFinishedAndReset() {
        if (mSubmitFence == 0) {
            return true;
        }
        // faster than glGetSynciv
        int status = mDevice.getGL().glClientWaitSync(mSubmitFence, 0, 0);
        if (status == GL_CONDITION_SATISFIED ||
                status == GL_ALREADY_SIGNALED) {
            mDevice.getGL().glDeleteSync(mSubmitFence);

            callFinishedCallbacks(true);
            releaseResources();

            return true;
        }
        return false;
    }

    @Override
    protected void waitUntilFinished() {
        glFinish();
    }
}
