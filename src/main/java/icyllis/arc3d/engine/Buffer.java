/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import icyllis.arc3d.engine.Engine.BufferUsageFlags;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a single device-visible memory region that may be used as mesh buffers and
 * staging buffers. A buffer cannot be accessed by both CPU and GPU simultaneously, it's
 * either mapped by engine or executing in command list.
 */
public abstract class Buffer extends Resource {

    /**
     * Maps for reading. The effect of writes is undefined.
     */
    public static final int kRead_MapMode = 0;
    /**
     * Maps for writing. The existing contents are discarded and the initial contents of the
     * buffer. Reads (even after overwriting initial contents) should be avoided for performance
     * reasons as the memory may not be cached.
     */
    public static final int kWriteDiscard_MapMode = 1;

    protected final long mSize;
    protected final int mUsage;

    private boolean mMapped;
    private long mMappedBuffer;
    private long mMapOffset;
    private long mMapSize;

    protected Buffer(Context context,
                     long size,
                     int usage) {
        super(context, /*budgeted*/true, /*wrapped*/false, size);
        assert (size > 0);
        mSize = size;
        mUsage = usage;
    }

    /**
     * @return allocation size of the buffer in bytes
     */
    public final long getSize() {
        return mSize;
    }

    /**
     * @return {@link BufferUsageFlags}
     */
    public final int getUsage() {
        return mUsage;
    }

    private static int getMapMode(int usage) {
        return (usage & BufferUsageFlags.kReadback) != 0
                ? kRead_MapMode
                : kWriteDiscard_MapMode;
    }

    /**
     * Maps the buffer to be read or written by the CPU.
     * <p>
     * Mapping works only for {@link BufferUsageFlags#kHostVisible} buffers. Writing to or
     * reading from a buffer that is currently executing in command buffer results in undefined
     * behavior. It is an error to draw from the buffer while it is mapped or transfer to/from
     * the buffer, no matter whether it's persistently mapped or not.
     * <p>
     * If the buffer is of type {@link BufferUsageFlags#kReadback} then it is mapped for
     * reading only. Otherwise it is mapped writing only. Writing to a buffer that is mapped for
     * reading or vice versa produces undefined results. If the buffer is mapped for writing
     * then the buffer's previous contents are invalidated.
     *
     * @return a valid pointer to the mapped data, or nullptr if lock failed
     */
    public final long map() {
        return map(0, mSize);
    }

    /**
     * Maps the buffer to be read or written by the CPU.
     * <p>
     * Mapping works only for {@link BufferUsageFlags#kHostVisible} buffers. Writing to or
     * reading from a buffer that is currently executing in command buffer results in undefined
     * behavior. It is an error to draw from the buffer while it is mapped or transfer to/from
     * the buffer, no matter whether it's persistently mapped or not.
     * <p>
     * If the buffer is of type {@link BufferUsageFlags#kReadback} then it is mapped for
     * reading only. Otherwise it is mapped writing only. Writing to a buffer that is mapped for
     * reading or vice versa produces undefined results. If the buffer is mapped for writing
     * then the buffer's previous contents are invalidated.
     *
     * @return a valid pointer to the mapped data, or nullptr if lock failed
     */
    public final long map(long offset, long size) {
        if ((mUsage & BufferUsageFlags.kHostVisible) == 0) {
            // never succeed
            return NULL;
        }
        if (mMapped) {
            assert offset == mMapOffset && size == mMapSize;
            // this may be nullptr if the last map() failed
            return mMappedBuffer;
        }
        assert offset >= 0 && offset + size <= mSize;
        mMapped = true;
        mMapOffset = offset;
        mMapSize = size;
        mMappedBuffer = onMap(getMapMode(mUsage), offset, size);
        return mMappedBuffer;
    }

    /**
     * Unmaps the buffer if it is mapped.
     * <p>
     * The pointer returned by the previous {@link #map(long, long)} will no longer be valid.
     */
    public final void unmap() {
        unmap(mMapOffset, mMapSize);
    }

    /**
     * Unmaps the buffer if it is mapped.
     * <p>
     * The pointer returned by the previous {@link #map(long, long)} will no longer be valid.
     */
    public final void unmap(long offset, long size) {
        if (isDestroyed()) {
            return;
        }
        if (mMapped) {
            assert offset >= mMapOffset && offset + size <= mMapOffset + mMapSize;
            onUnmap(getMapMode(mUsage), offset, size);
            mMapped = false;
            mMappedBuffer = NULL;
        }
    }

    protected abstract long onMap(int mode, long offset, long size);

    protected abstract void onUnmap(int mode, long offset, long size);

    /**
     * Queries whether the buffer has been mapped by {@link #map(long, long)},
     * this is mostly used for validation.
     *
     * @return true if the buffer is mapped, false otherwise.
     */
    public final boolean isMapped() {
        return mMapped;
    }

    /**
     * Queries the pointer returned by the previous {@link #map(long, long)} if
     * {@link #isMapped()} returns true, otherwise the pointer is invalid,
     * this is mostly used for validation.
     *
     * @return the pointer to the mapped buffer if mapped.
     */
    public final long getMappedBuffer() {
        return mMappedBuffer;
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
     * Fails for {@link BufferUsageFlags#kReadback}.
     * <p>
     * Note that buffer updates do not go through Context and therefore are
     * not serialized with other operations.
     *
     * @return returns true if the update succeeds, false otherwise.
     */
    public boolean updateData(int offset, int size, long data) {
        assert (data != NULL);
        if (isDestroyed() || isMapped()) {
            return false;
        }
        assert (size > 0 && offset + size <= getSize());

        return false;
    }
}
