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

package icyllis.arc3d.granite.geom;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.VertexInputLayout.Attribute;
import icyllis.arc3d.engine.VertexInputLayout.AttributeSet;
import icyllis.arc3d.granite.*;
import icyllis.arc3d.granite.shading.VaryingHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.Formatter;

/**
 * Analytic (SDF + HW derivatives) method to fill/stroke a butt/round/square stroked
 * arc curve, a circular sector, or a circular segment. The join type may be square or round,
 * for open arcs, and must be round for circular sector and circular segment.
 * The arc must be circular, not elliptical.
 */
public class AnalyticArcStep extends GeometryStep {

    /*
     * Per-instance attributes.
     */

    /**
     * (center X, center Y, start angle, sweep angle)
     */
    public static final Attribute LOCAL_ARC =
            new Attribute("LocalArc", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * X is circle radius.<br>
     * Y is stroke radius if stroked, or -1.0 if filled.<br>
     * Z is local AA radius.
     */
    public static final Attribute RADII =
            new Attribute("Radii", VertexAttribType.kFloat3, SLDataType.kFloat3);
    /**
     * W is half width for open arcs.
     */
    public static final Attribute RADII_ARC =
            new Attribute("Radii", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * Bitfield: <br>
     * 16-32 bits: painter's depth; <br>
     * 4-5 bits: join; <br>
     * 2-4 bits: dir; <br>
     * join=0: round, join=1: miter; <br>
     * dir=0: inside, dir=1: center, dir=2: outside; <br>
     */
    public static final Attribute FLAGS_AND_DEPTH =
            new Attribute("FlagsAndDepth", VertexAttribType.kUInt, SLDataType.kUInt);

    public static final AttributeSet INSTANCE_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, LOCAL_ARC, RADII, FLAGS_AND_DEPTH, MODEL_VIEW);
    public static final AttributeSet INSTANCE_ATTRIBS_FOR_ARC =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, LOCAL_ARC, RADII_ARC, FLAGS_AND_DEPTH, MODEL_VIEW);

    private final int mType;

    public AnalyticArcStep(int type) {
        super("AnalyticArcStep",
                switch (type) {
                    case ArcShape.kArc_Type -> "butt";
                    case ArcShape.kArcRound_Type -> "round";
                    case ArcShape.kArcSquare_Type -> "square";
                    case ArcShape.kPie_Type -> "pie";
                    case ArcShape.kChord_Type -> "chord";
                    default -> throw new AssertionError();
                },
                null, ArcShape.isOpenArc(type) ? INSTANCE_ATTRIBS_FOR_ARC : INSTANCE_ATTRIBS,
                FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA |
                        FLAG_HANDLE_SOLID_COLOR,
                PrimitiveType.kTriangleStrip,
                CommonDepthStencilSettings.kDirectDepthGreaterPass
        );
        mType = type;
    }

    @Override
    public void appendToKey(@NonNull KeyBuilder b) {

    }

