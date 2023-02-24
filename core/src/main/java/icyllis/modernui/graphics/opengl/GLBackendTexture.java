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

package icyllis.modernui.graphics.opengl;

import icyllis.modernui.graphics.engine.*;

import javax.annotation.Nonnull;

public final class GLBackendTexture extends BackendTexture {

    private final GLTextureInfo mInfo;
    final GLTextureParameters mParams;

    private final BackendFormat mBackendFormat;

    // The GLTextureInfo must have a valid mFormat, can NOT be modified anymore.
    public GLBackendTexture(int width, int height, GLTextureInfo info) {
        this(width, height, info, new GLTextureParameters(), GLBackendFormat.make(info.format,
                info.memoryHandle != -1));
        assert info.format != 0;
        // Make no assumptions about client's texture's parameters.
        glTextureParametersModified();
    }

    // Internally used by GLServer and GLTexture
    GLBackendTexture(int width, int height, GLTextureInfo info,
                     GLTextureParameters params, BackendFormat backendFormat) {
        super(width, height);
        mInfo = info;
        mParams = params;
        mBackendFormat = backendFormat;
    }

    @Override
    public int getBackend() {
        return Engine.BackendApi.kOpenGL;
    }

    @Override
    public boolean isExternal() {
        return mBackendFormat.isExternal();
    }

    @Override
    public boolean isMipmapped() {
        return mInfo.levelCount > 1;
    }

    @Override
    public boolean getGLTextureInfo(GLTextureInfo info) {
        info.set(mInfo);
        return true;
    }

    @Override
    public void glTextureParametersModified() {
        mParams.invalidate();
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
    public boolean isSameTexture(BackendTexture texture) {
        if (texture instanceof GLBackendTexture t) {
            return mInfo.texture == t.mInfo.texture;
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
