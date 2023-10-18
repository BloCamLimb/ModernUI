/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics;

import icyllis.arc3d.engine.GpuResource;
import icyllis.arc3d.engine.ISurface;
import icyllis.arc3d.opengl.*;
import icyllis.modernui.core.Core;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import java.nio.FloatBuffer;
import java.util.Objects;

import static icyllis.arc3d.opengl.GLCore.*;

/**
 * This class represents a framebuffer object. It is used for creation of
 * user-defined framebuffers compared to the default framebuffer, whose object
 * is a collection of attachments, for off-screen rendering or post-processing.
 * <p>
 * For post-processing, attach this to a set of textures otherwise to render
 * buffers. To output this framebuffer to screen, draw the attached textures
 * or copy the renderbuffer pixels to the default framebuffer that preserved
 * by window graphics context.
 * <p>
 * Losing the reference of the object will delete the framebuffer and all
 * attachments automatically.
 *
 * @see <a href="https://www.khronos.org/opengl/wiki/Framebuffer_Object">Framebuffer Object</a>
 */
public final class GLSurface implements AutoCloseable {

    public static final int NUM_RENDER_TARGETS = 3;

    // this can be Java GC-ed
    private final FloatBuffer mClearColor = BufferUtils.createFloatBuffer(4);

    private final GLTexture[] mColorAttachments = new GLTexture[NUM_RENDER_TARGETS];
    private GLAttachment mStencilAttachment;

    private int mBackingWidth;
    private int mBackingHeight;

    private int mFramebuffer;

    /**
     * Creates a framebuffer.
     */
    public GLSurface() {
    }

    public int get() {
        if (mFramebuffer == 0) {
            mFramebuffer = glGenFramebuffers();
        }
        return mFramebuffer;
    }

    /**
     * Binds this framebuffer object to both draw and read target.
     */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, get());
    }

    public void bindDraw() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, get());
    }

    public void bindRead() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, get());
    }

    /**
     * Clear the current color buffer set by {@link #setDrawBuffer(int)} to the color (0,0,0,0).
     * Clear the current depth buffer to 1.0f, and stencil buffer to 0.
     */
    public void clearColorBuffer() {
        // here drawbuffer is zero, because setDrawBuffer only set the buffer with index 0
        glClearBufferfv(GL_COLOR, 0, mClearColor);
    }

    public void clearStencilBuffer() {
        // for depth or stencil, the drawbuffer must be 0
        glClearBufferfi(GL_DEPTH_STENCIL, 0, 1.0f, 0);
    }

    /**
     * Set the color buffer for <code>layout(location = 0) out vec4 fragColor</code>.
     * That means the color buffer index is 0.
     * <p>
     * Note that only GL_COLOR_ATTACHMENT[x] or GL_NONE is accepted by a framebuffer
     * object. Values such as GL_FRONT_LEFT, GL_BACK are only accepted by the default
     * framebuffer (reserved by the window).
     *
     * @param buffer enum buffer
     */
    public void setDrawBuffer(int buffer) {
        glDrawBuffer(buffer);
    }

    public void setReadBuffer(int buffer) {
        glReadBuffer(buffer);
    }

    public int getBackingWidth() {
        return mBackingWidth;
    }

    public int getBackingHeight() {
        return mBackingHeight;
    }

    /**
     * Returns the attached texture with the given attachment point.
     *
     * @param attachment specify an attachment point
     * @return the raw ptr to texture
     * @throws NullPointerException attachment is not a texture or detached
     */
    @Nonnull
    public GLTexture getAttachedTexture(int attachment) {
        return Objects.requireNonNull(
                mColorAttachments[attachment - GL_COLOR_ATTACHMENT0]
        );
    }

    public void makeBuffers(int width, int height, boolean exact) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (exact) {
            if (mBackingWidth == width && mBackingHeight == height) {
                return;
            }
        } else {
            if (mBackingWidth >= width && mBackingHeight >= height) {
                return;
            }
        }
        mBackingWidth = width;
        mBackingHeight = height;
        var dContext = Core.requireDirectContext();
        for (int i = 0; i < NUM_RENDER_TARGETS; i++) {
            if (mColorAttachments[i] != null) {
                mColorAttachments[i].unref();
            }
            mColorAttachments[i] = (GLTexture) dContext
                    .getResourceProvider()
                    .createTexture(width, height,
                            GLBackendFormat.make(GL_RGBA8),
                            1,
                            ISurface.FLAG_BUDGETED,
                            null
                    );
            Objects.requireNonNull(mColorAttachments[i], "Failed to create G-buffer " + i);
            glFramebufferTexture(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0 + i,
                    mColorAttachments[i].getHandle(),
                    0
            );
        }
        if (mStencilAttachment != null) {
            mStencilAttachment.unref();
        }
        mStencilAttachment = GLAttachment.makeStencil(
                (GLDevice) dContext.getDevice(),
                width, height,
                1, GL_STENCIL_INDEX8
        );
        Objects.requireNonNull(mStencilAttachment, "Failed to create depth/stencil");
        glFramebufferRenderbuffer(
                GL_FRAMEBUFFER,
                GL_STENCIL_ATTACHMENT,
                GL_RENDERBUFFER,
                mStencilAttachment.getRenderbufferID()
        );
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer is not complete: " + status);
        }
    }

    @Override
    public void close() {
        if (mFramebuffer != 0) {
            glDeleteFramebuffers(mFramebuffer);
        }
        mFramebuffer = 0;
        for (int i = 0; i < NUM_RENDER_TARGETS; i++) {
            mColorAttachments[i] = GpuResource.move(mColorAttachments[i]);
        }
        mStencilAttachment = GpuResource.move(mStencilAttachment);
    }
}
