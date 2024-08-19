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

package icyllis.arc3d.engine.task;

import icyllis.arc3d.core.RefCnt;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.*;

public class CopyBufferTask extends Task {

    @SharedPtr
    private Buffer mSrcBuffer;
    @SharedPtr
    private Buffer mDstBuffer;
    private final long mSrcOffset;
    private final long mDstOffset;
    private final long mSize;

    CopyBufferTask(@SharedPtr Buffer srcBuffer, @SharedPtr Buffer dstBuffer,
                   long srcOffset, long dstOffset, long size) {
        mSrcBuffer = srcBuffer;
        mDstBuffer = dstBuffer;
        mSrcOffset = srcOffset;
        mDstOffset = dstOffset;
        mSize = size;
    }

    @SharedPtr
    public static CopyBufferTask make(@SharedPtr Buffer srcBuffer, @SharedPtr Buffer dstBuffer,
                                      long srcOffset, long dstOffset, long size) {
        assert (srcBuffer != null);
        assert (size <= srcBuffer.getSize() - srcOffset);
        assert (dstBuffer != null);
        assert (size <= dstBuffer.getSize() - dstOffset);
        return new CopyBufferTask(srcBuffer, dstBuffer, srcOffset, dstOffset, size);
    }

    @Override
    protected void deallocate() {
        mSrcBuffer = RefCnt.move(mSrcBuffer);
        mDstBuffer = RefCnt.move(mDstBuffer);
    }

    @Override
    public int prepare(RecordingContext context) {
        return RESULT_SUCCESS;
    }

    @Override
    public int execute(ImmediateContext context, CommandBuffer commandBuffer) {
        if (commandBuffer.copyBuffer(mSrcBuffer, mDstBuffer,
                mSrcOffset, mDstOffset, mSize)) {
            commandBuffer.trackResource(RefCnt.create(mSrcBuffer));
            commandBuffer.trackResource(RefCnt.create(mDstBuffer));
            return RESULT_SUCCESS;
        }
        return RESULT_FAILURE;
    }
}
