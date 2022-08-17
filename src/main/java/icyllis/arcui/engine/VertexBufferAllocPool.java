/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.engine;

import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A pool of vertex buffers.
 *
 * @see BufferAllocPool
 */
public class VertexBufferAllocPool extends BufferAllocPool {

    /**
     * Constructor.
     *
     * @param server         the server used to create the vertex buffers.
     * @param cpuBufferCache if non-null a cache for client side array buffers
     *                       or staging buffers used before data is uploaded to
     *                       GPU buffer objects.
     */
    public VertexBufferAllocPool(Server server, CpuBufferCache cpuBufferCache) {
        super(server, EngineTypes.GpuBufferType_Vertex, cpuBufferCache);
    }

    /**
     * Returns a block of memory to hold vertices. A buffer designated to hold
     * the vertices given to the caller. The buffer may or may not be locked.
     * The returned ptr remains valid until any of the following:
     * <ul>
     *      <li>this method is called again.</li>
     *      <li>{@link #flush()} is called.</li>
     *      <li>{@link #reset()} is called.</li>
     * </ul>
     * Once unmap on the pool is called the vertices are guaranteed to be in
     * the buffer at the offset indicated by baseVertex. Until that time they
     * may be in temporary storage and/or the buffer may be locked.
     *
     * @param mesh specifies the mesh to allocate space for
     * @return pointer to first vertex, or NULL if failed
     */
    public long makeSpace(Mesh mesh) {
        int vertexSize = mesh.getVertexSize();
        int vertexCount = mesh.getVertexCount();
        assert (vertexSize > 0 && vertexCount > 0);

        long ptr = makeSpace(vertexSize * vertexCount, vertexSize);
        if (ptr == NULL) {
            return NULL;
        }

        int back = mBufferBlocks.size() - 1;
        Buffer buf = mBufferBlocks.get(back);
        mesh.setVertexBuffer((GpuBuffer) buf, (int) ((ptr - mBufferPtr) / vertexSize));
        return ptr;
    }

    /**
     * Similar to {@link #makeSpace(Mesh)}, but returns a wrapper instead.
     *
     * @param mesh specifies the mesh to allocate space for
     * @return pointer to first vertex, or null if failed
     */
    @Nullable
    public ByteBuffer makeWriter(Mesh mesh) {
        int vertexSize = mesh.getVertexSize();
        int vertexCount = mesh.getVertexCount();
        assert (vertexSize > 0 && vertexCount > 0);

        int totalSize = vertexSize * vertexCount;
        long ptr = makeSpace(totalSize, vertexSize);
        if (ptr == NULL) {
            return null;
        }

        int back = mBufferBlocks.size() - 1;
        Buffer buf = mBufferBlocks.get(back);
        mesh.setVertexBuffer((GpuBuffer) buf, (int) ((ptr - mBufferPtr) / vertexSize));
        return MemoryUtil.memByteBuffer(ptr, totalSize);
    }
}