    @NonNull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return null;
    }

    @Override
    public void emitVaryings(VaryingHandler varyingHandler, boolean usesFastSolidColor) {
        // the local coords, center point is (0,0)
        varyingHandler.addVarying("f_ArcEdge", SLDataType.kFloat2);
        // cos(sweepAngle), sin(sweepAngle)
        varyingHandler.addVarying("f_Span", SLDataType.kFloat2,
                VaryingHandler.kCanBeFlat_Interpolation);
        if (ArcShape.isOpenArc(mType)) {
            // circle radius, stroke radius, stroke offset, half width
            varyingHandler.addVarying("f_Radii", SLDataType.kFloat4,
                    VaryingHandler.kCanBeFlat_Interpolation);
        } else {
            // circle radius, stroke radius, stroke offset
            varyingHandler.addVarying("f_Radii", SLDataType.kFloat3,
                    VaryingHandler.kCanBeFlat_Interpolation);
        }
        if (usesFastSolidColor) {
            // solid color
            varyingHandler.addVarying("f_Color", SLDataType.kFloat4,
                    VaryingHandler.kCanBeFlat_Interpolation);
        }
    }

    @Override
    public void emitVertexGeomCode(Formatter vs,
                                   @NonNull String worldPosVar,
                                   @Nullable String localPosVar,
                                   boolean usesFastSolidColor) {
        // {(-1,-1), (-1,1), (1,-1), (1,1)}
        // corner selector, CCW
        vs.format("vec2 position = vec2(gl_VertexID >> 1, gl_VertexID & 1) * 2.0 - 1.0;\n");

        // center x, center y, start angle, sweep angle
        vs.format("""
                vec2 center = %1$s.xy;
                vec2 angle = %1$s.zw;
                """, LOCAL_ARC.name());

        // the rotation is inverted for fragment's local arc
        // we always have a rect in local space
        if (ArcShape.isOpenArc(mType)) {
            // for square end, outset is the maximum of sin(x)+cos(x) = sqrt(2)
            vs.format("""
                    const float kOutset = %f;
                    """, mType == ArcShape.kArcSquare_Type ? MathUtil.SQRT2 : 1.0f);
            vs.format("""
                    int flags = int(%2$s);
                    float join = float((flags >> 4) & 1);
                    float dir = float((flags >> 2) & 3);
                    float strokeRad = max(%1$s.y, 0.0);
                    float strokeOffset = (step(join, 0.0) * dir - 1.0) * strokeRad;
                    vec2 localEdge = (%1$s.x + %1$s.w * kOutset + strokeRad * dir + %1$s.z) * position;
                    vec2 cs = vec2(cos(angle.x), sin(angle.x));
                    %3$s = mat2(cs.x,-cs.y,cs.y,cs.x) * localEdge;
                    %4$s = vec2(cos(angle.y), sin(angle.y));
                    %5$s = vec4(%1$s.xy, strokeOffset, %1$s.w);
                    """, RADII_ARC.name(), FLAGS_AND_DEPTH.name(), "f_ArcEdge", "f_Span", "f_Radii");
        } else {
            vs.format("""
                    int flags = int(%2$s);
                    float join = float((flags >> 4) & 1);
                    float dir = float((flags >> 2) & 3);
                    float strokeRad = max(%1$s.y, 0.0);
                    float strokeOffset = (step(join, 0.0) * dir - 1.0) * strokeRad;
                    vec2 localEdge = (%1$s.x + strokeRad * dir + %1$s.z) * position;
                    vec2 cs = vec2(cos(angle.x), sin(angle.x));
                    %3$s = mat2(cs.x,-cs.y,cs.y,cs.x) * localEdge;
                    %4$s = vec2(cos(angle.y), sin(angle.y));
                    %5$s = vec3(%1$s.xy, strokeOffset);
                    """, RADII.name(), FLAGS_AND_DEPTH.name(), "f_ArcEdge", "f_Span", "f_Radii");
        }

        if (usesFastSolidColor) {
            // setup pass through color
            vs.format("%s = %s;\n", "f_Color", SOLID_COLOR.name());
        }

        // setup position
        vs.format("""
                vec2 localPos = localEdge + center;
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
        fs.format("""
                vec3 radii = %1$s.xyz;
                vec2 q = %2$s;
                vec2 cs = %3$s;
                """, "f_Radii", "f_ArcEdge", "f_Span");
        if (ArcShape.isOpenArc(mType)) {
            fs.format("""
                    float thick = %1$s.w;
                    """, "f_Radii");
        }

        // the shader code is based on IQ's article
        // see https://iquilezles.org/articles/distfunctions2d/
        // Ring - exact, Arc - exact, Horseshoe - exact, Pie - exact,
        // Cut Disk - exact, respectively
        fs.format("""
                q.x = abs(q.x);
                """);
        if (mType == ArcShape.kArc_Type) {
            fs.format("""
                    q = mat2(cs.x,cs.y,-cs.y,cs.x) * q;
                    float dis = max( abs(length(q) - radii.x) - thick,
                                     length(vec2(q.x, max(0.0, abs(radii.x - q.y) - thick))) * sign(q.x) );
                    """);
        } else if (mType == ArcShape.kArcRound_Type) {
            // ndot
            fs.format("""
                    float dis = mix( abs(length(q) - radii.x),
                                     length(q - cs.yx * radii.x),
                                     cs.x*q.x > cs.y*q.y) - thick;
                    """);
        } else if (mType == ArcShape.kArcSquare_Type) {
            fs.format("""
                    float l = length(q);
                    q = mat2(-cs.x,cs.y,cs.y,cs.x) * q;
                    q = vec2( mix(l * sign(-cs.x), q.x, any(bvec2(q.y>0.0, q.x>0.0))),
                              mix(l, q.y, q.x>0.0) );
                    q = vec2( q.x - thick, abs(q.y - radii.x) - thick );
                    float dis = min(max(q.x, q.y), 0.0) + length(max(q, 0.0));
                    """);
        } else if (mType == ArcShape.kPie_Type) {
            fs.format("""
                    float l = length(q) - radii.x;
                    float m = length(q - cs.yx * clamp(dot(q,cs.yx), 0.0, radii.x));
                    float dis = max(l, m * sign(cs.x*q.x - cs.y*q.y));
                    """);
        } else {
            assert mType == ArcShape.kChord_Type;
            fs.format("""
                    float h = cs.x * radii.x;
                    float w = sqrt(radii.x*radii.x - h*h);
                    float s = max( q.x*q.x*(h-radii.x) + w*w*(h+radii.x-2.0*q.y), h*q.x-w*q.y );
                    float dis = mix( mix( length(q-vec2(w,h)), h - q.y, q.x<w ), length(q) - radii.x, s<0.0 );
                    """);
        }
        fs.format("""
                dis = mix(dis, abs(dis - radii.z) - radii.y, radii.y >= 0.0);
                """);

        fs.format("""
                float afwidth = fwidth(dis);
                float edgeAlpha = 1.0 - clamp(dis/afwidth+0.5, 0.0, 1.0);
                """);

        fs.format("""
                %s = vec4(edgeAlpha);
                """, outputCoverage);
    }

    @Override
    public void writeMesh(MeshDrawWriter writer, Draw draw,
            float @Nullable[] solidColor,
                          boolean mayRequireLocalCoords) {
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
        ArcShape shape = (ArcShape) draw.mGeometry;
        MemoryUtil.memPutFloat(instanceData + 16, shape.mCenterX);
        MemoryUtil.memPutFloat(instanceData + 20, shape.mCenterY);
        if (mType == ArcShape.kArcSquare_Type) {
            MemoryUtil.memPutFloat(instanceData + 24,
                    (shape.mSweepAngle * 0.5F + shape.mStartAngle + 90) * MathUtil.DEG_TO_RAD);
            MemoryUtil.memPutFloat(instanceData + 28,
                    (180 - shape.mSweepAngle * 0.5F) * MathUtil.DEG_TO_RAD);
        } else {
            MemoryUtil.memPutFloat(instanceData + 24,
                    (shape.mSweepAngle * 0.5F + shape.mStartAngle - 90) * MathUtil.DEG_TO_RAD);
            MemoryUtil.memPutFloat(instanceData + 28,
                    (shape.mSweepAngle * 0.5F) * MathUtil.DEG_TO_RAD);
        }
        MemoryUtil.memPutFloat(instanceData + 32, shape.mRadius);
        MemoryUtil.memPutFloat(instanceData + 36, draw.mHalfWidth);
        MemoryUtil.memPutFloat(instanceData + 40, draw.mAARadius);
        if (ArcShape.isOpenArc(mType)) {
            MemoryUtil.memPutFloat(instanceData + 44, shape.mHalfWidth);
            instanceData += 4;
        }
        int dir = switch (draw.mStrokeAlign) {
            default -> 4;
            case Paint.ALIGN_INSIDE -> 0;
            case Paint.ALIGN_OUTSIDE -> 8;
        };
        // only butt and square arc can have miter join
        int join = (mType == ArcShape.kArc_Type || mType == ArcShape.kArcSquare_Type)
                && draw.mJoinLimit >= MathUtil.SQRT2 ? 16 : 0;
        MemoryUtil.memPutInt(instanceData + 44, (draw.getDepth() << 16) | (join | dir));
        draw.mTransform.store(instanceData + 48);
        writer.endAppender();
    }
}
