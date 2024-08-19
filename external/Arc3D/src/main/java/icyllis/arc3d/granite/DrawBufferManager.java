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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.task.Task;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Manages dynamic and streaming GPU buffers.
 * <p>
 * This prefers to create a large ring buffer that is host visible and device visible.
 * If not available, creates a large staging buffer and a device local buffer.
 * <p>
 * For streaming buffers: use persistent mapped host-visible buffers for Vulkan and
 * OpenGL 4.4; use mutable buffer and CPU staging buffer for OpenGL 4.3 and below.
 */
//TODO
public class DrawBufferManager {

    /**
     * We expect buffers for meshes to be at least 128KB.
     */
    public static final int VERTEX_BUFFER_SIZE = 1 << 17;
    public static final int INDEX_BUFFER_SIZE = 1 << 13;
    public static final int UNIFORM_BUFFER_SIZE = 1 << 13;

    private static class BlockBuffer {

        final int mUsage;
        final int mOffsetAlignment; // must be power of two
        final int mBlockSize; // must be power of two
        @SharedPtr Buffer mBuffer;
        int mOffset;

        BlockBuffer(Caps caps, int usage, int blockSize) {
            mUsage = usage;
            mBlockSize = blockSize;
            if ((usage & Engine.BufferUsageFlags.kUniform) != 0) {
                // alignment is 256 at most, this is 256 on NVIDIA GPU
                // and smaller on integrated GPUs
                mOffsetAlignment = caps.minUniformBufferOffsetAlignment();
            } else {
                mOffsetAlignment = VertexInputLayout.Attribute.OFFSET_ALIGNMENT;
            }
        }
    }

    // @formatter:off
    static final int kVertexBufferIndex     = 0;
    static final int kIndexBufferIndex      = 1;
    static final int kUniformBufferIndex    = 2;
    final BlockBuffer[] mCurrentBuffers = new BlockBuffer[3];
    // @formatter:on

    final ObjectArrayList<@SharedPtr Buffer> mUsedBuffers = new ObjectArrayList<>();

    private final ResourceProvider mResourceProvider;

    // If mapping failed on Buffers created/managed by this DrawBufferManager or by the mapped
    // transfer buffers from the UploadManager, remember so that the next Recording will fail.
    private boolean mMappingFailed = false;

    public DrawBufferManager(Caps caps, ResourceProvider resourceProvider) {
        mResourceProvider = resourceProvider;
        mCurrentBuffers[kVertexBufferIndex] = new BlockBuffer(
                caps,
                Engine.BufferUsageFlags.kVertex | Engine.BufferUsageFlags.kHostVisible,
                VERTEX_BUFFER_SIZE
        );
        mCurrentBuffers[kIndexBufferIndex] = new BlockBuffer(
                caps,
                Engine.BufferUsageFlags.kIndex | Engine.BufferUsageFlags.kHostVisible,
                INDEX_BUFFER_SIZE
        );
        mCurrentBuffers[kUniformBufferIndex] = new BlockBuffer(
                caps,
                Engine.BufferUsageFlags.kUniform | Engine.BufferUsageFlags.kHostVisible,
                UNIFORM_BUFFER_SIZE
        );
    }

    /**
     * Allocate write-combining buffer and return a host coherent memory address.
     * Return pointer is 4-byte aligned, if NULL then allocation is failed.
     *
     * @param requiredBytes vertex stride (4-byte aligned) * count
     * @param outInfo       buffer bind info
     * @return write-only address, or NULL
     */
    public long getVertexPointer(int requiredBytes, BufferViewInfo outInfo) {
        return prepareMappedPointer(
                mCurrentBuffers[kVertexBufferIndex],
                requiredBytes,
                outInfo,
                "DirectVertexBuffer"
        );
    }

    /**
     * Allocate write-combining buffer and return a host coherent memory address.
     * The buffer can be used only for uniform buffer. Return pointer is 4-byte aligned
     * according to GPU requirements, if NULL then allocation is failed.
     *
     * @param requiredBytes uniform buffer size (aligned) * count
     * @param outInfo       buffer bind info
     * @return write-only address, or NULL
     * @see #alignUniformBlockSize(int)
     */
    public long getUniformPointer(int requiredBytes, BufferViewInfo outInfo) {
        return prepareMappedPointer(
                mCurrentBuffers[kUniformBufferIndex],
                requiredBytes,
                outInfo,
                "DirectUniformBuffer"
        );
    }

