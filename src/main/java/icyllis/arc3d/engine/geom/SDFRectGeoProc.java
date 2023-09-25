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

import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.shading.*;

import javax.annotation.Nonnull;

public class SDFRectGeoProc extends GeometryProcessor {

    protected SDFRectGeoProc() {
        super(SDFRect_GeoProc_ClassID);
    }

    @Nonnull
    @Override
    public String name() {
        return "SDFRect_GeomProc";
    }

    @Override
    public byte primitiveType() {
        return Engine.PrimitiveType.TriangleStrip;
    }

    @Override
    public void addToKey(Key.Builder b) {
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

            // emit attributes
            vertBuilder.emitAttributes(geomProc);
        }
    }
}
