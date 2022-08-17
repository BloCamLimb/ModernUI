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

import icyllis.arcui.core.SharedPtr;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import javax.annotation.Nullable;
import java.util.ArrayList;

import static icyllis.arcui.engine.EngineTypes.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * A pool of geometry buffers tied to a {@link Server}.
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
 * <b>NOTE:</b> You must call {@link #reset()} to reset or when this pool is no longer used.
 */
public abstract class BufferAllocPool implements AutoCloseable {

    public static final int DEFAULT_BUFFER_SIZE = 1 << 15;

    /**
     * A cache object that can be shared by multiple {@link BufferAllocPool} instances. It caches
     * cpu buffer allocations to avoid reallocating them.
     * <p>
     * <b>NOTE:</b> You must call {@link #releaseAll()} to reset or when this cache is no longer used.
     */
    public static class CpuBufferCache {

        private final CpuBuffer[] mBuffers;

        public CpuBufferCache(int maxBuffersToCache) {
            mBuffers = new CpuBuffer[maxBuffersToCache];
        }

        @SharedPtr
        public CpuBuffer makeBuffer(int size) {
            assert (size > 0);
            CpuBuffer result = null;
            if (size == DEFAULT_BUFFER_SIZE) {
                int i = 0;
                for (; i < mBuffers.length && mBuffers[i] != null; ++i) {
                    assert (mBuffers[i].size() == DEFAULT_BUFFER_SIZE);
                    if (mBuffers[i].unique()) {
                        result = mBuffers[i];
                    }
                }
                if (result == null && i < mBuffers.length) {
                    mBuffers[i] = result = new CpuBuffer(size);
                }
            }
            if (result == null) {
                return new CpuBuffer(size);
            }
            result.ref();
            return result;
        }

        public void releaseAll() {
            for (int i = 0; i < mBuffers.length && mBuffers[i] != null; ++i) {
                mBuffers[i].unref();
                mBuffers[i] = null;
            }
        }
    }

    private final Server mServer;
    private final int mBufferType;
    private final CpuBufferCache mCpuBufferCache;

    @SharedPtr
    final ArrayList<Buffer> mBufferBlocks = new ArrayList<>(8);
    final IntArrayList mFreeBytesBlocks = new IntArrayList(8);

    @SharedPtr
    private CpuBuffer mCpuStagingBuffer;
    long mBufferPtr;

    private int mBytesInUse;

    /**
     * Constructor.
     *
     * @param server         the server used to create the buffers.
     * @param bufferType     the type of buffers to create.
     * @param cpuBufferCache if non-null a cache for client side array buffers
     *                       or staging buffers used before data is uploaded to
     *                       GPU buffer objects.
     */
    protected BufferAllocPool(Server server, int bufferType, CpuBufferCache cpuBufferCache) {
        assert (bufferType >= 0 && bufferType < GpuBufferTypeCount);
        mServer = server;
        mBufferType = bufferType;
        mCpuBufferCache = cpuBufferCache;
    }

    @Override
    public void close() {
        deleteBlocks();
    }

    /**
     * Ensures all buffers are unmapped and have all data written to them.
     * Call before drawing using buffers from the pool.
     */
    public void flush() {
        // unmap or flush CPU data
        if (mBufferPtr != NULL) {
            assert (!mBufferBlocks.isEmpty());
            int back = mBufferBlocks.size() - 1;
            Buffer buf = mBufferBlocks.get(back);
            if (!buf.isCpuBuffer()) {
                GpuBuffer buffer = (GpuBuffer) buf;
                if (buffer.isMapped()) {
                    buffer.unmap();
                } else {
                    int flushSize = buf.size() - mFreeBytesBlocks.getInt(back);
                    flushCpuData(buffer, flushSize);
                }
            }
            mBufferPtr = NULL;
        }
    }

    /**
     * Invalidates all the data in the pool, unrefs non-preallocated buffers.
     */
    public void reset() {
        mBytesInUse = 0;
        deleteBlocks();
        resetCpuData(0);
    }

    /**
     * Frees data from makeSpaces in LIFO order.
     */
    public void putBack(int bytes) {
        while (bytes > 0) {
            // caller shouldn't try to put back more than they've taken
            assert (!mBufferBlocks.isEmpty());
            int back = mBufferBlocks.size() - 1;
            Buffer buf = mBufferBlocks.get(back);
            int usedBytes = buf.size() - mFreeBytesBlocks.getInt(back);
            if (bytes >= usedBytes) {
                bytes -= usedBytes;
                mBytesInUse -= usedBytes;
                // if we locked a vb to satisfy the make space and we're releasing
                // beyond it, then unmap it.
                if (!buf.isCpuBuffer()) {
                    GpuBuffer buffer = (GpuBuffer) buf;
                    if (buffer.isMapped()) {
                        buffer.unmap();
                    }
                }
                destroyBlock();
            } else {
                mFreeBytesBlocks.set(back, mFreeBytesBlocks.getInt(back) + bytes);
                mBytesInUse -= bytes;
                break;
            }
        }
    }

    /**
     * Returns a block of memory to hold data. A buffer designated to hold the
     * data is given to the caller. The buffer may or may not be locked. The
     * returned ptr remains valid until any of the following:
     * <ul>
     *      <li>this method is called again.</li>
     *      <li>{@link #flush()} is called.</li>
     *      <li>{@link #reset()} is called.</li>
     * </ul>
     * Once unmap on the pool is called the data is guaranteed to be in the
     * buffer at the offset indicated by offset. Until that time it may be
     * in temporary storage and/or the buffer may be locked.
     *
     * @param size      the amount of data to make space for
     * @param alignment alignment constraint from start of buffer
     * @return pointer to where the client should write the data.
     */
    protected long makeSpace(int size, int alignment) {
        assert (size > 0);
        assert (alignment > 0);

        if (mBufferPtr != NULL) {
            int back = mBufferBlocks.size() - 1;
            Buffer buffer = mBufferBlocks.get(back);
            int freeBytes = mFreeBytesBlocks.getInt(back);
            int usedBytes = buffer.size() - freeBytes;
            int padding = (alignment - usedBytes % alignment) % alignment;
            int alignedSize = size + padding;
            if (alignedSize <= 0) {
                return NULL;
            }
            if (alignedSize <= freeBytes) {
                memSet(mBufferPtr + usedBytes, 0, padding);
                usedBytes += padding;
                mFreeBytesBlocks.set(back, freeBytes - alignedSize);
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

        int back = mBufferBlocks.size() - 1;
        mFreeBytesBlocks.set(back, mFreeBytesBlocks.getInt(back) - size);
        mBytesInUse += size;
        return mBufferPtr;
    }

    @Nullable
    @SharedPtr
    protected Buffer createBuffer(int size) {
        if (mBufferType == GpuBufferType_DrawIndirect &&
                mServer.getCaps().useClientSideIndirectBuffers()) {
            // Create a CPU buffer.
            return mCpuBufferCache != null ? mCpuBufferCache.makeBuffer(size)
                    : new CpuBuffer(size);
        }
        return mServer.getContext().getResourceProvider()
                .createBuffer(size, mBufferType, AccessPattern_Dynamic);
    }

    private boolean createBlock(int size) {
        size = Math.max(size, DEFAULT_BUFFER_SIZE);

        Buffer buffer = createBuffer(size);
        if (buffer == null) {
            return false;
        }

        flush();

        mBufferBlocks.add(buffer);
        mFreeBytesBlocks.add(buffer.size());
        assert (mBufferBlocks.size() == mFreeBytesBlocks.size());

        // If the buffer is CPU-backed we "map" it because it is free to do so and saves a copy.
        // Otherwise when buffer mapping is supported we map if the buffer size is greater than the
        // threshold.
        if (buffer.isCpuBuffer()) {
            mBufferPtr = ((CpuBuffer) buffer).data();
        } else {
            if (size > mServer.getCaps().bufferMapThreshold()) {
                mBufferPtr = ((GpuBuffer) buffer).map();
            }
            // cannot map or failed to map
            if (mBufferPtr == NULL) {
                resetCpuData(buffer.size());
                mBufferPtr = mCpuStagingBuffer.data();
            }
        }
        assert (mBufferPtr != NULL);

        return true;
    }

    private void destroyBlock() {
        assert (!mBufferBlocks.isEmpty());
        int back = mBufferBlocks.size() - 1;
        Buffer buffer = mBufferBlocks.get(back);
        assert (buffer.isCpuBuffer() || !((GpuBuffer) buffer).isMapped());
        buffer.unref();
        mBufferBlocks.remove(back);
        mFreeBytesBlocks.removeInt(back);
        mBufferPtr = NULL;
    }

    private void deleteBlocks() {
        // unmap only
        if (!mBufferBlocks.isEmpty()) {
            int back = mBufferBlocks.size() - 1;
            Buffer buf = mBufferBlocks.get(back);
            if (!buf.isCpuBuffer()) {
                GpuBuffer buffer = (GpuBuffer) buf;
                if (buffer.isMapped()) {
                    buffer.unmap();
                }
            }
        }
        while (!mBufferBlocks.isEmpty()) {
            destroyBlock();
        }
        assert (mBufferPtr == NULL);
    }

    private void flushCpuData(GpuBuffer buffer, int size) {
        assert (!buffer.isMapped());
        assert (mCpuStagingBuffer != null && mCpuStagingBuffer.data() == mBufferPtr);
        assert (size <= mCpuStagingBuffer.size() && size <= buffer.size());

        if (size > mServer.getCaps().bufferMapThreshold()) {
            long data = buffer.map();
            if (data != NULL) {
                memCopy(mBufferPtr, data, size);
                buffer.unmap();
                return;
            }
        }
        buffer.updateData(mBufferPtr, /*offset=*/0, size);
    }

    private void resetCpuData(int size) {
        assert (size >= DEFAULT_BUFFER_SIZE || size == 0);
        if (size == 0) {
            if (mCpuStagingBuffer != null) {
                mCpuStagingBuffer.unref();
                mCpuStagingBuffer = null;
            }
            return;
        }
        if (mCpuStagingBuffer != null) {
            if (size <= mCpuStagingBuffer.size()) {
                return;
            }
            mCpuStagingBuffer.unref();
        }
        mCpuStagingBuffer = mCpuBufferCache != null ? mCpuBufferCache.makeBuffer(size)
                : new CpuBuffer(size);
    }
}
