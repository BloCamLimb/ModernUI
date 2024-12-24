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

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.MathUtil;

public final class Color4fShader implements Shader {

    // color components using non-premultiplied alpha
    private final float mR;
    private final float mG;
    private final float mB;
    private final float mA;
    private final ColorSpace mColorSpace;

    public Color4fShader(float r, float g, float b, float a,
                         ColorSpace colorSpace) {
        mR = r;
        mG = g;
        mB = b;
        mA = MathUtil.pin(a, 0.0f, 1.0f);
        mColorSpace = colorSpace;
    }

    @Override
    public boolean isOpaque() {
        return mA == 1.0f;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    public float r() {
        return mR;
    }

    public float g() {
        return mG;
    }

    public float b() {
        return mB;
    }

    public float a() {
        return mA;
    }

    public ColorSpace getColorSpace() {
        return mColorSpace;
    }

    @Override
    public void ref() {
    }

    @Override
    public void unref() {
    }

    @Override
    public boolean isTriviallyCounted() {
        return true;
    }
}
