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

/**
 * Types for interacting with GL resources created externally to pipeline. BackendObjects for GL
 * textures are really const GLTexture*. The {@link #mFormat} here should be a sized, internal format
 * for the texture. We use the sized format since the base internal formats are deprecated.
 * <p>
 * Note the target is always {@link GLCore#GL_TEXTURE_2D}. When importing external memory,
 * {@link #mMemoryHandle} is POSIX file descriptor or Win32 NT handle (though <code>HANDLE</code>
 * is defined as <code>void*</code>, we can safely truncate it because Win32 handles are
 * 32-bit significant). {@link #mMemoryObject} is OpenGL memory object. If it is an NT handle,
 * it must be released manually by the memory exporter (e.g. Vulkan).
 * <p>
 * We only provide single-sample textures, multisample can be only renderbuffers.
 */
public final class GLTextureInfo {

    public int mTexture;
    public int mFormat;
    public int mLevelCount = 0;
    public int mMemoryObject;
    public int mMemoryHandle = -1;

    public void set(GLTextureInfo info) {
        mTexture = info.mTexture;
        mFormat = info.mFormat;
        mLevelCount = info.mLevelCount;
        mMemoryObject = info.mMemoryObject;
        mMemoryHandle = info.mMemoryHandle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GLTextureInfo that = (GLTextureInfo) o;
        if (mTexture != that.mTexture) return false;
        if (mFormat != that.mFormat) return false;
        if (mLevelCount != that.mLevelCount) return false;
        if (mMemoryObject != that.mMemoryObject) return false;
        return mMemoryHandle == that.mMemoryHandle;
    }

    @Override
    public int hashCode() {
        int result = mTexture;
        result = 31 * result + mFormat;
        result = 31 * result + mLevelCount;
        result = 31 * result + mMemoryObject;
        result = 31 * result + mMemoryHandle;
        return result;
    }

    @Override
    public String toString() {
        return "GLTextureInfo{" +
                "mTexture=" + mTexture +
                ", mFormat=" + mFormat +
                ", mLevelCount=" + mLevelCount +
                ", mMemoryObject=" + mMemoryObject +
                ", mMemoryHandle=" + mMemoryHandle +
                '}';
    }
}
