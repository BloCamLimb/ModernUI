/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Core;

/**
 * A shader subclass that composites (blends) two shaders into one, with the given
 * {@link BlendMode composite mode}. This is used to construct a shader tree.
 *
 * @since 3.12
 */
public class ComposeShader extends Shader {

    /**
     * Create a new compose shader, given shaders source, destination, and a composite mode.
     * When the mode is applied, it will be given the result from shader src as its "src",
     * and the result from shader dst as its "dst".
     * <p>
     * You can release the given shaders immediately after creating the compose shader.
     *
     * @param mode The blend mode that combines the colors from the two shaders.
     * @param src  The colors from this shader are seen as the "src" by the mode
     * @param dst  The colors from this shader are seen as the "dst" by the mode
     */
    public ComposeShader(@NonNull BlendMode mode,
                         @NonNull Shader src, @NonNull Shader dst) {
        var shader = icyllis.arc3d.sketch.shaders.BlendShader.make(
                mode.getNativeBlendMode(),
                icyllis.arc3d.core.RefCnt.create(src.getNativeShader()),
                icyllis.arc3d.core.RefCnt.create(dst.getNativeShader())
        );
        if (shader == null) {
            throw new IllegalStateException("unreachable");
        }
        if (!shader.isTriviallyCounted()) {
            mCleanup = Core.registerNativeResource(this, shader);
        }
        mShader = shader;
    }
}
