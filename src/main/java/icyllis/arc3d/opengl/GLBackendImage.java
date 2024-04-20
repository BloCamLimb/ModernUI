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
import icyllis.arc3d.engine.BackendImage;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.*;

/**
 * When importing external memory,
 * {@link #memoryHandle} is POSIX file descriptor or Win32 NT handle. {@link #memoryObject} is
 * OpenGL memory object. If it is an NT handle, it must be released manually by the memory exporter
 * (e.g. Vulkan).
 */
public final class GLBackendImage extends BackendImage {

    private final GLImageInfo mInfo;
    // Null for renderbuffers.
    final GLImageMutableState mParams;

    /**
     * <code>GLuint</code> - image name
     */
    public int handle;

    /**
     * <code>GLsizei</code> - number of mip levels
     */
    public int levels = 0;

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

    private final BackendFormat mBackendFormat;

    // The GLTextureInfo must have a valid mFormat, can NOT be modified anymore.
    public GLBackendImage(int width, int height, GLImageInfo info) {
        this(width, height, info, new GLImageMutableState(), GLBackendFormat.make(info.mFormat));
        assert info.mFormat != 0;
        // Make no assumptions about client's texture's parameters.
        glTextureParametersModified();
    }

    // Internally used by GLContext and GLTexture
    GLBackendImage(int width, int height, GLImageInfo info,
                   GLImageMutableState params, BackendFormat backendFormat) {
        super(info, params);
        mInfo = info;
        mParams = params;
        mBackendFormat = backendFormat;
    }

    @Override
    public int getBackend() {
        return BackendApi.kOpenGL;
    }

    @Override
    public boolean isExternal() {
        return mBackendFormat.isExternal();
    }

    /*
     * Copies a snapshot of the {@link GLImageInfo} struct into the passed in pointer.
     */
    /*public void getGLImageInfo(GLImageInfo info) {
        info.set(mInfo);
    }*/

    public GLImageInfo getGLImageInfo() {
        return mInfo;
    }

    @Override
    public void glTextureParametersModified() {
        if (mParams != null) {
            mParams.invalidate();
        }
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isSameImage(BackendImage image) {
        if (image instanceof GLBackendImage that) {
            return handle == that.handle;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" +
                "mBackend=OpenGL" +
                ", mInfo=" + mInfo +
                ", mParams=" + mParams +
                ", mBackendFormat=" + mBackendFormat +
                '}';
    }
}
