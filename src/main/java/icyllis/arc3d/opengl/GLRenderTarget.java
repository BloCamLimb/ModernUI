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
import icyllis.arc3d.engine.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

import static org.lwjgl.opengl.GL30C.*;

/**
 * Represents OpenGL framebuffers.
 */
public final class GLRenderTarget extends GpuRenderTarget {

    // the color buffers, raw ptr
    // null for wrapped render targets
    @SharedPtr
    private GLTexture[] mColorAttachments;
    // the resolve buffers, raw ptr
    // null for wrapped/single-sampled/non-resolvable render targets
    @SharedPtr
    private GLTexture[] mResolveAttachments;

    @SharedPtr
    private GLTexture mDepthStencilAttachment;

    private int mRenderFramebuffer;
    private int mResolveFramebuffer;

    // if we need bind stencil buffers on next framebuffer bind call
    private boolean mRebindStencilBuffer;

    // should we delete framebuffers ourselves?
    private final boolean mOwnership;

    private BackendFormat mBackendFormat;
    private BackendRenderTarget mBackendRenderTarget;

    // Constructor for instances created by our engine. (has texture access)
    GLRenderTarget(Context context,
                   int width, int height,
                   int sampleCount,
                   int renderFramebuffer,
                   int resolveFramebuffer,
                   int numColorTargets,
                   GLTexture[] colorAttachments,
                   GLTexture[] resolveAttachments,
                   GLTexture depthStencilAttachment,
                   int surfaceFlags) {
        super(context, width, height, sampleCount, numColorTargets);
        assert (sampleCount > 0);
        mRenderFramebuffer = renderFramebuffer;
        mResolveFramebuffer = resolveFramebuffer;
        mOwnership = true;
        mColorAttachments = colorAttachments;
        mResolveAttachments = resolveAttachments;
        mDepthStencilAttachment = depthStencilAttachment;
        // color/resolve attachments may have different formats,
        // but we don't care about that
        GLTexture primaryAtt = (GLTexture) asImage();
        /*mBackendFormat = primaryAtt != null
                ? primaryAtt.getBackendFormat()
                : GLBackendFormat.make(GL_NONE);*/
        mSurfaceFlags |= surfaceFlags;
        //registerWithCache((surfaceFlags & ISurface.FLAG_BUDGETED) != 0);
    }

    // Constructor for instances wrapping backend objects. (no texture access)
    private GLRenderTarget(Context context,
                           int width, int height,
                           BackendFormat format,
                           int sampleCount,
                           int framebuffer,
                           boolean ownership,
                           @SharedPtr GLTexture depthStencilAttachment) {
        super(context, width, height, sampleCount, 1);
        assert (sampleCount > 0);
        assert (framebuffer != 0 || !ownership);
        mRenderFramebuffer = framebuffer;
        mResolveFramebuffer = 0;
        mBackendFormat = format;
        mOwnership = ownership;
        mDepthStencilAttachment = depthStencilAttachment;
        if (framebuffer == 0) {
            mSurfaceFlags |= FramebufferDesc.FLAG_GL_WRAP_DEFAULT_FB;
        }
        //registerWithCacheWrapped(false);
    }

    /**
     * Make a {@link GLRenderTarget} that wraps existing framebuffers without
     * accessing their backing buffers (texture and stencil).
     *
     * @param width  the effective width of framebuffer
     * @param height the effective height of framebuffer
     */
    @NonNull
    @SharedPtr
    public static GLRenderTarget makeWrapped(Context context,
                                             int width, int height,
                                             BackendFormat format,
                                             int sampleCount,
                                             int framebuffer,
                                             int depthBits,
                                             int stencilBits,
                                             boolean ownership) {
        assert (sampleCount > 0);
        assert (framebuffer != 0 || !ownership);
        GLTexture depthStencilAtt = null;
        if (depthBits > 0 || stencilBits > 0) {
            // We pick a "fake" actual format that matches the number of stencil bits. When wrapping
            // an FBO with some number of stencil bits all we care about in the future is that we have
            // a format with the same number of stencil bits. We don't even directly use the format or
            // any other properties. Thus, it is fine for us to just assign an arbitrary format that
            // matches the stencil bit count.
            int depthStencilFormat = 0;
            if (stencilBits == 16) {
                depthStencilFormat = GL_STENCIL_INDEX16;
            } else if (stencilBits == 8) {
                switch (depthBits) {
                    case 24 -> depthStencilFormat = GL_DEPTH24_STENCIL8;
                    case 32 -> depthStencilFormat = GL_DEPTH32F_STENCIL8;
                }
            } else if (stencilBits == 0) {
                switch (depthBits) {
                    case 16 -> depthStencilFormat = GL_DEPTH_COMPONENT16;
                    case 24 -> depthStencilFormat = GL_DEPTH_COMPONENT24;
                    case 32 -> depthStencilFormat = GL_DEPTH_COMPONENT32F;
                }
            }

            // We don't have the actual renderbufferID, but we need to make an attachment for the stencil,
            // so we just set it to an invalid value of 0 to make sure we don't explicitly use it or try
            // and delete it.
            /*depthStencilAtt = GLTexture.makeWrappedRenderbuffer(device,
                    width, height,
                    sampleCount,
                    depthStencilFormat,
                    0);*/
        }
        return new GLRenderTarget(context,
                width, height,
                format,
                sampleCount,
                framebuffer,
                ownership,
                depthStencilAtt);
    }

    public int getRenderFramebuffer() {
        return mRenderFramebuffer;
    }

    public int getResolveFramebuffer() {
        return mResolveFramebuffer;
    }

