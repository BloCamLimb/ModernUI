/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.MathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Add a block of data to the cache and return a stable pointer to the contents (assuming that a
 * resettable gatherer had accumulated the input data pointer).
 * <p>
 * If an identical block of data is already in the cache, that existing pointer is returned, making
 * pointer comparison suitable when comparing data blocks retrieved from the cache.
 */
public class UniformDataCache implements AutoCloseable {

    // key and value are same object
    private final HashMap<ByteBuffer, ByteBuffer> mPointers = new HashMap<>();

    /**
     * Find an existing data block with the contents of the given <var>block</var>.
     * The given <var>block</var> is just a memory view, and it can be reused by
     * changing its {@link ByteBuffer#position()} and {@link ByteBuffer#limit()}.
     * The {@link ByteBuffer#hashCode()} and {@link ByteBuffer#equals(Object)} of
     * the given <var>block</var> will be used. If absent, a copy of the given data
     * will be made and its memory is managed by this object, return value is a
     * stable pointer to that memory, you can simply check the reference equality
     * via <code>==</code> as it's deduplicated.
     *
     * @param block an immutable view of uniform data
     * @return a stable pointer to existing or copied uniform data
     */
    @Nullable
    public ByteBuffer insert(@Nonnull ByteBuffer block) {
        if (!block.hasRemaining()) {
            return null;
        }
        // the key of HashMap and the given ByteBuffer will never be the same object
        // so we are hashing and comparing their contents via vectorizedMismatch()
        ByteBuffer existing = mPointers.get(block);
        if (existing != null) {
            return existing;
        } else {
            ByteBuffer copy = allocate(block);
            mPointers.put(copy, copy);
            return copy;
        }
    }

    /**
     * The number of unique data blocks in the cache.
     */
    public int size() {
        return mPointers.size();
    }

    @Override
    public void close() {
        freeBlocks();
        mPointers.clear();
    }

    // To return a stable pointer, we may not modify the address of existing ByteBuffer,
    // then we don't use memRealloc
    //
    // Here we implement a linked list and block allocation, the capacity of each new block
    // is 1.5 times that of the previous one and 256-byte aligned, the address is 8 or 16-byte aligned
    // starting offset is 8-byte aligned (matching a pointer)
    // the initial block size is 1024 bytes based on the practical size of uniform data

    // the underlying block allocation
    static class Block {

        // pointer to native memory
        final long mStorage;
        final int mCapacity;
        int mPosition = 0;

        final Block mPrev;

        Block(Block prev, int capacity) {
            mPrev = prev;
            mStorage = nmemAlloc(capacity);
            if (mStorage == NULL) {
                // always do explicit check
                throw new OutOfMemoryError();
            }
            // the address is 8 or 16-byte aligned
            assert MathUtil.isAlign8(mStorage);
            mCapacity = capacity;
        }

        void free() {
            nmemFree(mStorage);
        }
    }

    Block mTail;

    ByteBuffer allocate(@Nonnull ByteBuffer block) {
        int size = block.remaining();
        if (mTail == null) {
            int initialCapacity = MathUtil.alignTo(
                    Math.max(size, 1024),
                    256
            ); // capacity is 256-byte aligned
            mTail = new Block(null, initialCapacity);
        }

        // starting offset is 8-byte aligned (matching a pointer)
        int offset = MathUtil.align8(mTail.mPosition);
        int end = offset + size;
        if (end > mTail.mCapacity) {
            int newCapacity = MathUtil.alignTo(
                    Math.max(size, mTail.mCapacity + (mTail.mCapacity >> 1)), // 1.5 times
                    256
            ); // capacity is 256-byte aligned
            Block prev = mTail;
            mTail = new Block(prev, newCapacity);

            offset = 0;
            end = size;
        }

        memCopy(
                /*src*/ memAddress(block),
                /*dst*/ mTail.mStorage + offset,
                size);
        mTail.mPosition = end;

        // must return a new object
        return memByteBuffer(mTail.mStorage, mTail.mCapacity)
                .position(offset)
                .limit(end);
    }

    void freeBlocks() {
        for (var block = mTail; block != null; block = block.mPrev) {
            block.free();
        }
        mTail = null;
    }
}
