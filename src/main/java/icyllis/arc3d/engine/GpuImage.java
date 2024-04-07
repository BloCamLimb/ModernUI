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

package icyllis.arc3d.engine;

import javax.annotation.Nonnull;

/**
 * Interface representing GPU images, which may be 2D or 3D.
 * <p>
 * A {@link GpuImage} may or may not be sampled by shaders, may be used as
 * color/depth/stencil attachments of a framebuffer. See {@link ISurface#FLAG_TEXTURABLE}
 * and {@link ISurface#FLAG_RENDERABLE}.
 */
public sealed interface GpuImage extends GpuSurface permits GpuImageBase, GpuTexture {

    /**
     * @return true if this surface has mipmaps and have been allocated
     */
    boolean isMipmapped();

    /**
     * @return number of mipmap levels, greater than 1 if mipmapped
     */
    int getMipLevelCount();

    /**
     * The pixel values of this surface cannot be modified (e.g. doesn't support write pixels or
     * mipmap regeneration). To be exact, only wrapped textures, external textures, stencil
     * attachments and MSAA color attachments can be read only.
     *
     * @return true if pixels in this surface are read-only
     */
    boolean isReadOnly();

    @Override
    default GpuImage asImage() {
        return this;
    }

    /**
     * Storage key of {@link GpuImage}, may be compared with {@link TextureProxy}.
     */
    final class ScratchKey implements IScratchKey {

        public int mWidth;
        public int mHeight;
        public int mFormat;
        public int mFlags;

        /**
         * Update this key with the given arguments, format can not be compressed.
         *
         * @return this
         */
        @Nonnull
        public ScratchKey compute(BackendFormat format,
                                  int width, int height,
                                  int sampleCount,
                                  int surfaceFlags) {
            assert (width > 0 && height > 0);
            assert (!format.isCompressed());
            mWidth = width;
            mHeight = height;
            mFormat = format.getFormatKey();
            mFlags = (surfaceFlags & (ISurface.FLAG_MIPMAPPED |
                    ISurface.FLAG_TEXTURABLE |
                    ISurface.FLAG_RENDERABLE |
                    ISurface.FLAG_MEMORYLESS |
                    ISurface.FLAG_PROTECTED)) | (sampleCount << 16);
            return this;
        }

        /**
         * Keep {@link TextureProxy#hashCode()} sync with this.
         */
        @Override
        public int hashCode() {
            int result = mWidth;
            result = 31 * result + mHeight;
            result = 31 * result + mFormat;
            result = 31 * result + mFlags;
            return result;
        }

        /**
         * Keep {@link TextureProxy#equals(Object)}} sync with this.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof ScratchKey key &&
                    mWidth == key.mWidth &&
                    mHeight == key.mHeight &&
                    mFormat == key.mFormat &&
                    mFlags == key.mFlags;
        }
    }
}
