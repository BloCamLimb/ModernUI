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

package icyllis.arc3d.engine.geom;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.graphene.DrawOp;
import icyllis.arc3d.engine.graphene.MeshDrawWriter;
import icyllis.arc3d.engine.shading.*;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

/**
 * Unlike {@link CircleProcessor}, this processor uses SDF and supports over-stroking.
 * The stroke direction is CENTER. This processor uses instance rendering and static
 * vertex data.
 */
public class SDFRoundRectGeoProc extends GeometryStep {

    /**
     * Per-vertex attributes.
     */
    // {(-1,-1), (-1, 1), (1, -1), (1, 1)}
    public static final VertexInputLayout.Attribute
            POSITION = new VertexInputLayout.Attribute("Position", VertexAttribType.kFloat2, SLDataType.kFloat2);
    /**
     * Per-instance attributes.
     */
    // per-multiplied color
    public static final VertexInputLayout.Attribute
            COLOR = new VertexInputLayout.Attribute("Color", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // scale x, translate x, scale y, translate y
    public static final VertexInputLayout.Attribute
            LOCAL_RECT = new VertexInputLayout.Attribute("LocalRect", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // radius, stroke radius (if stroke, or 0)
    public static final VertexInputLayout.Attribute
            RADII = new VertexInputLayout.Attribute("Radii", VertexAttribType.kFloat2, SLDataType.kFloat2);
    public static final VertexInputLayout.Attribute
            MODEL_VIEW = new VertexInputLayout.Attribute("ModelView", VertexAttribType.kFloat3, SLDataType.kFloat3x3);

    public static final VertexInputLayout.AttributeSet VERTEX_ATTRIBS = VertexInputLayout.AttributeSet.makeImplicit(
            0, POSITION);
    public static final VertexInputLayout.AttributeSet INSTANCE_ATTRIBS = VertexInputLayout.AttributeSet.makeImplicit(
            1, COLOR, LOCAL_RECT, RADII, MODEL_VIEW);

    private final boolean mStroke;

    public SDFRoundRectGeoProc(boolean stroke) {
        super(RoundRect_GeoProc_ClassID);
        mStroke = stroke;
        setVertexAttributes(0x1);
        setInstanceAttributes(0xF);
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
        b.addBool(mStroke, "stroke");
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return new Impl();
    }

    @Override
    protected VertexInputLayout.AttributeSet allVertexAttributes() {
        return VERTEX_ATTRIBS;
    }

    @Override
    protected VertexInputLayout.AttributeSet allInstanceAttributes() {
        return INSTANCE_ATTRIBS;
    }

    @Override
    public void writeVertices(MeshDrawWriter writer, DrawOp op, float[] solidColor) {
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

    private static class Impl extends ProgramImpl {

        @Override
        public void setData(UniformDataManager manager,
                            GeometryStep geomProc) {
        }

        @Override
        protected void onEmitCode(VertexGeomBuilder vertBuilder,
                                  FPFragmentBuilder fragBuilder,
                                  VaryingHandler varyingHandler,
                                  UniformHandler uniformHandler,
                                  ShaderCaps shaderCaps,
                                  GeometryStep geomProc,
                                  String outputColor,
                                  String outputCoverage,
                                  int[] texSamplers,
                                  ShaderVar localPos,
                                  ShaderVar worldPos) {
            final boolean stroke = ((SDFRoundRectGeoProc) geomProc).mStroke;

            // emit attributes
            vertBuilder.emitAttributes(geomProc);

            Varying rectEdge = new Varying(SLDataType.kFloat2);
            varyingHandler.addVarying("RectEdge", rectEdge);
            // add stroke radius and a full pixel bloat
            vertBuilder.codeAppendf("""
                    vec2 rectEdge = (%s.xz + %s.y + 1.0) * %s;
                    %s = rectEdge;
                    """, LOCAL_RECT.name(), RADII.name(), POSITION.name(), rectEdge.vsOut());
            fragBuilder.codeAppendf("""
                    vec2 rectEdge = %s;
                    """, rectEdge.fsIn());

            // setup pass through color
            fragBuilder.codeAppendf("""
                    vec4 %s;
                    """, outputColor);
            varyingHandler.addPassThroughAttribute(COLOR, outputColor,
                    VaryingHandler.kCanBeFlat_Interpolation);

            Varying sizeAndRadii = new Varying(SLDataType.kFloat4);
            varyingHandler.addVarying("SizeAndRadii", sizeAndRadii,
                    VaryingHandler.kCanBeFlat_Interpolation);
            vertBuilder.codeAppendf("""
                    %s = vec4(%s.xz, %s);
                    """, sizeAndRadii.vsOut(), LOCAL_RECT.name(), RADII.name());
            fragBuilder.codeAppendf("""
                    vec4 sizeAndRadii = %s;
                    """, sizeAndRadii.fsIn());

            // setup position
            vertBuilder.codeAppendf("""
                    vec2 localPos = rectEdge + %s.yw;
                    """, LOCAL_RECT.name());
            localPos.set("localPos", SLDataType.kFloat2);
            writeWorldPosition(vertBuilder, localPos, MODEL_VIEW.name(), worldPos);

            if (stroke) {
                fragBuilder.codeAppend("""
                        vec2 q = abs(rectEdge) - sizeAndRadii.xy + sizeAndRadii.z;
                        float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - sizeAndRadii.z;
                        """);
            } else {
                // simplified version
                fragBuilder.codeAppend("""
                        vec2 q = abs(rectEdge) - sizeAndRadii.xy + sizeAndRadii.z;
                        float d = length(max(q, 0.0)) - sizeAndRadii.z;
                        """);
            }
            if (stroke) {
                fragBuilder.codeAppend("""
                        d = abs(d) - sizeAndRadii.w;
                        """);
            }
            // use L2-norm of grad SDF
            fragBuilder.codeAppend("""
                    float afwidth = length(vec2(dFdx(d),dFdy(d)))*0.7;
                    float edgeAlpha = 1.0 - smoothstep(-afwidth,afwidth,d);
                    """);
            fragBuilder.codeAppendf("""
                    vec4 %s = vec4(edgeAlpha);
                    """, outputCoverage);
        }
    }
}
