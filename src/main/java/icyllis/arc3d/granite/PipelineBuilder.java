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

import icyllis.arc3d.core.BlendMode;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;

import java.util.*;

/**
 * Build AST for graphics pipeline.
 */
//TODO currently this build full GLSL source, wait for compiler backend development
public class PipelineBuilder {

    // devicePos + painter's depth is our worldPos
    public static final String WORLD_POS_VAR_NAME = "worldPos";
    public static final String LOCAL_COORDS_VARYING_NAME = "f_LocalCoords";

    public static final String PRIMITIVE_COLOR_VAR_NAME = "primitiveColor";

    public static final int MAIN_DRAW_BUFFER_INDEX = 0;

    public static final int PRIMARY_COLOR_OUTPUT_INDEX = 0;
    public static final int SECONDARY_COLOR_OUTPUT_INDEX = 1;

    public static final String PRIMARY_COLOR_OUTPUT_NAME = "FragColor0";
    public static final String SECONDARY_COLOR_OUTPUT_NAME = "FragColor1";

    private final Caps mCaps;
    private final GraphicsPipelineDesc mDesc;

    private final FragmentNode[] mRootNodes;

    private final VaryingHandler mVaryings;
    private final UniformHandler mGeometryUniforms;
    private final UniformHandler mFragmentUniforms;

    // depth only pass will disable blend, then default is DST
    private BlendInfo mBlendInfo = BlendInfo.DST;

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
        mGeometryUniforms = new UniformHandler(mCaps.shaderCaps(), UniformHandler.Std140Layout);
        mFragmentUniforms = new UniformHandler(mCaps.shaderCaps(), UniformHandler.Std140Layout);
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

