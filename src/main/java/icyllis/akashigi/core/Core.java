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
     */
    public static class SurfaceFlags {

        public static final int kNone = 0;
        /**
         * Indicates whether an allocation should count against a cache budget. Budgeted when
         * set, otherwise not budgeted.
         */
        public static final int kBudgeted = 1;
        /**
         * Indicates whether a backing store needs to be an exact match or can be larger than
         * is strictly necessary. Loose fit when set, otherwise exact fit.
         */
        public static final int kLooseFit = 1 << 1;
        /**
         * Used to say whether a texture has mip levels allocated or not. Mipmaps are allocated
         * when set, otherwise mipmaps are not allocated.
         */
        public static final int kMipmapped = 1 << 2;
        /**
         * Used to say whether a surface can be rendered to, whether a texture can be used as
         * color attachments. Renderable when set, otherwise not renderable.
         */
        public static final int kRenderable = 1 << 3;
        /**
         * Used to say whether texture is backed by protected memory. Protected when set, otherwise
         * not protected.
         */
        public static final int kProtected = 1 << 4;

        protected SurfaceFlags() {
        }
    }
}
