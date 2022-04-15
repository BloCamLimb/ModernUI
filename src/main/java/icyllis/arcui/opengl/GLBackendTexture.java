/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.opengl;

import icyllis.arcui.graphics.*;

import javax.annotation.Nonnull;

public final class GLBackendTexture extends BackendTexture {

    private final boolean mMipmapped;

    private final GLTextureInfo mInfo;
    final GLTextureParameters mParams;

    // lazy
    private GLBackendFormat mBackendFormat;

    // The GLTextureInfo must have a valid mFormat, can NOT be modified anymore.
    public GLBackendTexture(int width, int height, boolean mipmapped, GLTextureInfo info) {
        this(width, height, mipmapped, info, new GLTextureParameters());
        // Make no assumptions about client's texture's parameters.
        glTextureParametersModified();
    }

    GLBackendTexture(int width, int height, boolean mipmapped, GLTextureInfo info, GLTextureParameters params) {
        super(width, height);
        mMipmapped = mipmapped;
        mInfo = info;
        mParams = params;
    }

    @Override
    public int getBackend() {
        return Types.OPENGL;
    }

    @Override
    public boolean hasMipmaps() {
        return mMipmapped;
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
        if (mBackendFormat == null) {
            mBackendFormat = new GLBackendFormat(mInfo.mFormat, mInfo.mTarget);
        }
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isSameTexture(BackendTexture texture) {
        if (texture instanceof GLBackendTexture t) {
            return mInfo.mID == t.mInfo.mID;
        }
        return false;
    }
}
