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

package icyllis.arc3d.core.shaders;

import icyllis.arc3d.core.*;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/**
 * Shaders specify the source color(s) for what is being drawn. If a paint
 * has no shader, then the paint's color is used. If the paint has a
 * shader, then the shader's color(s) are use instead, but they are
 * modulated by the paint's alpha. This makes it easy to create a shader
 * once (e.g. bitmap tiling or gradient) and then change its transparency
 * w/o having to modify the original shader... only the paint's alpha needs
 * to be modified.
 */
public sealed interface Shader extends RefCounted
        permits BlendShader, Color4fShader, ColorShader, EmptyShader,
        GradientShader, ImageShader, LocalMatrixShader, RRectShader {

    // TileModes sync with SamplerDesc::AddressMode
    /**
     * Repeat the shader's image horizontally and vertically.
     */
    int TILE_MODE_REPEAT = 0;
    /**
     * Repeat the shader's image horizontally and vertically, alternating
     * mirror images so that adjacent images always seam.
     */
    int TILE_MODE_MIRROR = 1;
    /**
     * Replicate the edge color if the shader draws outside of its
     * original bounds.
     */
    int TILE_MODE_CLAMP = 2;
    /**
     * Only draw within the original domain, return transparent-black everywhere else.
     */
    int TILE_MODE_DECAL = 3;
    /**
     * @hidden
     */
    @ApiStatus.Internal
    int LAST_TILE_MODE = TILE_MODE_DECAL;

    /**
     * @hidden
     */
    @ApiStatus.Internal
    int
            GRADIENT_TYPE_NONE = 0,
            GRADIENT_TYPE_LINEAR = 1,
            GRADIENT_TYPE_RADIAL = 2,
            GRADIENT_TYPE_ANGULAR = 3;

    /**
     * Returns true if the shader is guaranteed to produce only opaque
     * colors, subject to the Paint using the shader to apply an opaque
     * alpha value. Subclasses should override this to allow some
     * optimizations.
     */
    default boolean isOpaque() {
        return false;
    }

    /**
     * Returns true if the shader is guaranteed to produce only a single color.
     * Subclasses can override this to allow loop-hoisting optimization.
     *
     * @hidden
     */
    @ApiStatus.Internal
    default boolean isConstant() {
        return false;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    default int asGradient() {
        return GRADIENT_TYPE_NONE;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Methods to create combinations or variants of shaders

    /**
     * Return a shader that will apply the specified localMatrix to this shader.
     * The specified matrix will be applied AFTER any matrix associated with this shader.
     */
    @NonNull
    @SharedPtr
    default Shader makeWithLocalMatrix(@NonNull Matrixc localMatrix) {
        var lm = new Matrix(localMatrix);
        Shader base; // raw ptr
        if (this instanceof LocalMatrixShader lms) {
            lm.preConcat(lms.getLocalMatrix());
            base = lms.getBase();
        } else {
            base = this;
        }
        base.ref();
        return new LocalMatrixShader(base, lm); // move
    }
}
