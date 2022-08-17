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
 * A pool of instance buffers.
 *
 * @see BufferAllocPool
 */
public class InstanceBufferAllocPool extends VertexBufferAllocPool {

    /**
     * Constructor.
     *
     * @param server         the server used to create the instance buffers.
     * @param cpuBufferCache if non-null a cache for client side array buffers
     *                       or staging buffers used before data is uploaded to
     *                       GPU buffer objects.
     */
    public InstanceBufferAllocPool(Server server, BufferAllocPool.CpuBufferCache cpuBufferCache) {
        super(server, cpuBufferCache);
        // instance buffers are seen as vertex buffers in certain context
        // but we allocate them from different pools
    }

    /**
     * Returns a block of memory to hold instances. A buffer designated to hold
     * the instances given to the caller. The buffer may or may not be locked.
     * The returned ptr remains valid until any of the following:
     * <ul>
     *      <li>this method is called again.</li>
     *      <li>{@link #flush()} is called.</li>
     *      <li>{@link #reset()} is called.</li>
     * </ul>
     * Once unmap on the pool is called the instances are guaranteed to be in
     * the buffer at the offset indicated by baseInstance. Until that time they
     * may be in temporary storage and/or the buffer may be locked.
     *
     * @param mesh specifies the mesh to allocate space for
     * @return pointer to first instance, or NULL if failed
     */
    @Override
    public long makeSpace(Mesh mesh) {
        int instanceSize = mesh.getInstanceSize();
        int instanceCount = mesh.getInstanceCount();
        assert (instanceSize > 0 && instanceCount > 0);

        long ptr = makeSpace(instanceSize * instanceCount, instanceSize);
        if (ptr == NULL) {
            return NULL;
        }

        int back = mBufferBlocks.size() - 1;
        Buffer buf = mBufferBlocks.get(back);
        mesh.setInstanceBuffer((GpuBuffer) buf, (int) ((ptr - mBufferPtr) / instanceSize));
        return ptr;
    }

    /**
     * Similar to {@link #makeSpace(Mesh)}, but returns a wrapper instead.
     *
     * @param mesh specifies the mesh to allocate space for
     * @return pointer to first instance, or null if failed
     */
    @Nullable
    @Override
    public ByteBuffer makeWriter(Mesh mesh) {
        int instanceSize = mesh.getInstanceSize();
        int instanceCount = mesh.getInstanceCount();
        assert (instanceSize > 0 && instanceCount > 0);

        int totalSize = instanceSize * instanceCount;
        long ptr = makeSpace(totalSize, instanceSize);
        if (ptr == NULL) {
            return null;
        }

        int back = mBufferBlocks.size() - 1;
        Buffer buf = mBufferBlocks.get(back);
        mesh.setInstanceBuffer((GpuBuffer) buf, (int) ((ptr - mBufferPtr) / instanceSize));
        return MemoryUtil.memByteBuffer(ptr, totalSize);
    }
}
