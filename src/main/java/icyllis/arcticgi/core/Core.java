/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.core;

/**
 * Constants and utilities for Core.
 */
public final class Core {

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link #SurfaceFlag_Budgeted} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted.
     * </li>
     *
     * <li>{@link #SurfaceFlag_BackingFit} -
     *  Indicates whether a backing store needs to be an exact match or can be larger than
     *  is strictly necessary. Exact when set, otherwise approx.
     * </li>
     *
     * <li>{@link #SurfaceFlag_Mipmapped} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated.
     * </li>
     *
     * <li>{@link #SurfaceFlag_Renderable} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #SurfaceFlag_Protected} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     * </ul>
     */
    public static final int
            SurfaceFlag_Default = 0,
            SurfaceFlag_Budgeted = 1,
            SurfaceFlag_BackingFit = 1 << 1,
            SurfaceFlag_Mipmapped = 1 << 2,
            SurfaceFlag_Renderable = 1 << 3,
            SurfaceFlag_Protected = 1 << 4;
}
