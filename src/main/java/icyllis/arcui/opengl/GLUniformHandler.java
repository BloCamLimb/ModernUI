/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.opengl;

import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.*;
import icyllis.arcui.engine.shading.ProgramBuilder;
import icyllis.arcui.engine.shading.UniformHandler;

import java.util.ArrayList;

/**
 * Builds a Uniform Block with std140 layout.
 */
public class GLUniformHandler extends UniformHandler {

    public static class GLUniformInfo extends UniformInfo {

        public int mOffset;

        public GLUniformInfo() {
        }
    }

    public static class GLSamplerInfo extends UniformInfo {

        public short mSwizzle;

        public GLSamplerInfo() {
        }
    }

    final ArrayList<GLUniformInfo> mUniforms = new ArrayList<>();
    final ArrayList<GLSamplerInfo> mSamplers = new ArrayList<>();

    int mCurrentOffset;

    GLUniformHandler(ProgramBuilder programBuilder) {
        super(programBuilder);
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
                                          boolean mangleName,
                                          int arrayCount) {
        assert (SLType.canBeUniformValue(type));

        char prefix = mangleName ? 'u' : '\0';
        String resolvedName = mProgramBuilder.nameVariable(prefix, name, mangleName);

        // In OpenGL, we can only use std140 layout
        int offset = getAlignedOffset(mCurrentOffset, type, arrayCount, false);
        mCurrentOffset += getAlignedStride(type, arrayCount, false);

        String layoutQualifier = "offset = " + offset;

        var tempInfo = new GLUniformInfo();
        tempInfo.mVariable = new ShaderVar(resolvedName,
                type,
                ShaderVar.TypeModifier_None,
                arrayCount,
                layoutQualifier,
                "");
        tempInfo.mVisibility = visibility;
        tempInfo.mOwner = owner;
        tempInfo.mRawName = name;
        tempInfo.mOffset = offset;

        int handle = mUniforms.size();
        mUniforms.add(tempInfo);
        return handle;
    }

    @Override
    protected int addSampler(BackendFormat backendFormat, int samplerState, short swizzle, String name) {
        assert (name != null && !name.isEmpty());
        assert (backendFormat.textureType() == EngineTypes.TextureType_2D);

        String mangleName = mProgramBuilder.nameVariable('u', name, /*mangle=*/true);

        int handle = mSamplers.size();

        String layoutQualifier = "location = " + handle;

        var tempInfo = new GLSamplerInfo();
        tempInfo.mVariable = new ShaderVar(mangleName,
                SLType.Sampler2D,
                ShaderVar.TypeModifier_Uniform,
                ShaderVar.NonArray,
                layoutQualifier,
                "");

        tempInfo.mVisibility = EngineTypes.Fragment_ShaderFlag;
        tempInfo.mOwner = null;
        tempInfo.mRawName = name;
        tempInfo.mSwizzle = swizzle;

        mSamplers.add(tempInfo);
        return handle;
    }

    @Override
    protected String samplerVariable(int handle) {
        return mSamplers.get(handle).mVariable.getName();
    }

    @Override
    protected short samplerSwizzle(int handle) {
        return mSamplers.get(handle).mSwizzle;
    }

    @Override
    protected void appendUniformDecls(int visibility, StringBuilder out) {
        assert (visibility != 0);

        boolean firstMember = false;
        boolean firstVisible = false;
        for (var uniform : mUniforms) {
            assert (SLType.canBeUniformValue(uniform.mVariable.getType()));
            if (!firstMember) {
                // Check to make sure we are starting our offset at 0 so the offset qualifier we
                // set on each variable in the uniform block is valid.
                assert (uniform.mOffset == 0);
                firstMember = true;
            }
            if ((uniform.mVisibility & visibility) != 0) {
                if (!firstVisible) {
                    out.append("layout(std140, binding = ");
                    out.append(UNIFORM_BINDING);
                    out.append(") uniform UniformBlock {\n");
                    firstVisible = true;
                }
                uniform.mVariable.appendDecl(out);
                out.append(";\n");
            }
        }
        if (firstVisible) {
            out.append("};\n");
        }

        for (var sampler : mSamplers) {
            assert (sampler.mVariable.getType() == SLType.Sampler2D);
            if ((sampler.mVisibility & visibility) != 0) {
                sampler.mVariable.appendDecl(out);
                out.append(";\n");
            }
        }
    }
}