    public void putBackVertexBytes(int unusedBytes) {

    }

    public int alignUniformBlockSize(int dataSize) {
        return MathUtil.alignTo(dataSize, mCurrentBuffers[kUniformBufferIndex].mOffsetAlignment);
    }

    private long prepareMappedPointer(BlockBuffer target,
                                      int requiredBytes,
                                      BufferViewInfo outInfo,
                                      String label) {
        // write-combining buffer is automatically mapped
        assert (target.mUsage & Engine.BufferUsageFlags.kHostVisible) != 0;
        prepareBuffer(target, requiredBytes, outInfo, label);
        if (outInfo.mBuffer == null) {
            assert mMappingFailed;
            return NULL;
        }
        assert !mMappingFailed;
        assert (outInfo.mBuffer == target.mBuffer);
        assert (outInfo.mBuffer.isMapped());
        long mappedPointer = outInfo.mBuffer.getMappedBuffer();
        assert mappedPointer != NULL;

        return mappedPointer + outInfo.mOffset;
    }

    private void prepareBuffer(BlockBuffer target,
                               int requiredBytes,
                               BufferViewInfo outInfo,
                               String label) {
        assert requiredBytes > 0;

        if (mMappingFailed) {
            outInfo.set(null);
            return;
        }

        int startOffset = -1;
        if (target.mBuffer != null) {
            startOffset = MathUtil.alignTo(target.mOffset, target.mOffsetAlignment);
            // capture overflow
            if (startOffset < 0 ||
                    startOffset > target.mBuffer.getSize() - requiredBytes) {
                mUsedBuffers.add(target.mBuffer);
                target.mBuffer = null;
                startOffset = -1;
            }
        }

        if (target.mBuffer == null) {
            long bufferSize = Math.min(
                    MathUtil.alignTo(requiredBytes, target.mBlockSize),
                    Integer.MAX_VALUE
            );
            target.mBuffer = mResourceProvider.findOrCreateBuffer(bufferSize,
                    target.mUsage, label);
            target.mOffset = 0;
            if (target.mBuffer == null) {
                setMappingFailed();
                outInfo.set(null);
                return;
            }
            if ((target.mUsage & Engine.BufferUsageFlags.kHostVisible) != 0) {
                long mappedPtr = target.mBuffer.map();
                if (mappedPtr == NULL) {
                    // Mapping a direct draw buffer failed
                    setMappingFailed();
                    outInfo.set(null);
                    return;
                }
            }
        }

        if (startOffset == -1) {
            startOffset = MathUtil.alignTo(target.mOffset, target.mOffsetAlignment);
        }
        outInfo.mBuffer = target.mBuffer;
        outInfo.mOffset = startOffset;
        outInfo.mSize = requiredBytes;
        target.mOffset = startOffset + requiredBytes;
    }

    private void setMappingFailed() {
        mMappingFailed = true;

        for (var buffer : mUsedBuffers) {
            if (buffer.isMapped()) {
                // no need to flush any data
                buffer.unmap(0);
            }
            buffer.unref();
        }
        mUsedBuffers.clear();

        for (var target : mCurrentBuffers) {
            target.mOffset = 0;
            if (target.mBuffer != null) {
                if (target.mBuffer.isMapped()) {
                    // no need to flush any data
                    target.mBuffer.unmap(0);
                }
                target.mBuffer = RefCnt.move(target.mBuffer);
            }
        }
    }

    public boolean hasMappingFailed() {
        return mMappingFailed;
    }

    /**
     * Finalizes all buffers and transfers ownership of them to given list.
     * Should not call if hasMappingFailed() returns true.
     *
     * @param outTasks        receive tasks
     * @param outResourceRefs receive ownership of resources
     */
    public void flush(Consumer<@SharedPtr Task> outTasks,
                      List<@SharedPtr Resource> outResourceRefs) {
        assert !mMappingFailed;

        for (var buffer : mUsedBuffers) {
            if (buffer.isMapped()) {
                buffer.unmap();
            }
        }
        // move all
        outResourceRefs.addAll(mUsedBuffers);
        mUsedBuffers.clear();

        for (var target : mCurrentBuffers) {
            target.mOffset = 0;
            if (target.mBuffer != null) {
                if (target.mBuffer.isMapped()) {
                    target.mBuffer.unmap();
                }
                outResourceRefs.add(target.mBuffer);
                target.mBuffer = null;
            }
        }
    }
}
