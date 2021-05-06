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

import icyllis.modernui.graphics.texture.Texture;

import javax.annotation.Nonnull;

import static org.lwjgl.opengl.GL43C.*;

/**
 * This class represents a framebuffer object. It is used for creation of
 * user-defined framebuffers compared to the default framebuffer, whose object
 * is a collection of attachments, for off-screen rendering or post-processing.
 * <p>
 * For post-processing, attach this to a set of textures otherwise to render
 * buffers. To output this framebuffer to screen, blit the attached textures
 * or copy the renderbuffer pixels to the default framebuffer that preserved
 * by window graphics context.
 *
 * @see <a href="https://www.khronos.org/opengl/wiki/Framebuffer_Object">Framebuffer Object</a>
 */
public final class Framebuffer implements AutoCloseable {

    private int mId = GLWrapper.INVALID_ID;

    private int mTarget;

    public Framebuffer() {

    }

    public int getId() {
        if (mId == GLWrapper.INVALID_ID)
            mId = glGenFramebuffers();
        return mId;
    }

    /**
     * Binds this framebuffer object to draw and read target.
     */
    public void bind() {
        GLWrapper.bindFramebuffer(getId());
        mTarget = GL_FRAMEBUFFER;
    }

    public void bindDraw() {
        GLWrapper.bindDrawFramebuffer(getId());
        mTarget = GL_DRAW_FRAMEBUFFER;
    }

    public void bindRead() {
        GLWrapper.bindReadFramebuffer(getId());
        mTarget = GL_READ_FRAMEBUFFER;
    }

    // min color attachments: 0-7
    public void attachTexture(int attachment, @Nonnull Texture texture, int level) {
        glFramebufferTexture(mTarget, attachment, texture.getId(), level);
    }

    // for cube map or 3D texture, specify a face or a layer
    public void attachTexturePart() {

    }

    @Override
    public void close() throws Exception {

    }
}
