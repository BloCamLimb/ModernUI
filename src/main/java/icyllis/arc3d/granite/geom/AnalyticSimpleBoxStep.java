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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.VertexInputLayout.Attribute;
import icyllis.arc3d.engine.VertexInputLayout.AttributeSet;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.granite.shading.UniformHandler;
import icyllis.arc3d.granite.shading.VaryingHandler;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Formatter;

/**
 * This technology draws zero-area lines (round cap, square cap, butt cap),
 * shaped lines (round, square; fill, stroke, stroke and fill), rectangles
 * (fill, stroke, stroke and fill; round join, miter join), round rectangles
 * with circular corners (fill, stroke, stroke and fill; four corners have the
 * same radius), circle (fill, stroke, stroke and fill), supports stroke
 * direction (inside, center, outside) using analytic method.
 * <p>
 * Always use instanced rendering, without per-vertex data, without index buffer,
 * without uniforms, emits coverage. Supports solid color, supports over-stroking,
 * supports device-independent antialiasing, supports 32 bit-per-channel color input,
 * support lines in local coordinates, supports hard-edge coverage (no AA), supports
 * any local-to-device transforms.
 */
public class AnalyticSimpleBoxStep extends GeometryStep {

    /*
     * Per-instance attributes.
     */

    /**
     * (left, top, right, bottom) or ((startX, startY), (stopX, stopY))
     */
    public static final Attribute LOCAL_RECT =
            new Attribute("LocalRect", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * X is corner radius for rect or half width for line.<br>
     * Y is stroke radius if stroked, or -1.0 if filled.<br>
     * Z is local AA radius.
     */
    public static final Attribute RADII =
            new Attribute("Radii", VertexAttribType.kFloat3, SLDataType.kFloat3);
    /**
     * Bitfield: <br>
     * 16-32 bits: painter's depth; <br>
     * 4-5 bits: join; <br>
     * 2-4 bits: dir; <br>
     * 0-2 bits: type; <br>
     * join=0: round, join=1: miter; <br>
     * dir=0: inside, dir=1: center, dir=2: outside; <br>
     * type=0: rect, type=1: round line, type=2 butt line; <br>
     */
    public static final Attribute FLAGS_AND_DEPTH =
            new Attribute("FlagsAndDepth", VertexAttribType.kUInt, SLDataType.kUInt);

    public static final AttributeSet INSTANCE_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, LOCAL_RECT, RADII, FLAGS_AND_DEPTH, MODEL_VIEW);

    private final boolean mAA;

    public AnalyticSimpleBoxStep(boolean aa) {
        super("AnalyticSimpleBoxStep",
                aa ? "aa" : "non-aa",
                null, INSTANCE_ATTRIBS,
                aa
                        ? (FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA |
                        FLAG_HANDLE_SOLID_COLOR)
                        : (FLAG_PERFORM_SHADING | FLAG_HANDLE_SOLID_COLOR),
                PrimitiveType.kTriangleStrip,
                CommonDepthStencilSettings.kDirectDepthGreaterPass
        );
        mAA = aa;
        assert instanceStride() == 84;
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
        // half width, half height
        varyingHandler.addVarying("f_Size", SLDataType.kFloat2,
                VaryingHandler.kCanBeFlat_Interpolation);
        // corner radius, stroke radius, stroke offset
        varyingHandler.addVarying("f_Radii", SLDataType.kFloat3,
                VaryingHandler.kCanBeFlat_Interpolation);
        // solid color
        varyingHandler.addVarying("f_Color", SLDataType.kFloat4,
                VaryingHandler.kCanBeFlat_Interpolation);
    }

    @Override
    public void emitUniforms(UniformHandler uniformHandler) {
    }

