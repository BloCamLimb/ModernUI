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

package icyllis.arc3d.engine.trash.ops;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.granite.GeometryStep;
import icyllis.arc3d.granite.shading.*;
import org.jspecify.annotations.NonNull;

import static icyllis.arc3d.engine.Engine.VertexAttribType;

@Deprecated
public final class CircularRRectOp extends MeshDrawOp {

    public CircularRRectOp() {
        super();
    }

    @NonNull
    @Override
    protected GraphicsPipelineDesc_Old onCreatePipelineInfo(ImageProxyView writeView,
                                                            int pipelineFlags) {
        return null;
    }

    @Override
    protected void onPrepareDraws(MeshDrawTarget target) {

    }

    @Override
    public void onExecute(OpFlushState state, Rect2f chainBounds) {

    }

    private static class Processor extends GeometryStep {

        private static final VertexInputLayout.Attribute POS = new VertexInputLayout.Attribute("Pos", VertexAttribType.kFloat2, SLDataType.kFloat2);

        public Processor() {
            super("CircularRRectProcessor", "", null, null, 0, (byte) 0, null);
        }

        @Override
        public void appendToKey(@NonNull KeyBuilder b) {

        }

        @NonNull
        @Override
        public ProgramImpl makeProgramImpl(ShaderCaps caps) {
            return new Impl();
        }

        private static class Impl extends ProgramImpl {

            private int mSizeUniform;
            private int mRadiusUniform;

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

                // emit attributes
                vertBuilder.emitAttributes(geomProc);
                //varyingHandler.addPassThroughAttribute(POS, "p");

                String sizeUniformName = uniformHandler.getUniformName(
                        mSizeUniform = uniformHandler.addUniform(
                                Engine.ShaderFlags.kFragment,
                                SLDataType.kFloat2,
                                "Size", -1));
                String radiusUniformName = uniformHandler.getUniformName(
                        mRadiusUniform = uniformHandler.addUniform(
                                Engine.ShaderFlags.kFragment,
                                SLDataType.kFloat,
                                "Radius", -1));
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
