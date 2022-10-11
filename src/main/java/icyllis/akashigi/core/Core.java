/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.core;

/**
 * Constants and utilities for Core.
 */
public final class Core {

    /**
     * Surface flags.
     *
     * <ul>
     * <li>{@link #SURFACE_FLAG_BUDGETED} -
     *  Indicates whether an allocation should count against a cache budget. Budgeted when
     *  set, otherwise not budgeted.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_LOOSE_FIT} -
     *  Indicates whether a backing store needs to be an exact match or can be larger than
     *  is strictly necessary. Loose fit when set, otherwise exact fit.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_MIPMAPPED} -
     *  Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
     *  when set, otherwise mipmaps are not allocated.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_RENDERABLE} -
     *  Used to say whether a surface can be rendered to, whether a texture can be used as
     *  color attachments. Renderable when set, otherwise not renderable.
     * </li>
     *
     * <li>{@link #SURFACE_FLAG_PROTECTED} -
     *  Used to say whether texture is backed by protected memory. Protected when set, otherwise
     *  not protected.
     * </li>
     * </ul>
     */
    public static final int
            SURFACE_FLAG_NONE = 0,
            SURFACE_FLAG_BUDGETED = 1,
            SURFACE_FLAG_LOOSE_FIT = 1 << 1,
            SURFACE_FLAG_MIPMAPPED = 1 << 2,
            SURFACE_FLAG_RENDERABLE = 1 << 3,
            SURFACE_FLAG_PROTECTED = 1 << 4;
}
