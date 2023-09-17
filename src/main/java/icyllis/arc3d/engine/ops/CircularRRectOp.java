/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

package icyllis.arc3d.engine.ops;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.SLType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.*;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.VertexAttribType;

public final class CircularRRectOp extends MeshDrawOp {

    public CircularRRectOp() {
        super();
    }

    @Override
    protected PipelineInfo onCreatePipelineInfo(SurfaceProxyView writeView,
                                                int pipelineFlags) {
        return null;
    }

    @Override
    protected void onPrepareDraws(MeshDrawTarget target) {

    }

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {

    }

    private static class Processor extends GeometryProcessor {

        private static final Attribute POS = new Attribute("Pos", VertexAttribType.kFloat2, SLType.kFloat2);

        public Processor() {
            super(CircularRRect_Geom_ClassID);
        }

        @Nonnull
        @Override
        public String name() {
            return "CircularRRect_GeometryProcessor";
        }

        @Override
        public byte primitiveType() {
            return 0;
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

                // emit attributes
                vertBuilder.emitAttributes(geomProc);
                varyingHandler.addPassThroughAttribute(POS, "p");

                String sizeUniformName = uniformHandler.getUniformName(
                        mSizeUniform = uniformHandler.addUniform(geomProc,
                                Engine.ShaderFlags.kFragment,
                                SLType.kFloat2,
                                "Size"));
                String radiusUniformName = uniformHandler.getUniformName(
                        mRadiusUniform = uniformHandler.addUniform(geomProc,
                                Engine.ShaderFlags.kFragment,
                                SLType.kFloat,
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
