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

package icyllis.arcui.gl;

import icyllis.arcui.hgi.*;

import javax.annotation.Nonnull;

import static icyllis.arcui.gl.GLCore.glFormatIsCompressed;

/**
 * Represents OpenGL 2D textures, can be used as textures and color attachments.
 */
public final class GLTexture extends Texture {

    private final GLTextureParameters mParameters;

    private int mTexture;
    private final int mFormat;
    private final boolean mOwnership;

    private final GLBackendTexture mBackendTexture;

    private final long mMemorySize;

    public GLTexture(GLServer server,
                     int width, int height,
                     int texture,
                     BackendFormat backendFormat,
                     int mipmapStatus,
                     boolean budgeted,
                     boolean ownership) {
        super(server, width, height, Types.TEXTURE_TYPE_2D, mipmapStatus);
        mTexture = texture;
        mFormat = backendFormat.getGLFormat();
        mParameters = new GLTextureParameters();
        mOwnership = ownership;
        if (glFormatIsCompressed(mFormat)) {
            setReadOnly();
        }

        final GLTextureInfo info = new GLTextureInfo();
        info.mTexture = texture;
        info.mFormat = mFormat;
        info.mLevelCount = getMaxMipmapLevel() + 1;
        mBackendTexture = new GLBackendTexture(mWidth, mHeight, info, mParameters, backendFormat);

        mMemorySize = computeSize(backendFormat, width, height, getSampleCount(), isMipmapped(), false);

        registerWithCache(budgeted);
    }

    /**
     * We have no multisample textures, but multisample renderbuffers.
     */
    @Override
    public int getSampleCount() {
        return 1;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendTexture.getBackendFormat();
    }

    @Nonnull
    @Override
    public BackendTexture getBackendTexture() {
        return mBackendTexture;
    }

    @Override
    public long getMemorySize() {
        return mMemorySize;
    }

    @Override
    protected void onFree() {
        super.onFree();
    }

    @Override
    protected void onDrop() {
        super.onDrop();
    }
}
