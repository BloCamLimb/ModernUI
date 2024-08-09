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

import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL11C.GL_NONE;
import static org.lwjgl.opengl.GL20C.glDrawBuffers;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32C.*;

public final class GLFramebuffer extends Framebuffer {

    private int mRenderFramebuffer;
    private int mResolveFramebuffer;

    private GLFramebuffer(Context context,
                          int renderFramebuffer,
                          int resolveFramebuffer) {
        super(context.getDevice());
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
        numColorAttachments = desc.mColorAttachments.length;
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
                if (attachmentDesc.mAttachment == null) {
                    // unused slot
                    drawBuffers[index] = GL_NONE;
                    continue;
                }
                GLImage attachment = (GLImage) attachmentDesc.mAttachment.get();
                assert attachment != null;
                attachColorAttachment(index,
                        attachment,
                        attachmentDesc.mMipLevel);
                drawBuffers[index] = GL_COLOR_ATTACHMENT0 + index;
            }
            glDrawBuffers(drawBuffers);
        }
        if (desc.mDepthStencilAttachment.mAttachment != null) {
            GLRenderbuffer attachment = (GLRenderbuffer) desc.mDepthStencilAttachment.mAttachment.get();
            assert attachment != null;
            //TODO attach depth texture besides renderbuffer
            int attachmentPoint;
            if (GLUtil.glFormatIsPackedDepthStencil(attachment.getFormat())) {
                attachmentPoint = GL_DEPTH_STENCIL_ATTACHMENT;
            } else if (GLUtil.glFormatDepthBits(attachment.getFormat()) > 0) {
                attachmentPoint = GL_DEPTH_ATTACHMENT;
            } else {
                assert GLUtil.glFormatStencilBits(attachment.getFormat()) > 0;
                attachmentPoint = GL_STENCIL_ATTACHMENT;
            }
            glFramebufferRenderbuffer(GL_FRAMEBUFFER,
                    attachmentPoint,
                    GL_RENDERBUFFER,
                    attachment.getHandle());
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
                if (attachmentDesc.mResolveAttachment == null) {
                    // unused slot
                    drawBuffers[index] = GL_NONE;
                    continue;
                }
                GLImage resolveAttachment = (GLImage) attachmentDesc.mResolveAttachment.get();
                assert resolveAttachment != null;
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
    protected void deallocate() {
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
}
