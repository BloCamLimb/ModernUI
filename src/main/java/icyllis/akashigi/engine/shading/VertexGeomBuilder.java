/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.engine.shading;

import icyllis.akashigi.engine.GeometryProcessor;
import icyllis.akashigi.engine.ShaderVar;

/**
 * Base class for vertex shader builder. This is the stage that computes input geometry for the
 * rasterizer.
 */
public interface VertexGeomBuilder extends ShaderBuilder {

    /**
     * Emits per-vertex and per-instance attributes to vertex shader inputs.
     *
     * @param geomProc the geometry processor
     */
    void emitAttributes(GeometryProcessor geomProc);

    /**
     * Emits world position and transforms it into the clip space.
     *
     * @param worldPos the world position
     */
    void emitNormalizedPosition(ShaderVar worldPos);
}
