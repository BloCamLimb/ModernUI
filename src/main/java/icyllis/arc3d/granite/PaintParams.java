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
import icyllis.arc3d.core.effects.ColorFilter;
import icyllis.arc3d.core.shaders.Shader;
import icyllis.arc3d.engine.KeyBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

//TODO currently we don't handle advanced blending
public final class PaintParams implements AutoCloseable {

    // color components using non-premultiplied alpha
    private final float mR; // 0..1
    private final float mG; // 0..1
    private final float mB; // 0..1
    private final float mA; // 0..1
    // A nullptr mPrimitiveBlender means there's no primitive color blending and it is skipped.
    // In the case where there is primitive blending, the primitive color is the source color and
    // the dest is the paint's color (or the paint's shader's computed color).
    @SharedPtr
    private final Blender mPrimitiveBlender;
    @SharedPtr
    private final Shader mShader;
    @SharedPtr
    private final ColorFilter mColorFilter;
    // A nullptr here means SrcOver blending
    @SharedPtr
    private final Blender mFinalBlender;
    private final boolean mDither;

    public PaintParams(@Nonnull Paint paint,
                       @Nullable @SharedPtr Blender primitiveBlender) {
        mR = paint.r();
        mG = paint.g();
        mB = paint.b();
        mA = paint.a();
        mPrimitiveBlender = primitiveBlender;
        mShader = paint.refShader();
        mColorFilter = paint.refColorFilter();
        mFinalBlender = paint.refBlender();
        mDither = paint.isDither();
        // antialias flag is already handled
    }

    @Override
    public void close() {
        RefCnt.move(mShader);
        RefCnt.move(mColorFilter);
        RefCnt.move(mFinalBlender);
        RefCnt.move(mPrimitiveBlender);
    }

    /**
     * Returns the value of the red component, in sRGB space.
     */
    public float r() {
        return mR;
    }

    /**
     * Returns the value of the green component, in sRGB space.
     */
    public float g() {
        return mG;
    }

    /**
     * Returns the value of the blue component, in sRGB space.
     */
    public float b() {
        return mB;
    }

    /**
     * Returns the value of the alpha component, in sRGB space.
     */
    public float a() {
        return mA;
    }

    public BlendMode getFinalBlendMode() {
        BlendMode blendMode = mFinalBlender != null
                ? mFinalBlender.asBlendMode()
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

    /**
     * Similar to {@link #getSolidColor(ImageInfo, float[])}, without a PaintParams instance.
     */
    public static boolean getSolidColor(Paint paint, ImageInfo targetInfo, float[] outColor) {
        if (paint.getShader() == null) {
            if (outColor != null) {
                float a = paint.a();
                outColor[0] = paint.r() * a;
                outColor[1] = paint.g() * a;
                outColor[2] = paint.b() * a;
                outColor[3] = a;
                var colorFilter = paint.getColorFilter();
                if (colorFilter != null) {
                    colorFilter.filterColor4f(outColor, outColor);
                }
            }
            return true;
        }
        return false;
    }

    private boolean shouldDither(int dstCT) {
        if (!mDither) {
            return false;
        }

        if (dstCT == ColorInfo.CT_UNKNOWN) {
            return false;
        }

        if (dstCT == ColorInfo.CT_RGB_565) {
            // always dither bits per channel < 8
            return true;
        }

        return mShader != null && !mShader.isConstant();
    }

    // Only dither UNorm targets
    private static float getDitherRange(int dstCT) {
        // We use 1 / (2^bitdepth-1) as the range since each channel can hold 2^bitdepth values
        return switch (dstCT) {
            case ColorInfo.CT_RGB_565 -> 1 / 31.f; // 5-bit
            case ColorInfo.CT_ALPHA_8,
                    ColorInfo.CT_GRAY_8,
                    ColorInfo.CT_GRAY_ALPHA_88,
                    ColorInfo.CT_R_8,
                    ColorInfo.CT_RG_88,
                    ColorInfo.CT_RGB_888,
                    ColorInfo.CT_RGB_888x,
                    ColorInfo.CT_RGBA_8888,
                    ColorInfo.CT_RGBA_8888_SRGB,
                    ColorInfo.CT_BGRA_8888 -> 1 / 255.f; // 8-bit
            case ColorInfo.CT_RGBA_1010102,
                    ColorInfo.CT_BGRA_1010102 -> 1 / 1023.f; // 10-bit
            case ColorInfo.CT_ALPHA_16,
                    ColorInfo.CT_R_16,
                    ColorInfo.CT_RG_1616,
                    ColorInfo.CT_RGBA_16161616 -> 1 / 32767.f; // 16-bit
            case ColorInfo.CT_UNKNOWN,
                    ColorInfo.CT_ALPHA_F16,
                    ColorInfo.CT_R_F16,
                    ColorInfo.CT_RG_F16,
                    ColorInfo.CT_RGBA_F16,
                    ColorInfo.CT_RGBA_F16_CLAMPED,
                    ColorInfo.CT_RGBA_F32 -> 0.f; // no dithering
            default -> throw new AssertionError(dstCT);
        };
    }

    private void appendPaintColorToKey(KeyContext keyContext,
                                       KeyBuilder keyBuilder,
                                       UniformDataGatherer uniformDataGatherer,
                                       TextureDataGatherer textureDataGatherer) {
        if (mShader != null) {
            FragmentUtils.appendToKey(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    mShader
            );
        } else {
            FragmentUtils.appendRGBOpaquePaintColorBlock(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );
        }
    }

    private void handlePaintAlpha(KeyContext keyContext,
                                  KeyBuilder keyBuilder,
                                  UniformDataGatherer uniformDataGatherer,
                                  TextureDataGatherer textureDataGatherer) {
        if (mShader == null && mPrimitiveBlender == null) {
            // If there is no shader and no primitive blending the input to the colorFilter stage
            // is just the premultiplied paint color.
            FragmentUtils.appendSolidColorShaderBlock(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    mR * mA, mG * mA, mB * mA, mA
            );
            return;
        }

        appendPaintColorToKey(
                keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer
        );
    }

    private void handleColorFilter(KeyContext keyContext,
                                   KeyBuilder keyBuilder,
                                   UniformDataGatherer uniformDataGatherer,
                                   TextureDataGatherer textureDataGatherer) {
        //TODO
        handlePaintAlpha(
                keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer
        );
    }

    private void handleDithering(KeyContext keyContext,
                                 KeyBuilder keyBuilder,
                                 UniformDataGatherer uniformDataGatherer,
                                 TextureDataGatherer textureDataGatherer) {
        int dstCT = keyContext.targetInfo().colorType();
        if (shouldDither(dstCT)) {
            float ditherRange = getDitherRange(dstCT);
            if (ditherRange != 0) {
                keyBuilder.addInt(FragmentStage.kCompose_BuiltinStageID);

                handleColorFilter(
                        keyContext,
                        keyBuilder,
                        uniformDataGatherer,
                        textureDataGatherer
                );

                FragmentUtils.appendDitherShaderBlock(
                        keyContext,
                        keyBuilder,
                        uniformDataGatherer,
                        textureDataGatherer,
                        ditherRange
                );
                return;
            }
        }

        handleColorFilter(
                keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer
        );
    }

    public void appendToKey(KeyContext keyContext,
                            KeyBuilder keyBuilder,
                            UniformDataGatherer uniformDataGatherer,
                            TextureDataGatherer textureDataGatherer) {
        //TODO
        handleDithering(
                keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer
        );
    }
}
