/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Allocates staging buffers to update GPU-only buffer and image sub resources.
 */
public class UploadBufferManager {

    public static final long SMALL_BUFFER_SIZE = 1 << 18; // 256 KB

    /**
     * Return an appropriate size for dedicated upload buffer to improve resource reuse.
     * Values <= 4 MB will pop up to the next power of 2. Values <= 64 MB will go up half
     * the floor power of 2. Values > 64 MB will go up to the next multiple of 16 MB.
     */
    public static long getLargeBufferSize(long minBytes) {
        assert minBytes >= SMALL_BUFFER_SIZE;

        if (MathUtil.isPow2(minBytes)) {
            return minBytes;
        }

        long ceilPow2 = MathUtil.ceilPow2(minBytes);
        if (minBytes <= (1 << 22)) { // 4 MB
            return ceilPow2;
        }

        if (minBytes <= (1 << 26)) { // 64 MB
            long floorPow2 = ceilPow2 >> 1;
            long mid = floorPow2 + (floorPow2 >> 1);

            if (minBytes <= mid) {
                return mid;
            }
            return ceilPow2;
        }

        return MathUtil.alignTo(minBytes, 1 << 24); // multiple of 16 MB
    }

    private final ResourceProvider mResourceProvider;

    private @SharedPtr Buffer mSmallBuffer;
    private long mSmallBufferOffset;

    private final ObjectArrayList<@SharedPtr Buffer> mUsedBuffers = new ObjectArrayList<>();

    public UploadBufferManager(ResourceProvider resourceProvider) {
        mResourceProvider = resourceProvider;
        //TODO take account of Vulkan's optimalBufferCopyOffsetAlignment and
        // optimalBufferCopyRowPitchAlignment
    }

    /**
     * Allocate a staging buffer for uploading, return mapped pointer.
     *
     * @param outInfo buffer bind info
     * @return write-only address, or NULL
     */
    public long getUploadPointer(long requiredBytes,
                                 long requiredAlignment,
                                 BufferViewInfo outInfo) {
        if (requiredBytes <= 0) {
            outInfo.set(null);
            return NULL;
        }

        // transfer buffer requires 4-byte aligned
        requiredAlignment = Math.max(requiredAlignment, 4);
        requiredBytes = MathUtil.alignTo(requiredBytes, requiredAlignment);

        if (requiredBytes >= SMALL_BUFFER_SIZE) {
            // Create a dedicated buffer for this request.
            @SharedPtr
            Buffer buffer = mResourceProvider.findOrCreateBuffer(getLargeBufferSize(requiredBytes),
                    Engine.BufferUsageFlags.kUpload | Engine.BufferUsageFlags.kHostVisible,
                    "UploadBuffer");

            long mappedPtr = buffer != null ? buffer.map() : NULL;
            if (mappedPtr == NULL) {
                RefCnt.move(buffer);
                outInfo.set(null);
                return NULL;
            }

            outInfo.mBuffer = buffer;
            outInfo.mOffset = 0;
            outInfo.mSize = requiredBytes;

            mUsedBuffers.add(buffer); // transfer ownership
            return mappedPtr;
        }

        // Try to reuse an already-allocated buffer.
        mSmallBufferOffset = MathUtil.alignTo(mSmallBufferOffset, requiredAlignment);
        if (mSmallBuffer != null && requiredBytes > mSmallBuffer.getSize() - mSmallBufferOffset) {
            mUsedBuffers.add(mSmallBuffer);
            mSmallBuffer = null;
        }

        if (mSmallBuffer == null) {
            mSmallBuffer = mResourceProvider.findOrCreateBuffer(SMALL_BUFFER_SIZE,
                    Engine.BufferUsageFlags.kUpload | Engine.BufferUsageFlags.kHostVisible,
                    "SmallUploadBuffer");
            mSmallBufferOffset = 0;

            if (mSmallBuffer == null || mSmallBuffer.map() == NULL) {
                mSmallBuffer = RefCnt.move(mSmallBuffer);
                outInfo.set(null);
                return NULL;
            }
        }

        long mappedPtr = mSmallBuffer.getMappedBuffer();
        assert mappedPtr != NULL;
        mappedPtr += mSmallBufferOffset;

        outInfo.mBuffer = mSmallBuffer;
        outInfo.mOffset = mSmallBufferOffset;
        outInfo.mSize = requiredBytes;

        mSmallBufferOffset += requiredBytes;

        return mappedPtr;
    }

    /**
     * Finalizes all buffers and transfers ownership of them to given list.
     *
     * @param outResourceRefs receive ownership of resources
     */
    public void flush(List<@SharedPtr Resource> outResourceRefs) {
        for (var buffer : mUsedBuffers) {
            assert buffer.isMapped();
            buffer.unmap();
        }
        // move all
        outResourceRefs.addAll(mUsedBuffers);
        mUsedBuffers.clear();

        mSmallBufferOffset = 0;
        if (mSmallBuffer != null) {
            assert mSmallBuffer.isMapped();
            mSmallBuffer.unmap();
            outResourceRefs.add(mSmallBuffer);
            mSmallBuffer = null;
        }
    }
}
