/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine.geom;

import icyllis.arcui.core.Matrix3;
import icyllis.arcui.core.SLType;
import icyllis.arcui.engine.*;
import icyllis.arcui.engine.shading.*;

import javax.annotation.Nonnull;

import static icyllis.arcui.engine.EngineTypes.*;
import static icyllis.arcui.engine.shading.ProgramDataManager.UniformHandle;

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
public class CircleGeometryProcessor extends GeometryProcessor {

    /**
     * Per-vertex attributes.
     */
    public static final Attribute
            POSITION = new Attribute("Position", Float2_VertexAttribType, SLType.Vec2);
    // edge x, edge y, outer radius, inner radius (stroke)
    public static final Attribute
            CIRCLE_EDGE = new Attribute("CircleEdge", Float4_VertexAttribType, SLType.Vec4);
    /**
     * Per-instance attributes.
     */
    public static final Attribute
            COLOR = new Attribute("Color", Float4_VertexAttribType, SLType.Vec4);
    /**
     * Per-instance attributes (optional).
     */
    public static final Attribute
            CLIP_PLANE = new Attribute("ClipPlane", Float3_VertexAttribType, SLType.Vec3),
            ISECT_PLANE = new Attribute("IsectPlane", Float3_VertexAttribType, SLType.Vec3),
            UNION_PLANE = new Attribute("UnionPlane", Float3_VertexAttribType, SLType.Vec3),
            ROUND_CAP_CENTERS = new Attribute("RoundCapCenters", Float4_VertexAttribType, SLType.Vec4);

    public static final AttributeSet VERTEX_FORMAT = AttributeSet.makeImplicit(
            POSITION, CIRCLE_EDGE);
    public static final AttributeSet INSTANCE_FORMAT = AttributeSet.makeImplicit(
            COLOR, CLIP_PLANE, ISECT_PLANE, UNION_PLANE, ROUND_CAP_CENTERS);

    private final Matrix3 mModelView;
    private final int mFlags;

    /**
     * @param modelView must be immutable, use {@link Matrix3#clone()} to ensure this
     */
    public CircleGeometryProcessor(boolean stroke, boolean clipPlane, boolean isectPlane,
                                   boolean unionPlane, boolean roundCaps, Matrix3 modelView) {
        super(Circle_Geom_ClassID);
        assert (!roundCaps || (stroke && clipPlane));
        int instanceMask = (clipPlane ? 1 << 1 : 0) |
                (isectPlane ? 1 << 2 : 0) |
                (unionPlane ? 1 << 3 : 0) |
                (roundCaps ? 1 << 4 : 0);
        mFlags = (stroke ? 1 : 0) | instanceMask;
        setVertexAttributes(VERTEX_FORMAT, 0x3);
        setInstanceAttributes(INSTANCE_FORMAT, 1 | instanceMask);
        mModelView = modelView;
    }

    @Nonnull
    @Override
    public String name() {
        return "Circle_GeometryProcessor";
    }

    @Override
    public void addToKey(KeyBuilder b) {
        b.addBits(5, mFlags, "stroke|clipPlane|isectPlane|unionPlane|roundCaps");
        b.addBits(ProgramImpl.MATRIX_KEY_BITS, ProgramImpl.computeMatrixKey(mModelView), "modelViewType");
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return new Impl();
    }

    private static class Impl extends ProgramImpl {

        private Matrix3 mModelViewState;
        private @UniformHandle int mModelViewUniform = INVALID_RESOURCE_HANDLE;

        @Override
        public void setData(ProgramDataManager pdm,
                            ShaderCaps shaderCaps,
                            GeometryProcessor geomProc) {
            mModelViewState = setTransform(pdm,
                    mModelViewUniform,
                    ((CircleGeometryProcessor) geomProc).mModelView,
                    mModelViewState);
        }

        @Override
        protected void onEmitCode(Args args) {
            final CircleGeometryProcessor geomProc = (CircleGeometryProcessor) args.mGeomProc;
            final boolean stroke = (geomProc.mFlags & 1) != 0;
            final boolean clipPlane = (geomProc.mFlags & (1 << 1)) != 0;
            final boolean isectPlane = (geomProc.mFlags & (1 << 2)) != 0;
            final boolean unionPlane = (geomProc.mFlags & (1 << 3)) != 0;
            final boolean roundCaps = (geomProc.mFlags & (1 << 4)) != 0;
            final VertexGeoBuilder vertBuilder = args.mVertBuilder;
            final FPFragmentBuilder fragBuilder = args.mFragBuilder;
            final VaryingHandler varyingHandler = args.mVaryingHandler;

            // emit attributes
            varyingHandler.emitAttributes(args.mGeomProc);
            fragBuilder.codeAppend("""
                    vec4 circleEdge;
                    """);
            varyingHandler.addPassThroughAttribute(CIRCLE_EDGE, "circleEdge");
            if (clipPlane) {
                fragBuilder.codeAppend("""
                        vec4 clipPlane;
                        """);
                varyingHandler.addPassThroughAttribute(CLIP_PLANE,
                        "clipPlane", VaryingHandler.Interpolation_CanBeFlat);
            }
            if (isectPlane) {
                fragBuilder.codeAppend("""
                        vec4 isectPlane;
                        """);
                varyingHandler.addPassThroughAttribute(ISECT_PLANE,
                        "isectPlane", VaryingHandler.Interpolation_CanBeFlat);
            }
            if (unionPlane) {
                fragBuilder.codeAppend("""
                        vec4 unionPlane;
                        """);
                varyingHandler.addPassThroughAttribute(UNION_PLANE,
                        "unionPlane", VaryingHandler.Interpolation_CanBeFlat);
            }
            Varying capRadius = new Varying(SLType.Float);
            if (roundCaps) {
                fragBuilder.codeAppend("""
                        vec4 roundCapCenters;
                        """);
                varyingHandler.addPassThroughAttribute(ROUND_CAP_CENTERS,
                        "roundCapCenters", VaryingHandler.Interpolation_CanBeFlat);
                varyingHandler.addVarying("capRadius", capRadius,
                        VaryingHandler.Interpolation_CanBeFlat);
                // This is the cap radius in normalized space where the outer radius is 1 and
                // circledEdge.w is the normalized inner radius.
                vertBuilder.codeAppendf("""
                        %s = (1.0 - %s.w) / 2.0;
                        """, capRadius.vsOut(), CIRCLE_EDGE.name());
            }

            // setup pass through color
            fragBuilder.codeAppendf("""
                    vec4 %s;
                    """, args.mOutputColor);
            varyingHandler.addPassThroughAttribute(COLOR, args.mOutputColor);

            // setup position
            mModelViewUniform = writeWorldPosition(args, POSITION.asShaderVar(), geomProc.mModelView);

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
                    """, args.mOutputCoverage);
        }
    }
}
