/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine.shading;

import icyllis.arc3d.core.SLType;
import icyllis.arc3d.engine.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nonnull;
import java.util.ArrayList;

import static icyllis.arc3d.engine.Engine.*;
import static icyllis.arc3d.engine.shading.UniformHandler.UniformHandle;
import static icyllis.arc3d.engine.shading.UniformHandler.SamplerHandle;

public abstract class ProgramBuilder {

    /**
     * Each root processor has an stage index. The GP is stage 0. The first root FP is stage 1,
     * the second root FP is stage 2, etc. The XP's stage index is last and its value depends on
     * how many root FPs there are. Names are mangled by appending _S<stage-index>.
     */
    private int mStageIndex = -1;

    /**
     * When emitting FP stages we track the children FPs as "substages" and do additional name
     * mangling based on where in the FP hierarchy we are. The first FP is stage index 1. It's first
     * child would be substage 0 of stage 1. If that FP also has three children then its third child
     * would be substage 2 of stubstage 0 of stage 1 and would be mangled as "_S1_c0_c2".
     */
    private final IntArrayList mSubstageIndices = new IntArrayList();

    public final VertexShaderBuilder mVS;
    public final FragmentShaderBuilder mFS;

    public final PipelineDesc mDesc;
    public final PipelineInfo mPipelineInfo;

    /**
     * Built-in uniform handles.
     */
    @UniformHandle
    public int mProjectionUniform = INVALID_RESOURCE_HANDLE;

    public GeometryProcessor.ProgramImpl mGPImpl;

    // This is used to check that we don't exceed the allowable number of resources in a shader.
    private int mNumFragmentSamplers;

    public ProgramBuilder(PipelineDesc desc, PipelineInfo pipelineInfo) {
        mDesc = desc;
        mPipelineInfo = pipelineInfo;
        mVS = new VertexShaderBuilder(this);
        mFS = new FragmentShaderBuilder(this);
    }

    public abstract Caps caps();

    public final ShaderCaps shaderCaps() {
        return caps().shaderCaps();
    }

    public final String nameVariable(char prefix, String name) {
        return nameVariable(prefix, name, true);
    }

    /**
     * Generates a name for a variable. The generated string will be name-prefixed by the prefix
     * char (unless the prefix is '\0'). It also will mangle the name to be stage-specific unless
     * explicitly asked not to. `nameVariable` can also be used to generate names for functions or
     * other types of symbols where unique names are important.
     */
    public final String nameVariable(char prefix, String name, boolean mangle) {
        String out;
        if (prefix == '\0') {
            out = name;
        } else {
            // Names containing "__" are reserved; add "x" if needed to avoid consecutive underscores.
            if (name.startsWith("_")) {
                out = prefix + "_x" + name;
            } else {
                out = prefix + "_" + name;
            }
        }
        if (mangle) {
            String suffix = getMangleSuffix();
            // Names containing "__" are reserved; add "x" if needed to avoid consecutive underscores.
            if (out.endsWith("_")) {
                out += "x" + suffix;
            } else {
                out += suffix;
            }
        }
        assert (!out.contains("__"));
        return out;
    }

    public abstract UniformHandler uniformHandler();

    public abstract VaryingHandler varyingHandler();

    protected final boolean emitAndInstallProcs() {
        // inputColor, inputCoverage
        String[] input = new String[2];
        if (!emitAndInstallGeomProc(input)) {
            return false;
        }
        if (!emitAndInstallFragProcs(input)) {
            return false;
        }
        //TODO currently hack here, XP impl
        mFS.codeAppendf("""
                %s = %s * %s;
                """, FragmentShaderBuilder.PRIMARY_COLOR_OUTPUT_NAME, input[0], input[1]);
        return true;
    }

    // advanceStage is called by program creator between each processor's emit code.  It increments
    // the stage index for variable name mangling, and also ensures verification variables in the
    // fragment shader are cleared.
    private void advanceStage() {
        mStageIndex++;
        mFS.nextStage();
    }

    @Nonnull
    private String getMangleSuffix() {
        assert mStageIndex >= 0;
        StringBuilder suffix = new StringBuilder("_S").append(mStageIndex);
        for (int c : mSubstageIndices) {
            suffix.append("_c").append(c);
        }
        return suffix.toString();
    }

    private boolean emitAndInstallGeomProc(String[] output) {
        final GeometryProcessor geomProc = mPipelineInfo.geomProc();

        // Program builders have a bit of state we need to clear with each effect
        advanceStage();
        if (output[0] == null) {
            output[0] = nameVariable('\0', "outputColor");
        }
        if (output[1] == null) {
            output[1] = nameVariable('\0', "outputCoverage");
        }

        assert (mProjectionUniform == INVALID_RESOURCE_HANDLE);
        mProjectionUniform = uniformHandler().addUniform(
                null,
                ShaderFlags.kVertex,
                SLType.kFloat4,
                UniformHandler.PROJECTION_NAME);

        mFS.codeAppendf("// Stage %d, %s\n", mStageIndex, geomProc.name());
        mVS.codeAppendf("// Geometry Processor %s\n", geomProc.name());

        assert (mGPImpl == null);
        mGPImpl = geomProc.makeProgramImpl(shaderCaps());

        @SamplerHandle
        int[] texSamplers = new int[geomProc.numTextureSamplers()];
        for (int i = 0; i < texSamplers.length; i++) {
            String name = "TextureSampler" + i;
            texSamplers[i] = emitSampler(
                    geomProc.textureSamplerState(i),
                    geomProc.textureSamplerSwizzle(i),
                    name);
            if (texSamplers[i] == INVALID_RESOURCE_HANDLE) {
                return false;
            }
        }

        mGPImpl.emitCode(
                mVS,
                mFS,
                varyingHandler(),
                uniformHandler(),
                shaderCaps(),
                geomProc,
                output[0],
                output[1],
                texSamplers
        );

        return true;
    }

    private boolean emitAndInstallFragProcs(String[] input) {
        //TODO currently no frag procs
        return true;
    }

    @SamplerHandle
    private int emitSampler(int samplerState, short swizzle, String name) {
        ++mNumFragmentSamplers;
        return uniformHandler().addSampler(samplerState, swizzle, name);
    }

    void appendDecls(ArrayList<ShaderVar> vars, StringBuilder out) {
        for (var var : vars) {
            var.appendDecl(out);
            out.append(";\n");
        }
    }
}
