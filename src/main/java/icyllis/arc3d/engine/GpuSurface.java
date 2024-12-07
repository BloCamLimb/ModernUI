/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.RawPtr;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Interface representing GPU surfaces.
 * <p>
 * There are two implementations: one is {@link Image}, which contains image data and
 * allocates memory; and the other is {@link GpuRenderTarget}, which is a container object
 * that represents a combination of {@link Image}s as attachments, and managed all objects
 * used by the rendering pipeline for the fixed combination of attachments (compatible render passes
 * and framebuffers).
 *
 * @see SurfaceProxy
 */
@Deprecated
public abstract class GpuSurface extends Resource {

    protected GpuSurface(Context context,
                         boolean budgeted,
                         boolean wrapped,
                         long memorySize) {
        super(context, budgeted, wrapped, memorySize);
    }

    /**
     * @return the width of the surface in pixels, greater than zero
     */
    public abstract int getWidth();

    /**
     * @return the height of the surface in pixels, greater than zero
     */
    public abstract int getHeight();

    /**
     * Returns the backend format of the surface.
     * <p>
     * If this is RT, returns the backend format of color attachment 0.
     */
    @NonNull
    public BackendFormat getBackendFormat() {
        return null;
    }

    public abstract int getDepthBits();

    public abstract int getStencilBits();

    /**
     * Returns the number of samples per pixel in color buffers (one if non-MSAA).
     *
     * @return the number of samples, greater than (multi-sampled) or equal to one
     */
    public abstract int getSampleCount();

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link ISurface#FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted.
     * </li>
     *
     * <li>{@link ISurface#FLAG_MIPMAPPED} -
     *  Used to say whether an image has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link Image} only.
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
     *  Means the pixels in the image are read-only. Non-renderable {@link Image} only.
     * </li>
     *
     * @return combination of the above flags
     */
    public abstract int getSurfaceFlags();

    /**
     * @return true if we are working with protected content
     */
    public abstract boolean isProtected();

    /**
     * If this object is image, returns this.
     * <p>
     * If this object is RT, returns the resolve attachment 0 if available,
     * or returns the color attachment 0 if available, or null.
     *
     * @return raw ptr to the image
     */
    @RawPtr
    public Image asImage() {
        return null;
    }

    /**
     * If this object is RT, returns this.
     * <p>
     * If this object is image, returns null.
     *
     * @return raw ptr to this
     */
    @RawPtr
    public GpuRenderTarget asRenderTarget() {
        return null;
    }
}
