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

package icyllis.akashigi.engine.shading;

import icyllis.akashigi.core.SLType;
import icyllis.akashigi.engine.Engine;
import icyllis.akashigi.engine.ShaderVar;

import javax.annotation.Nullable;

/**
 * This class implements the various fragment builder interfaces.
 */
public class FragmentShaderBuilder extends ShaderBuilderBase implements FPFragmentBuilder, XPFragmentBuilder {

    public static final int MAIN_DRAW_BUFFER_INDEX = 0;

    public static final int PRIMARY_COLOR_OUTPUT_INDEX = 0;
    public static final int SECONDARY_COLOR_OUTPUT_INDEX = 1;

    public static final String PRIMARY_COLOR_OUTPUT_NAME = "FragColor0";
    public static final String SECONDARY_COLOR_OUTPUT_NAME = "FragColor1";

    // fragment shader has at most two outputs, the second one is used for dual source blending
    private final ShaderVar mPrimaryOutput;
    private ShaderVar mSecondaryOutput;

    public FragmentShaderBuilder(ProgramBuilder programBuilder) {
        super(programBuilder);

        String layoutQualifier = "location = " + MAIN_DRAW_BUFFER_INDEX;
        mPrimaryOutput = new ShaderVar(PRIMARY_COLOR_OUTPUT_NAME, SLType.Vec4, ShaderVar.TypeModifier_Out,
                ShaderVar.NonArray, layoutQualifier, "");
        mPrimaryOutput.addLayoutQualifier("index = " + PRIMARY_COLOR_OUTPUT_INDEX);
    }

    @Override
    protected void onFinish() {
        mProgramBuilder.uniformHandler().appendUniformDecls(Engine.Fragment_ShaderFlag, uniforms());
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
        String layoutQualifier = "location = " + MAIN_DRAW_BUFFER_INDEX;
        mSecondaryOutput = new ShaderVar(SECONDARY_COLOR_OUTPUT_NAME, SLType.Vec4, ShaderVar.TypeModifier_Out,
                ShaderVar.NonArray, layoutQualifier, "");
        mSecondaryOutput.addLayoutQualifier("index = " + SECONDARY_COLOR_OUTPUT_INDEX);
    }

    @Nullable
    public String getSecondaryColorOutputName() {
        return (mSecondaryOutput == null) ? null : SECONDARY_COLOR_OUTPUT_NAME;
    }
}
