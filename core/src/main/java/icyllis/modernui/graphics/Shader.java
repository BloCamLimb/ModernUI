/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics;

import icyllis.arc3d.core.RawPtr;
import org.jetbrains.annotations.ApiStatus;

/**
 * Shaders specify the source color(s) for what is being drawn. If a paint
 * has no shader, then the paint's color is used. If the paint has a
 * shader, then the shader's color(s) are use instead, but they are
 * modulated by the paint's alpha. This makes it easy to create a shader
 * once (e.g. bitmap tiling or gradient) and then change its transparency
 * w/o having to modify the original shader... only the paint's alpha needs
 * to be modified.
 *
 * @since 3.11
 */
public abstract class Shader {

    /**
     * Tile modes, also known as address modes and wrap modes. The tile mode specifies
     * behavior of sampling with texture coordinates outside the image bounds.
     */
    public enum TileMode {
        /**
         * Repeat the shader's image horizontally and vertically.
         */
        REPEAT(icyllis.arc3d.core.shaders.Shader.TILE_MODE_REPEAT),
        /**
         * Repeat the shader's image horizontally and vertically, alternating
         * mirror images so that adjacent images always seam.
         */
        MIRROR(icyllis.arc3d.core.shaders.Shader.TILE_MODE_MIRROR),
        /**
         * Replicate the edge color if the shader draws outside of its
         * original bounds.
         */
        CLAMP(icyllis.arc3d.core.shaders.Shader.TILE_MODE_CLAMP),
        /**
         * Render the shader's image pixels only within its original bounds. If the shader
         * draws outside of its original bounds, transparent black is drawn instead.
         */
        DECAL(icyllis.arc3d.core.shaders.Shader.TILE_MODE_DECAL);

        final int nativeInt;

        TileMode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    /**
     * Returns null if this shader is no-op.
     *
     * @hidden
     */
    @ApiStatus.Internal
    @RawPtr
    public abstract icyllis.arc3d.core.shaders.Shader getNativeShader();
}
