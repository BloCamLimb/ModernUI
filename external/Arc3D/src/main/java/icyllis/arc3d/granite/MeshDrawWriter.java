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

import icyllis.arc3d.engine.*;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.system.MemoryUtil.NULL;

public class MeshDrawWriter implements AutoCloseable {

    private final DrawBufferManager mDrawBufferManager;
    private final DrawCommandList mCommandList;

    // Pipeline state matching currently bound pipeline
    private int mVertexBinding;
    private int mInstanceBinding;
    private int mVertexStride;
    private int mInstanceStride;

    private final BufferViewInfo mVertexBufferInfo = new BufferViewInfo();
    private final BufferViewInfo mIndexBufferInfo = new BufferViewInfo();
    private final BufferViewInfo mInstanceBufferInfo = new BufferViewInfo();
    private int mTemplateCount;

    private int mPendingCount; // # of vertices or instances (depending on mode) to be drawn
    private int mPendingBase; // vertex/instance offset (depending on mode) applied to buffer
    private boolean mPendingBufferBinds; // true if {fVertices,fIndices,fInstances} has changed since last draw

    // storage address for VertexWriter when GPU buffer mapping fails
    private long mFailureStorage = NULL;
    private int mFailureCapacity = 0;

    public MeshDrawWriter(DrawBufferManager drawBufferManager, DrawCommandList commandList) {
        mDrawBufferManager = drawBufferManager;
        mCommandList = commandList;
    }

    @Override
    public void close() {
        if (mFailureStorage != NULL) {
            MemoryUtil.nmemFree(mFailureStorage);
        }
        mFailureStorage = NULL;
    }

    public void newPipelineState(int vertexBinding,
                                 int instanceBinding,
                                 int vertexStride,
                                 int instanceStride) {
        flush();
        mVertexBinding = vertexBinding;
        mInstanceBinding = instanceBinding;
        mVertexStride = vertexStride;
        mInstanceStride = instanceStride;

        // NOTE: resetting pending base is sufficient to redo bindings for vertex/instance data that
        // is later appended but doesn't invalidate bindings for fixed buffers that might not need
        // to change between pipelines.
        mPendingBase = 0;
        assert (mPendingCount == 0);
    }

    public void newDynamicState() {
        flush();
    }

    // A == B && (A == null || C == D)
    // A != B || (A != null && C != D)

    // note if buffer is null, offset must be 0
    void setTemplate(@Nullable BufferViewInfo vertexBufferInfo,
                     @Nullable BufferViewInfo indexBufferInfo,
                     @Nullable BufferViewInfo instanceBufferInfo,
                     int templateCount) {
        boolean vertexChange = !mVertexBufferInfo.equals(vertexBufferInfo);
        boolean instanceChange = !mInstanceBufferInfo.equals(instanceBufferInfo);
        if (vertexChange || instanceChange ||
                !mIndexBufferInfo.equals(indexBufferInfo)) {
            if (mPendingCount > 0) {
                flush();
            }

            boolean willAppendVertices = templateCount == 0;
            boolean isAppendingVertices = mTemplateCount == 0;
            if (willAppendVertices != isAppendingVertices ||
                    (isAppendingVertices && vertexChange) ||
                    (!isAppendingVertices && instanceChange)) {
                // The buffer binding target for appended data is changing, so reset the base offset
                mPendingBase = 0;
            }

            mVertexBufferInfo.set(vertexBufferInfo);
            mIndexBufferInfo.set(indexBufferInfo);
            mInstanceBufferInfo.set(instanceBufferInfo);

            mTemplateCount = templateCount;

            mPendingBufferBinds = true;
        } else if ((templateCount >= 0 && templateCount != mTemplateCount) || // vtx or reg. instances
                (templateCount < 0 && mTemplateCount >= 0)) {              // dynamic index instances
            if (mPendingCount > 0) {
                flush();
            }
            if ((templateCount == 0) != (mTemplateCount == 0)) {
                // Switching from appending vertices to instances, or vice versa, so the pending
                // base vertex for appended data is invalid
                mPendingBase = 0;
            }
            mTemplateCount = templateCount;
        }


    }

