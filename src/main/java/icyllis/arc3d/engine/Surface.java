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

package icyllis.arc3d.engine;

import javax.annotation.Nonnull;

/**
 * Common interface between {@link Texture} and {@link RenderSurface}.
 */
public interface Surface {

    /**
     * Increases the reference count by 1 on the client pipeline.
     */
    void ref();

    /**
     * Decreases the reference count by 1 on the client pipeline.
     */
    void unref();

    /**
     * @return the width of the surface in pixels, greater than zero
     */
    int getWidth();

    /**
     * @return the height of the surface in pixels, greater than zero
     */
    int getHeight();

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     * Always returns one if the surface is a non-renderable {@link Texture}.
     *
     * @return the number of samples, greater than (multi-sampled) or equal to one
     */
    int getSampleCount();

    /**
     * @return the backend format of the surface
     */
    @Nonnull
    BackendFormat getBackendFormat();

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link Engine.SurfaceFlags#Budgeted} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link Texture} only.
     * </li>
     *
     * <li>{@link Engine.SurfaceFlags#Mipmapped} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Texture} only.
     * </li>
     *
     * <li>{@link Engine.SurfaceFlags#Renderable} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link Engine.SurfaceFlags#Protected} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link Engine.SurfaceFlags#READ_ONLY} -
     *  Means the pixels in the texture are read-only. {@link Texture} only.
     * </li>
     *
     * @return combination of the above flags
     */
    int getSurfaceFlags();

    /**
     * Returns null when this surface is a non-renderable {@link Texture}.
     *
     * @return raw ptr to the render target associated with the surface, may be null
     */
    RenderTarget getRenderTarget();
}
