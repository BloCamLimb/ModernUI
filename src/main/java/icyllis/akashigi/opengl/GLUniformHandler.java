/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.opengl;

import icyllis.akashigi.core.SLType;
import icyllis.akashigi.engine.*;
import icyllis.akashigi.engine.shading.ProgramBuilder;
import icyllis.akashigi.engine.shading.UniformHandler;
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
                                          int arrayCount) {
        assert (SLType.canBeUniformValue(type));
        assert (visibility != 0);

        assert (!name.contains("__"));
        String resolvedName;
        if (name.startsWith(NO_MANGLE_PREFIX)) {
            resolvedName = name;
        } else {
            resolvedName = mProgramBuilder.nameVariable('u', name);
        }
        assert (!resolvedName.contains("__"));

        // we can only use std140 layout in OpenGL
        int offset = getAlignedOffset(mCurrentOffset, type, arrayCount, Std140Layout);
        mCurrentOffset += getAlignedStride(type, arrayCount, Std140Layout);

        int handle = mUniforms.size();

        String layoutQualifier = "offset = " + offset;

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

        String resolvedName = mProgramBuilder.nameVariable('u', name);

        int handle = mSamplers.size();

        // equivalent to setting texture unit to index
        String layoutQualifier = "binding = " + handle;

        var tempInfo = new UniformInfo();
        tempInfo.mVariable = new ShaderVar(resolvedName,
                SLType.kSampler2D,
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
                    out.append(") uniform ");
                    out.append(UNIFORM_BLOCK_NAME);
                    out.append(" {\n");
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
            assert (sampler.mVariable.getType() == SLType.kSampler2D);
            if ((sampler.mVisibility & visibility) != 0) {
                sampler.mVariable.appendDecl(out);
                out.append(";\n");
            }
        }
    }
}
