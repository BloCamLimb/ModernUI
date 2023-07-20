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

import icyllis.modernui.graphics.MathUtil;

/**
 * Used to sub-allocate a large GPU buffer, typically for uniform buffers.
 */
public class RingBuffer {

    private final ResourceProvider mResourceProvider;

    private int mSize;
    private final int mAlignment;
    private final int mBufferType;

    // unsigned
    private int mHead;
    private int mTail;

    private int mBufferSeqNum;

    /**
     * Alignment is 256 in most cases.
     *
     * @param size       initial buffer size
     * @param alignment  alignment requirement for each slice
     * @param bufferType buffer type buffer usage flag
     */
    public RingBuffer(ResourceProvider resourceProvider,
                      int size, int alignment, int bufferType) {
        if (!MathUtil.isPow2(size)) {
            throw new IllegalArgumentException("size must be a power of two");
        }
        if (!MathUtil.isPow2(alignment)) {
            throw new IllegalArgumentException("alignment must be a power of two");
        }
        mResourceProvider = resourceProvider;
        mSize = size;
        mAlignment = alignment;
        mBufferType = bufferType;
    }

    protected final int nextOffset(int size) {
        int head = mHead;
        int tail = mTail;

        int modHead = head & (mSize - 1);
        int modTail = tail & (mSize - 1);

        if (head != tail && modHead == modTail) {
            return mSize;
        }

        // case 1: free space lies at the beginning and/or the end of the buffer
        if (modHead >= modTail) {
            // check for room at the end
            if (mSize - modHead < size) {
                // no room at the end, check the beginning
                if (modTail < size) {
                    // no room at the beginning
                    return mSize;
                }
                // we are going to allocate from the beginning, adjust head to '0' position
                head += mSize - modHead;
                modHead = 0;
            }
            // case 2: free space lies in the middle of the buffer, check for room there
        } else if (modTail - modHead < size) {
            // no room in the middle
            return mSize;
        }

        mHead = MathUtil.alignTo(head + size, mAlignment);
        return modHead;
    }

    public int allocate(int size, long data) {
        return mSize;
    }
}
