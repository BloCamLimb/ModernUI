/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.texture.Texture;
import icyllis.modernui.graphics.texture.Texture2D;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Cleaner;

import static icyllis.modernui.graphics.GLWrapper.*;

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
public final class Framebuffer implements AutoCloseable {

    private int mWidth;
    private int mHeight;

    private Ref mRef;
    private Int2ObjectMap<AutoCloseable> mAttachments;

    public Framebuffer(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int get() {
        if (mRef == null) {
            mRef = new Ref(this);
            mAttachments = new Int2ObjectArrayMap<>();
        }
        return mRef.framebuffer;
    }

    public void resize(int width, int height) {
        if (mWidth != width || mHeight != height) {
            mWidth = width;
            mHeight = height;
            for (var entry : mAttachments.int2ObjectEntrySet()) {
                AutoCloseable a = entry.getValue();
                if (a instanceof Texture2D) {
                    Texture2D texture = (Texture2D) a;
                    int internalFormat = glGetTextureParameteri(texture.get(), GL_TEXTURE_INTERNAL_FORMAT);
                    texture.close();
                    texture.init(internalFormat, width, height, 1);
                    glNamedFramebufferTexture(get(), entry.getIntKey(), texture.get(), 0);
                } else if (a instanceof Renderbuffer) {
                    Renderbuffer renderbuffer = (Renderbuffer) a;
                    int internalFormat = glGetRenderbufferParameteri(renderbuffer.get(), GL_RENDERBUFFER_INTERNAL_FORMAT);
                    renderbuffer.close();
                    renderbuffer.init(internalFormat, width, height);
                    glNamedFramebufferRenderbuffer(get(), entry.getIntKey(), GL_RENDERBUFFER, renderbuffer.get());
                }
            }
        }
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

    public void attachTexture(int attachPoint, int internalFormat) {
        Texture2D texture = new Texture2D();
        texture.init(internalFormat, mWidth, mHeight, 1);
        glNamedFramebufferTexture(get(), attachPoint, texture.get(), 0);
        mAttachments.put(attachPoint, texture);
    }

    public void attachRenderbuffer(int attachPoint, int internalFormat) {
        Renderbuffer renderbuffer = new Renderbuffer();
        renderbuffer.init(internalFormat, mWidth, mHeight);
        glNamedFramebufferRenderbuffer(get(), attachPoint, GL_RENDERBUFFER, renderbuffer.get());
        mAttachments.put(attachPoint, renderbuffer);
    }

    public void removeAttachment(int attachPoint) {
        if (mAttachments.remove(attachPoint) != null) {
            glNamedFramebufferTexture(get(), attachPoint, DEFAULT_TEXTURE, 0);
        }
    }

    public void clearAttachments() {
        int framebuffer = get();
        for (int point : mAttachments.keySet()) {
            glNamedFramebufferTexture(framebuffer, point, DEFAULT_TEXTURE, 0);
        }
        mAttachments.clear();
    }

    /**
     * Set the color buffer for <code>layout(location = 0) out vec4 fragColor</code>.
     *
     * @param buffer buf
     */
    public void setDrawBuffer(int buffer) {
        glNamedFramebufferDrawBuffer(get(), buffer);
    }

    /**
     * Returns the attached texture with the given attachment point.
     * If the attachment is not a texture or detached, 0 will be returned.
     *
     * @param attachPoint specify an attachment point
     * @return the texture name
     */
    public int getAttachedTextureRaw(int attachPoint) {
        AutoCloseable a = mAttachments.get(attachPoint);
        if (a instanceof Texture) {
            return ((Texture) a).get();
        }
        return DEFAULT_TEXTURE;
    }

    @Nullable
    public Texture2D getAttachedTexture(int attachPoint) {
        AutoCloseable a = mAttachments.get(attachPoint);
        if (a instanceof Texture2D) {
            return ((Texture2D) a);
        }
        return null;
    }

    @Nullable
    public Renderbuffer getAttachedRenderbuffer(int attachPoint) {
        AutoCloseable a = mAttachments.get(attachPoint);
        if (a instanceof Renderbuffer) {
            return (Renderbuffer) a;
        }
        return null;
    }

    @Override
    public void close() {
        if (mRef != null) {
            mRef.cleanup.clean();
            mRef = null;
            for (var a : mAttachments.values()) {
                try {
                    a.close();
                } catch (Exception ignored) {
                }
            }
            mAttachments = null;
        }
    }

    private static final class Ref implements Runnable {

        private final int framebuffer;
        private final Cleaner.Cleanable cleanup;

        private Ref(@Nonnull Framebuffer owner) {
            framebuffer = glCreateFramebuffers();
            cleanup = ModernUI.registerCleanup(owner, this);
        }

        @Override
        public void run() {
            deleteFramebufferAsync(framebuffer, this);
        }
    }
}
