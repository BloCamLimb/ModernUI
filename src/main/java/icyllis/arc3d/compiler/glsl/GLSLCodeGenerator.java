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

package icyllis.arc3d.compiler.glsl;

import icyllis.arc3d.compiler.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Standard GLSL code generator for OpenGL 3.3 or above and Vulkan 1.0 or above (Vulkan GLSL).
 * <p>
 * A GLSL shader is a UTF-8 encoded string. However, our compiler only outputs ASCII characters.
 */
public final class GLSLCodeGenerator extends CodeGenerator {

    public final TargetApi mOutputTarget;
    public final GLSLVersion mOutputVersion;

    public GLSLCodeGenerator(@NonNull ShaderCompiler compiler,
                             @NonNull TranslationUnit translationUnit,
                             @NonNull ShaderCaps shaderCaps) {
        super(compiler, translationUnit);
        mOutputTarget = Objects.requireNonNullElse(shaderCaps.mTargetApi, TargetApi.OPENGL_4_5);
        mOutputVersion = Objects.requireNonNullElse(shaderCaps.mGLSLVersion, GLSLVersion.GLSL_450);
    }

    @Nullable
    @Override
    public ByteBuffer generateCode() {
        return null;
    }
}
