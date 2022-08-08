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

package icyllis.arcui.opengl;

import icyllis.arcui.engine.BackendFormat;
import icyllis.arcui.engine.EngineTypes;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;

import static icyllis.arcui.opengl.GLCore.*;

public final class GLBackendFormat extends BackendFormat {

    private final int mFormat;
    private final int mTextureType;

    /**
     * @see BackendFormat#makeGL(int, int)
     */
    public GLBackendFormat(@NativeType("GLenum") int format, int textureType) {
        assert textureType == EngineTypes.TextureType_None ||
                textureType == EngineTypes.TextureType_2D ||
                textureType == EngineTypes.TextureType_External;
        mFormat = format;
        mTextureType = textureType;
    }

    @Override
    public int getBackend() {
        return EngineTypes.OpenGL;
    }

    @Override
    public int getTextureType() {
        return mTextureType;
    }

    @Override
    public int getChannelMask() {
        return glFormatChannels(getGLFormat());
    }

    @Override
    public int getGLFormat() {
        return glFormatFromEnum(mFormat);
    }

    @Override
    public int getGLFormatEnum() {
        return mFormat;
    }

    @Nonnull
    @Override
    public BackendFormat makeTexture2D() {
        if (mTextureType == EngineTypes.TextureType_2D) {
            return this;
        }
        return makeGL(mFormat, EngineTypes.TextureType_2D);
    }

    @Override
    public boolean isSRGB() {
        return mFormat == GL_SRGB8_ALPHA8;
    }

    @Override
    public int getCompressionType() {
        return glFormatCompressionType(getGLFormat());
    }

    @Override
    public int getBytesPerBlock() {
        return glFormatBytesPerBlock(getGLFormat());
    }

    @Override
    public int getStencilBits() {
        return glFormatStencilBits(getGLFormat());
    }

    @Override
    public int getFormatKey() {
        // it's okay to use GLenum (not sequential indexing though)
        return mFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return mFormat == ((GLBackendFormat) o).mFormat;
    }

    @Override
    public int hashCode() {
        return mFormat;
    }
}
