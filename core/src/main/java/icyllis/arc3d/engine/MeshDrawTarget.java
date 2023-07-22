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

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

/**
 * Abstract interface that supports creating vertices, indices, and meshes, as well as
 * invoking GPU draw operations.
 */
//TODO
public interface MeshDrawTarget {

    /**
     * Makes space for vertex data. The returned pointer is the location where vertex data
     * should be written. On return the buffer that will hold the data as well as an offset into
     * the buffer (in 'vertexSize' units) where the data will be placed.
     * <p>
     * This method requires {@link Mesh#getVertexSize()} and {@link Mesh#getVertexCount()} as
     * arguments and {@link Mesh#setVertexBuffer(Buffer, int, int)} as results.
     *
     * @return may NULL if failed
     */
    long makeVertexSpace(Mesh mesh);

    /**
     * Makes space for instance data. The returned pointer is the location where instance data
     * should be written. On return the buffer that will hold the data as well as an offset into
     * the buffer (in 'instanceSize' units) where the data will be placed.
     * <p>
     * This method requires {@link Mesh#getInstanceSize()} and {@link Mesh#getInstanceCount()} as
     * arguments and {@link Mesh#setInstanceBuffer(Buffer, int, int)} as results.
     *
     * @return may NULL if failed
     */
    long makeInstanceSpace(Mesh mesh);

    /**
     * Makes space for index data. The returned pointer is the location where index data
     * should be written. On return the buffer that will hold the data as well as an offset into
     * the buffer (in 'ushort' units) where the data will be placed.
     * <p>
     * This method requires {@link Mesh#getIndexCount()} as
     * arguments and {@link Mesh#setIndexBuffer(Buffer, int, int)} as results.
     *
     * @return may NULL if failed
     */
    long makeIndexSpace(Mesh mesh);

    /**
     * Helper method.
     *
     * @return may null if failed
     * @see #makeVertexSpace(Mesh)
     */
    @Nullable
    ByteBuffer makeVertexWriter(Mesh mesh);

    /**
     * Helper method.
     *
     * @return may null if failed
     * @see #makeInstanceSpace(Mesh)
     */
    @Nullable
    ByteBuffer makeInstanceWriter(Mesh mesh);

    /**
     * Helper method.
     *
     * @return may null if failed
     * @see #makeIndexSpace(Mesh)
     */
    @Nullable
    ByteBuffer makeIndexWriter(Mesh mesh);
}
