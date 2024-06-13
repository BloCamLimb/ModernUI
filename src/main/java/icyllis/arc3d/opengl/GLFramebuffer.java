/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

import static org.lwjgl.opengl.GL11C.GL_NONE;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32C.*;

public final class GLFramebuffer extends Resource {

    private int mRenderFramebuffer;
    private int mResolveFramebuffer;

    private GLFramebuffer(Context context,
                          int renderFramebuffer,
                          int resolveFramebuffer) {
        super(context, true, false, 0);
        mRenderFramebuffer = renderFramebuffer;
        mResolveFramebuffer = resolveFramebuffer;
    }

    private static void attachColorAttachment(int index, GLImage image, int mipLevel) {
        if (image instanceof GLRenderbuffer renderbuffer) {
            glFramebufferRenderbuffer(GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0 + index,
                    GL_RENDERBUFFER,
                    renderbuffer.getHandle());
        } else {
            GLTexture texture = (GLTexture) image;
            switch (texture.getTarget()) {
                case GL_TEXTURE_2D, GL_TEXTURE_2D_MULTISAMPLE -> {
                    glFramebufferTexture(GL_FRAMEBUFFER,
                            GL_COLOR_ATTACHMENT0 + index,
                            texture.getHandle(),
                            mipLevel);
                }
                default -> throw new UnsupportedOperationException();
            }
        }
    }

    @Nullable
    @SharedPtr
    public static GLFramebuffer make(Context context,
                                     FramebufferDesc desc) {
        GLDevice device = (GLDevice) context.getDevice();

        assert device.isOnExecutingThread();

        var gl = device.getGL();
        // There's an NVIDIA driver bug that creating framebuffer via DSA with attachments of
        // different dimensions will report GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT.
        // The workaround is to use traditional glGen* and glBind* (validate).
        // see https://forums.developer.nvidia.com/t/framebuffer-incomplete-when-attaching-color-buffers-of-different-sizes-with-dsa/211550
        final int renderFramebuffer = gl.glGenFramebuffers();
        if (renderFramebuffer == 0) {
            return null;
        }

        final int numColorAttachments;
        boolean hasColorAttachments = false;
        boolean hasColorResolveAttachments = false;
        numColorAttachments = desc.mNumColorAttachments;
        for (int i = 0; i < numColorAttachments; i++) {
            var attachmentDesc = desc.mColorAttachments[i];
            hasColorAttachments |= attachmentDesc.mAttachment != null;
            hasColorResolveAttachments |= attachmentDesc.mResolveAttachment != null;
        }

        // If we are using multisampling we will create two FBOs. We render to one and then resolve to
        // the texture bound to the other.
        final int resolveFramebuffer;
        if (hasColorResolveAttachments) {
            resolveFramebuffer = gl.glGenFramebuffers();
            if (resolveFramebuffer == 0) {
                gl.glDeleteFramebuffers(renderFramebuffer);
                return null;
            }
        } else {
            resolveFramebuffer = renderFramebuffer;
        }

        gl.glBindFramebuffer(GL_FRAMEBUFFER, renderFramebuffer);
        if (hasColorAttachments) {
            int[] drawBuffers = new int[numColorAttachments];
            for (int index = 0; index < numColorAttachments; index++) {
                var attachmentDesc = desc.mColorAttachments[index];
                GLImage attachment = (GLImage) attachmentDesc.mAttachment;
                if (attachment == null) {
                    // unused slot
                    drawBuffers[index] = GL_NONE;
                    continue;
                }
                attachColorAttachment(index,
                        attachment,
                        attachmentDesc.mMipLevel);
                drawBuffers[index] = GL_COLOR_ATTACHMENT0 + index;
            }
            glDrawBuffers(drawBuffers);
        }
        GLRenderbuffer glDepthStencilTarget = (GLRenderbuffer) desc.mDepthStencilAttachment.mAttachment;
        if (glDepthStencilTarget != null) {
            //TODO renderbuffer?
            glFramebufferRenderbuffer(GL_FRAMEBUFFER,
                    GL_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    glDepthStencilTarget.getHandle());
            if (GLUtil.glFormatIsPackedDepthStencil(glDepthStencilTarget.getFormat())) {
                glFramebufferRenderbuffer(GL_FRAMEBUFFER,
                        GL_DEPTH_STENCIL_ATTACHMENT,
                        GL_RENDERBUFFER,
                        glDepthStencilTarget.getHandle());
            }
        }
        if (!device.getCaps().skipErrorChecks()) {
            int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                gl.glDeleteFramebuffers(renderFramebuffer);
                gl.glDeleteFramebuffers(resolveFramebuffer);
                return null;
            }
        }

        if (hasColorResolveAttachments) {
            gl.glBindFramebuffer(GL_FRAMEBUFFER, resolveFramebuffer);
            int[] drawBuffers = new int[numColorAttachments];
            for (int index = 0; index < numColorAttachments; index++) {
                var attachmentDesc = desc.mColorAttachments[index];
                GLImage resolveAttachment = (GLImage) attachmentDesc.mResolveAttachment;
                if (resolveAttachment == null) {
                    // unused slot
                    drawBuffers[index] = GL_NONE;
                    continue;
                }
                attachColorAttachment(index,
                        resolveAttachment,
                        attachmentDesc.mMipLevel);
                drawBuffers[index] = GL_COLOR_ATTACHMENT0 + index;
            }
            glDrawBuffers(drawBuffers);
            if (!device.getCaps().skipErrorChecks()) {
                int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
                if (status != GL_FRAMEBUFFER_COMPLETE) {
                    gl.glDeleteFramebuffers(renderFramebuffer);
                    gl.glDeleteFramebuffers(resolveFramebuffer);
                    return null;
                }
            }
        }

