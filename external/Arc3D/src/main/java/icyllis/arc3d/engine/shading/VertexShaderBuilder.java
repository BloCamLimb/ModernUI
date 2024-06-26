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

package icyllis.arc3d.engine.shading;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.GeometryProcessor;
import icyllis.arc3d.engine.ShaderVar;

import java.util.ArrayList;

import static icyllis.arc3d.engine.Engine.ShaderFlags;

/**
 * This class implements the various vertex builder interfaces.
 */
public class VertexShaderBuilder extends ShaderBuilderBase implements VertexGeomBuilder {

    // vertex shader inputs are vertex attributes
    private final ArrayList<ShaderVar> mInputs = new ArrayList<>();

    public VertexShaderBuilder(PipelineBuilder pipelineBuilder) {
        super(pipelineBuilder);
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
            int locations = SLDataType.locations(var.getType());
            assert (locations > 0);
            // we have no arrays
            assert (!var.isArray());
            locationIndex += locations;
        }

        mPipelineBuilder.uniformHandler().appendUniformDecls(ShaderFlags.kVertex, uniforms());
        mPipelineBuilder.appendDecls(mInputs, inputs());
        mPipelineBuilder.varyingHandler().getVertDecls(outputs());
    }

    @Override
    public void emitAttributes(GeometryProcessor geomProc) {
        for (var attr : geomProc.vertexAttributes()) {
            ShaderVar var = attr.asShaderVar();
            assert (var.getTypeModifier() == ShaderVar.kIn_TypeModifier);
            mInputs.add(var);
        }
        for (var attr : geomProc.instanceAttributes()) {
            ShaderVar var = attr.asShaderVar();
            assert (var.getTypeModifier() == ShaderVar.kIn_TypeModifier);
            mInputs.add(var);
        }
    }

    @Override
    public void emitNormalizedPosition(ShaderVar devicePos) {
        if (devicePos.getType() == SLDataType.kFloat3) {
            // xy0w
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.zz * %2$s.yw, 0.0, %1$s.z);
                    """, devicePos.getName(), UniformHandler.PROJECTION_NAME);
        } else {
            assert (devicePos.getType() == SLDataType.kFloat2);
            // xy01
            codeAppendf("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %2$s.yw, 0.0, 1.0);
                    """, devicePos.getName(), UniformHandler.PROJECTION_NAME);
        }
    }
}
