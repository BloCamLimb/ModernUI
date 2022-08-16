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

package icyllis.arcui.engine.shading;

import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.GeometryProcessor;
import icyllis.arcui.engine.ShaderVar;
import icyllis.arcui.sksl.Compiler;

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
            int elementSize = SLType.locationSize(var.getType());
            assert (elementSize > 0);
            // we add this for now, there are currently no arrays though
            int numElements = var.isArray() ? var.getArrayCount() : 1;
            assert (numElements > 0);
            locationIndex += elementSize * numElements;
        }

        //TODO check max attrib count, minimum is 16

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
                    """, worldPos.getName(), Compiler.ORTHOPROJ_NAME);
        } else {
            assert (worldPos.getType() == SLType.Vec2);
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %2$s.yw, 0.0, 1.0);
                    """, worldPos.getName(), Compiler.ORTHOPROJ_NAME);
        }
    }
}
