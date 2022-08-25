/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arctic.engine.shading;

import icyllis.arctic.core.SLType;
import icyllis.arctic.engine.EngineTypes;
import icyllis.arctic.engine.ShaderVar;

import javax.annotation.Nullable;

/**
 * This class implements the various fragment builder interfaces.
 */
public class FragmentShaderBuilder extends ShaderBuilderBase implements FPFragmentBuilder, XPFragmentBuilder {

    public static final String PRIMARY_COLOR_OUTPUT_NAME = "FragColor0";
    public static final String SECONDARY_COLOR_OUTPUT_NAME = "FragColor1";

    // fragment shader has at most two outputs, the second one is used for dual source blending
    private final ShaderVar mPrimaryOutput;
    private ShaderVar mSecondaryOutput;

    public FragmentShaderBuilder(ProgramBuilder programBuilder) {
        super(programBuilder);

        mPrimaryOutput = new ShaderVar(PRIMARY_COLOR_OUTPUT_NAME, SLType.Vec4, ShaderVar.TypeModifier_Out,
                ShaderVar.NonArray, "location = 0, index = 0", "");
    }

    @Override
    protected void onFinish() {
        mProgramBuilder.uniformHandler().appendUniformDecls(EngineTypes.Fragment_ShaderFlag, uniforms());
        mProgramBuilder.varyingHandler().getFragDecls(inputs());

        mPrimaryOutput.appendDecl(outputs());
        outputs().append(";\n");
        if (mSecondaryOutput != null) {
            mSecondaryOutput.appendDecl(outputs());
            outputs().append(";\n");
        }
    }

    public void enableSecondaryOutput() {
        assert (mSecondaryOutput == null);
        mSecondaryOutput = new ShaderVar(SECONDARY_COLOR_OUTPUT_NAME, SLType.Vec4, ShaderVar.TypeModifier_Out,
                ShaderVar.NonArray, "location = 0, index = 1", "");
    }

    @Nullable
    public String getSecondaryColorOutputName() {
        return (mSecondaryOutput == null) ? null : SECONDARY_COLOR_OUTPUT_NAME;
    }
}
