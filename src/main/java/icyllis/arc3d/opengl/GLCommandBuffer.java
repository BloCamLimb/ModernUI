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
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.Objects;

import static org.lwjgl.opengl.GL33C.*;

/**
 * The OpenGL command buffer. The commands executed on {@link GLCommandBuffer} are
 * mostly the same as that on {@link GLDevice}, but {@link GLCommandBuffer} assumes some values
 * and will not handle dirty context.
 */
public final class GLCommandBuffer extends CommandBuffer {

    private static final int
            kDisabled_TriState = 0,
            kEnabled_TriState = 1,
            kUnknown_TriState = 2;

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

    private /*TriState*/ int mHWScissorTest;
    private /*TriState*/ int mHWColorWrite;
    private /*TriState*/ int mHWBlendState;
    /**
     * @see BlendInfo#EQUATION_ADD
     */
    private byte mHWBlendEquation;
    /**
     * @see BlendInfo#FACTOR_ZERO
     */
    private byte mHWBlendSrcFactor;
    private byte mHWBlendDstFactor;

    private /*TriState*/ int mHWDepthTest;
    private /*TriState*/ int mHWDepthWrite;
    /**
     * @see DepthStencilSettings#COMPARE_OP_NEVER
     */
    private byte mHWDepthCompareOp;

    private /*TriState*/ int mHWStencilTest;
    private DepthStencilSettings.Face mHWFrontStencil;
    private DepthStencilSettings.Face mHWBackStencil;

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

    // current pipeline state
    @RawPtr
    private GLGraphicsPipeline mGraphicsPipeline;

    private int mPrimitiveType;

    private int mIndexType;
    private long mIndexBufferOffset;

    @RawPtr
    private final GLBuffer[] mActiveVertexBuffers = new GLBuffer[Caps.MAX_VERTEX_BINDINGS];
    private final long[] mActiveVertexOffsets = new long[Caps.MAX_VERTEX_BINDINGS];

    // current render pass state
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

        mHWScissorTest = kUnknown_TriState;
        mHWScissorX = -1;
        mHWScissorY = -1;
        mHWScissorWidth = -1;
        mHWScissorHeight = -1;
        mHWViewportX = -1;
        mHWViewportY = -1;
        mHWViewportWidth = -1;
        mHWViewportHeight = -1;

        mHWColorWrite = kUnknown_TriState;
        mHWBlendState = kUnknown_TriState;
        mHWBlendEquation = BlendInfo.EQUATION_UNKNOWN;
        mHWBlendSrcFactor = BlendInfo.FACTOR_UNKNOWN;
        mHWBlendDstFactor = BlendInfo.FACTOR_UNKNOWN;
        mHWDepthTest = kUnknown_TriState;
        mHWDepthWrite = kUnknown_TriState;
        mHWDepthCompareOp = -1;
        mHWStencilTest = kUnknown_TriState;
        mHWFrontStencil = null;
        mHWBackStencil = null;

