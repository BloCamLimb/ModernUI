/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.BackendFormat;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static icyllis.arc3d.engine.Engine.BackendApi;
import static icyllis.arc3d.opengl.GLCore.*;

@Immutable
public final class GLBackendFormat extends BackendFormat {

    private static final Int2ObjectOpenHashMap<GLBackendFormat> FORMATS =
            new Int2ObjectOpenHashMap<>(GLUtil.LAST_FORMAT_INDEX + 1, Hash.FAST_LOAD_FACTOR);

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
        if (GLUtil.glFormatIsSupported(format)) {
            assert (format > 0);
            return FORMATS.computeIfAbsent((format) | (isExternal ? Integer.MIN_VALUE : 0),
                    k -> new GLBackendFormat(k & Integer.MAX_VALUE, k < 0));
        }
        return new GLBackendFormat(format, isExternal);
    }

    @Override
    public int getBackend() {
        return BackendApi.kOpenGL;
    }

    @Override
    public int getGLFormat() {
        return mFormat;
    }

    @Override
    public boolean isExternal() {
        return mIsExternal;
    }

    @Override
    public int getChannelFlags() {
        return GLUtil.glFormatChannels(mFormat);
    }

    @Nonnull
    @Override
    public BackendFormat makeInternal() {
        if (mIsExternal) {
            return make(mFormat, false);
        }
        return this;
    }

    @Override
    public boolean isSRGB() {
        return GLUtil.glFormatIsSRGB(mFormat);
    }

    @Override
    public int getCompressionType() {
        return GLUtil.glFormatCompressionType(mFormat);
    }

    @Override
    public int getBytesPerBlock() {
        return GLUtil.glFormatBytesPerBlock(mFormat);
    }

    @Override
    public int getStencilBits() {
        return GLUtil.glFormatStencilBits(mFormat);
    }

    @Override
    public int getFormatKey() {
        return mFormat;
    }

    @Override
    public int hashCode() {
        return mFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return mFormat == ((GLBackendFormat) o).mFormat;
    }

    @Override
    public String toString() {
        return "{mBackend=OpenGL" +
                ", mFormat=" + GLUtil.glFormatName(mFormat) +
                ", mIsExternal=" + mIsExternal +
                '}';
    }
}
