/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.engine.geom;

import icyllis.arcticgi.core.SLType;
import icyllis.arcticgi.engine.*;
import icyllis.arcticgi.engine.shading.*;

import javax.annotation.Nonnull;

import static icyllis.arcticgi.engine.EngineTypes.*;

/**
 * Unlike {@link CircleProcessor}, this processor uses SDF and supports over-stroking.
 * The stroke direction is CENTER. This processor uses instance rendering and static
 * vertex data.
 */
public class RoundRectProcessor extends GeometryProcessor {

    /**
     * Per-vertex attributes.
     */
    public static final Attribute
            POSITION = new Attribute("Position", Float2_VertexAttribType, SLType.Vec2);
    /**
     * Per-instance attributes.
     */
    public static final Attribute
            COLOR = new Attribute("Color", Float4_VertexAttribType, SLType.Vec4);
    // scale x, translate x, scale y, translate y
    public static final Attribute
            LOCAL_RECT = new Attribute("LocalRect", Float4_VertexAttribType, SLType.Vec4);
    // radius, stroke radius (stroke)
    public static final Attribute
            RADII = new Attribute("Radii", Float2_VertexAttribType, SLType.Vec2);
    public static final Attribute
            MODEL_VIEW = new Attribute("ModelView", Float3_VertexAttribType, SLType.Mat3);

    public static final AttributeSet VERTEX_ATTRIBS = AttributeSet.makeImplicit(
            POSITION);
    public static final AttributeSet INSTANCE_ATTRIBS = AttributeSet.makeImplicit(
            COLOR, LOCAL_RECT, RADII, MODEL_VIEW);

    private final boolean mStroke;

    public RoundRectProcessor(boolean stroke) {
        super(RoundRect_Geom_ClassID);
        mStroke = stroke;
        setVertexAttributes(VERTEX_ATTRIBS, 0x1);
        setInstanceAttributes(INSTANCE_ATTRIBS, 0xF);
    }

    @Nonnull
    @Override
    public String name() {
        return "RoundRect_GeometryProcessor";
    }

    @Override
    public void addToKey(KeyBuilder b) {
        b.addBool(mStroke, "stroke");
    }

    @Nonnull
    @Override
    public ProgramImpl makeProgramImpl(ShaderCaps caps) {
        return new Impl();
    }

    private static class Impl extends ProgramImpl {

        @Override
        public void setData(ProgramDataManager pdm,
                            ShaderCaps shaderCaps,
                            GeometryProcessor geomProc) {
        }

        @Override
        protected void onEmitCode(VertexGeoBuilder vertBuilder,
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
            final boolean stroke = ((RoundRectProcessor) geomProc).mStroke;

            // emit attributes
            vertBuilder.emitAttributes(geomProc);

            Varying rectEdge = new Varying(SLType.Vec2);
            varyingHandler.addVarying("RectEdge", rectEdge);
            vertBuilder.codeAppendf("""
                    vec2 rectEdge = (%s.xz + %s.y) * %s + 1.0;
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
                    VaryingHandler.Interpolation_CanBeFlat);

            // setup position
            vertBuilder.codeAppendf("""
                    vec2 localPos = rectEdge + %s.yw;
                    """, LOCAL_RECT.name());
            localPos.set("localPos", SLType.Vec2);
            writeWorldPosition(vertBuilder, localPos, MODEL_VIEW.name(), worldPos);

            Varying sizeAndRadii = new Varying(SLType.Vec4);
            varyingHandler.addVarying("SizeAndRadii", sizeAndRadii,
                    VaryingHandler.Interpolation_CanBeFlat);
            vertBuilder.codeAppendf("""
                    %s = vec4(%s.xz, %s);
                    """, sizeAndRadii.vsOut(), LOCAL_RECT.name(), RADII.name());
            fragBuilder.codeAppendf("""
                    vec4 sizeAndRadii = %s;
                    """, sizeAndRadii.fsIn());

            fragBuilder.codeAppend("""
                    vec2 q = abs(rectEdge) - sizeAndRadii.xy + sizeAndRadii.z;
                    float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - sizeAndRadii.z;
                    """);
            if (stroke) {
                fragBuilder.codeAppend("""
                        float edgeAlpha = 1.0 - smoothstep(-0.5, 0.5, abs(d) - sizeAndRadii.w);
                        """);
            } else {
                fragBuilder.codeAppend("""
                        float edgeAlpha = 1.0 - smoothstep(-0.5, 0.5, d);
                        """);
            }
            fragBuilder.codeAppendf("""
                    vec4 %s = vec4(edgeAlpha);
                    """, outputCoverage);
        }
    }
}
