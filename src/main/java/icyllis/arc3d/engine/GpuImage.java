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
}
