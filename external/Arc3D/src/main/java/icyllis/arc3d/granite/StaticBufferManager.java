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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.task.CopyBufferTask;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import static org.lwjgl.system.MemoryUtil.NULL;

public class StaticBufferManager {

    private static class CopyData {
        BufferViewInfo mSource;
        BufferViewInfo mTarget;

        CopyData(BufferViewInfo source, BufferViewInfo target) {
            mSource = source;
            mTarget = target;
        }
    }

    private static class BlockBuffer {

        final int mUsage;
        final int mOffsetAlignment; // must be power of two

        final ObjectArrayList<CopyData> mData = new ObjectArrayList<>();
        long mTotalRequiredBytes;

        BlockBuffer(Caps caps, int usage) {
            mUsage = usage;
            if ((usage & Engine.BufferUsageFlags.kUniform) != 0) {
                // alignment is 256 at most, this is 256 on NVIDIA GPU
                // and smaller on integrated GPUs
                mOffsetAlignment = caps.minUniformBufferOffsetAlignment();
            } else {
                mOffsetAlignment = VertexInputLayout.Attribute.OFFSET_ALIGNMENT;
            }
        }

        boolean allocateAndSetBindings(
                ResourceProvider resourceProvider,
                QueueManager queueManager,
                SharedResourceCache sharedResourceCache,
                String label
        ) {
            if (mTotalRequiredBytes == 0) {
                return true;
            }

            @SharedPtr
            Buffer buffer = resourceProvider.findOrCreateBuffer(
                    mTotalRequiredBytes, mUsage, label
            );
            if (buffer == null) {
                return false;
            }

            long offset = 0;
            for (var data : mData) {
                // Each copy range's size should be aligned to the max of the required buffer alignment and
                // the transfer alignment, so we can just increment the offset into the static buffer.
                assert (offset % mOffsetAlignment == 0);
                data.mTarget.mBuffer = buffer;
                data.mTarget.mOffset = offset;

                assert data.mSource.mSize == data.mTarget.mSize;
                var copyTask = CopyBufferTask.make(
                        RefCnt.create(data.mSource.mBuffer), RefCnt.create(data.mTarget.mBuffer),
                        data.mSource.mOffset, data.mTarget.mOffset,
                        data.mSource.mSize);
                if (!queueManager.addTask(copyTask)) {
                    copyTask.unref();
                    buffer.unref();
                    return false;
                }
                copyTask.unref();

                offset += data.mSource.mSize;
            }

            assert offset == mTotalRequiredBytes;
            sharedResourceCache.addStaticResource(buffer); // move
            return true;
        }
    }

    private ResourceProvider mResourceProvider;

    private UploadBufferManager mUploadManager;

    private BlockBuffer mVertexBuffer;
    private BlockBuffer mIndexBuffer;

    private boolean mMappingFailed = false;

    public StaticBufferManager(ResourceProvider resourceProvider,
                               Caps caps) {
        mResourceProvider = resourceProvider;
        mUploadManager = new UploadBufferManager(resourceProvider);
        mVertexBuffer = new BlockBuffer(caps, Engine.BufferUsageFlags.kVertex | Engine.BufferUsageFlags.kDeviceLocal);
        mIndexBuffer = new BlockBuffer(caps, Engine.BufferUsageFlags.kIndex | Engine.BufferUsageFlags.kDeviceLocal);
    }

    /**
     * The passed in {@link BufferViewInfo} is updated when finalize() is later called, to point to the
     * packed, GPU-private buffer at the appropriate offset. The data written to the returned Writer
     * is copied to the private buffer at that offset. 'binding' must live until finalize() returns.
     *
     * @return write-combining buffer address, or NULL
     */
    public long getVertexWriter(long requiredBytes, BufferViewInfo outInfo) {
        return prepareUploadBuffer(mVertexBuffer, requiredBytes, outInfo);
    }

    public long getIndexWriter(long requiredBytes, BufferViewInfo outInfo) {
        return prepareUploadBuffer(mIndexBuffer, requiredBytes, outInfo);
    }

    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAILURE = 1;
    public static final int RESULT_NO_WORK = 2;

    public int flush(QueueManager queueManager,
                     SharedResourceCache sharedResourceCache) {
        if (mMappingFailed) {
            return RESULT_FAILURE;
        }

        long totalRequiredBytes = mVertexBuffer.mTotalRequiredBytes +
                mIndexBuffer.mTotalRequiredBytes;
        if (totalRequiredBytes == 0) {
            return RESULT_NO_WORK;
        }

        if (!mVertexBuffer.allocateAndSetBindings(mResourceProvider,
                queueManager,
                sharedResourceCache,
                "StaticVertexBuffer")) {
            return RESULT_FAILURE;
        }
        if (!mIndexBuffer.allocateAndSetBindings(mResourceProvider,
                queueManager,
                sharedResourceCache,
                "StaticIndexBuffer")) {
            return RESULT_FAILURE;
        }
        // copy tasks hold refs, no need to own them anymore, just flush and make a clean exit
        ObjectArrayList<@SharedPtr Resource> resourceRefs = new ObjectArrayList<>();
        mUploadManager.flush(resourceRefs);
        resourceRefs.forEach(Resource::unref);
        resourceRefs.clear();

        return RESULT_SUCCESS;
    }

    private long prepareUploadBuffer(BlockBuffer target,
                                     long requiredBytes,
                                     BufferViewInfo outInfo) {
        assert outInfo != null;
        outInfo.mBuffer = null;
        outInfo.mOffset = 0;
        outInfo.mSize = 0;
        if (requiredBytes == 0 || mMappingFailed) {
            return NULL;
        }

        requiredBytes = MathUtil.alignTo(requiredBytes, target.mOffsetAlignment);

        var srcInfo = new BufferViewInfo();
        long mappedPtr = mUploadManager.getUploadPointer(
                requiredBytes,
                4,
                srcInfo
        );
        if (mappedPtr == NULL) {
            mMappingFailed = true;
            return NULL;
        }

        assert requiredBytes == srcInfo.mSize;
        outInfo.mSize = requiredBytes;
        target.mData.add(new CopyData(srcInfo, outInfo));
        target.mTotalRequiredBytes += requiredBytes;
        return mappedPtr;
    }
}