        return new GLFramebuffer(context, renderFramebuffer, resolveFramebuffer);
    }

    public int getRenderFramebuffer() {
        return mRenderFramebuffer;
    }

    public int getResolveFramebuffer() {
        return mResolveFramebuffer;
    }

    @Override
    protected void onRelease() {
        GLDevice device = (GLDevice) getDevice();
        assert device.isOnExecutingThread();
        if (mRenderFramebuffer != 0) {
            device.getGL().glDeleteFramebuffers(mRenderFramebuffer);
        }
        if (mRenderFramebuffer != mResolveFramebuffer) {
            assert (mResolveFramebuffer != 0);
            device.getGL().glDeleteFramebuffers(mResolveFramebuffer);
        }
        mRenderFramebuffer = 0;
        mResolveFramebuffer = 0;
    }

    public static class ResourceKey implements IResourceKey {

        public int mWidth;
        public int mHeight;
        public int mSampleCount;

        public static class ColorAttachmentDesc implements Cloneable {
            @Nullable
            public UniqueID mAttachment;
            @Nullable
            public UniqueID mResolveAttachment;
            public int mMipLevel;
            public int mArraySlice;

            @Override
            public int hashCode() {
                int result = Objects.hashCode(mAttachment);
                result = 31 * result + Objects.hashCode(mResolveAttachment);
                result = 31 * result + mMipLevel;
                result = 31 * result + mArraySlice;
                return result;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ColorAttachmentDesc that = (ColorAttachmentDesc) o;
                if (mMipLevel != that.mMipLevel) return false;
                if (mArraySlice != that.mArraySlice) return false;
                if (!Objects.equals(mAttachment, that.mAttachment)) return false;
                return Objects.equals(mResolveAttachment, that.mResolveAttachment);
            }

            @Override
            public ColorAttachmentDesc clone() {
                try {
                    return (ColorAttachmentDesc) super.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public int mNumColorAttachments;
        @Nullable
        public ColorAttachmentDesc[] mColorAttachments;

        @Nullable
        public UniqueID mDepthStencilAttachment;

        public ResourceKey() {
        }

        @SuppressWarnings("IncompleteCopyConstructor")
        public ResourceKey(ResourceKey other) {
            mWidth = other.mWidth;
            mHeight = other.mHeight;
            mSampleCount = other.mSampleCount;
            mNumColorAttachments = other.mNumColorAttachments;
            if (mNumColorAttachments > 0) {
                mColorAttachments = new ColorAttachmentDesc[mNumColorAttachments];
                for (int i = 0; i < mNumColorAttachments; i++) {
                    assert other.mColorAttachments != null;
                    mColorAttachments[i] = other.mColorAttachments[i].clone();
                }
            } else {
                mColorAttachments = null;
            }
            mDepthStencilAttachment = other.mDepthStencilAttachment;
        }

        @Nonnull
        public ResourceKey compute(@Nonnull FramebufferDesc desc) {
            mWidth = desc.mWidth;
            mHeight = desc.mHeight;
            mSampleCount = desc.mSampleCount;
            if (mColorAttachments == null) {
                mColorAttachments = new ColorAttachmentDesc[Caps.MAX_COLOR_TARGETS];
            }
            mNumColorAttachments = desc.mNumColorAttachments;
            for (int i = 0; i < mNumColorAttachments; i++) {
                var src = desc.mColorAttachments[i];
                var dst = mColorAttachments[i] = new ColorAttachmentDesc();
                dst.mAttachment = src.mAttachment != null
                        ? src.mAttachment.getUniqueID()
                        : null;
                dst.mResolveAttachment = src.mResolveAttachment != null
                        ? src.mResolveAttachment.getUniqueID()
                        : null;
                dst.mMipLevel = src.mMipLevel;
                dst.mArraySlice = src.mArraySlice;
            }
            mDepthStencilAttachment = desc.mDepthStencilAttachment.mAttachment != null
                    ? desc.mDepthStencilAttachment.mAttachment.getUniqueID()
                    : null;
            return this;
        }

        @Override
        public IResourceKey copy() {
            return new ResourceKey(this);
        }

        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mSampleCount;
            result = 31 * result + mNumColorAttachments;
            for (int i = 0; i < mNumColorAttachments; i++) {
                assert mColorAttachments != null;
                result = 31 * result + mColorAttachments[i].hashCode();
            }
            result = 31 * result + Objects.hashCode(mDepthStencilAttachment);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResourceKey that = (ResourceKey) o;

            if (mWidth != that.mWidth) return false;
            if (mHeight != that.mHeight) return false;
            if (mSampleCount != that.mSampleCount) return false;
            if (mNumColorAttachments != that.mNumColorAttachments) return false;
            if (!Objects.equals(mDepthStencilAttachment, that.mDepthStencilAttachment)) return false;
            if (mNumColorAttachments == 0) return true;
            assert mColorAttachments != null;
            assert that.mColorAttachments != null;
            return Arrays.equals(mColorAttachments, 0, mNumColorAttachments,
                    that.mColorAttachments, 0, mNumColorAttachments);
        }
    }
}