    public void flush() {
        // If nothing was appended, or the only appended data was through dynamic instances and the
        // final vertex count per instance is 0 (-1 in the sign encoded field), nothing should be drawn.
        if (mPendingCount == 0 || mTemplateCount == -1) {
            return;
        }
        if (mPendingBufferBinds) {
            if (mIndexBufferInfo.isValid()) {
                mCommandList.bindIndexBuffer(Engine.IndexType.kUShort, mIndexBufferInfo);
            }
            if (mVertexBufferInfo.isValid()) {
                assert mVertexBinding != -1 && mVertexStride > 0;
                mCommandList.bindVertexBuffer(mVertexBinding, mVertexBufferInfo);
            }
            if (mInstanceBufferInfo.isValid()) {
                assert mInstanceBinding != -1 && mInstanceStride > 0;
                mCommandList.bindVertexBuffer(mInstanceBinding, mInstanceBufferInfo);
            }
            mPendingBufferBinds = false;
        }

        if (mTemplateCount != 0) {
            // Instanced drawing
            int realVertexCount;
            if (mTemplateCount < 0) {
                realVertexCount = -mTemplateCount - 1;
                mTemplateCount = -1; // reset to re-accumulate max index account for next flush
            } else {
                realVertexCount = mTemplateCount;
            }

            if (mIndexBufferInfo.isValid()) {
                mCommandList.drawIndexedInstanced(realVertexCount, 0,
                        mPendingCount, mPendingBase, 0);
            } else {
                mCommandList.drawInstanced(mPendingCount, mPendingBase,
                        realVertexCount, 0);
            }
        } else {
            assert !mInstanceBufferInfo.isValid();
            if (mIndexBufferInfo.isValid()) {
                mCommandList.drawIndexed(mPendingCount, 0, mPendingBase);
            } else {
                mCommandList.draw(mPendingCount, mPendingBase);
            }
        }

        mPendingBase += mPendingCount;
        mPendingCount = 0;
    }

    private BufferViewInfo mCurrentTarget = null;
    private int mCurrentStride;

    private int mReservedCount;

    private long mCurrentWriter;
    private final BufferViewInfo mTempAllocInfo = new BufferViewInfo();

    private long getFailureStorage(int size) {
        if (size > mFailureCapacity) {
            long newStorage = MemoryUtil.nmemRealloc(mFailureStorage, size);
            if (newStorage == NULL) {
                throw new OutOfMemoryError();
            }
            mFailureStorage = newStorage;
            mFailureCapacity = size;
            return newStorage;
        }
        return mFailureStorage;
    }

    public void beginVertices() {
        assert mCurrentTarget == null;
        assert mVertexStride > 0;
        setTemplate(mVertexBufferInfo, null, null, 0);
        mCurrentTarget = mVertexBufferInfo;
        mCurrentStride = mVertexStride;
    }

    /**
     * Start writing instance data and bind static vertex buffer and index buffer.
     *
     * @param vertexBufferInfo
     * @param indexBufferInfo
     * @param vertexCount
     */
    public void beginInstances(@Nullable BufferViewInfo vertexBufferInfo,
                               @Nullable BufferViewInfo indexBufferInfo,
                               int vertexCount) {
        assert vertexCount > 0;
        assert mCurrentTarget == null;
        assert mInstanceStride > 0;
        setTemplate(vertexBufferInfo, indexBufferInfo,
                mInstanceBufferInfo, vertexCount);
        mCurrentTarget = mInstanceBufferInfo;
        mCurrentStride = mInstanceStride;
    }

    public void reserve(int count) {
        if (mReservedCount >= count) {
            return;
        }
        assert mCurrentTarget != null;
        if (mReservedCount > 0) {
            // Have contiguous bytes that can't satisfy request, so return them in the event the
            // DBM has additional contiguous bytes after the prior reserved range.
            mDrawBufferManager.putBackVertexBytes(mReservedCount * mCurrentStride);
        }

        mReservedCount = count;
        var writer = mDrawBufferManager.getVertexPointer(count * mCurrentStride,
                mTempAllocInfo);
        if (mTempAllocInfo.mBuffer != mCurrentTarget.mBuffer ||
                mTempAllocInfo.mOffset !=
                        (mCurrentTarget.mOffset + (long) (mPendingBase + mPendingCount) * mCurrentStride)) {
            // Not contiguous, so flush and update binding to 'mTempAllocInfo'
            flush();
            mCurrentTarget.set(mTempAllocInfo);
            mPendingBase = 0;
            mPendingBufferBinds = true;
        }
        mCurrentWriter = writer;
    }

    /**
     * The caller must write <code>count * stride</code> bytes to the pointer.
     *
     * @param count vertex count or instance count
     */
    public long append(int count) {
        assert count > 0;
        reserve(count);

        int size = count * mCurrentStride;
        if (mCurrentWriter == NULL) {
            // If the GPU mapped buffer failed, ensure we have a sufficiently large CPU address to
            // write to so that GeometrySteps don't have to worry about error handling. The Recording
            // will fail since the map failure is tracked by BufferManager.
            return getFailureStorage(size);
        }

        assert (mReservedCount >= count);
        mReservedCount -= count;
        mPendingCount += count;
        var writer = mCurrentWriter;
        mCurrentWriter += size;
        return writer;
    }

    public void endAppender() {
        assert mCurrentTarget != null;
        if (mReservedCount > 0) {
            mDrawBufferManager.putBackVertexBytes(mReservedCount * mCurrentStride);
        }
        mCurrentTarget = null;
        mCurrentWriter = NULL;
    }
}
