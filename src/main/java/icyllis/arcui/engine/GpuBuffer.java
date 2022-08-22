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

import static icyllis.arcui.engine.EngineTypes.GpuBufferType_XferDstToSrc;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a device memory block that <b>prefers</b> to allocate GPU memory.
 * Also known as geometric buffer, g-buffer. To be exact, GLBuffer or VkBuffer.
 */
public abstract class GpuBuffer extends GpuResource implements Buffer {

    /**
     * Maps for reading. The effect of writes is undefined.
     */
    protected static final int MapType_Read = 0;
    /**
     * Maps for writing. The existing contents are discarded and the initial contents of the
     * buffer. Reads (even after overwriting initial contents) should be avoided for performance
     * reasons as the memory may not be cached.
     */
    protected static final int MapType_WriteDiscard = 1;

    private final int mSize;
    private final int mBufferType;
    private final int mAccessPattern;

    protected long mMapPtr = NULL;

    protected GpuBuffer(Server server,
                        int size,
                        int bufferType,
                        int accessPattern) {
        super(server);
        mSize = size;
        mBufferType = bufferType;
        mAccessPattern = accessPattern;
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    public boolean isCpuBuffer() {
        return false;
    }

    public int bufferType() {
        return mBufferType;
    }

    public int accessPattern() {
        return mAccessPattern;
    }

    @Override
    public long getMemorySize() {
        return mSize;
    }

    /**
     * Maps the buffer to be read or written by the CPU.
     * <p>
     * It is an error to draw from the buffer while it is mapped or transfer to/from the buffer. It
     * may fail if the backend doesn't support mapping the buffer. Once a buffer is mapped,
     * subsequent calls to map() trivially succeed. No matter how many times map() is called,
     * unmap() will unmap the buffer on the first call if it is mapped.
     * <p>
     * If the buffer is of type GrGpuBufferType::kXferGpuToCpu then it is mapped for reading only.
     * Otherwise it is mapped writing only. Writing to a buffer that is mapped for reading or vice
     * versa produces undefined results. If the buffer is mapped for writing then the buffer's
     * previous contents are invalidated.
     *
     * @return a pointer to the data or NULL if the map fails.
     */
    public final long map() {
        if (wasDestroyed()) {
            return NULL;
        }
        if (mMapPtr == NULL) {
            onMap();
        }
        return mMapPtr;
    }

    protected abstract void onMap();

    /**
     * Unmaps the buffer if it is mapped.
     * <p>
     * The pointer returned by the previous map call will no longer be valid.
     */
    public final void unmap() {
        if (wasDestroyed()) {
            return;
        }
        assert (mMapPtr != NULL);
        onUnmap();
        mMapPtr = NULL;
    }

    protected abstract void onUnmap();

    /**
     * Queries whether the buffer has been mapped in program order.
     *
     * @return true if the buffer is mapped, false otherwise.
     */
    public boolean isMapped() {
        return mMapPtr != NULL;
    }

    /**
     * Updates the buffer data.
     * <p>
     * The size of the buffer will be preserved. The src data will be
     * placed at offset. If preserve is false then any remaining content
     * before/after the range [offset, offset+size) becomes undefined.
     * <p>
     * The buffer must not be mapped.
     * <p>
     * Fails for {@link EngineTypes#GpuBufferType_XferDstToSrc}.
     * <p>
     * Note that buffer updates do not go through Context and therefore are
     * not serialized with other operations.
     *
     * @return returns true if the update succeeds, false otherwise.
     */
    public boolean updateData(long data, int offset, int size) {
        assert (!isMapped());
        assert (size > 0 && offset + size <= mSize);
        assert (data != NULL);

        if (wasDestroyed()) {
            return false;
        }

        if (mBufferType == GpuBufferType_XferDstToSrc) {
            return false;
        }

        return onUpdateData(data, offset, size);
    }

    protected abstract boolean onUpdateData(long data, int offset, int size);
}
