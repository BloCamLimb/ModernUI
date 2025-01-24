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

import icyllis.arc3d.core.MathUtil;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * A Shader that represents a single color. In general, this effect can be accomplished by just
 * using the color field on the paint, but if an actual shader object is needed, this provides that
 * feature.  Note: like all shaders, at draw time the paint's alpha will be respected, and is
 * applied to the specified color.
 */
public final class ColorShader implements Shader {

    // stored in non-premultiplied alpha, extended sRGB
    private final float mR;
    private final float mG;
    private final float mB;
    private final float mA;

    /**
     * Create a ColorShader wrapping the given sRGB color.
     */
    public ColorShader(int color) {
        mR = ((color >> 16) & 0xff) / 255.0f;
        mG = ((color >> 8) & 0xff) / 255.0f;
        mB = (color & 0xff) / 255.0f;
        mA = (color >>> 24) / 255.0f;
    }

    /**
     * Create a ColorShader wrapping the given sRGB color.
     */
    public ColorShader(float r, float g, float b, float a) {
        mR = r;
        mG = g;
        mB = b;
        mA = MathUtil.pin(a, 0.0f, 1.0f);
    }

    @Override
    public boolean isOpaque() {
        return mA == 1.0f;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    /**
     * @return a copy of non-premultiplied color.
     */
    @Contract(value = " -> new", pure = true)
    public float @NonNull [] getColor() {
        return new float[]{mR, mG, mB, mA};
    }
}
