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

package icyllis.arc3d.granite.geom;

import icyllis.arc3d.core.RoundRect;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.granite.shading.VaryingHandler;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.util.Formatter;

/**
 * Unlike {@link CircleProcessor}, this processor uses SDF and supports over-stroking.
 * The stroke direction is CENTER. This processor uses instance rendering and static
 * vertex data.
 */
public class SDFRoundRectStep extends GeometryStep {

    /**
     * Per-instance attributes.
     */
    // per-multiplied color
    public static final VertexInputLayout.Attribute
            COLOR = new VertexInputLayout.Attribute("Color", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // scale x, translate x, scale y, translate y
    public static final VertexInputLayout.Attribute
            LOCAL_RECT = new VertexInputLayout.Attribute("LocalRect", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // radius, stroke radius (if stroke, or -1.0)
    public static final VertexInputLayout.Attribute
            RADII = new VertexInputLayout.Attribute("Radii", VertexAttribType.kFloat2, SLDataType.kFloat2);
    public static final VertexInputLayout.Attribute
            MODEL_VIEW = new VertexInputLayout.Attribute("ModelView", VertexAttribType.kFloat3, SLDataType.kFloat3x3);

    public static final VertexInputLayout.AttributeSet INSTANCE_ATTRIBS =
            VertexInputLayout.AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    COLOR, LOCAL_RECT, RADII, MODEL_VIEW);

    public SDFRoundRectStep() {
        super(RoundRect_GeoProc_ClassID, null, INSTANCE_ATTRIBS,
                FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA);
    }

    @Nonnull
    @Override
    public String name() {
        return "SDFRoundRect_GeomProc";
    }

    @Override
    public byte primitiveType() {
        return PrimitiveType.TriangleStrip;
    }

    @Override
    public void appendToKey(@Nonnull KeyBuilder b) {
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }

    @Override
    public void emitVaryings(VaryingHandler varyingHandler) {
        // the local coords, center point is (0,0)
        varyingHandler.addVarying("f_RectEdge", SLDataType.kFloat2);
        // half width, half height, corner radius and stroke radius
        varyingHandler.addVarying("f_SizeAndRadii", SLDataType.kFloat4,
                VaryingHandler.kCanBeFlat_Interpolation);
        // solid color
        varyingHandler.addVarying("f_Color", SLDataType.kFloat4,
                VaryingHandler.kCanBeFlat_Interpolation);
    }

    @Override
    public void emitVertexGeomCode(Formatter vs, boolean needsLocalCoords) {
        // {(-1,-1), (-1, 1), (1, -1), (1, 1)}
        vs.format("vec2 position = vec2(gl_VertexID >> 1, gl_VertexID & 1) * 2.0 - 1.0;");
        // add stroke radius and a full pixel bloat
        vs.format("""
                vec2 rectEdge = (%s.xz + %s.y + 2.0) * position;
                %s = rectEdge;
                """, LOCAL_RECT.name(), RADII.name(), "f_RectEdge");

        // setup pass through color
        vs.format("%s = %s;\n", "f_Color", COLOR.name());

        vs.format("""
                %s = vec4(%s.xz, %s);
                """, "f_SizeAndRadii", LOCAL_RECT.name(), RADII.name());

        // setup position
        vs.format("""
                vec2 localPos = rectEdge + %s.yw;
                """, LOCAL_RECT.name());
        // A float2 is promoted to a float3 if we add perspective via the matrix
        vs.format("vec3 %s = %s * vec3(%s, 1.0);\n",
                PipelineBuilder.WORLD_POS_VAR_NAME,
                MODEL_VIEW.name(),
                "localPos");
        if (needsLocalCoords) {
            vs.format("%s = %s;\n", PipelineBuilder.LOCAL_COORDS_VARYING_NAME, "localPos");
        }
    }

    @Override
    public void emitFragmentColorCode(Formatter fs, String outputColor) {
        // setup pass through color
        fs.format("%s = %s;\n", outputColor, "f_Color");
    }

    @Override
    public void emitFragmentCoverageCode(Formatter fs, String outputCoverage) {
        fs.format("""
                vec2 rectEdge = %s;
                """, "f_RectEdge");
        fs.format("""
                vec4 sizeAndRadii = %s;
                """, "f_SizeAndRadii");

        fs.format("""
                vec2 q = abs(rectEdge) - sizeAndRadii.xy + sizeAndRadii.z;
                float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - sizeAndRadii.z;
                if (sizeAndRadii.w >= 0) {
                    d = abs(d) - sizeAndRadii.w;
                }
                """);

        // use L2-norm of grad SDF
        fs.format("""
                float afwidth = length(vec2(dFdx(d),dFdy(d)))*0.7;
                float edgeAlpha = 1.0 - smoothstep(-afwidth,afwidth,d);
                """);
        fs.format("""
                %s = vec4(edgeAlpha);
                """, outputCoverage);
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw op, float[] solidColor) {
        writer.beginInstances(null, null, 4);
        ByteBuffer instanceData = writer.append(1);
        instanceData.putFloat(solidColor[0]);
        instanceData.putFloat(solidColor[1]);
        instanceData.putFloat(solidColor[2]);
        instanceData.putFloat(solidColor[3]);
        // local rect
        RoundRect localRect = (RoundRect) op.mGeometry;
        instanceData.putFloat((localRect.mRight - localRect.mLeft) * 0.5f);
        instanceData.putFloat((localRect.mLeft + localRect.mRight) * 0.5f);
        instanceData.putFloat((localRect.mBottom - localRect.mTop) * 0.5f);
        instanceData.putFloat((localRect.mTop + localRect.mBottom) * 0.5f);
        // radii
        instanceData.putFloat(localRect.mRadiusUL).putFloat(op.mStrokeRadius);
        var mat = op.mTransform;
        instanceData
                .putFloat(mat.m11).putFloat(mat.m12).putFloat(mat.m14)
                .putFloat(mat.m21).putFloat(mat.m22).putFloat(mat.m24)
                .putFloat(mat.m41).putFloat(mat.m42).putFloat(mat.m44);
        writer.endAppender();
    }
}
