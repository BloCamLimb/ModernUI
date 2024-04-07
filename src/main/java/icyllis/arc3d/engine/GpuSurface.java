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

import icyllis.arc3d.core.RawPtr;

import javax.annotation.Nonnull;

/**
 * Interface representing GPU surfaces.
 * <p>
 * There are two implementations: one is {@link GpuImage}, which contains image data and
 * allocates memory; and the other is {@link GpuFramebuffer}, which is a container object.
 *
 * @see SurfaceProxy
 */
public sealed interface GpuSurface extends GpuResource permits GpuImage, GpuFramebuffer {

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
     * Returns the backend format of the surface.
     * <p>
     * If this is framebuffer, returns the backend format of color attachment 0.
     */
    @Nonnull
    BackendFormat getBackendFormat();

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     *
     * @return the number of samples, greater than (multi-sampled) or equal to one
     */
    int getSampleCount();

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link ISurface#FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link GpuImage} or {@link GpuTexture} only.
     * </li>
     *
     * <li>{@link ISurface#FLAG_MIPMAPPED} -
     *  Used to say whether an image has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link GpuImage} or {@link GpuTexture} only.
     * </li>
     *
     * <li>{@link ISurface#FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether an image can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link ISurface#FLAG_PROTECTED} -
     *  Used to say whether image is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link ISurface#FLAG_READ_ONLY} -
     *  Means the pixels in the image are read-only. Non-renderable {@link GpuImage} only.
     * </li>
     *
     * @return combination of the above flags
     */
    int getSurfaceFlags();

    /**
     * @return true if we are working with protected content
     */
    boolean isProtected();

    /**
     * If this object is image, returns this.
     * <p>
     * If this object is framebuffer, returns the resolve attachment 0 if available,
     * or returns the color attachment 0 if available, or null.
     *
     * @return raw ptr to the image
     */
    @RawPtr
    default GpuImage asImage() {
        return null;
    }

    /**
     * If this object is texture, returns this.
     * <p>
     * If this object is framebuffer, returns the resolve attachment 0 if available,
     * or returns the color attachment 0 if available, or null.
     *
     * @return raw ptr to the texture
     */
    @RawPtr
    default GpuTexture asTexture() {
        return null;
    }

    /**
     * If this object is framebuffer, returns this.
     * <p>
     * If this object is image, returns null.
     *
     * @return raw ptr to this
     */
    @RawPtr
    default GpuFramebuffer asFramebuffer() {
        return null;
    }
}
