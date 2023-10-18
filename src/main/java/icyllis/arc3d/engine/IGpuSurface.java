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

/**
 * Interface representing primary surface data.
 */
public interface IGpuSurface extends ISurface {

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link #FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted. {@link GpuTexture} or RenderTexture only.
     * </li>
     *
     * <li>{@link #FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated. {@link GpuTexture} or RenderTexture only.
     * </li>
     *
     * <li>{@link #FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #FLAG_PROTECTED} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     *
     * <li>{@link #FLAG_READ_ONLY} -
     *  Means the pixels in the texture are read-only. Non-renderable {@link GpuTexture} only.
     * </li>
     *
     * @return combination of the above flags
     */
    int getSurfaceFlags();

    /**
     * If this object is texture, returns this.
     * <p>
     * If this object is framebuffer, returns the associated color buffer 0 if available,
     * or null (only OpenGL can return null).
     *
     * @return raw ptr to the texture
     */
    default GpuTexture asTexture() {
        return null;
    }

    ///// Common interface between RenderTexture and RenderSurface
    ///// The following methods are only valid when FLAG_RENDERABLE is set

    /**
     * If this object is framebuffer, returns this.
     * <p>
     * If this object is texture, returns the associated framebuffer if renderable, otherwise null.
     *
     * @return raw ptr to the framebuffer set associated with the RT, or null if non-renderable
     */
    default GpuRenderTarget asRenderTarget() {
        return null;
    }
}
