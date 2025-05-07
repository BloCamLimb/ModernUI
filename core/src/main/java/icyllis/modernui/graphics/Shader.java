/*
 * Modern UI.
 * Copyright (C) 2024-2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.lang.ref.Cleaner;

/**
 * Shaders specify the source color(s) for what is being drawn. If a paint
 * has no shader, then the paint's color is used. If the paint has a
 * shader, then the shader's color(s) are use instead, but they are
 * modulated by the paint's alpha. This makes it easy to create a shader
 * once (e.g. image tiling or gradient) and then change its transparency
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
        REPEAT(icyllis.arc3d.sketch.shaders.Shader.TILE_MODE_REPEAT),
        /**
         * Repeat the shader's image horizontally and vertically, alternating
         * mirror images so that adjacent images always seam.
         */
        MIRROR(icyllis.arc3d.sketch.shaders.Shader.TILE_MODE_MIRROR),
        /**
         * Replicate the edge color if the shader draws outside of its
         * original bounds.
         */
        CLAMP(icyllis.arc3d.sketch.shaders.Shader.TILE_MODE_CLAMP),
        /**
         * Render the shader's image pixels only within its original bounds. If the shader
         * draws outside of its original bounds, transparent black is drawn instead.
         */
        DECAL(icyllis.arc3d.sketch.shaders.Shader.TILE_MODE_DECAL);

        /**
         * @hidden
         */
        @ApiStatus.Internal
        public final int nativeInt;

        TileMode(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

    // closed by cleaner
    @Nullable
    volatile icyllis.arc3d.sketch.shaders.Shader mShader;
    Cleaner.Cleanable mCleanup;

    /**
     * Perform a deferred cleanup if the underlying resource is not released.
     * Manually mark the underlying resources closed, if needed. After this operation,
     * this shader represents a no-op, and its GPU resource will be reclaimed
     * as soon as possible after use.
     * <p>
     * This will not affect shaders already installed on the Paint, until the Paint
     * is reset or closed.
     * <p>
     * When this object becomes phantom-reachable, the system will automatically
     * do this cleanup operation.
     */
    public void release() {
        // order is important
        mShader = null;
        // cleaner is thread safe
        if (mCleanup != null) {
            mCleanup.clean();
        }
    }

    /**
     * Returns null if this shader is no-op.
     *
     * @hidden
     */
    @ApiStatus.Internal
    @icyllis.arc3d.core.RawPtr
    public icyllis.arc3d.sketch.shaders.Shader getNativeShader() {
        return mShader;
    }
}
