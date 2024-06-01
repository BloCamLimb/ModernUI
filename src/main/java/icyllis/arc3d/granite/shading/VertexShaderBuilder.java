/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite.shading;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.granite.GeometryStep;
import icyllis.arc3d.engine.ShaderVar;

import java.util.ArrayList;

import static icyllis.arc3d.engine.Engine.ShaderFlags;

/**
 * This class implements the various vertex builder interfaces.
 */
public class VertexShaderBuilder extends ShaderBuilderBase implements VertexGeomBuilder {

    // vertex shader inputs are vertex attributes
    private final ArrayList<ShaderVar> mInputs = new ArrayList<>();

    public VertexShaderBuilder(GraphicsPipelineBuilder pipelineBuilder) {
        super(pipelineBuilder);
    }

    @Override
    protected void onFinish() {
        // assign sequential locations, this setup *MUST* be consistent with
        // creating VertexArrayObject or PipelineVertexInputState later
        int locationIndex = 0;
        for (var var : mInputs) {
            var.addLayoutQualifier("location", locationIndex);

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
    public void emitAttributes(GeometryStep geomProc) {
        var inputLayout = geomProc.getInputLayout();
        for (int i = 0; i < inputLayout.getBindingCount(); i++) {
            var attrs = inputLayout.getAttributes(i);
            while (attrs.hasNext()) {
                var attr = attrs.next();
                ShaderVar var = attr.asShaderVar();
                assert (var.getTypeModifier() == ShaderVar.kIn_TypeModifier);
                mInputs.add(var);
            }
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
