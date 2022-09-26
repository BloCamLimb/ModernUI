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

package icyllis.arcticgi.engine;

import icyllis.arcticgi.core.SharedPtr;

/**
 * The interface used to receive geometry buffers from {@link MeshDrawTarget}
 * for mesh-drawing operations.
 */
public interface Mesh {

    /**
     * Returns the size of a vertex if enabled in the array.
     */
    default int getVertexSize() {
        return 0;
    }

    /**
     * Returns the number of vertices to draw in the batch.
     */
    default int getVertexCount() {
        return 0;
    }

    /**
     * The callback method for {@link MeshDrawTarget#makeVertexSpace(Mesh)} results.
     *
     * @param buffer     the vertex buffer that will hold the vertices, ref'ed
     * @param baseVertex the offset into buffer of the first vertex,
     *                   in units of the size of a vertex from layout param
     */
    default void setVertexBuffer(@SharedPtr Buffer buffer, int baseVertex) {
        throw new IllegalStateException();
    }

    /**
     * Returns the size of an instance if enabled in the array.
     */
    default int getInstanceSize() {
        return 0;
    }

    /**
     * Returns the number of instances to draw in the batch.
     */
    default int getInstanceCount() {
        return 0;
    }

    /**
     * The callback method for {@link MeshDrawTarget#makeInstanceSpace(Mesh)} results.
     *
     * @param buffer       the instance buffer that will hold the instances, ref'ed
     * @param baseInstance the offset into buffer of the first instance,
     *                     in units of the size of an instance from layout param
     */
    default void setInstanceBuffer(@SharedPtr Buffer buffer, int baseInstance) {
        throw new IllegalStateException();
    }
}
