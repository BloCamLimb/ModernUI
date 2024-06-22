/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.Device;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;

import java.util.Formatter;
import java.util.Locale;

/**
 * Build AST for graphics pipeline.
 */
public class PipelineBuilder {

    public static final String WORLD_POS_VAR_NAME = "worldPos";
    public static final String LOCAL_COORDS_VARYING_NAME = "f_LocalCoords";

    public static final int MAIN_DRAW_BUFFER_INDEX = 0;

    public static final int PRIMARY_COLOR_OUTPUT_INDEX = 0;
    public static final int SECONDARY_COLOR_OUTPUT_INDEX = 1;

    public static final String PRIMARY_COLOR_OUTPUT_NAME = "FragColor0";
    public static final String SECONDARY_COLOR_OUTPUT_NAME = "FragColor1";

    private Caps mCaps;

    private GeometryStep mGeometryStep;

    private VaryingHandler mVaryings;
    private UniformHandler mGeometryUniforms;
    private UniformHandler mFragmentUniforms;

    private StringBuilder mVertCode;
    private StringBuilder mFragCode;

    private boolean mSnippetRequirementFlags;

    public PipelineBuilder(Caps caps, GeometryStep geometryStep) {
        mCaps = caps;
        mGeometryStep = geometryStep;

        mVaryings = new VaryingHandler(caps.shaderCaps());
        mGeometryUniforms = new UniformHandler(caps.shaderCaps());
        mFragmentUniforms = new UniformHandler(caps.shaderCaps());
    }

    private boolean needsLocalCoords() {
        return false;
    }

    public void build() {

        mGeometryStep.emitVaryings(mVaryings);
        if (needsLocalCoords()) {
            mVaryings.addVarying(LOCAL_COORDS_VARYING_NAME, SLDataType.kFloat2);
        }
        mVaryings.finish();

        // first add the 2D orthographic projection
        mGeometryUniforms.addUniform(
                null,
                Engine.ShaderFlags.kVertex,
                SLDataType.kFloat4,
                UniformHandler.PROJECTION_NAME);
        mGeometryStep.emitUniforms(mGeometryUniforms);

        buildFragmentShader();
        buildVertexShader();
    }

    public void buildVertexShader() {
        StringBuilder out = new StringBuilder();
        out.append(mCaps.shaderCaps().mGLSLVersion.mVersionDecl);
        Formatter vs = new Formatter(out, Locale.ROOT);

        //// Uniforms
        mGeometryUniforms.appendUniformDecls(Engine.ShaderFlags.kVertex, out);

        //// Attributes
        var inputLayout = mGeometryStep.getInputLayout();
        // assign sequential locations, this setup *MUST* be consistent with
        // creating VertexArrayObject or PipelineVertexInputState later
        int locationIndex = 0;
        for (int i = 0; i < inputLayout.getBindingCount(); i++) {
            var attrs = inputLayout.getAttributes(i);
            while (attrs.hasNext()) {
                var attr = attrs.next();
                ShaderVar var = attr.asShaderVar();
                assert (var.getTypeModifier() == ShaderVar.kIn_TypeModifier);

                var.addLayoutQualifier("location", locationIndex);

                // matrix type can consume multiple locations
                int locations = SLDataType.locations(var.getType());
                assert (locations > 0);
                // we have no arrays
                assert (!var.isArray());
                locationIndex += locations;

                var.appendDecl(out);
                out.append(";\n");
            }
        }

        //// Varyings
        mVaryings.getVertDecls(out);

        //// Entry Point
        out.append("void main() {\n");

        mGeometryStep.emitVertexGeomCode(vs, needsLocalCoords());

        // map into clip space
        vs.format("""
                gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.ww * %2$s.yw, %1$s.zw);
                """, WORLD_POS_VAR_NAME, UniformHandler.PROJECTION_NAME);

        out.append("}");
        mVertCode = out;
    }

    public void buildFragmentShader() {
        StringBuilder out = new StringBuilder();
        out.append(mCaps.shaderCaps().mGLSLVersion.mVersionDecl);
        Formatter fs = new Formatter(out, Locale.ROOT);
        // If we're doing analytic coverage, we must also be doing shading.
        assert !mGeometryStep.emitsCoverage() || mGeometryStep.performsShading();

        //// Uniforms
        if (mGeometryStep.emitsCoverage()) {
            mGeometryUniforms.appendUniformDecls(Engine.ShaderFlags.kFragment, out);
        }
        //TODO fragment uniforms

        //// Varyings
        mVaryings.getFragDecls(out);

        //// Outputs
        String layoutQualifier = "location = " + MAIN_DRAW_BUFFER_INDEX;
        var primaryOutput = new ShaderVar(PRIMARY_COLOR_OUTPUT_NAME, SLDataType.kFloat4,
                ShaderVar.kOut_TypeModifier,
                ShaderVar.kNonArray, layoutQualifier, "");
        primaryOutput.appendDecl(out);
        out.append(";\n");

        //// Entry Point
        out.append("void main() {\n");

        out.append("vec4 initialColor;\n");
        mGeometryStep.emitFragmentColorCode(fs, "initialColor");

        if (mGeometryStep.emitsCoverage()) {
            out.append("vec4 outputCoverage;\n");
            mGeometryStep.emitFragmentCoverageCode(fs, "outputCoverage");
            fs.format("%s = %s * %s;\n", PRIMARY_COLOR_OUTPUT_NAME, "initialColor", "outputCoverage");
        } else {
            fs.format("%s = %s;\n", PRIMARY_COLOR_OUTPUT_NAME, "initialColor");
        }

        out.append("}");
        mFragCode = out;
    }

    public StringBuilder getVertCode() {
        return mVertCode;
    }

    public StringBuilder getFragCode() {
        return mFragCode;
    }
}
