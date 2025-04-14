/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.task;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.RecordingContext;
import org.jspecify.annotations.NonNull;

public class CopyImageTask extends Task {

    @SharedPtr
    private ImageViewProxy mSrcProxy;
    private final int mSrcL, mSrcT, mSrcR, mSrcB;
    @SharedPtr
    private ImageViewProxy mDstProxy;
    private final int mDstX, mDstY;
    private final int mDstLevel;

    CopyImageTask(@SharedPtr ImageViewProxy srcProxy,
                  int srcL, int srcT, int srcR, int srcB,
                  @SharedPtr ImageViewProxy dstProxy,
                  int dstX, int dstY, int dstLevel) {
        mSrcProxy = srcProxy;
        mSrcL = srcL;
        mSrcT = srcT;
        mSrcR = srcR;
        mSrcB = srcB;
        mDstProxy = dstProxy;
        mDstX = dstX;
        mDstY = dstY;
        mDstLevel = dstLevel;
    }

    @SharedPtr
    public static CopyImageTask make(@SharedPtr ImageViewProxy srcProxy,
                                     @NonNull Rect2ic subset,
                                     @SharedPtr ImageViewProxy dstProxy,
                                     int dstX, int dstY, int dstLevel) {
        if (srcProxy == null || dstProxy == null) {
            RefCnt.move(srcProxy);
            RefCnt.move(dstProxy);
            return null;
        }
        return new CopyImageTask(srcProxy,
                subset.left(), subset.top(), subset.right(), subset.bottom(),
                dstProxy, dstX, dstY, dstLevel);
    }

    @Override
    protected void deallocate() {
        mSrcProxy = RefCnt.move(mSrcProxy);
        mDstProxy = RefCnt.move(mDstProxy);
    }

    @Override
    public int prepare(RecordingContext context) {
        //TODO
        if (!mDstProxy.instantiateIfNonLazy(context.getResourceProvider())) {
            return RESULT_FAILURE;
        }
        return RESULT_SUCCESS;
    }

    @Override
    public int execute(ImmediateContext context, CommandBuffer commandBuffer) {
        assert mSrcProxy.isInstantiated();
        if (commandBuffer.copyImage(
                mSrcProxy.getImage(),
                mSrcL, mSrcT, mSrcR, mSrcB,
                mDstProxy.getImage(),
                mDstX, mDstY,
                mDstLevel
        )) {
            commandBuffer.trackCommandBufferResource(mSrcProxy.refImage());
            commandBuffer.trackCommandBufferResource(mDstProxy.refImage());
            return RESULT_SUCCESS;
        }
        return RESULT_FAILURE;
    }
}
