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
import icyllis.akashigi.engine.*;

import java.util.ArrayList;

/**
 * This class implements the various vertex builder interfaces.
 */
public class VertexShaderBuilder extends ShaderBuilderBase implements VertexGeoBuilder {

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
            int locationSize = SLType.locationSize(var.getType());
            assert (locationSize > 0);
            // we have no arrays
            assert (!var.isArray());
            locationIndex += locationSize;
        }

        mProgramBuilder.uniformHandler().appendUniformDecls(Engine.Vertex_ShaderFlag, uniforms());
        mProgramBuilder.appendDecls(mInputs, inputs());
        mProgramBuilder.varyingHandler().getVertDecls(outputs());
    }

    @Override
    public void emitAttributes(GeometryProcessor geomProc) {
        for (var it = geomProc.vertexAttributes(); it.hasNext(); ) {
            var var = it.next().asShaderVar();
            assert (var.getTypeModifier() == ShaderVar.TypeModifier_In);
            mInputs.add(var);
        }
        for (var it = geomProc.instanceAttributes(); it.hasNext(); ) {
            var var = it.next().asShaderVar();
            assert (var.getTypeModifier() == ShaderVar.TypeModifier_In);
            mInputs.add(var);
        }
    }

    @Override
    public void emitNormalizedPosition(ShaderVar worldPos) {
        if (worldPos.getType() == SLType.Vec3) {
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.zz * %2$s.yw, 0.0, %1$s.z);
                    """, worldPos.getName(), UniformHandler.PROJECTION_NAME);
        } else {
            assert (worldPos.getType() == SLType.Vec2);
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %2$s.yw, 0.0, 1.0);
                    """, worldPos.getName(), UniformHandler.PROJECTION_NAME);
        }
    }
}
