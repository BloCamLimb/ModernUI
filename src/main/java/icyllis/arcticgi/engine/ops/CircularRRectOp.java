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

package icyllis.arcticgi.engine.ops;

import icyllis.arcticgi.core.Rect2f;
import icyllis.arcticgi.core.SLType;
import icyllis.arcticgi.engine.*;
import icyllis.arcticgi.engine.shading.*;

import javax.annotation.Nonnull;

import static icyllis.arcticgi.engine.EngineTypes.Float2_VertexAttribType;

public final class CircularRRectOp extends MeshDrawOp {

    public static final int sClassID = genOpClassID();

    public CircularRRectOp() {
        super(sClassID);
    }

    @Nonnull
    @Override
    public String name() {
        return "CircularRRectSDFOp";
    }

    @Override
    public void onPrePrepare(RecordingContext context) {

    }

    @Override
    public void onPrepare(OpFlushState state) {

    }

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {

    }

    private static class Processor extends GeometryProcessor {

        private static final Attribute POS = new Attribute("Pos", Float2_VertexAttribType, SLType.Vec2);

        public Processor() {
            super(CircularRRect_Geom_ClassID);
        }

        @Nonnull
        @Override
        public String name() {
            return "CircularRRect_GeometryProcessor";
        }

        @Override
        public void addToKey(KeyBuilder b) {

        }

        @Nonnull
        @Override
        public ProgramImpl makeProgramImpl(ShaderCaps caps) {
            return new Impl();
        }

        private static class Impl extends ProgramImpl {

            private int mSizeUniform;
            private int mRadiusUniform;

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

                // emit attributes
                vertBuilder.emitAttributes(geomProc);
                varyingHandler.addPassThroughAttribute(POS, "p");

                String sizeUniformName = uniformHandler.getUniformName(
                        mSizeUniform = uniformHandler.addUniform(geomProc,
                                EngineTypes.Fragment_ShaderFlag,
                                SLType.Vec2,
                                "Size"));
                String radiusUniformName = uniformHandler.getUniformName(
                        mRadiusUniform = uniformHandler.addUniform(geomProc,
                                EngineTypes.Fragment_ShaderFlag,
                                SLType.Float,
                                "Radius"));
                fragBuilder.codeAppendf("""
                                vec2 q = abs(p) - %1$s + %2$s;
                                float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - %2$s;
                                """,
                        sizeUniformName,
                        radiusUniformName);

                localPos.set(POS.name(), POS.dstType());
            }
        }
    }
}
