/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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
import icyllis.arc3d.engine.task.TaskList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Used by {@link SurfaceDevice}
 */
public final class SurfaceDrawContext implements AutoCloseable {

    private final ImageInfo mImageInfo;

    @SharedPtr
    private final ImageViewProxy mReadView;
    private final short mWriteSwizzle;

    private TaskList mDrawTaskList;

    private Matrix4 mLastTransform; // for deduplication
    private final ObjectArrayList<Draw> mPendingDraws = new ObjectArrayList<>();
    private int mNumSteps;

    // Load and store information for the current pending draws.
    private byte mPendingLoadOp = Engine.LoadOp.kLoad;
    private byte mPendingStoreOp = Engine.StoreOp.kStore;
    private final float[] mPendingClearColor = new float[4];

    private SurfaceDrawContext(@SharedPtr ImageViewProxy readView,
                               short writeSwizzle,
                               ImageInfo imageInfo) {
        mReadView = readView;
        mWriteSwizzle = writeSwizzle;
        mImageInfo = imageInfo;
        mDrawTaskList = new TaskList();
    }

    //TODO currently we don't handle MSAA
    @Nullable
    public static SurfaceDrawContext make(
            RecordingContext context,
            @SharedPtr ImageViewProxy targetView,
            ImageInfo deviceInfo) {
        if (targetView == null) {
            return null;
        }
        if (context == null || context.isDiscarded()) {
            targetView.unref();
            return null;
        }
        if (deviceInfo.alphaType() != ColorInfo.AT_OPAQUE &&
                deviceInfo.alphaType() != ColorInfo.AT_PREMUL) {
            // we only render to premultiplied alpha type
            targetView.unref();
            return null;
        }
        if (!targetView.getDesc().isRenderable()) {
            targetView.unref();
            return null;
        }

        // Accept an approximate-fit surface, but make sure it's at least as large as the device's
        // logical size.
        // TODO: validate that the color type and alpha type are compatible with the target's info
        assert targetView.getWidth() >= deviceInfo.width() &&
                targetView.getHeight() >= deviceInfo.height();

        short writeSwizzle = context.getCaps().getWriteSwizzle(
                targetView.getDesc(), deviceInfo.colorType());

        return new SurfaceDrawContext(
                targetView,
                writeSwizzle,
                deviceInfo);
    }

    /**
     * @return raw ptr to the read view
     */
    @RawPtr
    public ImageViewProxy getReadView() {
        return mReadView;
    }

    public ImageInfo getImageInfo() {
        return mImageInfo;
    }

    /**
     * @see ColorInfo.ColorType
     */
    public int getColorType() {
        return mImageInfo.colorType();
    }

    /**
     * @see ColorInfo.AlphaType
     */
    public int getAlphaType() {
        return mImageInfo.alphaType();
    }

    public int getWidth() {
        return mReadView.getWidth();
    }

    public int getHeight() {
        return mReadView.getHeight();
    }

    public boolean isMipmapped() {
        return mReadView.isMipmapped();
    }

    /**
     * Read view and write view have the same origin.
     *
     * @see Engine.SurfaceOrigin
     */
    public int getOrigin() {
        return mReadView.getOrigin();
    }

    /**
     * @see Swizzle
     */
    public short getReadSwizzle() {
        return mReadView.getSwizzle();
    }

    /**
     * @param clearColor premultiplied color, null means (0,0,0,0)
     */
    public void clear(float[] clearColor) {
        discard();

        mPendingLoadOp = Engine.LoadOp.kClear;
        if (clearColor != null) {
            System.arraycopy(clearColor, 0, mPendingClearColor, 0, 4);
        } else {
            Arrays.fill(mPendingClearColor, 0.0f);
        }
    }

    public void discard() {

        mPendingDraws.forEach(Draw::close);
        mPendingDraws.clear();
        mNumSteps = 0;

        mPendingLoadOp = Engine.LoadOp.kDiscard;
    }

    public int pendingNumSteps() {
        return mNumSteps;
    }

    public void recordDraw(Draw draw) {
        assert !draw.mDrawBounds.isEmpty();
        assert !draw.mScissorRect.isEmpty() &&
                draw.mScissorRect.left() >= 0 &&
                draw.mScissorRect.top() >= 0 &&
                draw.mScissorRect.right() <= mImageInfo.width() &&
                draw.mScissorRect.bottom() <= mImageInfo.height();
        //TODO add stencil validation?

        draw.mTransform = getStableTransform(draw.mTransform);

        mPendingDraws.add(draw);
        mNumSteps += draw.mRenderer.numSteps();
    }

    public void recordDependency(@SharedPtr Task task) {
        mDrawTaskList.appendTask(task);
    }

    public void flush(RecordingContext context) {

        assert mPendingDraws.isEmpty() == (mNumSteps == 0);
        if (mPendingDraws.isEmpty() && mPendingLoadOp != Engine.LoadOp.kClear) {
            return;
        }

        DrawPass pass = DrawPass.make(
                context,
                mPendingDraws,
                mNumSteps,
                mReadView,
                mImageInfo);
        mPendingDraws.forEach(Draw::close);
        mPendingDraws.clear();
        mNumSteps = 0;

        if (pass != null) {

            var renderPassDesc = new RenderPassDesc();
            renderPassDesc.mNumColorAttachments = 1;
            var colorDesc = renderPassDesc.mColorAttachments[0] = new RenderPassDesc.ColorAttachmentDesc();
            colorDesc.mDesc = mReadView.getDesc();
            colorDesc.mLoadOp = mPendingLoadOp;
            colorDesc.mStoreOp = mPendingStoreOp;
            //TODO MSAA and depth stencil attachment

            RenderPassTask renderPassTask = RenderPassTask.make(
                    pass,
                    renderPassDesc,
                    RefCnt.create(mReadView),
                    null,
                    mPendingClearColor
            );
            mDrawTaskList.appendTask(renderPassTask);
        }

        // Now that there is content drawn to the target, that content must be loaded on any subsequent
        // render pass.
        mPendingLoadOp = Engine.LoadOp.kLoad;
        mPendingStoreOp = Engine.StoreOp.kStore;
    }

    @Nullable
    @SharedPtr
    public DrawTask snap(RecordingContext context) {
        flush(context);

        if (mDrawTaskList.isEmpty()) {
            return null;
        }

        DrawTask task = new DrawTask(RefCnt.create(mReadView), mDrawTaskList);
        mDrawTaskList = new TaskList();
        return task;
    }

    /**
     * Destructs this context.
     */
    @Override
    public void close() {
        mReadView.unref();
        mDrawTaskList.close();
        mPendingDraws.forEach(Draw::close);
        mPendingDraws.clear();
    }

    private Matrix4 getStableTransform(Matrix4 transform) {
        Matrix4 last = mLastTransform;
        if (!transform.equals(last)) {
            var copy = transform.clone();
            mLastTransform = copy;
            return copy;
        }
        return last;
    }
}