    /**
     * Make sure the stencil attachment is valid. Even though a color buffer op doesn't use stencil,
     * our FBO still needs to be "framebuffer complete". If this render target is already bound,
     * this method ensures the stencil attachment is valid (attached or detached), if stencil buffers
     * reset right before.
     */
    public void bindStencil() {
        if (!mRebindStencilBuffer) {
            return;
        }
        /*int framebuffer = mSampleFramebuffer;
        GLAttachment stencilBuffer = (GLAttachment) mStencilBuffer;
        if (stencilBuffer != null) {
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    stencilBuffer.getRenderbufferID());
            if (GLUtil.glFormatIsPackedDepthStencil(stencilBuffer.getFormat())) {
                glNamedFramebufferRenderbuffer(framebuffer,
                        GL_DEPTH_ATTACHMENT,
                        GL_RENDERBUFFER,
                        stencilBuffer.getRenderbufferID());
            } else {
                glNamedFramebufferRenderbuffer(framebuffer,
                        GL_DEPTH_ATTACHMENT,
                        GL_RENDERBUFFER,
                        0);
            }
        } else {
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    0);
            glNamedFramebufferRenderbuffer(framebuffer,
                    GL_DEPTH_ATTACHMENT,
                    GL_RENDERBUFFER,
                    0);
        }*/
        mRebindStencilBuffer = false;
    }

    @NonNull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @RawPtr
    @Nullable
    @Override
    public GLTexture getColorAttachment() {
        return mColorAttachments != null ? mColorAttachments[0] : null;
    }

    @RawPtr
    @Nullable
    @Override
    public GLTexture getColorAttachment(int index) {
        return mColorAttachments != null ? mColorAttachments[index] : null;
    }

    @RawPtr
    @Override
    public GLTexture @Nullable[] getColorAttachments() {
        return mColorAttachments;
    }

    @RawPtr
    @Nullable
    @Override
    public GLTexture getResolveAttachment() {
        return mResolveAttachments != null ? mResolveAttachments[0] : null;
    }

    @RawPtr
    @Nullable
    @Override
    public GLTexture getResolveAttachment(int index) {
        return mResolveAttachments != null ? mResolveAttachments[index] : null;
    }

    @RawPtr
    @Override
    public GLTexture @Nullable[] getResolveAttachments() {
        return mResolveAttachments;
    }

    @RawPtr
    @Nullable
    @Override
    public GLTexture getDepthStencilAttachment() {
        return mDepthStencilAttachment;
    }

    @Override
    public int getDepthBits() {
        return mDepthStencilAttachment != null ? mDepthStencilAttachment.getDepthBits() : 0;
    }

    @Override
    public int getStencilBits() {
        return mDepthStencilAttachment != null ? mDepthStencilAttachment.getStencilBits() : 0;
    }

    @NonNull
    @Override
    public BackendRenderTarget getBackendRenderTarget() {
        if (mBackendRenderTarget == null) {
            final GLFramebufferInfo info = new GLFramebufferInfo();
            info.mFramebuffer = mRenderFramebuffer;
            mBackendRenderTarget = new GLBackendRenderTarget(
                    getWidth(), getHeight(), getSampleCount(), getDepthBits(), getStencilBits(), info);
        }
        return mBackendRenderTarget;
    }

    @Override
    protected boolean canAttachStencil() {
        // Only modify the framebuffer attachments if we have created it.
        // Public APIs do not currently allow for wrap-only ownership,
        // so we can safely assume that if an object is owner, we created it.
        return mOwnership;
    }

    /*@Override
    protected void attachStencilBuffer(@SharedPtr Attachment stencilBuffer) {
        if (stencilBuffer == null && mStencilBuffer == null) {
            // No need to do any work since we currently don't have a stencil attachment,
            // and we're not actually adding one.
            return;
        }

        // We defer attaching the new stencil buffer until the next time our framebuffer is bound.
        if (mStencilBuffer != stencilBuffer) {
            mRebindStencilBuffer = true;
        }

        mStencilBuffer = GpuResourceBase.move(mStencilBuffer, stencilBuffer);
    }*/

    @Override
    protected void onRelease() {
        if (mOwnership) {
            if (mRenderFramebuffer != 0) {
                getDevice().getGL().glDeleteFramebuffers(mRenderFramebuffer);
            }
            if (mRenderFramebuffer != mResolveFramebuffer) {
                assert (mResolveFramebuffer != 0);
                getDevice().getGL().glDeleteFramebuffers(mResolveFramebuffer);
            }
        }
        clearAttachments();
        mRenderFramebuffer = 0;
        mResolveFramebuffer = 0;
    }

    private void clearAttachments() {
        if (mColorAttachments != null) {
            for (int i = 0; i < mColorAttachments.length; i++) {
                mColorAttachments[i] = RefCnt.move(mColorAttachments[i]);
            }
        }
        if (mResolveAttachments != null) {
            for (int i = 0; i < mResolveAttachments.length; i++) {
                mResolveAttachments[i] = RefCnt.move(mResolveAttachments[i]);
            }
        }
        mDepthStencilAttachment = RefCnt.move(mDepthStencilAttachment);
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }

    @Override
    public String toString() {
        return "GLFramebuffer{" +
                "mRenderFramebuffer=" + mRenderFramebuffer +
                ", mResolveFramebuffer=" + mResolveFramebuffer +
                ", mColorAttachments=" + Arrays.toString(mColorAttachments) +
                ", mResolveAttachments=" + Arrays.toString(mResolveAttachments) +
                ", mDepthStencilAttachment=" + mDepthStencilAttachment +
                ", mSampleCount=" + getSampleCount() +
                ", mOwnership=" + mOwnership +
                ", mBackendFormat=" + mBackendFormat +
                '}';
    }
}