    public PipelineDesc.GraphicsPipelineInfo build() {

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
        mDesc.geomStep().emitSamplers(mFragmentUniforms);

        for (var root : mRootNodes) {
            getNodeUniforms(root);
        }

        buildFragmentShader();
        buildVertexShader();

        var info = new PipelineDesc.GraphicsPipelineInfo();
        info.mPrimitiveType = mDesc.getPrimitiveType();
        info.mInputLayout = mDesc.geomStep().getInputLayout();
        info.mVertSource = mVertCode;
        info.mFragSource = mFragCode;
        info.mBlendInfo = mBlendInfo;
        info.mDepthStencilSettings = mDesc.getDepthStencilSettings();
        info.mPipelineLabel = mDesc.geomStep().name();

        return info;
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

        // shader will define the world pos local variable
        mDesc.geomStep().emitVertexGeomCode(vs,
                WORLD_POS_VAR_NAME,
                needsLocalCoords() ? LOCAL_COORDS_VARYING_NAME : null);

        // map into clip space
        // remember to preserve the painter's depth in depth buffer, it must be first multiplied by w,
        // as it will be divided by w to viewport
        if (mCaps.depthClipNegativeOneToOne()) {
            // [0,1] -> [-1,1]
            vs.format("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.ww * %2$s.yw, (%1$s.z * 2.0 - 1.0) * %1$s.w, %1$s.w);
                    """, WORLD_POS_VAR_NAME, UniformHandler.PROJECTION_NAME);
        } else {
            // zero to one, default behavior
            vs.format("""
                    gl_Position = vec4(%1$s.xy * %2$s.xz + %1$s.ww * %2$s.yw, %1$s.z * %1$s.w, %1$s.w);
                    """, WORLD_POS_VAR_NAME, UniformHandler.PROJECTION_NAME);
        }

        out.append("}");
        mVertCode = out;
    }

    public void buildFragmentShader() {
        StringBuilder out = new StringBuilder();
        if (mDesc.isDepthOnlyPass()) {
            // Depth-only draw so no fragment shader to compile
            mFragCode = out;
            return;
        }

        BlendMode blendMode = mDesc.getFinalBlendMode();
        assert blendMode != null;

        BlendFormula coverageBlendFormula = null;
        if (mDesc.geomStep().emitsCoverage()) {
            //TODO: Determine whether draw is opaque and pass that to getBlendFormula.
            // we can avoid dual source blending if src is opaque
            coverageBlendFormula = BlendFormula.getBlendFormula(
                    /*isOpaque=*/false, /*hasCoverage=*/true, blendMode
            );
            if (coverageBlendFormula == null) {
                coverageBlendFormula = BlendFormula.getBlendFormula(
                        /*isOpaque=*/false, /*hasCoverage=*/true, BlendMode.SRC_OVER
                );
                assert coverageBlendFormula != null;
            }
        }

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
        {
            String layoutQualifier = "location=" + MAIN_DRAW_BUFFER_INDEX;
            ShaderVar primaryOutput = new ShaderVar(PRIMARY_COLOR_OUTPUT_NAME, SLDataType.kFloat4,
                    ShaderVar.kOut_TypeModifier,
                    ShaderVar.kNonArray, layoutQualifier, "");
            ShaderVar secondaryOutput = null;
            if (coverageBlendFormula != null && coverageBlendFormula.hasSecondaryOutput()) {
                secondaryOutput = new ShaderVar(SECONDARY_COLOR_OUTPUT_NAME, SLDataType.kFloat4,
                        ShaderVar.kOut_TypeModifier,
                        ShaderVar.kNonArray, layoutQualifier, "");
                primaryOutput.addLayoutQualifier("index", PRIMARY_COLOR_OUTPUT_INDEX);
                secondaryOutput.addLayoutQualifier("index", SECONDARY_COLOR_OUTPUT_INDEX);
            }
            primaryOutput.appendDecl(out);
            out.append(";\n");
            if (secondaryOutput != null) {
                secondaryOutput.appendDecl(out);
                out.append(";\n");
            }
        }

        ShaderCodeSource.emitDefinitions(mRootNodes, new IdentityHashMap<>(), fs);

        //// Entry Point
        out.append("void main() {\n");

        String outputColor;
        if (mDesc.usesFastSolidColor()) {
            out.append("vec4 initialColor;\n");
            mDesc.geomStep().emitFragmentColorCode(fs, "initialColor");
            outputColor = "initialColor";
        } else {
            if (mDesc.geomStep().emitsPrimitiveColor()) {
                fs.format("vec4 %s;\n", PRIMITIVE_COLOR_VAR_NAME);
                mDesc.geomStep().emitFragmentColorCode(fs, PRIMITIVE_COLOR_VAR_NAME);
            }
            outputColor = "vec4(0)";
        }
        String localCoords = needsLocalCoords() ? LOCAL_COORDS_VARYING_NAME : "vec2(0)";
        for (FragmentNode root : mRootNodes) {
            outputColor = ShaderCodeSource.invoke_node(root,
                    localCoords, outputColor, "vec4(1)", fs);
        }

        if (mDesc.geomStep().emitsCoverage()) {
            out.append("vec4 outputCoverage;\n");
            mDesc.geomStep().emitFragmentCoverageCode(fs, "outputCoverage");
            assert coverageBlendFormula != null;
            mBlendInfo = new BlendInfo(
                    coverageBlendFormula.mEquation,
                    coverageBlendFormula.mSrcFactor,
                    coverageBlendFormula.mDstFactor,
                    coverageBlendFormula.modifiesDst() // color write mask
            );
            appendColorOutput(fs,
                    coverageBlendFormula.mPrimaryOutput,
                    PRIMARY_COLOR_OUTPUT_NAME,
                    outputColor, "outputCoverage");
            if (coverageBlendFormula.hasSecondaryOutput()) {
                appendColorOutput(fs,
                        coverageBlendFormula.mSecondaryOutput,
                        SECONDARY_COLOR_OUTPUT_NAME,
                        outputColor, "outputCoverage");
            }
        } else {
            fs.format("%s = %s;\n", PRIMARY_COLOR_OUTPUT_NAME, outputColor);
            mBlendInfo = BlendInfo.getSimpleBlendInfo(blendMode);
            if (mBlendInfo == null) {
                mBlendInfo = BlendInfo.getSimpleBlendInfo(BlendMode.SRC_OVER);
                assert mBlendInfo != null;
            }
        }

        out.append("}");
        mFragCode = out;
    }

    private static void appendColorOutput(Formatter fs,
                                          byte outputType,
                                          String output,
                                          String inColor,
                                          String inCoverage) {
        switch (outputType) {
            case BlendFormula.OUTPUT_TYPE_ZERO:
                fs.format("%s = vec4(0.0);\n", output);
                break;
            case BlendFormula.OUTPUT_TYPE_COVERAGE:
                fs.format("%s = %s;\n", output, inCoverage);
                break;
            case BlendFormula.OUTPUT_TYPE_MODULATE:
                fs.format("%s = %s * %s;\n", output, inColor, inCoverage);
                break;
            case BlendFormula.OUTPUT_TYPE_SRC_ALPHA_MODULATE:
                fs.format("%s = %s.a * %s;\n", output, inColor, inCoverage);
                break;
            case BlendFormula.OUTPUT_TYPE_ONE_MINUS_SRC_ALPHA_MODULATE:
                fs.format("%s = (1.0 - %s.a) * %s;\n", output, inColor, inCoverage);
                break;
            case BlendFormula.OUTPUT_TYPE_ONE_MINUS_SRC_COLOR_MODULATE:
                fs.format("%s = (1.0 - %s) * %s;\n", output, inColor, inCoverage);
                break;
            default:
                throw new AssertionError("Unsupported output type.");
        }
    }
}
