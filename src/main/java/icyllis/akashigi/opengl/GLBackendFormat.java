/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.opengl;

import icyllis.akashigi.engine.BackendFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import java.util.Objects;

import static icyllis.akashigi.engine.Engine.OpenGL;
import static icyllis.akashigi.opengl.GLCore.*;

public final class GLBackendFormat extends BackendFormat {

    private static final Int2ObjectOpenHashMap<GLBackendFormat> sGLBackendFormats =
            new Int2ObjectOpenHashMap<>(GLTypes.LAST_FORMAT + 1, 0.8f);

    private final int mFormat;
    private final boolean mIsExternal;

    /**
     * @see #make(int, boolean)
     */
    GLBackendFormat(@NativeType("GLenum") int format, boolean isExternal) {
        mFormat = format;
        mIsExternal = isExternal;
    }

    @Nonnull
    public static GLBackendFormat make(@NativeType("GLenum") int format) {
        return make(format, false);
    }

    @Nonnull
    public static GLBackendFormat make(@NativeType("GLenum") int format, boolean isExternal) {
        if (isKnownFormat(format)) {
            Objects.checkIndex(format, Integer.MAX_VALUE);
            int key = (format) | (isExternal ? Integer.MIN_VALUE : 0);
            GLBackendFormat backendFormat = sGLBackendFormats.get(key);
            if (backendFormat != null) {
                return backendFormat;
            }
            backendFormat = new GLBackendFormat(format, isExternal);
            sGLBackendFormats.put(key, backendFormat);
            return backendFormat;
        }
        return new GLBackendFormat(format, isExternal);
    }

    @Override
    public int getBackend() {
        return OpenGL;
    }

    @Override
    public boolean isExternal() {
        return mIsExternal;
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
        if (mIsExternal) {
            return make(mFormat, false);
        }
        return this;
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

    @Override
    public String toString() {
        return "GLBackendFormat{" +
                "mFormat=" + glFormatName(mFormat) +
                ", mIsExternal=" + mIsExternal +
                '}';
    }
}
