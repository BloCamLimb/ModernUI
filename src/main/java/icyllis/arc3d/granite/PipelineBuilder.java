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
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;

import java.util.*;

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
    private GraphicsPipelineDesc mDesc;

    private FragmentNode[] mRootNodes;

    private VaryingHandler mVaryings;
    private UniformHandler mGeometryUniforms;
    private UniformHandler mFragmentUniforms;

    private StringBuilder mVertCode;
    private StringBuilder mFragCode;

    private int mSnippetRequirementFlags;

    public PipelineBuilder(Device device, GraphicsPipelineDesc desc) {
        mCaps = device.getCaps();
        mDesc = desc;

        mRootNodes = desc.getRootNodes(device.getShaderCodeSource());

        for (FragmentNode root : mRootNodes) {
            mSnippetRequirementFlags |= root.requirementFlags();
        }

        mVaryings = new VaryingHandler(mCaps.shaderCaps());
        mGeometryUniforms = new UniformHandler(mCaps.shaderCaps());
        mFragmentUniforms = new UniformHandler(mCaps.shaderCaps());
    }

    private boolean needsLocalCoords() {
        return (mSnippetRequirementFlags & FragmentStage.kLocalCoords_ReqFlag) != 0;
    }

    private void getNodeUniforms(FragmentNode node) {
        node.stage().generateUniforms(
                mFragmentUniforms,
                node.stageIndex()
        );
        for (var child : node.children()) {
            getNodeUniforms(child);
        }
    }

    public void build() {

        mDesc.geomStep().emitVaryings(mVaryings);
        if (needsLocalCoords()) {
            mVaryings.addVarying(LOCAL_COORDS_VARYING_NAME, SLDataType.kFloat2);
        }
        mVaryings.finish();

        // first add the 2D orthographic projection
        mGeometryUniforms.addUniform(
                Engine.ShaderFlags.kVertex,
                SLDataType.kFloat4,
                UniformHandler.PROJECTION_NAME,
                -1);
        mDesc.geomStep().emitUniforms(mGeometryUniforms);

        for (var root : mRootNodes) {
            getNodeUniforms(root);
        }

        buildFragmentShader();
        buildVertexShader();
    }

    public void buildVertexShader() {
        StringBuilder out = new StringBuilder();
        out.append(mCaps.shaderCaps().mGLSLVersion.mVersionDecl);
        Formatter vs = new Formatter(out, Locale.ROOT);

        //// Uniforms
        mGeometryUniforms.appendUniformDecls(Engine.ShaderFlags.kVertex,
                DrawPass.GEOMETRY_UNIFORM_BINDING, "GeometryUniforms", out);

        //// Attributes
        var inputLayout = mDesc.geomStep().getInputLayout();
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

        mDesc.geomStep().emitVertexGeomCode(vs, needsLocalCoords());

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
        assert !mDesc.geomStep().emitsCoverage() || mDesc.geomStep().performsShading();

        //// Uniforms
        if (mDesc.geomStep().emitsCoverage()) {
            mGeometryUniforms.appendUniformDecls(Engine.ShaderFlags.kFragment,
                    DrawPass.GEOMETRY_UNIFORM_BINDING, "GeometryUniforms", out);
        }
        mFragmentUniforms.appendUniformDecls(Engine.ShaderFlags.kFragment,
                DrawPass.FRAGMENT_UNIFORM_BINDING, "FragmentUniforms", out);
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

        ShaderCodeSource.emitDefinitions(mRootNodes, new IdentityHashMap<>(), fs);

        //// Entry Point
        out.append("void main() {\n");

        out.append("vec4 initialColor;\n");
        mDesc.geomStep().emitFragmentColorCode(fs, "initialColor");

        String outputColor = "initialColor";
        String localCoords = needsLocalCoords() ? LOCAL_COORDS_VARYING_NAME : "vec2(0)";
        for (FragmentNode root : mRootNodes) {
            outputColor = ShaderCodeSource.emitGlueCode(root,
                    localCoords, outputColor, "vec4(1)", fs);
        }

        if (mDesc.geomStep().emitsCoverage()) {
            out.append("vec4 outputCoverage;\n");
            mDesc.geomStep().emitFragmentCoverageCode(fs, "outputCoverage");
            fs.format("%s = %s * %s;\n", PRIMARY_COLOR_OUTPUT_NAME, outputColor, "outputCoverage");
        } else {
            fs.format("%s = %s;\n", PRIMARY_COLOR_OUTPUT_NAME, outputColor);
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
