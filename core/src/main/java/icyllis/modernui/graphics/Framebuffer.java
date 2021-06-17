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
import icyllis.modernui.graphics.texture.Texture2D;

import javax.annotation.Nonnull;
import java.lang.ref.Cleaner;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * This class represents a framebuffer object. It is used for creation of
 * user-defined framebuffers compared to the default framebuffer, whose object
 * is a collection of attachments, for off-screen rendering or post-processing.
 * <p>
 * For post-processing, attach this to a set of textures otherwise to render
 * buffers. To output this framebuffer to screen, blit the attached textures
 * or copy the renderbuffer pixels to the default framebuffer that preserved
 * by window graphics context.
 * <p>
 * Losing the reference of the object will delete the framebuffer automatically.
 *
 * @see <a href="https://www.khronos.org/opengl/wiki/Framebuffer_Object">Framebuffer Object</a>
 */
public final class Framebuffer implements AutoCloseable {

    private Ref mRef;

    public Framebuffer() {
    }

    public int get() {
        if (mRef == null)
            mRef = new Ref(this);
        return mRef.framebuffer;
    }

    /**
     * Binds this framebuffer object to draw and read target.
     */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, get());
    }

    public void bindDraw() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, get());
    }

    public void bindRead() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, get());
        glBindRenderbuffer();
    }

    // min color attachments: 0-7
    public void attachTexture(int attachment, @Nonnull Texture2D texture, int level) {
        glFramebufferTexture(mTarget, attachment, texture.get(), level);
    }

    // for cube map or 3D texture, specify a face or a layer
    public void attachTexturePart() {

    }

    @Override
    public void close() {
        if (mRef != null) {
            mRef.cleanup.clean();
            mRef = null;
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
