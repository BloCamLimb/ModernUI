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

package icyllis.arc3d.opengl;

import icyllis.modernui.core.Core;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.lwjgl.BufferUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;

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
public final class GLFramebufferCompat extends GLObjectCompat {

    private static GLFramebufferCompat sSwapFramebuffer;

    // this can be Java GC-ed
    private final FloatBuffer mClearColor = BufferUtils.createFloatBuffer(4);

    private final int mSampleCount;

    @Nullable
    private Int2ObjectArrayMap<Attachment> mAttachments;

    /**
     * Creates a framebuffer.
     *
     * @param sampleCount number of samples
     */
    public GLFramebufferCompat(int sampleCount) {
        mSampleCount = Math.max(1, sampleCount);
    }

    /**
     * Blit the given framebuffer color buffer of read buffer to a swap framebuffer
     * texture. This is used with a MSAA target for blending with other targets.
     *
     * @param framebuffer source framebuffer
     * @param colorBuffer color buffer
     * @return sampled texture
     */
    @Nonnull
    public static GLTextureCompat resolve(@Nonnull GLFramebufferCompat framebuffer, int colorBuffer) {
        if (sSwapFramebuffer == null) {
            sSwapFramebuffer = new GLFramebufferCompat(1);
            sSwapFramebuffer.addTextureAttachment(GL_COLOR_ATTACHMENT0, GL_RGBA8);
            sSwapFramebuffer.setDrawBuffer(GL_COLOR_ATTACHMENT0);
        }
        Attachment src = framebuffer.getAttachment(colorBuffer);
        int w = src.getWidth();
        int h = src.getHeight();
        sSwapFramebuffer.getAttachment(GL_COLOR_ATTACHMENT0).make(w, h, false);
        glBlitNamedFramebuffer(framebuffer.get(), sSwapFramebuffer.get(), 0, 0, w, h,
                0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        return sSwapFramebuffer.getAttachedTexture(GL_COLOR_ATTACHMENT0);
    }

    @Override
    public int get() {
        if (ref == null) {
            ref = new Ref(this);
        }
        return ref.mId;
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

    public boolean isMultisampled() {
        return mSampleCount > 1;
    }

    @Nonnull
    private Int2ObjectMap<Attachment> getAttachments() {
        if (mAttachments == null) {
            mAttachments = new Int2ObjectArrayMap<>();
        }
        return mAttachments;
    }

    public void addTextureAttachment(int attachmentPoint, int internalFormat) {
        getAttachments().put(attachmentPoint, new TextureAttachment(this, attachmentPoint, internalFormat));
    }

    public void addRenderbufferAttachment(int attachmentPoint, int internalFormat) {
        getAttachments().put(attachmentPoint, new RenderbufferAttachment(this, attachmentPoint, internalFormat));
    }

    public void removeAttachment(int attachmentPoint) {
        if (mAttachments == null) {
            return;
        }
        Attachment attachment = mAttachments.remove(attachmentPoint);
        if (attachment != null) {
            attachment.close();
            glNamedFramebufferTexture(get(), attachmentPoint, GLCore.DEFAULT_TEXTURE, 0);
        }
        if (mAttachments.isEmpty()) {
            mAttachments = null;
        }
    }

    public void clearAttachments() {
        if (mAttachments == null) {
            return;
        }
        int framebuffer = get();
        for (var entry : mAttachments.int2ObjectEntrySet()) {
            entry.getValue().close();
            glNamedFramebufferTexture(framebuffer, entry.getIntKey(), GLCore.DEFAULT_TEXTURE, 0);
        }
        mAttachments.clear();
        mAttachments = null;
    }

    /**
     * Set the color used for {@link #clearColorBuffer()}, default clear color is (0,0,0,0).
     */
    public void setClearColor(float r, float g, float b, float a) {
        mClearColor.put(r).put(g).put(b).put(a).flip();
    }

    /**
     * Clear the current color buffer set by {@link #setDrawBuffer(int)} to the color
     * set by {@link #setClearColor(float, float, float, float)}, default clear color is (0,0,0,0).
     */
    public void clearColorBuffer() {
        // here drawbuffer is zero, because setDrawBuffer only set the buffer with index 0
        glClearNamedFramebufferfv(get(), GL_COLOR, 0, mClearColor);
    }

    /**
     * Clear the current depth buffer to 1.0f, and stencil buffer to 0.
     */
    public void clearDepthStencilBuffer() {
        // for depth or stencil, the drawbuffer must be 0
        glClearNamedFramebufferfi(get(), GL_DEPTH_STENCIL, 0, 1.0f, 0);
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
        glNamedFramebufferDrawBuffer(get(), buffer);
    }

    public void setReadBuffer(int buffer) {
        glNamedFramebufferReadBuffer(get(), buffer);
    }

    @Nonnull
    public Attachment getAttachment(int attachmentPoint) {
        if (mAttachments != null) {
            Attachment a = mAttachments.get(attachmentPoint);
            if (a != null) {
                return a;
            }
        }
        throw new IllegalStateException("No attachment " + attachmentPoint);
    }

    /**
     * Returns the attached texture with the given attachment point.
     *
     * @param attachmentPoint specify an attachment point
     * @return the texture name
     * @throws IllegalArgumentException attachment is not a texture or detached
     */
    @Nonnull
    public GLTextureCompat getAttachedTexture(int attachmentPoint) {
        if (mAttachments != null) {
            Attachment a = mAttachments.get(attachmentPoint);
            if (a instanceof TextureAttachment) {
                return ((TextureAttachment) a).mTexture;
            }
        }
        throw new IllegalStateException("No attachment " + attachmentPoint);
    }

    @Nonnull
    public GLRenderbufferCompat getAttachedRenderbuffer(int attachmentPoint) {
        if (mAttachments != null) {
            AutoCloseable a = mAttachments.get(attachmentPoint);
            if (a instanceof RenderbufferAttachment) {
                return ((RenderbufferAttachment) a).mRenderbuffer;
            }
        }
        throw new IllegalStateException("No attachment " + attachmentPoint);
    }

    public void makeBuffers(int width, int height, boolean exact) {
        if (mAttachments == null) {
            return;
        }
        for (Attachment attachment : mAttachments.values()) {
            attachment.make(width, height, exact);
        }
    }

    @Override
    public void close() {
        super.close();
        if (mAttachments != null) {
            mAttachments.values().forEach(Attachment::close);
            mAttachments.clear();
            mAttachments = null;
        }
    }

    private static final class Ref extends GLObjectCompat.Ref {

        private Ref(@Nonnull GLFramebufferCompat owner) {
            super(owner, glCreateFramebuffers());
        }

        @Override
        public void run() {
            Core.executeOnRenderThread(() -> glDeleteFramebuffers(mId));
        }
    }

    public static abstract class Attachment implements AutoCloseable {

        final WeakReference<GLFramebufferCompat> mFramebuffer;
        final int mAttachmentPoint;
        final int mInternalFormat;

        int mWidth;
        int mHeight;

        private Attachment(GLFramebufferCompat framebuffer, int attachmentPoint, int internalFormat) {
            mFramebuffer = new WeakReference<>(framebuffer);
            mAttachmentPoint = attachmentPoint;
            mInternalFormat = internalFormat;
        }

        public abstract boolean make(int width, int height, boolean exact);

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        @Override
        public void close() {
            mWidth = 0;
            mHeight = 0;
        }
    }

    private static class TextureAttachment extends Attachment {

        private final GLTextureCompat mTexture;

        protected TextureAttachment(GLFramebufferCompat framebuffer, int attachmentPoint, int internalFormat) {
            super(framebuffer, attachmentPoint, internalFormat);
            if (framebuffer.mSampleCount > 1) {
                mTexture = new GLTextureCompat(GL_TEXTURE_2D_MULTISAMPLE);
            } else {
                mTexture = new GLTextureCompat(GL_TEXTURE_2D);
            }
        }

        @Override
        public boolean make(int width, int height, boolean exact) {
            if (width <= 0 || height <= 0) {
                return false;
            }
            if (exact ? mWidth != width || mHeight != height :
                    mWidth < width || mHeight < height) {
                mWidth = width;
                mHeight = height;
                mTexture.close();
                GLFramebufferCompat framebuffer = mFramebuffer.get();
                if (framebuffer == null) {
                    return false;
                }
                if (framebuffer.mSampleCount > 1) {
                    mTexture.allocate2DMS(mInternalFormat, width, height, framebuffer.mSampleCount);
                } else {
                    mTexture.allocate2D(mInternalFormat, width, height, 0);
                }
                glNamedFramebufferTexture(framebuffer.get(), mAttachmentPoint, mTexture.get(), 0);
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            super.close();
            mTexture.close();
        }
    }

    private static class RenderbufferAttachment extends Attachment {

        private final GLRenderbufferCompat mRenderbuffer;

        protected RenderbufferAttachment(GLFramebufferCompat framebuffer, int attachmentPoint, int internalFormat) {
            super(framebuffer, attachmentPoint, internalFormat);
            mRenderbuffer = new GLRenderbufferCompat();
        }

        @Override
        public boolean make(int width, int height, boolean exact) {
            if (width <= 0 || height <= 0) {
                return false;
            }
            if (exact ? mWidth != width || mHeight != height :
                    mWidth < width || mHeight < height) {
                mWidth = width;
                mHeight = height;
                mRenderbuffer.close();
                GLFramebufferCompat framebuffer = mFramebuffer.get();
                if (framebuffer == null) {
                    return false;
                }
                mRenderbuffer.allocate(mInternalFormat, width, height, framebuffer.mSampleCount);
                glNamedFramebufferRenderbuffer(framebuffer.get(), mAttachmentPoint, GL_RENDERBUFFER,
                        mRenderbuffer.get());
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            super.close();
            mRenderbuffer.close();
        }
    }
}
