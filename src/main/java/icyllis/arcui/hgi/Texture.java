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

import javax.annotation.Nonnull;

/**
 * Represents 2D textures can be sampled by shaders, can also be used as attachments
 * of render targets.
 * <p>
 * By default, a Texture is not renderable (not created with a RenderTarget), all
 * mipmaps (including the base level) are dirty. But it can be promoted to renderable
 * whenever needed (i.e. lazy initialization), then we call it a RenderTexture or
 * TextureRenderTarget. The texture will be the main color buffer of the single
 * sample framebuffer of the render target. So we can cache these framebuffers with
 * texture. With promotion, the scratch key is changed and the sample count (MSAA)
 * is locked. Additionally, it may create more surfaces and attach them to it. These
 * surfaces are budgeted but cannot be reused. In most cases, we reuse textures, so
 * these surfaces are reused together. When renderable is not required, the cache
 * will give priority to the texture without promotion. See {@link RenderTargetProxy}.
 */
public abstract class Texture extends Surface {

    private final int mTextureType;
    private final int mMaxMipmapLevel;

    private int mMipmapStatus;
    private boolean mReadOnly;

    @SmartPtr
    private ReleaseCallback mReleaseCallback;

    protected Texture(Server server,
                      int width, int height,
                      int textureType,
                      int mipmapStatus) {
        super(server, width, height);
        mTextureType = textureType;
        mMipmapStatus = mipmapStatus;
        if (mipmapStatus == Types.MIPMAP_STATUS_NONE) {
            mMaxMipmapLevel = 0;
        } else {
            mMaxMipmapLevel = 31 - Integer.numberOfLeadingZeros(Math.max(mWidth, mHeight));
        }
        if (textureType == Types.TEXTURE_TYPE_EXTERNAL) {
            setReadOnly();
        }
    }

    /**
     * The pixel values of this texture cannot be modified (e.g. doesn't support write pixels or
     * mipmap regeneration). To be exact, only wrapped textures, external textures can be read only.
     *
     * @return true if pixels in this texture are read-only
     */
    @Override
    public final boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * Determined by subclass constructors.
     */
    protected final void setReadOnly() {
        mReadOnly = true;
    }

    /**
     * @return either {@link Types#TEXTURE_TYPE_2D} or {@link Types#TEXTURE_TYPE_EXTERNAL}
     */
    public final int getTextureType() {
        return mTextureType;
    }

    /**
     * @return the backend texture of this texture
     */
    @Nonnull
    public abstract BackendTexture getBackendTexture();

    /**
     * @return true if mipmaps have been allocated, or called has mipmaps
     */
    @Override
    public final boolean isMipmapped() {
        return mMipmapStatus != Types.MIPMAP_STATUS_NONE;
    }

    public final boolean areMipmapsDirty() {
        return mMipmapStatus == Types.MIPMAP_STATUS_DIRTY;
    }

    public final void markMipmapsDirty() {
        assert isMipmapped();
        mMipmapStatus = Types.MIPMAP_STATUS_DIRTY;
    }

    public final void markMipmapsClean() {
        assert isMipmapped();
        mMipmapStatus = Types.MIPMAP_STATUS_VALID;
    }

    public final int getMipmapStatus() {
        return mMipmapStatus;
    }

    public final int getMaxMipmapLevel() {
        return mMaxMipmapLevel;
    }

    /**
     * Unmanaged backends (e.g. Vulkan) may want to specially handle the release proc in order to
     * ensure it isn't called until GPU work related to the resource is completed.
     */
    public void setReleaseCallback(@SmartPtr ReleaseCallback callback) {
        if (mReleaseCallback != null) {
            mReleaseCallback.unref();
        }
        mReleaseCallback = callback;
    }

    /**
     * @return surface flags, texture are color attachments so no framebuffer-related flags
     */
    public final int getFlags() {
        int flags = 0;
        if (isReadOnly()) {
            flags |= Types.INTERNAL_SURFACE_FLAG_READ_ONLY;
        }
        if (isProtected()) {
            flags |= Types.INTERNAL_SURFACE_FLAG_PROTECTED;
        }
        return flags;
    }

    @Override
    protected void onFree() {
        if (mReleaseCallback != null) {
            mReleaseCallback.unref();
        }
        mReleaseCallback = null;
    }

    @Override
    protected void onDrop() {
        if (mReleaseCallback != null) {
            mReleaseCallback.unref();
        }
        mReleaseCallback = null;
    }

    public static long computeSize(BackendFormat format,
                                   int width, int height,
                                   int sampleCount,
                                   boolean mipmapped,
                                   boolean approx) {
        assert width > 0 && height > 0;
        assert sampleCount > 0;
        assert sampleCount == 1 || !mipmapped;
        // For external formats we do not actually know the real size of the resource, so we just return
        // 0 here to indicate this.
        if (format.getTextureType() == Types.TEXTURE_TYPE_EXTERNAL) {
            return 0;
        }
        if (approx) {
            width = ResourceProvider.makeApprox(width);
            height = ResourceProvider.makeApprox(height);
        }
        long size = DataUtils.numBlocks(format.getCompressionType(), width, height) *
                format.getBytesPerBlock();
        assert size > 0;
        if (mipmapped) {
            size *= 4.0 / 3.0;
        } else {
            size *= sampleCount;
        }
        return size;
    }
}
