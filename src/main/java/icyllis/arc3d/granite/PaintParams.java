/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.ImageInfo;
import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.sketch.effects.ColorFilter;
import icyllis.arc3d.sketch.shaders.Shader;
import icyllis.arc3d.engine.KeyBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Parameters used for shading.
 */
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
    private final @Nullable Blender mPrimitiveBlender;
    @SharedPtr
    private final @Nullable Shader mShader;
    private final @Nullable ColorFilter mColorFilter;
    // A nullptr here means SrcOver blending
    private final @Nullable Blender mFinalBlender;
    private final boolean mDither;

    public PaintParams(@NonNull Paint paint,
                       @Nullable Blender primitiveBlender) {
        mR = paint.r();
        mG = paint.g();
        mB = paint.b();
        mA = paint.a();
        mPrimitiveBlender = primitiveBlender;
        mShader = paint.refShader();
        mColorFilter = paint.getColorFilter();
        mFinalBlender = paint.getBlender();
        mDither = paint.isDither();
        // antialias flag is already handled
    }

    @Override
    public void close() {
        RefCnt.move(mShader);
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

    @RawPtr
    public @Nullable Shader getShader() {
        return mShader;
    }

    @RawPtr
    public @Nullable ColorFilter getColorFilter() {
        return mColorFilter;
    }

    @RawPtr
    public @Nullable Blender getFinalBlender() {
        return mFinalBlender;
    }

    @RawPtr
    public @Nullable Blender getPrimitiveBlender() {
        return mPrimitiveBlender;
    }

    @NonNull
    public BlendMode getFinalBlendMode() {
        BlendMode blendMode = mFinalBlender != null
                ? mFinalBlender.asBlendMode()
                : BlendMode.SRC_OVER;
        return blendMode != null ? blendMode : BlendMode.SRC;
    }

    /**
     * Map into destination color space, in color and result color are non-premultiplied.
     */
    public static float[] prepareColorForDst(float[] color,
                                             ImageInfo dstInfo,
                                             boolean copyOnWrite) {
        ColorSpace dstCS = dstInfo.colorSpace();
        if (dstCS != null && !dstCS.isSrgb()) {
            float[] result = copyOnWrite ? Arrays.copyOfRange(color, 0, 4) : color;
            return ColorSpace.connect(ColorSpace.get(ColorSpace.Named.SRGB), dstCS)
                    .transform(result);
        }
        return color;
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
    public boolean getSolidColor(ImageInfo targetInfo, float @Nullable [] outColor) {
        if (mShader == null && mPrimitiveBlender == null) {
            if (outColor != null) {
                outColor[0] = mR;
                outColor[1] = mG;
                outColor[2] = mB;
                outColor[3] = mA;
                prepareColorForDst(outColor, targetInfo, false);
                for (int i = 0; i < 3; i++) {
                    outColor[i] *= outColor[3];
                }
                if (mColorFilter != null) {
                    mColorFilter.filterColor4f(outColor, outColor, targetInfo.colorSpace());
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
                outColor[0] = paint.r();
                outColor[1] = paint.g();
                outColor[2] = paint.b();
                outColor[3] = paint.a();
                prepareColorForDst(outColor, targetInfo, false);
                for (int i = 0; i < 3; i++) {
                    outColor[i] *= outColor[3];
                }
                var colorFilter = paint.getColorFilter();
                if (colorFilter != null) {
                    colorFilter.filterColor4f(outColor, outColor, targetInfo.colorSpace());
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

        if (dstCT == ColorInfo.CT_BGR_565) {
            // always dither bits per channel < 8
            return true;
        }

        return mShader != null && !mShader.isConstant();
    }

    // Only dither UNorm targets
    private static float getDitherRange(int dstCT) {
        // We use 1 / (2^bitdepth-1) as the range since each channel can hold 2^bitdepth values
        return switch (dstCT) {
            case ColorInfo.CT_BGR_565 -> 1 / 31.f; // 5-bit
            case ColorInfo.CT_ALPHA_8,
                 ColorInfo.CT_GRAY_8,
                 ColorInfo.CT_GRAY_ALPHA_88,
                 ColorInfo.CT_R_8,
                 ColorInfo.CT_RG_88,
                 ColorInfo.CT_RGB_888,
                 ColorInfo.CT_RGBX_8888,
                 ColorInfo.CT_RGBA_8888,
                 ColorInfo.CT_ABGR_8888,
                 ColorInfo.CT_RGBA_8888_SRGB,
                 ColorInfo.CT_BGRA_8888,
                 ColorInfo.CT_ARGB_8888 -> 1 / 255.f; // 8-bit
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
            FragmentHelpers.appendToKey(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    mShader
            );
        } else {
            FragmentHelpers.appendRGBOpaquePaintColorBlock(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );
        }
    }

    private void handlePrimitiveColor(KeyContext keyContext,
                                      KeyBuilder keyBuilder,
                                      UniformDataGatherer uniformDataGatherer,
                                      TextureDataGatherer textureDataGatherer) {
        if (mPrimitiveBlender != null) {
            keyBuilder.addInt(FragmentStage.kBlend_BuiltinStageID);

            // src
            appendPaintColorToKey(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );

            // dst
            FragmentHelpers.appendPrimitiveColorBlock(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );

            // blend
            FragmentHelpers.appendToKey(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    mPrimitiveBlender
            );
        } else {
            appendPaintColorToKey(
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
            FragmentHelpers.appendSolidColorShaderBlock(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    mR * mA, mG * mA, mB * mA, mA
            );
            return;
        }

        if (mA != 1.0f) {
            keyBuilder.addInt(FragmentStage.kBlend_BuiltinStageID);

            // src
            handlePrimitiveColor(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );

            // dst
            FragmentHelpers.appendAlphaOnlyPaintColorBlock(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );

            // blend
            FragmentHelpers.appendFixedBlendMode(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    BlendMode.SRC_IN
            );
        } else {
            handlePrimitiveColor(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );
        }
    }

    private void handleColorFilter(KeyContext keyContext,
                                   KeyBuilder keyBuilder,
                                   UniformDataGatherer uniformDataGatherer,
                                   TextureDataGatherer textureDataGatherer) {
        if (mColorFilter != null) {
            keyBuilder.addInt(FragmentStage.kCompose_BuiltinStageID);

            handlePaintAlpha(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );

            FragmentHelpers.appendToKey(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    mColorFilter
            );
        } else {
            handlePaintAlpha(
                    keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer
            );
        }
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

                FragmentHelpers.appendDitherShaderBlock(
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
