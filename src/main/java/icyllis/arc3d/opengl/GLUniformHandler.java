/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.opengl;

import icyllis.arc3d.compiler.GLSLVersion;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.PipelineBuilder;
import icyllis.arc3d.engine.shading.UniformHandler;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.util.ArrayList;

/**
 * Builds a Uniform Block with std140 layout.
 */
public class GLUniformHandler extends UniformHandler {

    final ArrayList<UniformInfo> mUniforms = new ArrayList<>();
    final ArrayList<UniformInfo> mSamplers = new ArrayList<>();
    final ShortArrayList mSamplerSwizzles = new ShortArrayList();

    int mCurrentOffset;

    GLUniformHandler(PipelineBuilder pipelineBuilder) {
        super(pipelineBuilder);
    }

    @Override
    public ShaderVar getUniformVariable(int handle) {
        return mUniforms.get(handle).mVariable;
    }

    @Override
    public int numUniforms() {
        return mUniforms.size();
    }

    @Override
    public UniformInfo uniform(int index) {
        return mUniforms.get(index);
    }

    @Override
    protected int internalAddUniformArray(Processor owner,
                                          int visibility,
                                          byte type,
                                          String name,
                                          int arrayCount) {
        assert (SLDataType.canBeUniformValue(type));
        assert (visibility != 0);

        assert (!name.contains("__"));
        String resolvedName;
        if (name.startsWith(NO_MANGLE_PREFIX)) {
            resolvedName = name;
        } else {
            resolvedName = mPipelineBuilder.nameVariable('u', name);
        }
        assert (!resolvedName.contains("__"));

        // we can only use std140 layout in OpenGL
        int offset = getAlignedOffset(mCurrentOffset, type, arrayCount, Std140Layout);
        mCurrentOffset += getAlignedStride(type, arrayCount, Std140Layout);

        int handle = mUniforms.size();

        String layoutQualifier;
        if (mPipelineBuilder.shaderCaps().mGLSLVersion.isAtLeast(GLSLVersion.GLSL_450)) {
            // ARB_enhanced_layouts or GLSL 440
            layoutQualifier = "offset = " + offset;
        } else {
            layoutQualifier = "";
        }

        var tempInfo = new UniformInfo();
        tempInfo.mVariable = new ShaderVar(resolvedName,
                type,
                ShaderVar.kNone_TypeModifier,
                arrayCount,
                layoutQualifier,
                "");
        tempInfo.mVisibility = visibility;
        tempInfo.mOwner = owner;
        tempInfo.mRawName = name;
        tempInfo.mOffset = offset;

        mUniforms.add(tempInfo);
        return handle;
    }

    @Override
    protected int addSampler(int samplerState, short swizzle, String name) {
        assert (name != null && !name.isEmpty());

        String resolvedName = mPipelineBuilder.nameVariable('u', name);

        int handle = mSamplers.size();

        String layoutQualifier;
        if (mPipelineBuilder.shaderCaps().mGLSLVersion.isAtLeast(GLSLVersion.GLSL_430)) {
            // ARB_shading_language_420pack
            // equivalent to setting texture unit to index
            layoutQualifier = "binding = " + handle;
        } else {
            layoutQualifier = "";
        }

        var tempInfo = new UniformInfo();
        tempInfo.mVariable = new ShaderVar(resolvedName,
                SLDataType.kSampler2D,
                ShaderVar.kUniform_TypeModifier,
                ShaderVar.kNonArray,
                layoutQualifier,
                "");
        tempInfo.mVisibility = Engine.ShaderFlags.kFragment;
        tempInfo.mOwner = null;
        tempInfo.mRawName = name;

        mSamplers.add(tempInfo);
        mSamplerSwizzles.add(swizzle);
        assert (mSamplers.size() == mSamplerSwizzles.size());

        return handle;
    }

    @Override
    protected String samplerVariable(int handle) {
        return mSamplers.get(handle).mVariable.getName();
    }

    @Override
    protected short samplerSwizzle(int handle) {
        return mSamplerSwizzles.getShort(handle);
    }

    @Override
    protected void appendUniformDecls(int visibility, StringBuilder out) {
        assert (visibility != 0);

        boolean firstMember = false;
        boolean firstVisible = false;
        for (var uniform : mUniforms) {
            assert (SLDataType.canBeUniformValue(uniform.mVariable.getType()));
            if (!firstMember) {
                // Check to make sure we are starting our offset at 0 so the offset qualifier we
                // set on each variable in the uniform block is valid.
                assert (uniform.mOffset == 0);
                firstMember = true;
            }
            if ((uniform.mVisibility & visibility) != 0) {
                firstVisible = true;
            }
        }
        // The uniform block definition for all shader stages must be exactly the same
        if (firstVisible) {
            out.append("layout(std140");
            if (mPipelineBuilder.shaderCaps().mGLSLVersion.isAtLeast(GLSLVersion.GLSL_430)) {
                // ARB_shading_language_420pack
                out.append(", binding = ");
                out.append(UNIFORM_BINDING);
            }
            out.append(") uniform ");
            out.append(UNIFORM_BLOCK_NAME);
            out.append(" {\n");
            for (var uniform : mUniforms) {
                uniform.mVariable.appendDecl(out);
                out.append(";\n");
            }
            out.append("};\n");
        }

        for (var sampler : mSamplers) {
            assert (sampler.mVariable.getType() == SLDataType.kSampler2D);
            if ((sampler.mVisibility & visibility) != 0) {
                sampler.mVariable.appendDecl(out);
                out.append(";\n");
            }
        }
    }
}