    @Override
    public void emitVertexGeomCode(Formatter vs,
                                   @Nonnull String worldPosVar,
                                   @Nullable String localPosVar) {
        // {(-1,-1), (-1,1), (1,-1), (1,1)}
        // corner selector, CCW
        vs.format("vec2 position = vec2(gl_VertexID >> 1, gl_VertexID & 1) * 2.0 - 1.0;\n");

        // scale x, translate x, scale y, translate y
        vs.format("""
                vec2 scale = (%1$s.zw - %1$s.xy) * 0.5;
                vec2 translate = (%1$s.xy + %1$s.zw) * 0.5;
                """, LOCAL_RECT.name());

        // dir=0: inside, dir=1: center, dir=2: outside
        // rect edge = (size + stroke radius * dir + local AA radius) * corner selector
        // stroke offset = (dir - 1) * stroke radius

        // for miter join, always inner stroke and increase size

        // type >= 1, handle line
        // cos(atan(x)) = inversesqrt(1+x^2)
        // sin(atan(x)) = cos(atan(x)) * x
        vs.format("""
                int flags = int(%2$s);
                float join = float((flags >> 4) & 1);
                float dir = float((flags >> 2) & 3);
                int type = flags & 3;
                vec2 localEdge;
                float strokeRad = max(%1$s.y, 0.0);
                float strokeOffset = (step(join, 0.0) * dir - 1.0) * strokeRad;
                if (type >= 1) {
                    float len = length(scale);
                    vec2 size = vec2(len, %1$s.x);
                    localEdge = (size + strokeRad * dir + %1$s.z) * position;
                    %3$s = localEdge;
                    %4$s = size + join * dir * strokeRad;
                    %5$s = vec3(mix(%1$s.x, 0.0, type >= 2), %1$s.y, strokeOffset);
                    vec2 cs = scale / len;
                    localEdge = mat2(cs.x,cs.y,-cs.y,cs.x) * localEdge;
                } else {
                    localEdge = (scale + strokeRad * dir + %1$s.z) * position;
                    %3$s = localEdge;
                    %4$s = scale + join * dir * strokeRad;
                    %5$s = vec3(%1$s.xy, strokeOffset);
                }
                """, RADII.name(), FLAGS_AND_DEPTH.name(), "f_RectEdge", "f_Size", "f_Radii");

        // setup pass through color
        vs.format("%s = %s;\n", "f_Color", SOLID_COLOR.name());

        // setup position
        vs.format("""
                vec2 localPos = localEdge + translate;
                """);
        // A float2 is promoted to a float3 if we add perspective via the matrix
        vs.format("vec3 devicePos = %s * vec3(localPos, 1.0);\n",
                MODEL_VIEW.name());
        vs.format("vec4 %s = vec4(devicePos.xy, float(%s >> 16u) / 65535.0, devicePos.z);\n",
                worldPosVar,
                FLAGS_AND_DEPTH.name());
        if (localPosVar != null) {
            vs.format("%s = localPos;\n", localPosVar);
        }
    }

    @Override
    public void emitFragmentColorCode(Formatter fs, String outputColor) {
        // setup pass through color
        fs.format("%s = %s;\n", outputColor, "f_Color");
    }

    @Override
    public void emitFragmentCoverageCode(Formatter fs, String outputCoverage) {
        // corner radius, stroke radius, stroke offset
        fs.format("""
                vec3 radii = %s;
                vec2 q = abs(%s) - %s + radii.x;
                """, "f_Radii", "f_RectEdge", "f_Size");

        fs.format("""
                float dis = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radii.x;
                dis = mix(dis, abs(dis - radii.z) - radii.y, radii.y >= 0.0);
                """);

        if (mAA) {
            // we previously used L2-norm of grad(SDF) as:
            // float afwidth = length(vec2(dFdx(dis),dFdy(dis))) * 0.7021;
            // float edgeAlpha = 1.0 - smoothstep(-afwidth, afwidth, dis);
            // 0.7021 < 0.7071 (slightly smaller than half of the diagonal)
            // however we found that L1 norm (fwidth(dis) * 0.5) and linear interpolation
            // are better when geometry is axis-aligned, L1 may be faster than L2
            fs.format("""
                    float afwidth = fwidth(dis);
                    float edgeAlpha = 1.0 - clamp(dis/afwidth+0.5, 0.0, 1.0);
                    """);
            assert outputCoverage != null;
            fs.format("""
                %s = vec4(edgeAlpha);
                """, outputCoverage);
        } else {
            // hard edge
            // discard may reduce performance on certain GPUs, however, in non-AA case,
            // we would like to get a mask for z-culling
            fs.format("""
                    float edgeAlpha = 1.0 - step(0.0, dis);
                    if (edgeAlpha <= 0.0) discard;
                    """);
            assert outputCoverage == null;
        }
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
        SimpleShape shape = (SimpleShape) draw.mGeometry;
        MemoryUtil.memPutFloat(instanceData + 16, shape.left());
        MemoryUtil.memPutFloat(instanceData + 20, shape.top());
        MemoryUtil.memPutFloat(instanceData + 24, shape.right());
        MemoryUtil.memPutFloat(instanceData + 28, shape.bottom());
        // radii
        MemoryUtil.memPutFloat(instanceData + 32, shape.getSimpleRadiusX());
        MemoryUtil.memPutFloat(instanceData + 36, draw.mHalfWidth);
        MemoryUtil.memPutFloat(instanceData + 40, draw.mAARadius);
        int dir = switch (draw.mStrokeAlign) {
            default -> 4;
            case Paint.ALIGN_INSIDE -> 0;
            case Paint.ALIGN_OUTSIDE -> 8;
        };
        int type = switch (shape.getType()) {
            default -> 0;
            case SimpleShape.kLine_Type -> 2;
            case SimpleShape.kLineRound_Type -> 1;
        };
        // only butt/square line and rect can have miter join
        int join = (type == 2 || shape.isRect()) && draw.mJoinLimit >= MathUtil.SQRT2 ? 16 : 0;
        MemoryUtil.memPutInt(instanceData + 44, (draw.getDepth() << 16) | (join | dir | type));
        draw.mTransform.store(instanceData + 48);
        writer.endAppender();
    }
}