        mHWActiveTextureUnit = -1;
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
            GLFramebuffer framebuffer = mDevice.findOrCreateFramebuffer(framebufferDesc);
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
            var floatValues = stack.mallocFloat(4);
            for (int i = 0; i < renderPassDesc.mNumColorAttachments; i++) {
                var attachmentDesc = renderPassDesc.mColorAttachments[i];
                boolean colorLoadClear = attachmentDesc.mLoadOp == Engine.LoadOp.kClear;
                if (colorLoadClear) {
                    flushColorWrite(true);
                    floatValues.put(0, clearColors, i << 2, 4);
                    mDevice.getGL().glClearBufferfv(GL_COLOR,
                            i,  // draw buffer
                            floatValues);
                }
            }
            boolean depthStencilLoadClear = renderPassDesc.mDepthStencilAttachment.mDesc != null &&
                    renderPassDesc.mDepthStencilAttachment.mLoadOp == Engine.LoadOp.kClear;
            if (depthStencilLoadClear) {
                boolean hasDepth = renderPassDesc.mDepthStencilAttachment.mDesc.getDepthBits() > 0;
                boolean hasStencil = renderPassDesc.mDepthStencilAttachment.mDesc.getStencilBits() > 0;
                if (hasDepth) {
                    flushDepthWrite(true);
                }
                if (hasStencil) {
                    mDevice.getGL().glStencilMask(0xFFFFFFFF); // stencil will be flushed later
                    mHWFrontStencil = null;
                    mHWBackStencil = null;
                }
                if (hasDepth && hasStencil) {
                    mDevice.getGL().glClearBufferfi(GL_DEPTH_STENCIL,
                            0,
                            clearDepth, clearStencil);
                } else if (hasDepth) {
                    floatValues.put(0, clearDepth)
                            .limit(1);
                    mDevice.getGL().glClearBufferfv(GL_DEPTH,
                            0,
                            floatValues);
                } else if (hasStencil) {
                    var intValues = stack.ints(clearStencil);
                    mDevice.getGL().glClearBufferiv(GL_STENCIL,
                            0,
                            intValues);
                }
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
                mDevice.getGL().glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer.getResolveFramebuffer());
                var b = mContentBounds;
                mDevice.getGL().glBlitFramebuffer(
                        b.mLeft, b.mTop, b.mRight, b.mBottom, // src rect
                        b.mLeft, b.mTop, b.mRight, b.mBottom, // dst rect
                        GL_COLOR_BUFFER_BIT, GL_NEAREST);
            }
        }

        if (mDevice.getCaps().hasInvalidateFramebufferSupport()) {
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
                boolean depthStencilStoreDiscard = renderPassDesc.mDepthStencilAttachment.mDesc != null &&
                        renderPassDesc.mDepthStencilAttachment.mStoreOp == Engine.StoreOp.kDiscard;
                if (depthStencilStoreDiscard) {
                    boolean hasDepth = renderPassDesc.mDepthStencilAttachment.mDesc.getDepthBits() > 0;
                    boolean hasStencil = renderPassDesc.mDepthStencilAttachment.mDesc.getStencilBits() > 0;
                    if (hasDepth && hasStencil) {
                        attachmentsToDiscard.put(framebuffer == null
                                ? GL_DEPTH_STENCIL
                                : GL_DEPTH_STENCIL_ATTACHMENT);
                    } else if (hasDepth) {
                        attachmentsToDiscard.put(framebuffer == null
                                ? GL_DEPTH
                                : GL_DEPTH_ATTACHMENT);
                    } else if (hasStencil) {
                        attachmentsToDiscard.put(framebuffer == null
                                ? GL_STENCIL
                                : GL_STENCIL_ATTACHMENT);
                    }
                }
                attachmentsToDiscard.flip();
                if (attachmentsToDiscard.hasRemaining()) {
                    //TODO try to use invalidateSubFramebuffer
                    mDevice.getGL().glInvalidateFramebuffer(GL_READ_FRAMEBUFFER, attachmentsToDiscard);
                }
            }
        }

        // We only track the Framebuffer within RenderPass, no need to track CommandBuffer usage
        mHWFramebuffer = RefCnt.move(framebuffer);
        mRenderPassDesc = null;
    }

    @Override
    protected boolean onCopyBuffer(@RawPtr Buffer srcBuffer,
                                   @RawPtr Buffer dstBuffer,
                                   long srcOffset,
                                   long dstOffset,
                                   long size) {
        assert !srcBuffer.isMapped();
        assert !dstBuffer.isMapped();
        GLBuffer glSrc = (GLBuffer) srcBuffer;
        GLBuffer glDst = (GLBuffer) dstBuffer;
        long clientBufferPtr = glSrc.getClientUploadBuffer();
        if (glSrc.getHandle() == 0 &&
                clientBufferPtr == MemoryUtil.NULL) {
            // lazy initialization failed
            return false;
        }
        if (glDst.getHandle() == 0 ||
                glDst.getClientUploadBuffer() != MemoryUtil.NULL) {
            // lazy initialization failed
            return false;
        }
        assert glSrc.getHandle() == 0 || clientBufferPtr == MemoryUtil.NULL;

        if (clientBufferPtr != MemoryUtil.NULL) {
            if (mDevice.getCaps().hasDSASupport()) {
                mDevice.getGL().glNamedBufferSubData(
                        glDst.getHandle(),
                        dstOffset,
                        size,
                        clientBufferPtr + srcOffset
                );
            } else {
                int target = glDst.getTarget();
                mDevice.getGL().glBindBuffer(target, glDst.getHandle());
                mDevice.getGL().glBufferSubData(
                        target,
                        dstOffset,
                        size,
                        clientBufferPtr + srcOffset
                );
            }
        } else if (mDevice.getCaps().hasDSASupport()) {
            mDevice.getGL().glCopyNamedBufferSubData(
                    glSrc.getHandle(),
                    glDst.getHandle(),
                    srcOffset,
                    dstOffset,
                    size
            );
        } else {
            mDevice.getGL().glBindBuffer(GL_COPY_READ_BUFFER, glSrc.getHandle());
            mDevice.getGL().glBindBuffer(GL_COPY_WRITE_BUFFER, glDst.getHandle());
            mDevice.getGL().glCopyBufferSubData(
                    GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER,
                    srcOffset, dstOffset,
                    size
            );
        }
        return true;
    }

    @Override
    protected boolean onCopyBufferToImage(@RawPtr Buffer srcBuffer,
                                          @RawPtr Image dstImage,
                                          int srcColorType,
                                          int dstColorType,
                                          BufferImageCopyData[] copyData) {
        GLBuffer glBuffer = (GLBuffer) srcBuffer;
        GLTexture glTexture = (GLTexture) dstImage;

        long clientBufferPtr = glBuffer.getClientUploadBuffer();
        if (glBuffer.getHandle() == 0 &&
                clientBufferPtr == MemoryUtil.NULL) {
            // lazy initialization failed
            return false;
        }
        // there's either PBO or client array
        assert glBuffer.getHandle() == 0 || clientBufferPtr == MemoryUtil.NULL;

        int glFormat = glTexture.getFormat();
        int srcFormat = mDevice.getCaps().getPixelsExternalFormat(
                glFormat, dstColorType, srcColorType, /*write*/true
        );
        if (srcFormat == 0) {
            return false;
        }
        int srcType = mDevice.getCaps().getPixelsExternalType(
                glFormat, dstColorType, srcColorType
        );
        if (srcType == 0) {
            return false;
        }

        GLInterface gl = mDevice.getGL();
        boolean dsa = mDevice.getCaps().hasDSASupport();
        int target = glTexture.getTarget();
        int handle = glTexture.getHandle();
        int boundTexture = 0;
        //TODO not only 2D
        if (!dsa) {
            boundTexture = gl.glGetInteger(GL_TEXTURE_BINDING_2D);
            if (handle != boundTexture) {
                gl.glBindTexture(target, handle);
            }
        }

        GLTextureMutableState mutableState = glTexture.getGLMutableState();
        if (mutableState.mBaseMipmapLevel != 0) {
            if (dsa) {
                gl.glTextureParameteri(handle, GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                gl.glTexParameteri(target, GL_TEXTURE_BASE_LEVEL, 0);
            }
            mutableState.mBaseMipmapLevel = 0;
        }
        int maxLevel = glTexture.getMipLevelCount() - 1; // minus base level
        if (mutableState.mMaxMipmapLevel != maxLevel) {
            if (dsa) {
                gl.glTextureParameteri(handle, GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                gl.glTexParameteri(target, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            // Bug fixed by Arc3D
            mutableState.mMaxMipmapLevel = maxLevel;
        }

        gl.glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        gl.glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        int bpp = ColorInfo.bytesPerPixel(srcColorType);
        assert (glBuffer.getUsage() & Engine.BufferUsageFlags.kUpload) != 0;
        gl.glBindBuffer(GL_PIXEL_UNPACK_BUFFER, glBuffer.getHandle());

        for (var data : copyData) {

            long trimRowBytes = (long) data.mWidth * bpp;
            if (data.mBufferRowBytes != trimRowBytes) {
                int rowLength = (int) (data.mBufferRowBytes / bpp);
                gl.glPixelStorei(GL_UNPACK_ROW_LENGTH, rowLength);
            } else {
                gl.glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            }

            if (dsa) {
                gl.glTextureSubImage2D(handle, data.mMipLevel,
                        data.mX, data.mY, data.mWidth, data.mHeight,
                        srcFormat, srcType,
                        clientBufferPtr + data.mBufferOffset);
            } else {
                gl.glTexSubImage2D(target, data.mMipLevel,
                        data.mX, data.mY, data.mWidth, data.mHeight,
                        srcFormat, srcType,
                        clientBufferPtr + data.mBufferOffset);
            }
        }

        gl.glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        if (!dsa) {
            if (handle != boundTexture) {
                gl.glBindTexture(target, boundTexture);
            }
        }

        return true;
    }

    @Override
    protected boolean onCopyImage(@RawPtr Image srcImage,
                                  int srcL, int srcT, int srcR, int srcB,
                                  @RawPtr Image dstImage,
                                  int dstX, int dstY,
                                  int mipLevel) {
        GLImage glSrc = (GLImage) srcImage;
        GLImage glDst = (GLImage) dstImage;
        return mDevice.copyImage(
                glSrc,
                srcL, srcT, srcR, srcB,
                glDst,
                dstX, dstY, mipLevel
        );
    }

    /**
     * Flush scissor test.
     *
     * @param enable whether to enable scissor test
     */
    public void flushScissorTest(boolean enable) {
        if (enable) {
            if (mHWScissorTest != kEnabled_TriState) {
                mDevice.getGL().glEnable(GL_SCISSOR_TEST);
                mHWScissorTest = kEnabled_TriState;
            }
        } else {
            if (mHWScissorTest != kDisabled_TriState) {
                mDevice.getGL().glDisable(GL_SCISSOR_TEST);
                mHWScissorTest = kDisabled_TriState;
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
            if (mHWColorWrite != kEnabled_TriState) {
                mDevice.getGL().glColorMask(true, true, true, true);
                mHWColorWrite = kEnabled_TriState;
            }
        } else {
            if (mHWColorWrite != kDisabled_TriState) {
                mDevice.getGL().glColorMask(false, false, false, false);
                mHWColorWrite = kDisabled_TriState;
            }
        }
    }

    public void flushDepthWrite(boolean enable) {
        if (enable) {
            if (mHWDepthWrite != kEnabled_TriState) {
                mDevice.getGL().glDepthMask(true);
                mHWDepthWrite = kEnabled_TriState;
            }
        } else {
            if (mHWDepthWrite != kDisabled_TriState) {
                mDevice.getGL().glDepthMask(false);
                mHWDepthWrite = kDisabled_TriState;
            }
        }
    }

    @Override
    public boolean bindGraphicsPipeline(@RawPtr GraphicsPipeline graphicsPipeline) {
        Arrays.fill(mActiveVertexBuffers, null);

        mGraphicsPipeline = (GLGraphicsPipeline) graphicsPipeline;

        @RawPtr
        GLProgram program = mGraphicsPipeline.getProgram();
        if (program == null) {
            return false;
        }
        if (mHWProgram != program) {
            // active program will not be deleted, so no collision
            mDevice.getGL().glUseProgram(program.getProgram());
            mHWProgram = program;
        }
        @RawPtr
        GLVertexArray vertexArray = mGraphicsPipeline.getVertexArray();
        assert vertexArray != null;
        if (mHWVertexArray != vertexArray) {
            // active vertex array will not be deleted, so no collision
            mDevice.getGL().glBindVertexArray(vertexArray.getHandle());
            mHWVertexArray = vertexArray;
        }
        mPrimitiveType = GLUtil.toGLPrimitiveType(mGraphicsPipeline.getPrimitiveType());

        BlendInfo blendInfo = mGraphicsPipeline.getBlendInfo();
        boolean blendOff = blendInfo.blendShouldDisable() || !blendInfo.mColorWrite;
        if (blendOff) {
            if (mHWBlendState != kDisabled_TriState) {
                mDevice.getGL().glDisable(GL_BLEND);
                mHWBlendState = kDisabled_TriState;
            }
        } else {
            if (mHWBlendState != kEnabled_TriState) {
                mDevice.getGL().glEnable(GL_BLEND);
                mHWBlendState = kEnabled_TriState;
            }
        }
        if (mHWBlendEquation != blendInfo.mEquation) {
            mDevice.getGL().glBlendEquation(
                    GLUtil.toGLBlendEquation(blendInfo.mEquation)
            );
            mHWBlendEquation = blendInfo.mEquation;
        }
        if (mHWBlendSrcFactor != blendInfo.mSrcFactor ||
                mHWBlendDstFactor != blendInfo.mDstFactor) {
            mDevice.getGL().glBlendFunc(
                    GLUtil.toGLBlendFactor(blendInfo.mSrcFactor),
                    GLUtil.toGLBlendFactor(blendInfo.mDstFactor)
            );
            mHWBlendSrcFactor = blendInfo.mSrcFactor;
            mHWBlendDstFactor = blendInfo.mDstFactor;
        }
        flushColorWrite(blendInfo.mColorWrite);

        DepthStencilSettings ds = mGraphicsPipeline.getDepthStencilSettings();
        if (ds.mDepthTest) {
            if (mHWDepthTest != kEnabled_TriState) {
                mDevice.getGL().glEnable(GL_DEPTH_TEST);
                mHWDepthTest = kEnabled_TriState;
            }
        } else {
            if (mHWDepthTest != kDisabled_TriState) {
                mDevice.getGL().glDisable(GL_DEPTH_TEST);
                mHWDepthTest = kDisabled_TriState;
            }
        }
        flushDepthWrite(ds.mDepthWrite);
        if (ds.mDepthCompareOp != mHWDepthCompareOp) {
            mDevice.getGL().glDepthFunc(
                    GLUtil.toGLCompareFunc(ds.mDepthCompareOp)
            );
            mHWDepthCompareOp = ds.mDepthCompareOp;
        }
        if (ds.mStencilTest) {
            if (mHWStencilTest != kEnabled_TriState) {
                mDevice.getGL().glEnable(GL_STENCIL_TEST);
                mHWStencilTest = kEnabled_TriState;
            }
            if (!Objects.equals(mHWFrontStencil, ds.mFrontFace) ||
                    !Objects.equals(mHWBackStencil, ds.mBackFace)) {
                //TODO note that we assume LowerLeft origin here
                if (ds.isTwoSided()) {
                    setup_gl_stencil_state(mDevice.getGL(),
                            ds.mFrontFace, GL_FRONT);
                    setup_gl_stencil_state(mDevice.getGL(),
                            ds.mBackFace, GL_BACK);
                } else {
                    setup_gl_stencil_state(mDevice.getGL(),
                            ds.mFrontFace, GL_FRONT_AND_BACK);
                }
                mHWFrontStencil = ds.mFrontFace;
                mHWBackStencil = ds.mBackFace;
            }
        } else {
            if (mHWStencilTest != kDisabled_TriState) {
                mDevice.getGL().glDisable(GL_STENCIL_TEST);
                mHWStencilTest = kDisabled_TriState;
            }
        }

        /*return mPipelineState.bindUniforms(mCmdBuffer, pipelineInfo,
                mRenderTarget.getWidth(), mRenderTarget.getHeight());*/
        return true;
    }

    private static void setup_gl_stencil_state(GLInterface gl,
                                               DepthStencilSettings.Face face,
                                               int glFace) {
        int glFailOp = GLUtil.toGLStencilOp(face.mFailOp);
        int glPassOp = GLUtil.toGLStencilOp(face.mPassOp);
        int glDepthFailOp = GLUtil.toGLStencilOp(face.mDepthFailOp);
        int glFunc = GLUtil.toGLCompareFunc(face.mCompareOp);

        if (glFace == GL_FRONT_AND_BACK) {
            gl.glStencilOp(glFailOp, glDepthFailOp, glPassOp);
            gl.glStencilFunc(glFunc, face.mReference, face.mCompareMask);
            gl.glStencilMask(face.mWriteMask);
        } else {
            gl.glStencilOpSeparate(glFace, glFailOp, glDepthFailOp, glPassOp);
            gl.glStencilFuncSeparate(glFace, glFunc, face.mReference, face.mCompareMask);
            gl.glStencilMaskSeparate(glFace, face.mWriteMask);
        }
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
                                   @RawPtr Sampler sampler, short swizzle) {
        assert (texture != null && texture.isSampledImage());
        GLTexture glTexture = (GLTexture) texture;
        GLSampler glSampler = (GLSampler) sampler;
        boolean dsa = mDevice.getCaps().hasDSASupport();
        int target = glTexture.getTarget();
        int handle = glTexture.getHandle();
        int imageType = glTexture.getImageType();
        if (mHWTextureStates[binding][imageType] != glTexture.getUniqueID()) {
            if (dsa) {
                mDevice.getGL().glBindTextureUnit(binding, handle);
            } else {
                setTextureUnit(binding);
                mDevice.getGL().glBindTexture(target, handle);
            }
            mHWTextureStates[binding][imageType] = glTexture.getUniqueID();
        }
        if (mHWSamplerStates[binding] != glSampler.getUniqueID()) {
            mDevice.getGL().glBindSampler(binding, glSampler.getHandle());
            mHWSamplerStates[binding] = glSampler.getUniqueID();
        }
        GLTextureMutableState mutableState = glTexture.getGLMutableState();
        if (mutableState.mBaseMipmapLevel != 0) {
            if (dsa) {
                mDevice.getGL().glTextureParameteri(handle, GL_TEXTURE_BASE_LEVEL, 0);
            } else {
                mDevice.getGL().glTexParameteri(target, GL_TEXTURE_BASE_LEVEL, 0);
            }
            mutableState.mBaseMipmapLevel = 0;
        }
        int maxLevel = glTexture.getMipLevelCount() - 1; // minus base level
        if (mutableState.mMaxMipmapLevel != maxLevel) {
            if (dsa) {
                mDevice.getGL().glTextureParameteri(handle, GL_TEXTURE_MAX_LEVEL, maxLevel);
            } else {
                mDevice.getGL().glTexParameteri(target, GL_TEXTURE_MAX_LEVEL, maxLevel);
            }
            mutableState.mMaxMipmapLevel = maxLevel;
        }
        if (mutableState.mSwizzle != swizzle) {
            // OpenGL ES does not support GL_TEXTURE_SWIZZLE_RGBA
            for (int i = 0; i < 4; ++i) {
                int swiz = switch ((swizzle >> (i << 2)) & 0xF) {
                    case Swizzle.COMPONENT_R    -> GL_RED;
                    case Swizzle.COMPONENT_G    -> GL_GREEN;
                    case Swizzle.COMPONENT_B    -> GL_BLUE;
                    case Swizzle.COMPONENT_A    -> GL_ALPHA;
                    case Swizzle.COMPONENT_ZERO -> GL_ZERO;
                    case Swizzle.COMPONENT_ONE  -> GL_ONE;
                    default -> throw new AssertionError(swizzle);
                };
                // swizzle enums are sequential
                int channel = GL_TEXTURE_SWIZZLE_R + i;
                if (dsa) {
                    mDevice.getGL().glTextureParameteri(handle, channel, swiz);
                } else {
                    mDevice.getGL().glTexParameteri(target, channel, swiz);
                }
            }
            mutableState.mSwizzle = swizzle;
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
        mDevice.purgeStaleResources();
        mDevice.flushRenderCalls();

        var gl = mDevice.getGL();
        // common raster state
        gl.glDisable(GL_LINE_SMOOTH);
        gl.glDisable(GL_POLYGON_SMOOTH);

        gl.glDisable(GL_DITHER);
        gl.glEnable(GL_MULTISAMPLE);

        // common blend state
        gl.glDisable(GL_COLOR_LOGIC_OP);

        gl.glDisable(GL_POLYGON_OFFSET_FILL);

        // We don't use face culling.
        gl.glDisable(GL_CULL_FACE);
        // We do use separate stencil. Our algorithms don't care which face is front vs. back so
        // just set this to the default for self-consistency.
        gl.glFrontFace(GL_CCW);

        // we only ever use lines in hairline mode
        gl.glLineWidth(1);
        gl.glDisable(GL_PROGRAM_POINT_SIZE);
    }

    @Override
    protected boolean submit(QueueManager queueManager) {
        mDevice.purgeStaleResources();
        resetStates();
        mSubmitFence = mDevice.getGL().glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        // glFlush is required after fence creation
        mDevice.getGL().glFlush();
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
        int status = mDevice.getGL().glClientWaitSync(mSubmitFence, 0, 0L);
        if (status == GL_CONDITION_SATISFIED ||
                status == GL_ALREADY_SIGNALED) {
            mDevice.getGL().glDeleteSync(mSubmitFence);
            mSubmitFence = 0;

            callFinishedCallbacks(true);
            releaseResources();

            return true;
        }
        return false;
    }

    @Override
    protected void waitUntilFinished() {
        mDevice.getGL().glFinish();
    }
}
