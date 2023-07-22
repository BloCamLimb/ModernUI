/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.core.SharedPtr;

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
     * @param buffer            the shared ptr to the vertex buffer that will hold the vertices
     * @param baseVertex        the offset into buffer of the first vertex,
     *                          in units of the size of a vertex from layout param
     * @param actualVertexCount the actual number of vertices allocated
     */
    default void setVertexBuffer(@SharedPtr Buffer buffer, int baseVertex, int actualVertexCount) {
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
     * @param buffer              the shared ptr to the instance buffer that will hold the instances
     * @param baseInstance        the offset into buffer of the first instance,
     *                            in units of the size of an instance from layout param
     * @param actualInstanceCount the actual number of instances allocated
     */
    default void setInstanceBuffer(@SharedPtr Buffer buffer, int baseInstance, int actualInstanceCount) {
        throw new IllegalStateException();
    }

    /**
     * Returns the number of indices to draw in the mesh.
     */
    default int getIndexCount() {
        return 0;
    }

    /**
     * The callback method for {@link MeshDrawTarget#makeIndexSpace(Mesh)} results.
     *
     * @param buffer           the shared ptr to the index buffer that will hold the indices
     * @param baseIndex        the offset into buffer of the first index,
     *                         in units of the size of an index from layout param
     * @param actualIndexCount the actual number of indices allocated
     */
    default void setIndexBuffer(@SharedPtr Buffer buffer, int baseIndex, int actualIndexCount) {
        throw new IllegalStateException();
    }
}
