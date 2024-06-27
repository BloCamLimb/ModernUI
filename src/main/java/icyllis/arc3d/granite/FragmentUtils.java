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
 * Build {@link icyllis.arc3d.engine.KeyBuilder PaintParamsKey} and collect
 * uniform data and texture sampler desc.
 */
public class FragmentUtils {

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

    public static void appendImageShaderBlock(
            KeyContext keyContext,
            KeyBuilder keyBuilder,
            UniformDataGatherer uniformDataGatherer,
            TextureDataGatherer textureDataGatherer,
            Rect2fc subset,
            int tileModeX, int tileModeY,
            SamplingOptions sampling,
            int imageWidth, int imageHeight,
            @SharedPtr ImageViewProxy view
    ) {

        boolean useHwTiling = !sampling.mUseCubic && subset.contains(0, 0, imageWidth, imageHeight);

        if (useHwTiling) {
            uniformDataGatherer.write2f(1.f / imageWidth, 1.f / imageHeight);
            keyBuilder.addInt(FragmentStage.kHWImageShader_BuiltinStageID);
        }

        SamplerDesc samplerDesc = SamplerDesc.make(
                sampling.mMagFilter,
                sampling.mMinFilter,
                sampling.mMipmap,
                useHwTiling ? tileModeX : SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE,
                useHwTiling ? tileModeY : SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE,
                SamplerDesc.ADDRESS_MODE_REPEAT
        );

        textureDataGatherer.add(view, samplerDesc);
    }

    /**
     * Add implementation details, for the specified backend, of this SkShader to the
     * provided key.
     *
     * @param keyContext backend context for key creation
     * @param keyBuilder builder for creating the key for this SkShader
     * @param gatherer   if non-null, storage for this colorFilter's data
     * @param shader     This function is a no-op if shader is null.
     */
    public static void appendToKey(KeyContext keyContext,
                                   KeyBuilder keyBuilder,
                                   UniformDataGatherer uniformDataGatherer,
                                   TextureDataGatherer textureDataGatherer,
                                   Shader shader) {
        if (shader == null) {
            return;
        }
        if (shader instanceof ImageShader) {
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
                                      ColorShader shader) {
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
                                      ImageShader shader) {
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
                view);
    }
}
