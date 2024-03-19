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
import icyllis.arc3d.engine.BackendTexture;
import org.lwjgl.opengl.GL11C;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.*;

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

    // Internally used by GLContext and GLTexture
    GLBackendTexture(int width, int height, GLTextureInfo info,
                     GLTextureParameters params, BackendFormat backendFormat) {
        super(width, height);
        mInfo = info;
        mParams = params;
        mBackendFormat = backendFormat;
    }

    @Override
    public int getBackend() {
        return BackendApi.kOpenGL;
    }

    @Override
    public int getTextureType() {
        if (mInfo.target == GL11C.GL_TEXTURE_2D) {
            return TextureType.k2D;
        }
        return TextureType.kNone;
    }

    @Override
    public boolean isExternal() {
        return mBackendFormat.isExternal();
    }

    @Override
    public boolean isMipmapped() {
        return mInfo.levels > 1;
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
            return mInfo.handle == t.mInfo.handle;
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
