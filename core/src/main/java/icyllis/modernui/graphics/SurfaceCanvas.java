/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.modernui.math.Matrix4;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Direct implementation to Canvas.
 */
public abstract class SurfaceCanvas extends Canvas {

    // local MCRec stack
    private final Deque<MCRec> mMCStack = new ArrayDeque<>(16);
    private final Pool<MCRec> mMCRecPool = Pools.simple(32);

    private final BaseDevice mBaseDevice;

    private int mSaveCount;

    public SurfaceCanvas(BaseDevice device) {
        mSaveCount = 1;
        mMCStack.push(new MCRec(device));

        mBaseDevice = device;
    }

    // the bottom-most device in the stack, only changed by init(). Image properties and the final
    // canvas pixels are determined by this device.
    @Nonnull
    private BaseDevice baseDevice() {
        return mBaseDevice;
    }

    // the top-most device in the stack, will change within saveLayer()'s. All drawing and clipping
    // operations should route to this device.
    @Nonnull
    private BaseDevice topDevice() {
        return getMCRec().mDevice;
    }

    // points to top of stack
    @Nonnull
    private MCRec getMCRec() {
        return mMCStack.getFirst();
    }

    /**
     * @inheritDoc
     */
    @Override
    public int save() {
        mSaveCount++;
        getMCRec().mDeferredSaveCount++;
        return mSaveCount - 1;
    }

    private void doSave() {
        willSave();
        getMCRec().mDeferredSaveCount--;
        internalSave();
    }

    private void checkForDeferredSave() {
        if (getMCRec().mDeferredSaveCount > 0) {
            doSave();
        }
    }

    protected void willSave() {
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getSaveCount() {
        return mSaveCount;
    }

    private void internalSave() {
        MCRec next = mMCRecPool.acquire();
        if (next == null) {
            next = new MCRec();
        }
        next.set(getMCRec());
        mMCStack.addFirst(next);
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom) {
        return false;
    }

    @Override
    public void drawColor(int color, BlendMode mode) {
        // paint may be modified for recording canvas, so not impl in super class
        Paint paint = Paint.take();
        paint.setColor(color);
        paint.setBlendMode(mode);
        drawPaint(paint);
        paint.drop();
    }

    @Override
    public void drawPaint(Paint paint) {
        // drawPaint does not call internalQuickReject() because computing its geometry is not free
        // (see getLocalClipBounds()), and the two conditions below are sufficient.
        if (paint.nothingToDraw() || isClipEmpty()) {
            return;
        }
        topDevice().drawPaint(paint);
    }

    @Override
    public void drawPoint(float x, float y, Paint paint) {

    }

    @Override
    public boolean isClipEmpty() {
        return topDevice().getClipType() == BaseDevice.CLIP_TYPE_EMPTY;
    }

    @Override
    public boolean isClipRect() {
        return topDevice().getClipType() == BaseDevice.CLIP_TYPE_RECT;
    }

    /**
     * This is the record we keep for each save/restore level in the stack.
     * Since a level optionally copies the matrix and/or stack, we have pointers
     * for these fields. If the value is copied for this level, the copy is
     * stored in the ...Storage field, and the pointer points to that. If the
     * value is not copied for this level, we ignore ...Storage, and just point
     * at the corresponding value in the previous level in the stack.
     */
    private static final class MCRec {

        // This points to the device of the top-most layer (which may be lower in the stack), or
        // to the canvas's fBaseDevice. The MCRec does not own the device.
        BaseDevice mDevice;

        final Matrix4 mMatrix = new Matrix4();
        int mDeferredSaveCount;

        MCRec() {
        }

        MCRec(BaseDevice device) {
            mDevice = device;
            mMatrix.setIdentity();
            mDeferredSaveCount = 0;
        }

        void set(MCRec prev) {
            mDevice = prev.mDevice;
            mMatrix.set(prev.mMatrix);
            mDeferredSaveCount = 0;
        }
    }
}
