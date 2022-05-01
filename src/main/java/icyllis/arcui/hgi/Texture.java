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

    private final boolean mReadOnly;

    public Texture(Server server, int width, int height, boolean isReadOnly) {
        super(server, width, height);
        mReadOnly = isReadOnly;
    }

    public int getTextureType() {
        return 0;
    }

    /**
     * Describes the backend texture of this texture.
     */
    @Nonnull
    public abstract BackendTexture getBackendTexture();

    public abstract boolean isMipmapped();

    public int getMipmapStatus() {
        return 0;
    }

    @Override
    public final boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * @return surface flags
     */
    public final int getFlags() {
        int flags = 0;
        if (mReadOnly) {
            flags |= Types.INTERNAL_SURFACE_FLAG_READ_ONLY;
        }
        if (isProtected()) {
            flags |= Types.INTERNAL_SURFACE_FLAG_PROTECTED;
        }
        return flags;
    }

    @Override
    protected Object computeScratchKey() {
        BackendFormat format = getBackendFormat();
        if (format.isCompressed()) {
            return super.computeScratchKey();
        }
        return computeScratchKey(format, mWidth, mHeight, false, 1,
                isMipmapped(), isProtected(), false);
    }

    public static final ThreadLocal<Key> sThreadLocalKey = ThreadLocal.withInitial(Key::new);

    @Nonnull
    public static Object computeScratchKey(BackendFormat format,
                                           int width, int height,
                                           boolean renderable,
                                           int samples,
                                           boolean mipmapped,
                                           boolean isProtected,
                                           boolean lookup) {
        assert width > 0 && height > 0;
        assert samples > 0;
        assert samples == 1 || renderable;
        Key key = lookup ? sThreadLocalKey.get() : new Key();
        key.mWidth = width;
        key.mHeight = height;
        key.mFormat = format.getFormatKey();
        key.mFlags = (mipmapped ? 1 : 0) | (isProtected ? 2 : 0) | (renderable ? 4 : 0) | (samples << 3);
        return key;
    }

    private static class Key {

        private int mWidth;
        private int mHeight;
        private int mFormat;
        private int mFlags;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            if (mWidth != key.mWidth) return false;
            if (mHeight != key.mHeight) return false;
            if (mFormat != key.mFormat) return false;
            return mFlags == key.mFlags;
        }

        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mFormat;
            result = 31 * result + mFlags;
            return result;
        }
    }
}
