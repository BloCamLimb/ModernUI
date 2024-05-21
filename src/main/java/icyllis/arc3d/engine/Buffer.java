/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.engine;

import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a single device-visible memory region that may be used as mesh buffers and
 * staging buffers. A buffer cannot be accessed by both CPU and GPU simultaneously, it's
 * either locked by engine or executing in command list.
 */
public abstract class Buffer extends Resource {

    /**
     * Locks for reading. The effect of writes is undefined.
     */
    public static final int kRead_LockMode = 0;
    /**
     * Locks for writing. The existing contents are discarded and the initial contents of the
     * buffer. Reads (even after overwriting initial contents) should be avoided for performance
     * reasons as the memory may not be cached.
     */
    public static final int kWriteDiscard_LockMode = 1;

    protected final long mSize;
    protected final int mUsage;

    private long mLockOffset;
    private long mLockSize;

    protected Buffer(Device device,
                     long size,
                     int usage) {
        super(device);
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
     * @return {@link Engine.BufferUsageFlags}
     */
    public final int getUsage() {
        return mUsage;
    }

    @Override
    public final long getMemorySize() {
        return mSize;
    }

    private static int getLockMode(int usage) {
        return (usage & Engine.BufferUsageFlags.kTransferDst) != 0
                ? kRead_LockMode
                : kWriteDiscard_LockMode;
    }

    /**
     * Locks the buffer to be read or written by the CPU.
     * <p>
     * It is an error to draw from the buffer while it is locked or transfer to/from the buffer.
     * Once a buffer is locked, subsequent calls to this method will throw an exception.
     * <p>
     * If the buffer is of type {@link Engine.BufferUsageFlags#kTransferDst} then it is locked for
     * reading only. Otherwise it is locked writing only. Writing to a buffer that is locked for
     * reading or vice versa produces undefined results. If the buffer is locked for writing
     * then the buffer's previous contents are invalidated.
     *
     * @return a valid pointer to the locked data
     */
    public final long lock() {
        if (isDestroyed()) {
            return NULL;
        }
        if (isLocked()) {
            throw new IllegalStateException("Already locked");
        }
        mLockOffset = 0;
        mLockSize = mSize;
        return onLock(getLockMode(mUsage), 0, mSize);
    }

    /**
     * Locks the buffer to be read or written by the CPU.
     * <p>
     * It is an error to draw from the buffer while it is locked or transfer to/from the buffer.
     * Once a buffer is locked, subsequent calls to this method will throw an exception.
     * <p>
     * If the buffer is of type {@link Engine.BufferUsageFlags#kTransferDst} then it is locked for
     * reading only. Otherwise it is locked writing only. Writing to a buffer that is locked for
     * reading or vice versa produces undefined results. If the buffer is locked for writing
     * then the buffer's previous contents are invalidated.
     *
     * @return a valid pointer to the locked data, or nullptr if lock failed
     */
    public final long lock(long offset, long size) {
        if (isDestroyed()) {
            return NULL;
        }
        if (isLocked()) {
            throw new IllegalStateException("Already locked");
        }
        Objects.checkFromIndexSize(offset, size, mSize);
        mLockOffset = offset;
        mLockSize = size;
        return onLock(getLockMode(mUsage), offset, size);
    }

    /**
     * Unlocks the buffer if it is locked.
     * <p>
     * The pointer returned by the previous {@link #lock(long, long)} will no longer be valid.
     */
    public final void unlock() {
        if (isDestroyed()) {
            return;
        }
        if (isLocked()) {
            onUnlock(getLockMode(mUsage), mLockOffset, mLockSize);
        }
        assert (!isLocked());
    }

    /**
     * Unlocks the buffer if it is locked.
     * <p>
     * The pointer returned by the previous {@link #lock(long, long)} will no longer be valid.
     */
    public final void unlock(long offset, long size) {
        if (isDestroyed()) {
            return;
        }
        if (isLocked()) {
            if (offset < mLockOffset || size > mLockSize) {
                throw new IllegalStateException();
            }
            onUnlock(getLockMode(mUsage), offset, size);
        }
        assert (!isLocked());
    }

    protected abstract long onLock(int mode, long offset, long size);

    protected abstract void onUnlock(int mode, long offset, long size);

    /**
     * Queries whether the buffer has been locked by {@link #lock(long, long)},
     * this is mostly used for validation.
     *
     * @return true if the buffer is locked, false otherwise.
     */
    public abstract boolean isLocked();

    /**
     * Queries the pointer returned by the previous {@link #lock(long, long)} if
     * {@link #isLocked()} returns true, otherwise the pointer is invalid,
     * this is mostly used for validation.
     *
     * @return the pointer to the locked buffer if locked.
     */
    public abstract long getLockedBuffer();

    /**
     * Updates the buffer data.
     * <p>
     * The size of the buffer will be preserved. The src data will be
     * placed at offset. If preserve is false then any remaining content
     * before/after the range [offset, offset+size) becomes undefined.
     * <p>
     * The buffer must not be locked.
     * <p>
     * Fails for {@link Engine.BufferUsageFlags#kTransferDst}.
     * <p>
     * Note that buffer updates do not go through Context and therefore are
     * not serialized with other operations.
     *
     * @return returns true if the update succeeds, false otherwise.
     */
    public boolean updateData(int offset, int size, long data) {
        assert (data != NULL);
        if (isDestroyed() || isLocked()) {
            return false;
        }
        assert (size > 0 && offset + size <= mSize);

        if ((mUsage & Engine.BufferUsageFlags.kTransferDst) != 0) {
            return false;
        }

        return onUpdateData(offset, size, data);
    }

    protected abstract boolean onUpdateData(int offset, int size, long data);
}
