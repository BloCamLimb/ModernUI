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

package icyllis.arc3d.engine.geom;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.*;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.*;

/**
 * The output of this effect is a modulation of the input color and coverage for a circle. It
 * operates in a space normalized by the circle radius (outer radius in the case of a stroke)
 * with origin at the circle center. Three vertex/instance attributes are used:
 * <ul>
 *    <li>vec2 : position in local space of the bounding geometry vertices</li>
 *    <li>vec4 : (p.xy, outerRad, innerRad)
 *            p is the position in the normalized local space.
 *            outerRad is the outerRadius in local space.
 *            innerRad is the innerRadius in normalized local space (ignored if not stroking).</li>
 *    <li>vec4 : color</li>
 * </ul>
 * Additional clip planes are supported for rendering circular arcs. The additional planes are
 * either intersected or unioned together. Up to three planes are supported (an initial plane,
 * a plane intersected with the initial plane, and a plane unioned with the first two). Only two
 * are useful for any given arc, but having all three in one instance allows combining different
 * types of arcs.
 * Round caps for stroking are allowed as well. The caps are specified as two circle center points
 * in the same space as p.xy.
 */
public class CircleProcessor extends GeometryProcessor {

    /**
     * Per-vertex attributes.
     */
    public static final Attribute
            POSITION = new Attribute("Position", VertexAttribType.kFloat2, SLDataType.kFloat2);
    // edge x, edge y, outer radius, inner radius (stroke)
    public static final Attribute
            CIRCLE_EDGE = new Attribute("CircleEdge", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * Per-instance attributes.
     */
    public static final Attribute
            COLOR = new Attribute("Color", VertexAttribType.kFloat4, SLDataType.kFloat4);
    /**
     * Per-instance attributes (optional).
     */
    public static final Attribute
            CLIP_PLANE = new Attribute("ClipPlane", VertexAttribType.kFloat3, SLDataType.kFloat3),
            ISECT_PLANE = new Attribute("IsectPlane", VertexAttribType.kFloat3, SLDataType.kFloat3),
            UNION_PLANE = new Attribute("UnionPlane", VertexAttribType.kFloat3, SLDataType.kFloat3),
            ROUND_CAP_CENTERS = new Attribute("RoundCapCenters", VertexAttribType.kFloat4, SLDataType.kFloat4);
    public static final Attribute
            MODEL_VIEW = new Attribute("ModelView", VertexAttribType.kFloat3, SLDataType.kFloat3x3);

    public static final AttributeSet VERTEX_FORMAT = AttributeSet.makeImplicit(
            POSITION, CIRCLE_EDGE);
    public static final AttributeSet INSTANCE_FORMAT = AttributeSet.makeImplicit(
            COLOR, CLIP_PLANE, ISECT_PLANE, UNION_PLANE, ROUND_CAP_CENTERS, MODEL_VIEW);

    private final int mFlags;

    public CircleProcessor(boolean stroke, boolean clipPlane, boolean isectPlane,
                           boolean unionPlane, boolean roundCaps) {
        super(Circle_Geom_ClassID);
        assert (!roundCaps || (stroke && clipPlane));
        int instanceMask = (clipPlane ? 1 << 1 : 0) |
                (isectPlane ? 1 << 2 : 0) |
                (unionPlane ? 1 << 3 : 0) |
                (roundCaps ? 1 << 4 : 0);
        mFlags = (stroke ? 1 : 0) | instanceMask;
        setVertexAttributes(0x3);
        setInstanceAttributes(0x1 | instanceMask | 0x20);
    }

    @Nonnull
    @Override
    public String name() {
        return "Circle_GeometryProcessor";
    }

    @Override
    public byte primitiveType() {
        return PrimitiveType.TriangleList;
    }

    @Override
    public void appendToKey(@Nonnull KeyBuilder b) {
        b.addBits(5, mFlags, "stroke|clipPlane|isectPlane|unionPlane|roundCaps");
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return new Impl();
    }

    @Override
    protected AttributeSet allVertexAttributes() {
        return VERTEX_FORMAT;
    }

    @Override
    protected AttributeSet allInstanceAttributes() {
        return INSTANCE_FORMAT;
    }

    private static class Impl extends ProgramImpl {

        @Override
        public void setData(UniformDataManager manager,
                            GeometryProcessor geomProc) {
        }

        @Override
        protected void onEmitCode(VertexGeomBuilder vertBuilder,
                                  FPFragmentBuilder fragBuilder,
                                  VaryingHandler varyingHandler,
                                  UniformHandler uniformHandler,
                                  ShaderCaps shaderCaps,
                                  GeometryProcessor geomProc,
                                  String outputColor,
                                  String outputCoverage,
                                  int[] texSamplers,
                                  ShaderVar localPos,
                                  ShaderVar worldPos) {
            final int flags = ((CircleProcessor) geomProc).mFlags;
            final boolean stroke = (flags & 1) != 0;
            final boolean clipPlane = (flags & (1 << 1)) != 0;
            final boolean isectPlane = (flags & (1 << 2)) != 0;
            final boolean unionPlane = (flags & (1 << 3)) != 0;
            final boolean roundCaps = (flags & (1 << 4)) != 0;

            // emit attributes
            vertBuilder.emitAttributes(geomProc);
            fragBuilder.codeAppend("""
                    vec4 circleEdge;
                    """);
            varyingHandler.addPassThroughAttribute(CIRCLE_EDGE, "circleEdge");
            if (clipPlane) {
                fragBuilder.codeAppend("""
                        vec3 clipPlane;
                        """);
                varyingHandler.addPassThroughAttribute(CLIP_PLANE,
                        "clipPlane", VaryingHandler.kCanBeFlat_Interpolation);
            }
            if (isectPlane) {
                fragBuilder.codeAppend("""
                        vec3 isectPlane;
                        """);
                varyingHandler.addPassThroughAttribute(ISECT_PLANE,
                        "isectPlane", VaryingHandler.kCanBeFlat_Interpolation);
            }
            if (unionPlane) {
                fragBuilder.codeAppend("""
                        vec3 unionPlane;
                        """);
                varyingHandler.addPassThroughAttribute(UNION_PLANE,
                        "unionPlane", VaryingHandler.kCanBeFlat_Interpolation);
            }
            Varying capRadius = new Varying(SLDataType.kFloat);
            if (roundCaps) {
                fragBuilder.codeAppend("""
                        vec4 roundCapCenters;
                        """);
                varyingHandler.addPassThroughAttribute(ROUND_CAP_CENTERS,
                        "roundCapCenters", VaryingHandler.kCanBeFlat_Interpolation);
                varyingHandler.addVarying("CapRadius", capRadius,
                        VaryingHandler.kCanBeFlat_Interpolation);
                // This is the cap radius in normalized space where the outer radius is 1 and
                // circledEdge.w is the normalized inner radius.
                vertBuilder.codeAppendf("""
                        %s = (1.0 - %s.w) / 2.0;
                        """, capRadius.vsOut(), CIRCLE_EDGE.name());
            }

            // setup pass through color
            fragBuilder.codeAppendf("""
                    vec4 %s;
                    """, outputColor);
            varyingHandler.addPassThroughAttribute(COLOR, outputColor,
                    VaryingHandler.kCanBeFlat_Interpolation);

            // setup position
            localPos.set(POSITION.name(), POSITION.dstType());
            writeWorldPosition(vertBuilder, localPos, MODEL_VIEW.name(), worldPos);

            fragBuilder.codeAppend("""
                    float d = length(circleEdge.xy);
                    float distanceToOuterEdge = circleEdge.z * (1.0 - d);
                    float edgeAlpha = clamp(distanceToOuterEdge, 0.0, 1.0);
                    """);
            if (stroke) {
                fragBuilder.codeAppend("""
                        float distanceToInnerEdge = circleEdge.z * (d - circleEdge.w);
                        float innerAlpha = clamp(distanceToInnerEdge, 0.0, 1.0);
                        edgeAlpha *= innerAlpha;
                        """);
            }

            if (clipPlane) {
                fragBuilder.codeAppend("""
                        float clip = clamp(circleEdge.z * dot(circleEdge.xy,
                                clipPlane.xy) + clipPlane.z, 0.0, 1.0);
                        """);
                if (isectPlane) {
                    fragBuilder.codeAppend("""
                            clip *= clamp(circleEdge.z * dot(circleEdge.xy,
                                    isectPlane.xy) + isectPlane.z, 0.0, 1.0);
                            """);
                }
                if (unionPlane) {
                    fragBuilder.codeAppend("""
                            clip = clamp(clip + clamp(circleEdge.z * dot(circleEdge.xy,
                                    unionPlane.xy) + unionPlane.z, 0.0, 1.0), 0.0, 1.0);
                            """);
                }
                fragBuilder.codeAppend("""
                        edgeAlpha *= clip;
                        """);
                if (roundCaps) {
                    // We compute coverage of the round caps as circles at the butt caps produced
                    // by the clip planes. The inverse of the clip planes is applied so that there
                    // is no double counting.
                    fragBuilder.codeAppendf("""
                            float dcap1 = circleEdge.z * (%s - length(circleEdge.xy -
                                                                      roundCapCenters.xy));
                            float dcap2 = circleEdge.z * (%s - length(circleEdge.xy -
                                                                      roundCapCenters.zw));
                            float capAlpha = (1.0 - clip) * (max(dcap1, 0.0) + max(dcap2, 0.0));
                            edgeAlpha = min(edgeAlpha + capAlpha, 1.0);
                            """, capRadius.fsIn(), capRadius.fsIn());
                }
            }
            fragBuilder.codeAppendf("""
                    vec4 %s = vec4(edgeAlpha);
                    """, outputCoverage);
        }
    }
}
