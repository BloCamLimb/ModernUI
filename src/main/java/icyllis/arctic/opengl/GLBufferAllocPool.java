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

package icyllis.arctic.opengl;

import icyllis.arctic.core.SharedPtr;
import icyllis.arctic.engine.*;
import org.lwjgl.system.APIUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * In OpenGL, we use CPU staging buffers since most drivers made some optimizations.
 */
public abstract class GLBufferAllocPool extends BufferAllocPool {

    private final CpuBufferCache mCpuBufferCache;

    @SharedPtr
    private CpuBuffer mCpuStagingBuffer;

    protected ByteBuffer mWriter;

    /**
     * Constructor.
     *
     * @param server         the server used to create the buffers.
     * @param bufferType     the type of buffers to create.
     * @param cpuBufferCache the cache for staging buffers used before data
     *                       is uploaded to GPU buffer objects.
     */
    protected GLBufferAllocPool(GLServer server, int bufferType, CpuBufferCache cpuBufferCache) {
        super(server, bufferType);
        mCpuBufferCache = cpuBufferCache;
    }

    /**
     * Constructor.
     *
     * @param server         the server used to create the vertex buffers.
     * @param cpuBufferCache the cache for staging buffers used before data
     *                       is uploaded to GPU buffer objects.
     */
    @Nonnull
    public static GLBufferAllocPool makeVertex(GLServer server, CpuBufferCache cpuBufferCache) {
        return new VertexPool(server, cpuBufferCache);
    }

    /**
     * Constructor.
     *
     * @param server         the server used to create the instance buffers.
     * @param cpuBufferCache the cache for staging buffers used before data
     *                       is uploaded to GPU buffer objects.
     */
    @Nonnull
    public static GLBufferAllocPool makeInstance(GLServer server, CpuBufferCache cpuBufferCache) {
        return new InstancePool(server, cpuBufferCache);
    }

    @Override
    public void flush() {
        // flush CPU staging buffer
        if (mBufferPtr != NULL) {
            assert (mIndex >= 0);
            GpuBuffer buffer = mBuffers[mIndex];
            int flushSize = buffer.size() - mFreeBytes[mIndex];

            assert (!buffer.isMapped());
            assert (mCpuStagingBuffer != null && mCpuStagingBuffer.data() == mBufferPtr);
            assert (flushSize <= mCpuStagingBuffer.size() && flushSize <= buffer.size());

            buffer.updateData(mBufferPtr, /*offset=*/0, flushSize);
            mBufferPtr = NULL;
        }
    }

    @Override
    public void reset() {
        super.reset();
        if (mCpuStagingBuffer != null) {
            mCpuStagingBuffer.unref();
            mCpuStagingBuffer = null;
        }
    }

    @Override
    protected long lockBuffer(GpuBuffer buffer) {
        if (mCpuStagingBuffer != null) {
            if (buffer.size() <= mCpuStagingBuffer.size()) {
                return mCpuStagingBuffer.data();
            }
            mCpuStagingBuffer.unref();
        }
        mCpuStagingBuffer = mCpuBufferCache.makeBuffer(buffer.size());
        return mCpuStagingBuffer.data();
    }

    @Override
    protected void unlockBuffer(GpuBuffer buffer) {
        // leave as is
    }

    private static class VertexPool extends GLBufferAllocPool {

        public VertexPool(GLServer server, CpuBufferCache cpuBufferCache) {
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

            GpuBuffer buffer = mBuffers[mIndex];
            long offset = ptr - mBufferPtr;
            assert (offset % vertexSize == 0);
            mesh.setVertexBuffer(buffer, (int) (offset / vertexSize));
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

            GpuBuffer buffer = mBuffers[mIndex];
            long offset = ptr - mBufferPtr;
            assert (offset % vertexSize == 0);
            mesh.setVertexBuffer(buffer, (int) (offset / vertexSize));
            return APIUtil.apiGetMappedBuffer(mWriter, ptr, totalSize);
        }
    }

    private static class InstancePool extends GLBufferAllocPool {

        public InstancePool(GLServer server, CpuBufferCache cpuBufferCache) {
            super(server, EngineTypes.GpuBufferType_Vertex, cpuBufferCache);
            // instance buffers are also vertex buffers, but we allocate them from a different pool
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

            GpuBuffer buffer = mBuffers[mIndex];
            long offset = ptr - mBufferPtr;
            assert (offset % instanceSize == 0);
            mesh.setInstanceBuffer(buffer, (int) (offset / instanceSize));
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

            GpuBuffer buffer = mBuffers[mIndex];
            long offset = ptr - mBufferPtr;
            assert (offset % instanceSize == 0);
            mesh.setInstanceBuffer(buffer, (int) (offset / instanceSize));
            return APIUtil.apiGetMappedBuffer(mWriter, ptr, totalSize);
        }
    }
}
