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

import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.Engine;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.system.NativeType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static icyllis.arc3d.opengl.GLCore.*;

@Immutable
public final class GLBackendFormat extends BackendFormat {

    private static final Int2ObjectMap<GLBackendFormat> FORMATS =
            new Int2ObjectOpenHashMap<>(LAST_FORMAT_INDEX + 1);

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
        if (glFormatIsSupported(format)) {
            if (format < 0) {
                throw new IllegalArgumentException();
            }
            return FORMATS.computeIfAbsent(format | (isExternal ? Integer.MIN_VALUE : 0),
                    k -> new GLBackendFormat(k & Integer.MAX_VALUE, k < 0));
        }
        return new GLBackendFormat(format, isExternal);
    }

    @Override
    public int getBackend() {
        return Engine.BackendApi.kOpenGL;
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
        return glFormatChannels(mFormat);
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
        return glFormatIsSRGB(mFormat);
    }

    @Override
    public int getCompressionType() {
        return glFormatCompressionType(mFormat);
    }

    @Override
    public int getBytesPerBlock() {
        return glFormatBytesPerBlock(mFormat);
    }

    @Override
    public int getStencilBits() {
        return glFormatStencilBits(mFormat);
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
        return "{backend=OpenGL" +
                ", format=" + glFormatName(mFormat) +
                ", isExternal=" + mIsExternal +
                '}';
    }
}
