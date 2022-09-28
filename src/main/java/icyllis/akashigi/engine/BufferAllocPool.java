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

package icyllis.akashigi.engine;

import icyllis.akashigi.core.SharedPtr;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static icyllis.akashigi.engine.Engine.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * A pool of geometry buffers tied to a {@link Server}. (Render thread only.)
 * <p>
 * The pool allows a client to make space for geometry and then put back excess
 * space if it over allocated. When a client is ready to draw from the pool
 * it calls unmap on the pool ensure buffers are ready for drawing. The pool
 * can be reset after drawing is completed to recycle space.
 * <p>
 * At creation time a minimum per-buffer size can be specified. Additionally,
 * a number of buffers to preallocate can be specified. These will
 * be allocated at the min size and kept around until the pool is destroyed.
 * <p>
 * <b>NOTE:</b> You must call {@link #reset()} to reset for next flush.
 */
public abstract class BufferAllocPool implements AutoCloseable {

    public static final int DEFAULT_BUFFER_SIZE = 1 << 16;

    private final Server mServer;
    private final int mBufferType;

    // blocks
    @SharedPtr
    protected Buffer[] mBuffers = new Buffer[8];
    protected int[] mFreeBytes = new int[8];
    protected int mIndex = -1;

    protected long mBufferPtr;

    private int mBytesInUse;

    /**
     * Constructor.
     *
     * @param server     the server used to create the buffers.
     * @param bufferType the type of buffers to create.
     */
    protected BufferAllocPool(Server server, int bufferType) {
        assert (bufferType == GpuBufferType_Vertex || bufferType == GpuBufferType_Index);
        mServer = server;
        mBufferType = bufferType;
    }

    @Override
    public void close() {
        reset();
    }

    /**
     * Ensures all buffers are unlocked and have all data written to them.
     * Call before drawing using buffers from the pool.
     */
    public abstract void flush();

    /**
     * Invalidates all the data in the pool, unrefs non-preallocated buffers.
     * This should be called at the end of each frame and destructor.
     */
    public void reset() {
        mBytesInUse = 0;
        deleteBlocks();
    }

    /**
     * Frees data from makeSpaces in LIFO order.
     */
    public void putBack(int bytes) {
        while (bytes > 0) {
            // caller shouldn't try to put back more than they've taken
            assert (mIndex >= 0);
            Buffer buffer = mBuffers[mIndex];
            int usedBytes = buffer.size() - mFreeBytes[mIndex];
            if (bytes >= usedBytes) {
                bytes -= usedBytes;
                mBytesInUse -= usedBytes;
                // if we locked a vb to satisfy the make space and we're releasing
                // beyond it, then unlock it without flushing.
                unlockBuffer(buffer);
                assert (mIndex >= 0);
                mBuffers[mIndex--] = Resource.move(buffer);
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
     *      <li>{@link #close()} is called.</li>
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
     *      <li>{@link #close()} is called.</li>
     * </ul>
     * Once unmap on the pool is called the data is guaranteed to be in the
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
            Buffer buffer = mBuffers[mIndex];
            int usedBytes = buffer.size() - mFreeBytes[mIndex];
            int padding = (alignment - usedBytes % alignment) % alignment;
            int alignedSize = size + padding;
            if (alignedSize <= 0) {
                return NULL;
            }
            if (alignedSize <= mFreeBytes[mIndex]) {
                memSet(mBufferPtr + usedBytes, 0, padding);
                usedBytes += padding;
                mFreeBytes[mIndex] -= alignedSize;
                mBytesInUse += alignedSize;
                return mBufferPtr + usedBytes;
            }
        }

        // We could honor the space request using by a partial update of the current
        // VB (if there is room). But we don't currently use draw calls to GL that
        // allow the driver to know that previously issued draws won't read from
        // the part of the buffer we update. Also, when this was written the GL
        // buffer implementation was cheating on the actual buffer size by shrinking
        // the buffer in updateData() if the amount of data passed was less than
        // the full buffer size. This is old code and both concerns may be obsolete.

        if (!createBlock(size)) {
            return NULL;
        }
        assert (mBufferPtr != NULL);

        mFreeBytes[mIndex] -= size;
        mBytesInUse += size;
        return mBufferPtr;
    }

    @Nullable
    @SharedPtr
    protected Buffer createBuffer(int size) {
        // We use Stream for VBO and IBO because we are 2D rendering, there are few vertices and may
        // frequently change (in each frame).
        return mServer.getContext().getResourceProvider()
                .createBuffer(size, mBufferType, AccessPattern_Stream);
    }

    /**
     * Implements this to create staging buffers.
     *
     * @param buffer the top GPU buffer in the pool
     */
    protected abstract long lockBuffer(Buffer buffer);

    /**
     * Implements this to drop the staging buffer without flushing.
     *
     * @param buffer the top GPU buffer in the pool
     */
    //TODO can we delete this even for vulkan?
    protected abstract void unlockBuffer(Buffer buffer);

    private boolean createBlock(int size) {
        size = Math.max(size, DEFAULT_BUFFER_SIZE);

        Buffer buffer = createBuffer(size);
        if (buffer == null) {
            return false;
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
        mFreeBytes[mIndex] = buffer.size();

        // ensure we unlocked the previous buffer
        assert (mBufferPtr == NULL);
        mBufferPtr = lockBuffer(buffer);
        // ensure we locked the current buffer
        assert (mBufferPtr != NULL);

        return true;
    }

    private void deleteBlocks() {
        if (mIndex >= 0) {
            unlockBuffer(mBuffers[mIndex]);
        }
        while (mIndex >= 0) {
            Buffer buffer = mBuffers[mIndex];
            mBuffers[mIndex--] = Resource.move(buffer);
            mBufferPtr = NULL;
        }
        assert (mBufferPtr == NULL);
    }
}
