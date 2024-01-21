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
import icyllis.arc3d.engine.Engine.PrimitiveType;
import icyllis.arc3d.engine.Engine.VertexAttribType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.*;

import javax.annotation.Nonnull;

/**
 * Generates rectangle geometric primitive, uses instanced rendering.
 * <p>
 * Signed distance function (exact):
 * <pre>{@code
 * float sdBox(vec2 p, vec2 b) {
 *     vec2 q = abs(p)-b;
 *     return length(max(q,0.0)) + min(max(q.x,q.y),0.0);
 * }
 * }</pre>
 * <p>
 * Descriptor:
 * <ul>
 * <li>0-1 bits: use distance-to-edge antialiasing</li>
 * <li>1-2 bits: fill/stroke</li>
 * <li>2-3 bits: use instanced view matrix</li>
 * </ul>
 * <pre>{@code
 * struct Instance {
 *     ubyte4/float4 color; // premultiplied color
 *     float4 box;          // rectangle (radius x, center x, radius y, center y) in local space
 *     float2 stroke;       // optional (stroke radius, stroke position)
 *     float3 viewMatrix[3]; // optional view matrix
 * }
 * }</pre>
 */
public class SDFRectGeoProc extends GeometryProcessor {

    /**
     * Per-vertex attributes.
     */
    // {(-1,-1), (-1, 1), (1, -1), (1, 1)}
    public static final Attribute
            POSITION = new Attribute("Position", VertexAttribType.kFloat2, SLDataType.kFloat2);
    /**
     * Per-instance attributes.
     */
    // per-multiplied color
    public static final Attribute
            COLOR = new Attribute("Color", VertexAttribType.kUByte4_norm, SLDataType.kFloat4);
    // scale x, translate x, scale y, translate y
    public static final Attribute
            BOX = new Attribute("Box", VertexAttribType.kFloat4, SLDataType.kFloat4);
    // stroke radius, stroke position (if stroke, or 0, 0)
    public static final Attribute
            STROKE = new Attribute("Stroke", VertexAttribType.kFloat2, SLDataType.kFloat2);
    public static final Attribute
            VIEW_MATRIX = new Attribute("ViewMatrix", VertexAttribType.kFloat3, SLDataType.kFloat3x3);

    public static final AttributeSet VERTEX_ATTRIBS = AttributeSet.makeImplicit(
            POSITION);
    public static final AttributeSet INSTANCE_ATTRIBS = AttributeSet.makeImplicit(
            COLOR, BOX, STROKE, VIEW_MATRIX);

    public static final int FLAG_ANTIALIASING = 0x1;
    public static final int FLAG_STROKE = 0x2;
    public static final int FLAG_INSTANCED_MATRIX = 0x4;

    private final int mFlags;

    public SDFRectGeoProc(int flags) {
        super(SDFRect_GeoProc_ClassID);
        mFlags = flags;
        setVertexAttributes(VERTEX_ATTRIBS, 0x1);
        setInstanceAttributes(INSTANCE_ATTRIBS, 0x3 | ((flags & 0x6) << 1));
    }

    @Nonnull
    @Override
    public String name() {
        return "SDFRect_GeomProc";
    }

    @Override
    public byte primitiveType() {
        return PrimitiveType.TriangleStrip;
    }

    @Override
    public void addToKey(KeyBuilder b) {
        b.addBits(3, mFlags, "gpFlags");
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return new Impl();
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
            final var gp = (SDFRectGeoProc) geomProc;
            final boolean gpAA = (gp.mFlags & FLAG_ANTIALIASING) != 0;
            final boolean gpStroke = (gp.mFlags & FLAG_STROKE) != 0;
            final boolean gpMatrix = (gp.mFlags & FLAG_INSTANCED_MATRIX) != 0;

            // emit attributes
            vertBuilder.emitAttributes(geomProc);

            Varying rectEdge = new Varying(SLDataType.kFloat2);
            varyingHandler.addVarying("RectEdge", rectEdge);
            // add stroke radius and a full pixel bloat
            vertBuilder.codeAppendf("""
                    vec2 rectEdge = (%s.xz + %s.x - %s.y + 1.0) * %s;
                    %s = rectEdge;
                    """, BOX.name(), STROKE.name(), STROKE.name(), POSITION.name(), rectEdge.vsOut());
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
                    """, sizeAndRadii.vsOut(), BOX.name(), STROKE.name());
            fragBuilder.codeAppendf("""
                    vec4 sizeAndRadii = %s;
                    """, sizeAndRadii.fsIn());

            // setup position
            vertBuilder.codeAppendf("""
                    vec2 localPos = rectEdge + %s.yw;
                    """, BOX.name());
            localPos.set("localPos", SLDataType.kFloat2);
            if (gpMatrix) {
                writeWorldPosition(vertBuilder, localPos, VIEW_MATRIX.name(), worldPos);
            } else {
                writePassthroughWorldPosition(vertBuilder, localPos, worldPos);
            }

            fragBuilder.codeAppend("""
                        vec2 q = abs(rectEdge) - sizeAndRadii.xy;
                        float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0));
                        """);
            if (gpStroke) {
                fragBuilder.codeAppend("""
                        d = abs(d + sizeAndRadii.w) - sizeAndRadii.z;
                        """);
            }
            if (gpAA) {
                // use L2-norm of grad SDF
                fragBuilder.codeAppend("""
                        float afwidth = length(vec2(dFdx(d),dFdy(d)))*0.7;
                        float edgeAlpha = 1.0 - smoothstep(-afwidth,afwidth,d);
                        """);
            } else {
                fragBuilder.codeAppend("""
                        float edgeAlpha = step(d,0.0);
                        """);
            }
            fragBuilder.codeAppendf("""
                    vec4 %s = vec4(edgeAlpha);
                    """, outputCoverage);
        }
    }
}
