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
import icyllis.arc3d.core.shaders.*;
import icyllis.arc3d.engine.*;

import javax.annotation.Nullable;

/**
 * Build {@link icyllis.arc3d.engine.Key PaintParamsKey} and collect
 * uniform data and texture sampler desc.
 *
 * @see ShaderCodeSource
 * @see FragmentStage
 */
public class FragmentUtils {

    public static final ColorSpace.Rgb.TransferParameters LINEAR_TRANSFER_PARAMETERS =
            new ColorSpace.Rgb.TransferParameters(1.0, 0.0, 0.0, 0.0, 1.0);

    private static void append_transfer_function_uniform(
            ColorSpace.Rgb.TransferParameters tf,
            UniformDataGatherer uniformDataGatherer
    ) {
        // vec4 and vec4 array have the same alignment rule
        uniformDataGatherer.write4f((float) tf.g, (float) tf.a, (float) tf.b, (float) tf.c);
        uniformDataGatherer.write4f((float) tf.d, (float) tf.e, (float) tf.f, 0.0f);
    }

    /**
     * Compute color space transform parameters and add uniforms,
     * see {@link PixelUtils}.
     */
    public static void appendColorSpaceUniforms(
            @Nullable ColorSpace srcCS, @ColorInfo.AlphaType int srcAT,
            @Nullable ColorSpace dstCS, @ColorInfo.AlphaType int dstAT,
            UniformDataGatherer uniformDataGatherer
    ) {
        // Opaque outputs are treated as the same alpha type as the source input.
        if (dstAT == ColorInfo.AT_OPAQUE) {
            dstAT = srcAT;
        }

        if (srcCS == null) {
            srcCS = ColorSpace.get(ColorSpace.Named.SRGB);
        }
        if (dstCS == null) {
            dstCS = srcCS;
        }

        boolean srcXYZ = srcCS.getModel() == ColorSpace.Model.XYZ;
        boolean dstXYZ = dstCS.getModel() == ColorSpace.Model.XYZ;
        var srcRGB = srcCS.getModel() == ColorSpace.Model.RGB
                ? (ColorSpace.Rgb) srcCS : null;
        var dstRGB = dstCS.getModel() == ColorSpace.Model.RGB
                ? (ColorSpace.Rgb) dstCS : null;

        // we handle RGB space with known transfer parameters and XYZ space
        boolean csXform = (srcXYZ || (srcRGB != null && srcRGB.getTransferParameters() != null)) &&
                (dstXYZ || (dstRGB != null && dstRGB.getTransferParameters() != null)) &&
                !srcCS.equals(dstCS);

        int flags = 0;

        if (csXform || srcAT != dstAT) {
            if (srcAT == ColorInfo.AT_PREMUL) {
                flags |= PixelUtils.kColorSpaceXformFlagUnpremul;
            }
            if (srcAT != ColorInfo.AT_OPAQUE && dstAT == ColorInfo.AT_PREMUL) {
                flags |= PixelUtils.kColorSpaceXformFlagPremul;
            }
        }

        if (csXform) {
            flags |= PixelUtils.kColorSpaceXformFlagGamutTransform;

            if (srcRGB != null && !LINEAR_TRANSFER_PARAMETERS.equals(srcRGB.getTransferParameters())) {
                flags |= PixelUtils.kColorSpaceXformFlagLinearize;
            }
            if (dstRGB != null && !LINEAR_TRANSFER_PARAMETERS.equals(dstRGB.getTransferParameters())) {
                flags |= PixelUtils.kColorSpaceXformFlagEncode;
            }

            float[] transform = ColorSpace.Connector.Rgb.computeTransform(
                    srcXYZ, srcRGB, dstXYZ, dstRGB
            );

            uniformDataGatherer.write1i(flags);
            append_transfer_function_uniform(srcRGB == null ? LINEAR_TRANSFER_PARAMETERS
                    : srcRGB.getTransferParameters(), uniformDataGatherer);
            uniformDataGatherer.writeMatrix3f(0, transform);
            append_transfer_function_uniform(dstRGB == null ? LINEAR_TRANSFER_PARAMETERS
                    : dstRGB.getTransferParameters(), uniformDataGatherer);
        } else {
            uniformDataGatherer.write1i(flags);
            append_transfer_function_uniform(LINEAR_TRANSFER_PARAMETERS, uniformDataGatherer);
            uniformDataGatherer.writeMatrix3f(Matrix.identity());
            append_transfer_function_uniform(LINEAR_TRANSFER_PARAMETERS, uniformDataGatherer);
        }
    }

