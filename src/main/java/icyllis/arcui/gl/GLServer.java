/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.gl;

import icyllis.arcui.hgi.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import javax.annotation.Nullable;

import static icyllis.arcui.gl.GLCore.*;

public final class GLServer extends Server {

    final GLCaps mCaps;

    private int mDrawFramebuffer = 0;

    private GLServer(DirectContext context, GLCaps caps) {
        super(context, caps);
        mCaps = caps;
    }

    /**
     * Create a GLServer with OpenGL context current in the current thread.
     *
     * @param context the owner context
     * @param options the context options
     * @return the server or null if failed to create
     */
    @SuppressWarnings("ConstantConditions")
    @Nullable
    public static GLServer make(DirectContext context, ContextOptions options) {
        // get or create
        GLCapabilities caps;
        try {
            caps = GL.getCapabilities();
            if (caps == null) {
                // checks may be disabled
                caps = GL.createCapabilities();
            }
        } catch (IllegalStateException e) {
            // checks may be enabled
            caps = GL.createCapabilities();
        }
        if (caps == null) {
            return null;
        }
        return new GLServer(context, new GLCaps(options, caps));
    }

    public void bindFramebuffer(int target, int framebuffer) {
        glBindFramebuffer(target, framebuffer);
        if (target == GL_FRAMEBUFFER || target == GL_DRAW_FRAMEBUFFER) {
            mDrawFramebuffer = framebuffer;
        }
        onFramebufferChanged();
    }

    public void deleteFramebuffer(int framebuffer) {
        glDeleteFramebuffers(framebuffer);
        // Deleting the currently bound framebuffer rebinds to 0.
        if (mDrawFramebuffer == framebuffer) {
            onFramebufferChanged();
            mDrawFramebuffer = 0;
        }
    }

    private void onFramebufferChanged() {

    }

    @Nullable
    @Override
    protected Texture onCreateTexture(int width, int height,
                                      BackendFormat format,
                                      boolean budgeted,
                                      boolean isProtected,
                                      int mipLevels) {
        // We don't support protected textures in core profile.
        if (isProtected) {
            return null;
        }
        // We only support TEXTURE_2D.
        if (format.getTextureType() != Types.TEXTURE_TYPE_2D) {
            return null;
        }
        int f = format.getGLFormat();
        int tex = createTexture(width, height, f, mipLevels);
        if (tex == 0) {
            return null;
        }
        return new GLTexture(this, width, height, f, tex, mipLevels > 1, budgeted, true);
    }

    private int createTexture(int width, int height, int format, int levels) {
        assert width > 0;
        assert height > 0;
        assert format != GLTypes.FORMAT_UNKNOWN;
        assert !glFormatIsCompressed(format);
        assert levels > 0;

        FormatInfo info = mCaps.mFormatTable[format];
        int internalFormat = info.mInternalFormatForTexture;

        if (internalFormat != 0) {
            assert (info.mFlags & FormatInfo.TEXTURE_FLAG) != 0;
            assert (info.mFlags & FormatInfo.USE_TEX_STORAGE_FLAG) != 0;
            int texture = glCreateTextures(GL_TEXTURE_2D);
            if (texture == 0) {
                return 0;
            }
            glTextureStorage2D(texture, levels, internalFormat, width, height);
            return texture;
        }
        return 0;
    }
}
