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

/**
 * Build {@link icyllis.arc3d.engine.Key PaintParamsKey} and collect
 * uniform data and texture sampler desc.
 */
public class FragmentUtils {

    public static final ColorSpace.Rgb.TransferParameters LINEAR_TRANSFER_PARAMETERS =
            new ColorSpace.Rgb.TransferParameters(1.0, 0.0, 0.0, 0.0, 1.0);

    public static final int kColorSpaceXformFlagUnpremul = 0x1;
    public static final int kColorSpaceXformFlagLinearize = 0x2;
    public static final int kColorSpaceXformFlagGamutTransform = 0x4;
    public static final int kColorSpaceXformFlagEncode = 0x8;
    public static final int kColorSpaceXformFlagPremul = 0x10;

    private static void append_transfer_function_uniform(
            ColorSpace.Rgb.TransferParameters tf,
            UniformDataGatherer uniformDataGatherer
    ) {
        // vec4 and vec4 array have the same alignment rule
        uniformDataGatherer.write4f((float) tf.g, (float) tf.a, (float) tf.b, (float) tf.c);
        uniformDataGatherer.write4f((float) tf.d, (float) tf.e, (float) tf.f, 0.0f);
    }

    /**
     * Compute color space transform parameters and add uniforms.
     */
    public static void appendColorSpaceUniforms(
            ColorSpace srcCS, @ColorInfo.AlphaType int srcAT,
            ColorSpace dstCS, @ColorInfo.AlphaType int dstAT,
            UniformDataGatherer uniformDataGatherer
    ) {
        if (dstAT == ColorInfo.AT_OPAQUE) {
            dstAT = srcAT;
        }

        if (srcCS == null) {
            srcCS = ColorSpace.get(ColorSpace.Named.SRGB);
        }
        if (dstCS == null) {
            dstCS = srcCS;
        }

        var src = srcCS instanceof ColorSpace.Rgb
                ? (ColorSpace.Rgb) srcCS : null;
        var dst = dstCS instanceof ColorSpace.Rgb
                ? (ColorSpace.Rgb) dstCS : null;

        // we don't handle non-RGB space and non-sRGB-like transfer parameters
        boolean csXform = src != null && dst != null &&
                src.getTransferParameters() != null &&
                dst.getTransferParameters() != null &&
                !src.equals(dst);

        int flags = 0;

        if (csXform || srcAT != dstAT) {
            if (srcAT == ColorInfo.AT_PREMUL) {
                flags |= kColorSpaceXformFlagUnpremul;
            }
            if (srcAT != ColorInfo.AT_OPAQUE && dstAT == ColorInfo.AT_PREMUL) {
                flags |= kColorSpaceXformFlagPremul;
            }
        }

        if (csXform) {
            flags |= kColorSpaceXformFlagGamutTransform;

            if (!LINEAR_TRANSFER_PARAMETERS.equals(src.getTransferParameters())) {
                flags |= kColorSpaceXformFlagLinearize;
            }
            if (!LINEAR_TRANSFER_PARAMETERS.equals(dst.getTransferParameters())) {
                flags |= kColorSpaceXformFlagEncode;
            }

            float[] transform = ColorSpace.Connector.Rgb.computeTransform(
                    src, dst, ColorSpace.RenderIntent.RELATIVE
            );

            uniformDataGatherer.write1i(flags);
            append_transfer_function_uniform(src.getTransferParameters(), uniformDataGatherer);
            uniformDataGatherer.writeMatrix3f(0, transform);
            append_transfer_function_uniform(dst.getTransferParameters(), uniformDataGatherer);
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

    public static void appendImageShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            Rect2fc subset,
            int tileModeX, int tileModeY,
            SamplingOptions sampling,
            int imageWidth, int imageHeight,
            ColorSpace srcCS, @ColorInfo.AlphaType int srcAT,
            @SharedPtr ImageViewProxy view
    ) {

        boolean useHwTiling = !sampling.mUseCubic && subset.contains(0, 0, imageWidth, imageHeight);

        if (useHwTiling) {
            uniformDataGatherer.write2f(1.f / imageWidth, 1.f / imageHeight);
            keyBuilder.addInt(FragmentStage.kHWImageShader_BuiltinStageID);
        }
        appendColorSpaceUniforms(
                srcCS,
                srcAT,
                keyContext.targetInfo().colorSpace(),
                ColorInfo.AT_PREMUL,
                uniformDataGatherer);

        SamplerDesc samplerDesc = SamplerDesc.make(
                sampling.mMagFilter,
                sampling.mMinFilter,
                sampling.mMipmap,
                useHwTiling ? tileModeX : SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE,
                useHwTiling ? tileModeY : SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE,
                SamplerDesc.ADDRESS_MODE_REPEAT
        );

        textureDataGatherer.add(view, samplerDesc); // move
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
        if (!(shader.getImage() instanceof TextureImage imageToDraw)) {
            keyBuilder.addInt(FragmentStage.kError_BuiltinStageID);
            return;
        }

        @SharedPtr
        ImageViewProxy view = RefCnt.create(imageToDraw.getImageViewProxy());

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
                imageToDraw.getColorSpace(),
                imageToDraw.getAlphaType(),
                view);
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
            if (imageShader.getImage() instanceof TextureImage textureImage) {
                var view = textureImage.getImageViewProxy();
                if (view.getOrigin() == Engine.SurfaceOrigin.kLowerLeft) {
                    matrix.setScaleTranslate(1, -1, 0, view.getHeight());
                }
            }
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
}
