/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.engine.*;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class MeshDrawWriter {

    private StreamBufferManager mStreamBufferManager;
    private DrawCommandList mCommandList;

    // Pipeline state matching currently bound pipeline
    private int fVertexStride;
    private int fInstanceStride;

    private final BufferViewInfo mVertexBufferInfo = new BufferViewInfo();
    private final BufferViewInfo mIndexBufferInfo =new BufferViewInfo();
    private final BufferViewInfo mInstanceBufferInfo =new BufferViewInfo();
    private int mTemplateCount;

    private int fPendingCount; // # of vertices or instances (depending on mode) to be drawn
    private int fPendingBase; // vertex/instance offset (depending on mode) applied to buffer
    private boolean fPendingBufferBinds; // true if {fVertices,fIndices,fInstances} has changed since last draw

    public MeshDrawWriter() {
    }

    public void newPipelineState(int vertexStride,
                                 int instanceStride) {
        flush();
        fVertexStride = vertexStride;
        fInstanceStride = instanceStride;

        // NOTE: resetting pending base is sufficient to redo bindings for vertex/instance data that
        // is later appended but doesn't invalidate bindings for fixed buffers that might not need
        // to change between pipelines.
        fPendingBase = 0;
        assert (fPendingCount == 0);
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
            if (fPendingCount > 0) {
                flush();
            }

            boolean willAppendVertices = templateCount == 0;
            boolean isAppendingVertices = mTemplateCount == 0;
            if (willAppendVertices != isAppendingVertices ||
                    (isAppendingVertices && vertexChange) ||
                    (!isAppendingVertices && instanceChange)) {
                // The buffer binding target for appended data is changing, so reset the base offset
                fPendingBase = 0;
            }

            mVertexBufferInfo.set(vertexBufferInfo);
            mIndexBufferInfo.set(indexBufferInfo);
            mInstanceBufferInfo.set(instanceBufferInfo);

            mTemplateCount = templateCount;

            fPendingBufferBinds = true;
        } else if ((templateCount >= 0 && templateCount != mTemplateCount) || // vtx or reg. instances
                (templateCount < 0 && mTemplateCount >= 0)) {              // dynamic index instances
            if (fPendingCount > 0) {
                flush();
            }
            if ((templateCount == 0) != (mTemplateCount == 0)) {
                // Switching from appending vertices to instances, or vice versa, so the pending
                // base vertex for appended data is invalid
                fPendingBase = 0;
            }
            mTemplateCount = templateCount;
        }


    }

    public void flush() {
        // If nothing was appended, or the only appended data was through dynamic instances and the
        // final vertex count per instance is 0 (-1 in the sign encoded field), nothing should be drawn.
        if (fPendingCount == 0 || mTemplateCount == -1) {
            return;
        }
        if (fPendingBufferBinds) {
            mCommandList.bindBuffers(mVertexBufferInfo,
                    mInstanceBufferInfo,
                    mIndexBufferInfo,
                    Engine.IndexType.kUShort);
            fPendingBufferBinds = false;
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
                        fPendingCount, fPendingBase, 0);
            } else {
                mCommandList.drawInstanced(fPendingCount, fPendingBase,
                        realVertexCount, 0);
            }
        } else {
            assert !mInstanceBufferInfo.isValid();
            if (mIndexBufferInfo.isValid()) {
                mCommandList.drawIndexed(fPendingCount, 0, fPendingBase);
            } else {
                mCommandList.draw(fPendingCount, fPendingBase);
            }
        }

        fPendingBase += fPendingCount;
        fPendingCount = 0;
    }

    private BufferViewInfo mCurrentTarget = null;
    private int mCurrentStride;

    private int mReservedCount;

    private ByteBuffer mCurrentWriter;
    private final BufferViewInfo mTempAllocInfo = new BufferViewInfo();

    public void beginVertices() {
        assert mCurrentTarget == null;
        assert fVertexStride > 0;
        setTemplate(mVertexBufferInfo, null, null, 0);
        mCurrentTarget = mVertexBufferInfo;
        mCurrentStride = fVertexStride;
    }

    /**
     * Start writing instance data and bind static vertex buffer and index buffer.
     * @param vertexBufferInfo
     * @param indexBufferInfo
     * @param vertexCount
     */
    public void beginInstances(@Nullable BufferViewInfo vertexBufferInfo,
                               @Nullable BufferViewInfo indexBufferInfo,
                               int vertexCount) {
        assert vertexCount > 0;
        assert mCurrentTarget == null;
        assert fInstanceStride > 0;
        setTemplate(vertexBufferInfo, indexBufferInfo,
                mInstanceBufferInfo, vertexCount);
        mCurrentTarget = mInstanceBufferInfo;
        mCurrentStride = fInstanceStride;
    }

    public void reserve(int count) {
        if (mReservedCount >= count) {
            return;
        }
        assert mCurrentTarget != null;
        if (mReservedCount > 0) {
            // Have contiguous bytes that can't satisfy request, so return them in the event the
            // DBM has additional contiguous bytes after the prior reserved range.
            mStreamBufferManager.putBackVertexBytes(mReservedCount * mCurrentStride);
        }

        mReservedCount = count;
        // NOTE: Cannot bind tuple directly to fNextWriter, compilers don't produce the right
        // move assignment.
        var writer = mStreamBufferManager.getVertexWriter(count * mCurrentStride,
                mTempAllocInfo);
        if (mTempAllocInfo.mBuffer != mCurrentTarget.mBuffer ||
                mTempAllocInfo.mOffset !=
                        (mCurrentTarget.mOffset + (long) (fPendingBase + fPendingCount) * mCurrentStride)) {
            // Not contiguous, so flush and update binding to 'reservedChunk'
            flush();
            mCurrentTarget.set(mTempAllocInfo);
            fPendingBase = 0;
            fPendingBufferBinds = true;
        }
        mCurrentWriter = writer;
    }

    /**
     * The caller must write <code>count * stride</code> bytes to the pointer.
     *
     * @param count vertex count or instance count
     */
    public ByteBuffer append(int count) {
        assert count > 0;
        reserve(count);

        assert (mReservedCount >= count);
        mReservedCount -= count;
        fPendingCount += count;
        return mCurrentWriter;
    }

    public void endAppender() {
        assert mCurrentTarget != null;
        if (mReservedCount > 0) {
            mStreamBufferManager.putBackVertexBytes(mReservedCount * mCurrentStride);
        }
        mCurrentTarget = null;
        mCurrentWriter = null;
    }
}
