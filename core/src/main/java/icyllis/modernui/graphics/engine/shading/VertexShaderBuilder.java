/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.graphics.engine.shading;

import icyllis.modernui.graphics.engine.*;

import java.util.ArrayList;

import static icyllis.modernui.graphics.engine.Engine.ShaderFlags;

/**
 * This class implements the various vertex builder interfaces.
 */
public class VertexShaderBuilder extends ShaderBuilderBase implements VertexGeomBuilder {

    // vertex shader inputs are vertex attributes
    private final ArrayList<ShaderVar> mInputs = new ArrayList<>();

    public VertexShaderBuilder(ProgramBuilder programBuilder) {
        super(programBuilder);
    }

    @Override
    protected void onFinish() {
        // assign sequential locations, this setup *MUST* be consistent with
        // creating VertexArrayObject or PipelineVertexInputState later
        int locationIndex = 0;
        for (var var : mInputs) {
            String location = "location = " + locationIndex;
            var.addLayoutQualifier(location);

            // may contain matrix that takes up multiple locations
            int locationCount = ShaderDataType.locationCount(var.getType());
            assert (locationCount > 0);
            // we have no arrays
            assert (!var.isArray());
            locationIndex += locationCount;
        }

        mProgramBuilder.uniformHandler().appendUniformDecls(ShaderFlags.kVertex, uniforms());
        mProgramBuilder.appendDecls(mInputs, inputs());
        mProgramBuilder.varyingHandler().getVertDecls(outputs());
    }

    @Override
    public void emitAttributes(GeometryProcessor geomProc) {
        for (var attr : geomProc.getVertexAttributes()) {
            ShaderVar var = attr.asShaderVar();
            assert (var.getTypeModifier() == ShaderVar.kIn_TypeModifier);
            mInputs.add(var);
        }
        for (var attr : geomProc.getInstanceAttributes()) {
            ShaderVar var = attr.asShaderVar();
            assert (var.getTypeModifier() == ShaderVar.kIn_TypeModifier);
            mInputs.add(var);
        }
    }

    @Override
    public void emitNormalizedPosition(ShaderVar worldPos) {
        if (worldPos.getType() == ShaderDataType.kFloat3) {
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.zz * %2$s.yw, 0.0, %1$s.z);
                    """, worldPos.getName(), UniformHandler.PROJECTION_NAME);
        } else {
            assert (worldPos.getType() == ShaderDataType.kFloat2);
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %2$s.yw, 0.0, 1.0);
                    """, worldPos.getName(), UniformHandler.PROJECTION_NAME);
        }
    }
}
