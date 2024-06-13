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
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private final boolean mAA;

    public SDFRoundRectStep(boolean aa) {
        super(RoundRect_GeoProc_ClassID, null, INSTANCE_ATTRIBS,
                aa
                        ? (FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA |
                        FLAG_HANDLE_SOLID_COLOR)
                        : (FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_EMIT_01_COVERAGE |
                        FLAG_HANDLE_SOLID_COLOR)
        );
        mAA = aa;
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
        //TODO pass local AA radius instead of 2.0
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

        if (mAA) {
            // use L2-norm of grad SDF
            fs.format("""
                    float afwidth = length(vec2(dFdx(d),dFdy(d)))*0.7;
                    float edgeAlpha = 1.0 - smoothstep(-afwidth,afwidth,d);
                    """);
        } else {
            // hard edge
            fs.format("""
                    float edgeAlpha = 1.0 - step(0.0,d);
                    if (edgeAlpha <= 0.0) discard;
                    """);
        }
        fs.format("""
                %s = vec4(edgeAlpha);
                """, outputCoverage);
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw draw, @Nullable float[] solidColor) {
        writer.beginInstances(null, null, 4);
        long instanceData = writer.append(1);
        if (solidColor != null) {
            MemoryUtil.memPutFloat(instanceData, solidColor[0]);
            MemoryUtil.memPutFloat(instanceData + 4, solidColor[1]);
            MemoryUtil.memPutFloat(instanceData + 8, solidColor[2]);
            MemoryUtil.memPutFloat(instanceData + 12, solidColor[3]);
        } else {
            // 0.0F is 0s
            MemoryUtil.memPutLong(instanceData, 0);
            MemoryUtil.memPutLong(instanceData + 8, 0);
        }
        // local rect
        RoundRect localRect = (RoundRect) draw.mGeometry;
        MemoryUtil.memPutFloat(instanceData + 16, (localRect.mRight - localRect.mLeft) * 0.5f);
        MemoryUtil.memPutFloat(instanceData + 20, (localRect.mLeft + localRect.mRight) * 0.5f);
        MemoryUtil.memPutFloat(instanceData + 24, (localRect.mBottom - localRect.mTop) * 0.5f);
        MemoryUtil.memPutFloat(instanceData + 28, (localRect.mTop + localRect.mBottom) * 0.5f);
        // radii
        MemoryUtil.memPutFloat(instanceData + 32, localRect.mRadiusUL);
        MemoryUtil.memPutFloat(instanceData + 36, draw.mStrokeRadius);
        var mat = draw.mTransform;
        mat.storeAs2D(instanceData + 40);
        writer.endAppender();
    }
}
