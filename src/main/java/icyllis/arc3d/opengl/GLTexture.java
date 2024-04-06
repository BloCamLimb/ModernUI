/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.engine.*;

import javax.annotation.Nonnull;

/**
 * Represents OpenGL textures.
 */
public final class GLTexture extends GLImage implements GpuTexture {

    private final GLBackendTexture mBackendTexture;

    GLTexture(GLDevice device,
              int width, int height,
              GLImageInfo info,
              BackendFormat format,
              int flags) {
        super(device, width, height, info, format, flags);
        assert info.handle != 0;
        mBackendTexture = new GLBackendTexture(width, height, info, new GLTextureParameters(), format);
        if (mBackendTexture.isMipmapped()) {
            mFlags |= ISurface.FLAG_MIPMAPPED;
        }
    }

    public GLTexture(GLDevice device,
                     int width, int height,
                     GLImageInfo info,
                     GLTextureParameters params,
                     BackendFormat format,
                     int ioType,
                     boolean cacheable,
                     boolean ownership) {
        super(device, width, height, info, format, ioType, cacheable, ownership);
        mBackendTexture = new GLBackendTexture(width, height, info, params, format);
    }

    @Nonnull
    @Override
    public BackendTexture getBackendTexture() {
        return mBackendTexture;
    }

    @Nonnull
    public GLTextureParameters getParameters() {
        return mBackendTexture.mParams;
    }

    @Override
    public boolean isExternal() {
        return mBackendTexture.isExternal();
    }
}
