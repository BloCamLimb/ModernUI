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

import javax.annotation.Nonnull;

/**
 * Shaders specify the source color(s) for what is being drawn. If a paint
 * has no shader, then the paint's color is used. If the paint has a
 * shader, then the shader's color(s) are use instead, but they are
 * modulated by the paint's alpha. This makes it easy to create a shader
 * once (e.g. bitmap tiling or gradient) and then change its transparency
 * w/o having to modify the original shader... only the paint's alpha needs
 * to be modified.
 */
public abstract class Shader extends RefCnt {

    // TileModes sync with SamplerDesc::AddressMode
    /**
     * Repeat the shader's image horizontally and vertically.
     */
    public static final int TILE_MODE_REPEAT = 0;
    /**
     * Repeat the shader's image horizontally and vertically, alternating
     * mirror images so that adjacent images always seam.
     */
    public static final int TILE_MODE_MIRROR = 1;
    /**
     * Replicate the edge color if the shader draws outside of its
     * original bounds.
     */
    public static final int TILE_MODE_CLAMP = 2;
    /**
     * Only draw within the original domain, return transparent-black everywhere else.
     */
    public static final int TILE_MODE_DECAL = 3;
    @ApiStatus.Internal
    public static final int LAST_TILE_MODE = TILE_MODE_DECAL;

    public static final int GRADIENT_TYPE_NONE = 0;
    public static final int GRADIENT_TYPE_LINEAR = 1;
    public static final int GRADIENT_TYPE_RADIAL = 2;
    public static final int GRADIENT_TYPE_ANGULAR = 3;

    protected Shader() {
    }

    @Override
    protected void deallocate() {
    }

    public boolean isOpaque() {
        return false;
    }

    /**
     * Returns true if the shader is guaranteed to produce only a single color.
     * Subclasses can override this to allow loop-hoisting optimization.
     */
    public boolean isConstant() {
        return false;
    }

    public int asGradient() {
        return GRADIENT_TYPE_NONE;
    }

    //////////////////////////////////////////////////////////////////////////
    //  Methods to create combinations or variants of shaders

    /**
     * Return a shader that will apply the specified localMatrix to this shader.
     * The specified matrix will be applied AFTER any matrix associated with this shader.
     */
    @Nonnull
    @SharedPtr
    public Shader makeWithLocalMatrix(@Nonnull Matrixc localMatrix) {
        var lm = new Matrix(localMatrix);
        Shader base; // raw ptr
        if (this instanceof LocalMatrixShader lms) {
            lm.preConcat(lms.getLocalMatrix());
            base = lms.getBase();
        } else {
            base = this;
        }
        return new LocalMatrixShader(RefCnt.create(base), lm);
    }
}
