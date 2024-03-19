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

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

/**
 * Types for interacting with GL resources created externally to pipeline. BackendObjects for GL
 * textures are really const GLTexture*. The {@link #format} here should be a sized, internal format
 * for the texture. We use the sized format since the base internal formats are deprecated.
 * <p>
 * Note the target can be {@link GL30C#GL_RENDERBUFFER}. When importing external memory,
 * {@link #memoryHandle} is POSIX file descriptor or Win32 NT handle. {@link #memoryObject} is
 * OpenGL memory object. If it is an NT handle, it must be released manually by the memory exporter
 * (e.g. Vulkan).
 */
public final class GLTextureInfo {

    /**
     * <code>GLenum</code> - image namespace
     */
    public int target = GL11C.GL_TEXTURE_2D;
    /**
     * <code>GLuint</code> - image name
     */
    public int handle;
    /**
     * <code>GLenum</code> - sized internal format
     */
    public int format;
    /**
     * <code>GLsizei</code> - number of mip levels
     */
    public int levels = 0;
    /**
     * <code>GLsizei</code> - number of samples
     */
    public int samples = 0;
    /**
     * <code>GLuint</code> - memory
     */
    public int memoryObject;
    /**
     * <pre>{@code
     * union {
     *     int fd; // file descriptor
     *     HANDLE handle; // win32 handle
     * };
     * }</pre>
     */
    public long memoryHandle = -1;

    public void set(GLTextureInfo info) {
        target = info.target;
        handle = info.handle;
        format = info.format;
        levels = info.levels;
        samples = info.samples;
        memoryObject = info.memoryObject;
        memoryHandle = info.memoryHandle;
    }

    @Override
    public int hashCode() {
        int result = target;
        result = 31 * result + handle;
        result = 31 * result + format;
        result = 31 * result + levels;
        result = 31 * result + samples;
        result = 31 * result + memoryObject;
        result = 31 * result + (int) (memoryHandle ^ (memoryHandle >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof GLTextureInfo info)
            return target == info.target &&
                    handle == info.handle &&
                    format == info.format &&
                    levels == info.levels &&
                    samples == info.samples &&
                    memoryObject == info.memoryObject &&
                    memoryHandle == info.memoryHandle;
        return false;
    }

    @Override
    public String toString() {
        return '{' +
                "target=" + target +
                ", handle=" + handle +
                ", format=" + GLUtil.glFormatName(format) +
                ", levels=" + levels +
                ", samples=" + samples +
                ", memoryObject=" + memoryObject +
                ", memoryHandle=" + memoryHandle +
                '}';
    }
}
