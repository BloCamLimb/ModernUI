/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

import icyllis.arcticgi.engine.BackendFormat;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;

import static icyllis.arcticgi.engine.Engine.*;
import static icyllis.arcticgi.opengl.GLCore.*;

public final class GLBackendFormat extends BackendFormat {

    private static final Long2ObjectOpenHashMap<GLBackendFormat> sGLBackendFormats =
            new Long2ObjectOpenHashMap<>(25, 0.8f);

    private final int mFormat;
    private final int mTextureType;

    /**
     * @see #make(int, int)
     */
    public GLBackendFormat(@NativeType("GLenum") int format, int textureType) {
        assert textureType == TextureType_None ||
                textureType == TextureType_2D ||
                textureType == TextureType_External;
        mFormat = format;
        mTextureType = textureType;
    }

    @Nonnull
    public static GLBackendFormat make(@NativeType("GLenum") int format, int textureType) {
        if (isUnknownFormat(format)) {
            return new GLBackendFormat(format, textureType);
        }
        return sGLBackendFormats.computeIfAbsent(format | ((long) textureType << 32),
                key -> new GLBackendFormat((int) key, (int) (key >> 32))); // this lambda is singleton
    }

    @Override
    public int getBackend() {
        return OpenGL;
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
        if (mTextureType == TextureType_2D) {
            return this;
        }
        return make(mFormat, TextureType_2D);
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
    public int getKey() {
        return glFormatFromEnum(mFormat);
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
