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

import icyllis.modernui.annotation.SharedPtr;
import icyllis.modernui.graphics.MathUtil;
import org.lwjgl.system.APIUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A pool of geometry buffers tied to a {@link Server}.
 * <p>
 * The pool allows a client to make space for geometry and then put back excess
 * space if it over allocated. When a client is ready to draw from the pool
 * it calls {@link #flush()} on the pool ensure buffers are ready for drawing.
 * The pool can be reset after drawing is completed to recycle space. After that,
 * all GPU buffers can't be touched again (imagine we have triple buffering).
 * <p>
 * At creation time a minimum per-buffer size can be specified. Additionally,
 * a number of buffers to pre-allocate can be specified. These will be allocated
 * at the minimum size and kept around until the pool is destroyed.
 */
public abstract class BufferAllocPool {

    /**
     * We expect buffers for meshes to be at least 64KB.
     */
    public static final int DEFAULT_BUFFER_SIZE = 1 << 16;

    private final Server mServer;
    private final int mBufferType;

    // blocks
    @SharedPtr
    protected GpuBuffer[] mBuffers = new GpuBuffer[8];
    protected int[] mFreeBytes = new int[8];
    protected int mIndex = -1;

    protected long mBufferPtr;

    private int mBytesInUse;

    protected ByteBuffer mWriter;

    /**
     * Constructor.
     *
     * @param server     the server used to create the buffers.
     * @param bufferType the type of buffers to create.
     */
    protected BufferAllocPool(Server server, int bufferType) {
        assert (bufferType == Engine.BufferUsageFlags.kVertex || bufferType == Engine.BufferUsageFlags.kIndex);
        mServer = server;
        mBufferType = bufferType;
    }

    /**
     * Constructor.
     *
     * @param server the server used to create the vertex buffers.
     */
    @Nonnull
    public static BufferAllocPool makeVertexPool(Server server) {
        return new VertexPool(server);
    }

    /**
     * Constructor.
     *
     * @param server the server used to create the instance buffers.
     */
    @Nonnull
    public static BufferAllocPool makeInstancePool(Server server) {
        return new InstancePool(server);
    }

    /**
     * Ensures all buffers are unlocked and have all data written to them.
     * Call before drawing using buffers from the pool.
     */
    public void flush() {
        if (mBufferPtr != NULL) {
            assert (mIndex >= 0);
            GpuBuffer buffer = mBuffers[mIndex];
            int usedBytes = buffer.getSize() - mFreeBytes[mIndex];
            assert (buffer.isLocked());
            assert (buffer.getLockedBuffer() == mBufferPtr);
            buffer.unlock(/*offset=*/0, usedBytes);
            mBufferPtr = NULL;
        }
    }

    /**
     * Invalidates all the data in the pool, unrefs non-pre-allocated buffers.
     * This should be called at the end of each frame and destructor.
     */
    public void reset() {
        mBytesInUse = 0;
        if (mIndex >= 0) {
            assert (mBufferPtr != NULL);
            GpuBuffer buffer = mBuffers[mIndex];
            assert (buffer.isLocked());
            assert (buffer.getLockedBuffer() == mBufferPtr);
            buffer.unlock();
            mBufferPtr = NULL;
        }
        while (mIndex >= 0) {
            GpuBuffer buffer = mBuffers[mIndex];
            assert (!buffer.isLocked());
            mBuffers[mIndex--] = GpuResource.move(buffer);
        }
        assert (mIndex == -1);
        assert (mBufferPtr == NULL);
    }

    /**
     * Frees data from makeSpaces in LIFO order.
     */
    public void putBack(int bytes) {
        while (bytes > 0) {
            // caller shouldn't try to put back more than they've taken
            assert (mIndex >= 0);
            GpuBuffer buffer = mBuffers[mIndex];
            int usedBytes = buffer.getSize() - mFreeBytes[mIndex];
            if (bytes >= usedBytes) {
                bytes -= usedBytes;
                mBytesInUse -= usedBytes;
                assert (buffer.isLocked());
                assert (buffer.getLockedBuffer() == mBufferPtr);
                buffer.unlock(/*offset=*/0, usedBytes);
                assert (!buffer.isLocked());
                mBuffers[mIndex--] = GpuResource.move(buffer);
                mBufferPtr = NULL;
            } else {
                mFreeBytes[mIndex] += bytes;
                mBytesInUse -= bytes;
                break;
            }
        }
    }

    /**
     * Returns a block of memory to hold vertices/instances/indices. A buffer
     * designated to hold the vertices/instances/indices given to the caller.
     * The buffer may or may not be locked. The returned ptr remains valid
     * until any of the following:
     * <ul>
     *      <li>this method is called again.</li>
     *      <li>{@link #flush()} is called.</li>
     *      <li>{@link #reset()} is called.</li>
     * </ul>
     * Once {@link #flush()} on the pool is called the vertices/instances/indices
     * are guaranteed to be in the buffer at the offset indicated by baseVertex/
     * baseInstance/firstIndex. Until that time they may be in temporary storage
     * and/or the buffer may be locked.
     *
     * @param mesh specifies the mesh to allocate space for
     * @return pointer to first vertex/instance/index, or NULL if failed
     */
    public abstract long makeSpace(Mesh mesh);

    /**
     * Similar to {@link #makeSpace(Mesh)}, but returns a wrapper instead.
     *
     * @param mesh specifies the mesh to allocate space for
     * @return pointer to first vertex/instance/index, or null if failed
     */
    @Nullable
    public abstract ByteBuffer makeWriter(Mesh mesh);

    /**
     * Returns a block of memory to hold data. A buffer designated to hold the
     * data is given to the caller. The buffer may or may not be locked. The
     * returned ptr remains valid until any of the following:
     * <ul>
     *      <li>this method is called again.</li>
     *      <li>{@link #flush()} is called.</li>
     *      <li>{@link #reset()} is called.</li>
     * </ul>
     * Once {@link #flush()} on the pool is called the data is guaranteed to be in the
     * buffer at the offset indicated by offset. Until that time it may be
     * in temporary storage and/or the buffer may be locked.
     *
     * @param size      the amount of data to make space for
     * @param alignment alignment constraint from start of buffer
     * @return pointer to where the client should write the data, may be nullptr.
     */
    protected long makeSpace(int size, int alignment) {
        assert (size > 0);
        assert (alignment > 0);

        if (mBufferPtr != NULL) {
            assert (mIndex >= 0);
            GpuBuffer buffer = mBuffers[mIndex];
            int pos = buffer.getSize() - mFreeBytes[mIndex];
            int pad = MathUtil.alignUpPad(pos, alignment);
            int alignedSize = size + pad;
            if (alignedSize <= 0) {
                return NULL; // overflow
            }
            if (alignedSize <= mFreeBytes[mIndex]) {
                mFreeBytes[mIndex] -= alignedSize;
                mBytesInUse += alignedSize;
                return mBufferPtr + pos + pad;
            }
        }

        int blockSize = Math.max(size, DEFAULT_BUFFER_SIZE);

        @SharedPtr
        GpuBuffer buffer = mServer.getContext().getResourceProvider()
                .createBuffer(blockSize, mBufferType | Engine.BufferUsageFlags.kStream);
        if (buffer == null) {
            return NULL;
        }

        // we only lock one buffer at the same time, so unlock the previous buffer
        flush();

        int cap = mBuffers.length;
        if (++mIndex >= cap) {
            cap = cap + (cap >> 1);
            mBuffers = Arrays.copyOf(mBuffers, cap);
            mFreeBytes = Arrays.copyOf(mFreeBytes, cap);
        }
        mBuffers[mIndex] = buffer;
        mFreeBytes[mIndex] = buffer.getSize() - size;
        mBytesInUse += size;

        assert (mBufferPtr == NULL);
        mBufferPtr = buffer.lock();
        assert (mBufferPtr != NULL);
        assert (buffer.isLocked());
        assert (buffer.getLockedBuffer() == mBufferPtr);
        return mBufferPtr;
    }

    private static class VertexPool extends BufferAllocPool {

        public VertexPool(Server server) {
            super(server, Engine.BufferUsageFlags.kVertex);
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
         * Once {@link #flush()} on the pool is called the vertices are guaranteed to be in
         * the buffer at the offset indicated by baseVertex. Until that time they
         * may be in temporary storage and/or the buffer may be locked.
         *
         * @param mesh specifies the mesh to allocate space for
         * @return pointer to first vertex, or NULL if failed
         */
        @Override
        public long makeSpace(Mesh mesh) {
            int vertexSize = mesh.getVertexSize();
            int vertexCount = mesh.getVertexCount();
            assert (vertexSize > 0 && vertexCount > 0);

            int totalSize = vertexSize * vertexCount;
            long ptr = makeSpace(totalSize, vertexSize);
            if (ptr == NULL) {
                return NULL;
            }

            GpuBuffer buffer = mBuffers[mIndex];
            int offset = (int) (ptr - mBufferPtr);
            assert (offset % vertexSize == 0);
            mesh.setVertexBuffer(buffer, offset / vertexSize, vertexCount);
            return ptr;
        }

        /**
         * Similar to {@link #makeSpace(Mesh)}, but returns a wrapper instead.
         *
         * @param mesh specifies the mesh to allocate space for
         * @return pointer to first vertex, or null if failed
         */
        @Nullable
        @Override
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
            int offset = (int) (ptr - mBufferPtr);
            assert (offset % vertexSize == 0);
            mesh.setVertexBuffer(buffer, offset / vertexSize, vertexCount);

            ByteBuffer writer = APIUtil.apiGetMappedBuffer(mWriter, mBufferPtr, buffer.getSize());
            assert (writer != null);
            writer.position(offset);
            writer.limit(offset + totalSize);
            mWriter = writer;
            return writer;
        }
    }

    private static class InstancePool extends BufferAllocPool {

        public InstancePool(Server server) {
            super(server, Engine.BufferUsageFlags.kVertex);
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
         * Once {@link #flush()} on the pool is called the instances are guaranteed to be in
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

            int totalSize = instanceSize * instanceCount;
            long ptr = makeSpace(totalSize, instanceSize);
            if (ptr == NULL) {
                return NULL;
            }

            GpuBuffer buffer = mBuffers[mIndex];
            int offset = (int) (ptr - mBufferPtr);
            assert (offset % instanceSize == 0);
            mesh.setInstanceBuffer(buffer, offset / instanceSize, instanceCount);
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
            int offset = (int) (ptr - mBufferPtr);
            assert (offset % instanceSize == 0);
            mesh.setInstanceBuffer(buffer, offset / instanceSize, instanceCount);

            ByteBuffer writer = APIUtil.apiGetMappedBuffer(mWriter, mBufferPtr, buffer.getSize());
            assert (writer != null);
            writer.position(offset);
            writer.limit(offset + totalSize);
            mWriter = writer;
            return writer;
        }
    }
}
