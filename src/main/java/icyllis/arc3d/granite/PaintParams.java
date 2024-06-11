/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.KeyBuilder;

//TODO currently we don't handle advanced blending
public final class PaintParams {

    // color components using non-premultiplied alpha
    private final float mR; // 0..1
    private final float mG; // 0..1
    private final float mB; // 0..1
    private final float mA; // 0..1
    private final Blender mPrimitiveBlender;
    private final Shader mShader;
    private final ColorFilter mColorFilter;
    private final Blender mBlender;
    private final boolean mDither;

    public PaintParams(Paint paint,
                       Blender primitiveBlender) {
        mR = paint.r();
        mG = paint.g();
        mB = paint.b();
        mA = paint.a();
        mPrimitiveBlender = primitiveBlender;
        mShader = paint.getShader();
        mColorFilter = paint.getColorFilter();
        mBlender = paint.getBlender();
        mDither = paint.isDither();
        // antialias flag is already handled
    }

    /**
     * Returns the value of the red component.
     */
    public float r() {
        return mR;
    }

    /**
     * Returns the value of the green component.
     */
    public float g() {
        return mG;
    }

    /**
     * Returns the value of the blue component.
     */
    public float b() {
        return mB;
    }

    /**
     * Returns the value of the alpha component.
     */
    public float a() {
        return mA;
    }

    public BlendMode getFinalBlendMode() {
        BlendMode blendMode = mBlender != null
                ? mBlender.asBlendMode()
                : BlendMode.SRC_OVER;
        return blendMode != null ? blendMode : BlendMode.SRC;
    }

    /**
     * Returns true if the paint can be simplified to a solid color,
     * and stores the solid color.
     */
    public boolean isSolidColor() {
        return getSolidColor(null, null);
    }

    /**
     * Returns true if the paint can be simplified to a solid color,
     * and stores the solid color. The color will be transformed to the
     * target's color space and premultiplied.
     */
    public boolean getSolidColor(ImageInfo targetInfo, float[] outColor) {
        //TODO color space transform, extract solid color from shader
        if (mShader == null && mPrimitiveBlender == null) {
            if (outColor != null) {
                outColor[0] = mR * mA;
                outColor[1] = mG * mA;
                outColor[2] = mB * mA;
                outColor[3] = mA;
                if (mColorFilter != null) {
                    mColorFilter.filterColor4f(outColor, outColor);
                }
            }
            return true;
        }
        return false;
    }

    public void toKey(KeyContext keyContext,
                      KeyBuilder keyBuilder,
                      UniformDataGatherer uniformDataGatherer,
                      TextureDataGatherer textureDataGatherer) {
        //TODO
    }
}