    public static void appendSolidColorShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            float r, float g, float b, float a
    ) {
        uniformDataGatherer.write4f(r, g, b, a);

        keyBuilder.addInt(FragmentStage.kSolidColorShader_BuiltinStageID);
    }

    public static void appendRGBOpaquePaintColorBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer
    ) {
        uniformDataGatherer.writePaintColor(keyContext.r(), keyContext.g(), keyContext.b(), keyContext.a());

        keyBuilder.addInt(FragmentStage.kRGBOpaquePaintColor_BuiltinStageID);
    }

    public static void appendAlphaOnlyPaintColorBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer
    ) {
        uniformDataGatherer.writePaintColor(keyContext.r(), keyContext.g(), keyContext.b(), keyContext.a());

        keyBuilder.addInt(FragmentStage.kAlphaOnlyPaintColor_BuiltinStageID);
    }

    public static void appendDitherShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            float range
    ) {
        uniformDataGatherer.write1f(range);

        keyBuilder.addInt(FragmentStage.kDitherShader_BuiltinStageID);
    }

    public static void appendLocalMatrixShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            Matrixc localMatrix
    ) {
        Matrix inverse = new Matrix();
        localMatrix.invert(inverse);
        uniformDataGatherer.writeMatrix3f(inverse);

        keyBuilder.addInt(FragmentStage.kLocalMatrixShader_BuiltinStageID);
    }

    public static final int kCubicClampUnpremul = 0;
    public static final int kCubicClampPremul = 1;

    public static void appendImageShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            Rect2fc subset,
            int tileModeX, int tileModeY,
            SamplingOptions sampling,
            int imageWidth, int imageHeight,
            @ColorInfo.AlphaType int srcAT,
            @SharedPtr ImageViewProxy view
    ) {
        boolean useHwTiling = !sampling.mUseCubic &&
                subset.contains(0, 0, imageWidth, imageHeight);
        int filterMode = SamplingOptions.FILTER_MODE_NEAREST; // for subset

        if (useHwTiling || !sampling.mUseCubic) {
            // cubic does not require this
            uniformDataGatherer.write2f(1.f / imageWidth, 1.f / imageHeight);
        }
        if (useHwTiling) {
            // hardware (fast)
            keyBuilder.addInt(FragmentStage.kHWImageShader_BuiltinStageID);
        } else {
            // strict subset
            assert sampling.mMipmapMode == SamplingOptions.MIPMAP_MODE_NONE;
            uniformDataGatherer.write4f(subset.left(), subset.top(),
                    subset.right(), subset.bottom());
            if (sampling.mUseCubic) {
                // Cubic sampling is handled in a shader, with the actual texture sampled by with
                // nearest-neighbor
                assert sampling.mMinFilter == SamplingOptions.FILTER_MODE_NEAREST &&
                        sampling.mMagFilter == SamplingOptions.FILTER_MODE_NEAREST;
                uniformDataGatherer.writeMatrix4f(0,
                        ImageShader.makeCubicMatrix(sampling.mCubicB, sampling.mCubicC));
                uniformDataGatherer.write1i(srcAT == ColorInfo.AT_PREMUL
                        ? kCubicClampPremul
                        : kCubicClampUnpremul);
                keyBuilder.addInt(FragmentStage.kCubicImageShader_BuiltinStageID);
            } else {
                // Use linear filter if either is linear
                filterMode = sampling.mMinFilter | sampling.mMagFilter;
                uniformDataGatherer.write1i(filterMode);
                keyBuilder.addInt(FragmentStage.kImageShader_BuiltinStageID);
            }
            uniformDataGatherer.write1i(tileModeX);
            uniformDataGatherer.write1i(tileModeY);
        }

        var samplerDesc = useHwTiling
                ? SamplerDesc.make(
                sampling.mMagFilter,
                sampling.mMinFilter,
                sampling.mMipmapMode,
                tileModeX,
                tileModeY,
                SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE)
                : SamplerDesc.make(filterMode);

        textureDataGatherer.add(view, samplerDesc); // move
    }

    public static class GradientData {

        static final int kNumInternalStorageStops = 8;

        // Layout options for angular gradient.
        float mBias;
        float mScale;

        int mTileMode;
        int mNumStops;

        float[] mColors = new float[kNumInternalStorageStops * 4];
        float[] mOffsets = new float[kNumInternalStorageStops];

        Gradient1DShader mSrcShader; // raw ptr

        int mInterpolation;

        GradientData(Gradient1DShader shader,
                     float bias, float scale,
                     int tileMode,
                     int numStops,
                     float[] colors,
                     float[] offsets,
                     int interpolation) {
            mSrcShader = shader;
            mBias = bias;
            mScale = scale;
            mTileMode = tileMode;
            mNumStops = numStops;
            mInterpolation = interpolation;

            if (mNumStops <= kNumInternalStorageStops) {
                System.arraycopy(colors, 0, mColors, 0, mNumStops * 4);
                if (offsets != null) {
                    System.arraycopy(offsets, 0, mOffsets, 0, mNumStops);
                } else {
                    // uniform stops
                    for (int i = 0; i < mNumStops; ++i) {
                        mOffsets[i] = (float) i / (mNumStops - 1);
                    }
                }

                // Extend the colors and offset, if necessary, to fill out the arrays.
                // The unrolled binary search implementation assumes excess stops match the last real value.
                int last = mNumStops - 1;
                for (int i = mNumStops; i < kNumInternalStorageStops; ++i) {
                    System.arraycopy(mColors, last * 4, mColors, i * 4, 4);
                    mOffsets[i] = mOffsets[last];
                }
            }
        }
    }

    private static void append_gradient_head(
            GradientData gradientData,
            UniformDataGatherer uniformDataGatherer
    ) {
        if (gradientData.mNumStops <= GradientData.kNumInternalStorageStops) {
            if (gradientData.mNumStops <= 4) {
                // Round up to 4 stops.
                uniformDataGatherer.write4fv(
                        0, 4, gradientData.mColors
                );
                uniformDataGatherer.write4fv(
                        0, 1, gradientData.mOffsets
                );
            } else {
                //noinspection ConstantValue
                assert gradientData.mNumStops <= 8;
                // Round up to 8 stops.
                uniformDataGatherer.write4fv(
                        0, 8, gradientData.mColors
                );
                uniformDataGatherer.write4fv(
                        0, 2, gradientData.mOffsets
                );
            }
        }
    }

    private static void append_gradient_tail(
            GradientData gradientData,
            UniformDataGatherer uniformDataGatherer
    ) {
        if (gradientData.mNumStops > GradientData.kNumInternalStorageStops) {
            uniformDataGatherer.write1i(gradientData.mNumStops);
        }
        uniformDataGatherer.write1i(gradientData.mTileMode);
        uniformDataGatherer.write1i(
                GradientShader.Interpolation.getColorSpace(gradientData.mInterpolation));
        uniformDataGatherer.write1i(
                GradientShader.Interpolation.isInPremul(gradientData.mInterpolation) ? 1 : 0);
    }

    public static void appendGradientShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            GradientData gradData
    ) {
        //TODO texture-based gradient
        int stageID = FragmentStage.kError_BuiltinStageID;
        switch (gradData.mSrcShader.asGradient()) {
            case Shader.GRADIENT_TYPE_LINEAR -> {
                stageID = gradData.mNumStops <= 4
                        ? FragmentStage.kLinearGradientShader4_BuiltinStageID
                        : gradData.mNumStops <= 8
                        ? FragmentStage.kLinearGradientShader8_BuiltinStageID
                        : FragmentStage.kError_BuiltinStageID;
                append_gradient_head(gradData, uniformDataGatherer);
                append_gradient_tail(gradData, uniformDataGatherer);
            }
            case Shader.GRADIENT_TYPE_RADIAL -> {
                stageID = gradData.mNumStops <= 4
                        ? FragmentStage.kRadialGradientShader4_BuiltinStageID
                        : gradData.mNumStops <= 8
                        ? FragmentStage.kRadialGradientShader8_BuiltinStageID
                        : FragmentStage.kError_BuiltinStageID;
                append_gradient_head(gradData, uniformDataGatherer);
                append_gradient_tail(gradData, uniformDataGatherer);
            }
            case Shader.GRADIENT_TYPE_ANGULAR -> {
                stageID = gradData.mNumStops <= 4
                        ? FragmentStage.kAngularGradientShader4_BuiltinStageID
                        : gradData.mNumStops <= 8
                        ? FragmentStage.kAngularGradientShader8_BuiltinStageID
                        : FragmentStage.kError_BuiltinStageID;
                append_gradient_head(gradData, uniformDataGatherer);
                uniformDataGatherer.write1f(gradData.mBias);
                uniformDataGatherer.write1f(gradData.mScale);
                append_gradient_tail(gradData, uniformDataGatherer);
            }
        }
        keyBuilder.addInt(stageID);
    }

    private static void append_gradient_to_key(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            Gradient1DShader shader,
            float bias, float scale
    ) {
        var colorTransformer = new Gradient1DShader.ColorTransformer(
                shader, keyContext.targetInfo().colorSpace()
        );

        GradientData data = new GradientData(
                shader,
                bias, scale,
                shader.getTileMode(),
                shader.getColorCount(),
                colorTransformer.mColors,
                colorTransformer.mPositions,
                shader.getInterpolation()
        );

        // Are we interpreting premul colors? We use this later to decide if we need to inject a final
        // premultiplication step.
        boolean inputPremul = GradientShader.Interpolation.isInPremul(
                shader.getInterpolation()
        );

        switch (GradientShader.Interpolation.getColorSpace(shader.getInterpolation())) {
            case GradientShader.Interpolation.kLab_ColorSpace,
                    GradientShader.Interpolation.kOKLab_ColorSpace,
                    GradientShader.Interpolation.kOKLabGamutMap_ColorSpace,
                    GradientShader.Interpolation.kHSL_ColorSpace,
                    GradientShader.Interpolation.kHWB_ColorSpace,
                    GradientShader.Interpolation.kLCH_ColorSpace,
                    GradientShader.Interpolation.kOKLCH_ColorSpace,
                    GradientShader.Interpolation.kOKLCHGamutMap_ColorSpace ->
                // In these exotic spaces, unpremul the colors if necessary (no need to do this if
                // they're all opaque), and then convert them to the intermediate ColorSpace
                    inputPremul = false;
        }

        // Now transform from intermediate to destination color space. There are two tricky things here:
        // 1) Normally, we'd pass dstInfo to the transform effect. However, if someone is rendering to
        //    a non-color managed surface (nullptr dst color space), and they chose to interpolate in
        //    any of the exotic spaces, that transform would do nothing, and leave the colors in
        //    whatever intermediate space we chose. That could even be something like XYZ, which will
        //    produce nonsense. So, in this particular case, we break Skia's rules, and treat a null
        //    destination as sRGB.
        ColorSpace dstColorSpace = keyContext.targetInfo().colorSpace();
        if (dstColorSpace == null) {
            dstColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
        }

        // 2) Alpha type: We already tweaked our idea of "inputPremul" above -- if we interpolated in a
        //    non-RGB space, then we had to unpremul the colors to get proper conversion back to RGB.
        //    Our final goal is to emit premul colors, but under certain conditions we don't need to do
        //    anything to achieve that: i.e. its interpolating already premul colors (inputPremul) or
        //    all the colors have a = 1, in which case premul is a no op. Note that this allOpaque check
        //    is more permissive than SkGradientBaseShader's isOpaque(), since we can optimize away the
        //    make-premul op for two point conical gradients (which report false for isOpaque).
        int intermediateAlphaType = inputPremul ? ColorInfo.AT_PREMUL : ColorInfo.AT_UNPREMUL;
        int dstAlphaType = ColorInfo.AT_PREMUL;

        // The gradient block and colorSpace conversion block need to be combined
        // (via the Compose block) so that the localMatrix block can treat them as
        // one child.
        keyBuilder.addInt(FragmentStage.kCompose_BuiltinStageID);

        appendGradientShaderBlock(
                keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer,
                data);

        appendColorSpaceUniforms(
                colorTransformer.mIntermediateColorSpace, intermediateAlphaType,
                dstColorSpace, dstAlphaType, uniformDataGatherer);
        keyBuilder.addInt(FragmentStage.kColorSpaceXformColorFilter_BuiltinStageID);
    }

    /**
     * Add implementation details, for the specified backend, of this SkShader to the
     * provided key.
     *
     * @param keyContext backend context for key creation
     * @param keyBuilder builder for creating the key for this SkShader
     * @param shader     This function is a no-op if shader is null.
     */
    public static void appendToKey(KeyContext keyContext,
                                   KeyBuilder keyBuilder,
                                   UniformDataGatherer uniformDataGatherer,
                                   TextureDataGatherer textureDataGatherer,
                                   @RawPtr Shader shader) {
        if (shader == null) {
            return;
        }
        if (shader instanceof LocalMatrixShader) {
            append_to_key(keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    (LocalMatrixShader) shader);
        } else if (shader instanceof ImageShader) {
            append_to_key(keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    (ImageShader) shader);
        } else if (shader instanceof ColorShader) {
            append_to_key(keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    (ColorShader) shader);
        } else if (shader instanceof Gradient1DShader) {
            append_to_key(keyContext,
                    keyBuilder,
                    uniformDataGatherer,
                    textureDataGatherer,
                    (Gradient1DShader) shader);
        }
    }

    private static void append_to_key(KeyContext keyContext,
                                      KeyBuilder keyBuilder,
                                      UniformDataGatherer uniformDataGatherer,
                                      TextureDataGatherer textureDataGatherer,
                                      @RawPtr ColorShader shader) {
        int color = shader.getColor();
        float r = ((color >> 16) & 0xff) / 255.0f;
        float g = ((color >> 8) & 0xff) / 255.0f;
        float b = (color & 0xff) / 255.0f;
        float a = (color >>> 24) / 255.0f;
        appendSolidColorShaderBlock(
                keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer,
                r * a, g * a, b * a, a
        );
    }

    private static void append_to_key(KeyContext keyContext,
                                      KeyBuilder keyBuilder,
                                      UniformDataGatherer uniformDataGatherer,
                                      TextureDataGatherer textureDataGatherer,
                                      @RawPtr ImageShader shader) {
        if (!(shader.getImage() instanceof Image_Engine imageToDraw)) {
            keyBuilder.addInt(FragmentStage.kError_BuiltinStageID);
            return;
        }

        @SharedPtr
        ImageViewProxy view = RefCnt.create(imageToDraw.getImageViewProxy());
        if (view == null) {
            keyBuilder.addInt(FragmentStage.kError_BuiltinStageID);
            return;
        }

        int srcAlphaType = imageToDraw.getAlphaType();
        int dstAlphaType = ColorInfo.AT_PREMUL;

        keyBuilder.addInt(FragmentStage.kCompose_BuiltinStageID);

        appendImageShaderBlock(keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer,
                shader.getSubset(),
                shader.getTileModeX(),
                shader.getTileModeY(),
                shader.getSampling(),
                view.getWidth(),
                view.getHeight(),
                srcAlphaType,
                view);

        appendColorSpaceUniforms(
                imageToDraw.getColorSpace(),
                srcAlphaType,
                keyContext.targetInfo().colorSpace(),
                dstAlphaType,
                uniformDataGatherer);
        keyBuilder.addInt(FragmentStage.kColorSpaceXformColorFilter_BuiltinStageID);
    }

    private static void append_to_key(KeyContext keyContext,
                                      KeyBuilder keyBuilder,
                                      UniformDataGatherer uniformDataGatherer,
                                      TextureDataGatherer textureDataGatherer,
                                      @RawPtr LocalMatrixShader shader) {
        @RawPtr
        var baseShader = shader.getBase();

        var matrix = new Matrix();
        if (baseShader instanceof ImageShader imageShader) {
            if (imageShader.getImage() instanceof Image_Engine textureImage) {
                var view = textureImage.getImageViewProxy();
                if (view.getOrigin() == Engine.SurfaceOrigin.kLowerLeft) {
                    matrix.setScaleTranslate(1, -1, 0, view.getHeight());
                }
            }
        } else if (baseShader instanceof Gradient1DShader gradShader) {
            var gradMatrix = gradShader.getGradientMatrix();

            boolean res = gradMatrix.invert(matrix);
            assert res;
        }

        matrix.postConcat(shader.getLocalMatrix());

        appendLocalMatrixShaderBlock(keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer,
                matrix);

        appendToKey(keyContext,
                keyBuilder,
                uniformDataGatherer,
                textureDataGatherer,
                baseShader);
    }

    private static void append_to_key(KeyContext keyContext,
                                      KeyBuilder keyBuilder,
                                      UniformDataGatherer uniformDataGatherer,
                                      TextureDataGatherer textureDataGatherer,
                                      @RawPtr Gradient1DShader shader) {
        switch (shader.asGradient()) {
            case Shader.GRADIENT_TYPE_LINEAR, Shader.GRADIENT_TYPE_RADIAL -> {
                append_gradient_to_key(
                        keyContext,
                        keyBuilder,
                        uniformDataGatherer,
                        textureDataGatherer,
                        shader,
                        0.0f,
                        0.0f
                );
            }
            case Shader.GRADIENT_TYPE_ANGULAR -> {
                var angularGrad = (AngularGradient) shader;
                append_gradient_to_key(
                        keyContext,
                        keyBuilder,
                        uniformDataGatherer,
                        textureDataGatherer,
                        shader,
                        angularGrad.getTBias(),
                        angularGrad.getTScale()
                );
            }
        }
    }
}
