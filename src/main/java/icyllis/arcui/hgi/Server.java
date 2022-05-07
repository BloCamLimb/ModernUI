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

package icyllis.arcui.hgi;

import icyllis.arcui.sksl.ShaderCompiler;

import javax.annotation.Nullable;

/**
 * Represents the application-controlled 3D API server, holding a reference
 * to DirectContext. It is responsible for creating / deleting 3D API objects,
 * controlling binding status, uploading and downloading data, transferring
 * 3D API draw commands, etc.
 */
public abstract class Server {

    final DirectContext mContext;
    final Caps mCaps;
    final ShaderCompiler mCompiler;

    public Server(DirectContext context, Caps caps) {
        assert context != null && caps != null;
        mContext = context;
        mCaps = caps;
        mCompiler = new ShaderCompiler(caps.mShaderCaps);
    }

    public final DirectContext getContext() {
        return mContext;
    }

    /**
     * Gets the capabilities of the draw target.
     */
    public final Caps getCaps() {
        return mCaps;
    }

    public final ShaderCompiler getShaderCompiler() {
        return mCompiler;
    }

    public abstract ThreadSafePipelineBuilder getPipelineBuilder();

    /**
     * Creates a texture object and allocates its server memory. In other words, the
     * image data is dirty and needs to be uploaded later. If mipmapped, also allocates
     * <code>(31 - CLZ(max(width,height)))</code> mipmaps in addition to the base level.
     * NPoT (non-power-of-two) dimensions are always supported. Compressed format are
     * supported.
     *
     * @param width       the width of the texture to be created
     * @param height      the height of the texture to be created
     * @param format      the backend format for the texture
     * @param mipmapped   should the texture be allocated with mipmaps
     * @param budgeted    should the texture count against the resource cache budget
     * @param isProtected should the texture be created as protected
     * @return the referenced texture object if successful, otherwise nullptr
     */
    @Nullable
    public final Texture createTexture(int width, int height,
                                       BackendFormat format,
                                       boolean mipmapped,
                                       boolean budgeted,
                                       boolean isProtected) {
        if (format.isCompressed()) {
            return null;
        }
        if (!mCaps.validateTextureParams(width, height, format)) {
            return null;
        }
        return onCreateTexture(width, height, format, budgeted, isProtected, mipmapped ?
                Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(width, height)) : 1);
    }

    /**
     * Overridden by backend-specific derived class to create objects.
     * <p>
     * Texture size and format support will have already been validated in base class
     * before onCreateTexture is called.
     */
    @Nullable
    protected abstract Texture onCreateTexture(int width, int height,
                                               BackendFormat format,
                                               boolean budgeted,
                                               boolean isProtected,
                                               int mipLevels);
}
