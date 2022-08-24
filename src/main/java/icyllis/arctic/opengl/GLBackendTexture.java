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

package icyllis.arctic.opengl;

import icyllis.arctic.engine.*;

import javax.annotation.Nonnull;

public final class GLBackendTexture extends BackendTexture {

    private final GLTextureInfo mInfo;
    final GLTextureParameters mParams;

    private final BackendFormat mBackendFormat;

    // The GLTextureInfo must have a valid mFormat, can NOT be modified anymore.
    public GLBackendTexture(int width, int height, GLTextureInfo info) {
        this(width, height, info, new GLTextureParameters(), BackendFormat.makeGL(info.mFormat,
                info.mMemoryHandle != -1 ? EngineTypes.TextureType_External : EngineTypes.TextureType_2D));
        assert info.mFormat != 0;
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
        return EngineTypes.OpenGL;
    }

    @Override
    public int getTextureType() {
        return mBackendFormat.textureType();
    }

    @Override
    public boolean isMipmapped() {
        return mInfo.mLevelCount > 1;
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
            return mInfo.mTexture == t.mInfo.mTexture;
        }
        return false;
    }
}
