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
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Formatter;

/**
 * Analytic (SDF + HW derivatives) method to fill/stroke a butt/round/square stroked
 * arc curve. The join type may be square or round. The arc must be circular, not elliptical.
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
     * X is half width.<br>
     * Y is circle radius.<br>
     * Z is stroke radius if stroked, or -1.0 if filled.<br>
     * <p>
     * W is local AA radius.
     */
    public static final Attribute RADII =
            new Attribute("Radii", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * X is a bitfield: <br>
     * 4-5 bits: join; <br>
     * 2-4 bits: dir; <br>
     * join=0: round, join=1: miter; <br>
     * dir=0: inside, dir=1: center, dir=2: outside; <br>
     */
    public static final Attribute FLAGS_AND_DEPTH =
            new Attribute("FlagsAndDepth", VertexAttribType.kFloat2, SLDataType.kFloat2);

    public static final AttributeSet INSTANCE_ATTRIBS =
            AttributeSet.makeImplicit(VertexInputLayout.INPUT_RATE_INSTANCE,
                    SOLID_COLOR, LOCAL_ARC, RADII, FLAGS_AND_DEPTH, MODEL_VIEW);

    private final int mCapType;

    /**
     * The arc is a closed shape, paint's cap is ignored, this cap determines the shape
     * of the arc itself. Butt -> Ring, Round -> Arc, Square -> Horseshoe.
     */
    public AnalyticArcStep(int capType) {
        super("AnalyticArcStep",
                switch (capType) {
                    case Paint.CAP_BUTT -> "butt";
                    case Paint.CAP_ROUND -> "round";
                    case Paint.CAP_SQUARE -> "square";
                    default -> throw new AssertionError();
                },
                null, INSTANCE_ATTRIBS,
                FLAG_PERFORM_SHADING | FLAG_EMIT_COVERAGE | FLAG_OUTSET_BOUNDS_FOR_AA |
                        FLAG_HANDLE_SOLID_COLOR,
                PrimitiveType.TriangleStrip,
                CommonDepthStencilSettings.kDirectDepthGreaterPass
        );
        mCapType = capType;
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
        varyingHandler.addVarying("f_ArcEdge", SLDataType.kFloat2);
        // cos(sweepAngle), sin(sweepAngle)
        varyingHandler.addVarying("f_Span", SLDataType.kFloat2,
                VaryingHandler.kCanBeFlat_Interpolation);
        // half width, circle radius, stroke radius, stroke offset
        varyingHandler.addVarying("f_Radii", SLDataType.kFloat4,
                VaryingHandler.kCanBeFlat_Interpolation);
        // solid color
        varyingHandler.addVarying("f_Color", SLDataType.kFloat4,
                VaryingHandler.kCanBeFlat_Interpolation);
    }

    @Override
    public void emitVertexGeomCode(Formatter vs,
                                   @Nonnull String worldPosVar,
                                   @Nullable String localPosVar) {
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
        vs.format("""
                int flags = int(%2$s.x);
                float join = float((flags >> 4) & 1);
                float dir = float((flags >> 2) & 3);
                float strokeRad = max(%1$s.z, 0.0);
                float strokeOffset = (step(join, 0.0) * dir - 1.0) * strokeRad;
                vec2 localEdge = (%1$s.x + %1$s.y + strokeRad * dir + %1$s.w) * position;
                vec2 cs = vec2(cos(angle.x), sin(angle.x));
                %3$s = mat2(cs.x,-cs.y,cs.y,cs.x) * localEdge;
                %4$s = vec2(cos(angle.y), sin(angle.y));
                %5$s = vec4(%1$s.xyz, strokeOffset);
                """, RADII.name(), FLAGS_AND_DEPTH.name(), "f_ArcEdge", "f_Span", "f_Radii");

        // setup pass through color
        vs.format("%s = %s;\n", "f_Color", SOLID_COLOR.name());

        // setup position
        vs.format("""
                vec2 localPos = localEdge + center;
                """);
        // A float2 is promoted to a float3 if we add perspective via the matrix
        vs.format("vec3 devicePos = %s * vec3(localPos, 1.0);\n",
                MODEL_VIEW.name());
        vs.format("vec4 %s = vec4(devicePos.xy, %s.y, devicePos.z);\n",
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
                float thick = %1$s.x;
                vec3 radii = %1$s.yzw;
                vec2 q = %2$s;
                vec2 cs = %3$s;
                """, "f_Radii", "f_ArcEdge", "f_Span");

        // the shader code is based on IQ's article
        // see https://iquilezles.org/articles/distfunctions2d/
        // Ring - exact, Arc - exact, Horseshoe - exact, respectively
        fs.format("""
                q.x = abs(q.x);
                """);
        if (mCapType == Paint.CAP_BUTT) {
            fs.format("""
                    q = mat2(cs.x,cs.y,-cs.y,cs.x) * q;
                    float dis = max( abs(length(q) - radii.x) - thick,
                                     length(vec2(q.x, max(0.0, abs(radii.x - q.y) - thick))) * sign(q.x) );
                    """);
        } else if (mCapType == Paint.CAP_ROUND) {
            // ndot
            fs.format("""
                    float dis = mix( abs(length(q) - radii.x),
                                     length(q - cs.yx * radii.x),
                                     cs.x*q.x > cs.y*q.y) - thick;
                    """);
        } else {
            assert mCapType == Paint.CAP_SQUARE;
            fs.format("""
                    float l = length(q);
                    q = mat2(-cs.x,cs.y,cs.y,cs.x) * q;
                    q = vec2( mix(l * sign(-cs.x), q.x, any(bvec2(q.y>0.0, q.x>0.0))),
                              mix(l, q.y, q.x>0.0) );
                    q = vec2( q.x - thick, abs(q.y - radii.x) - thick );
                    float dis = min(max(q.x, q.y), 0.0) + length(max(q, 0.0));
                    """);
        }

        fs.format("""
                dis = mix(dis, abs(dis - radii.z) - radii.y, radii.y >= 0);
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
        ArcShape shape = (ArcShape) draw.mGeometry;
        MemoryUtil.memPutFloat(instanceData + 16, shape.mCenterX);
        MemoryUtil.memPutFloat(instanceData + 20, shape.mCenterY);
        MemoryUtil.memPutFloat(instanceData + 24,
                (shape.mSweepAngle * 0.5F + shape.mStartAngle + (mCapType == Paint.CAP_SQUARE ? 90 : -90)) * MathUtil.DEG_TO_RAD);
        MemoryUtil.memPutFloat(instanceData + 28,
                (shape.mSweepAngle * 0.5F) * MathUtil.DEG_TO_RAD);
        MemoryUtil.memPutFloat(instanceData + 32, shape.mHalfWidth);
        MemoryUtil.memPutFloat(instanceData + 36, shape.mRadius);
        MemoryUtil.memPutFloat(instanceData + 40, draw.mStrokeRadius);
        MemoryUtil.memPutFloat(instanceData + 44, draw.mAARadius);
        int dir = switch (draw.mStrokeAlign) {
            default -> 4;
            case Paint.ALIGN_INSIDE -> 0;
            case Paint.ALIGN_OUTSIDE -> 8;
        };
        // only butt and square arc can have miter join
        int join = (mCapType != Paint.CAP_ROUND) && draw.mJoinLimit >= MathUtil.SQRT2 ? 16 : 0;
        MemoryUtil.memPutFloat(instanceData + 48, (float) (join | dir));
        MemoryUtil.memPutFloat(instanceData + 52, draw.getDepthAsFloat());
        draw.mTransform.storeAs2D(instanceData + 56);
        writer.endAppender();
    }
}
