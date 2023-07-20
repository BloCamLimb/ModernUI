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

package icyllis.arc3d.core;

import icyllis.modernui.graphics.*;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Arc 3D implementation to Canvas.
 */
public abstract class Canvas extends icyllis.modernui.graphics.Canvas {

    /**
     * SaveLayerFlags provides options that may be used in any combination in SaveLayerRec,
     * defining how layer allocated by saveLayer() operates.
     */
    @MagicConstant(flags = {INIT_WITH_PREVIOUS_SAVE_LAYER_FLAG, F16_COLOR_TYPE_SAVE_LAYER_FLAG})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SaveLayerFlag {
    }

    /**
     * Initializes with previous contents.
     */
    public static final int INIT_WITH_PREVIOUS_SAVE_LAYER_FLAG = 1 << 2;

    /**
     * Instead of matching previous layer's color type, use F16.
     */
    public static final int F16_COLOR_TYPE_SAVE_LAYER_FLAG = 1 << 4;

    // getSaveLayerStrategy()'s return value may suppress full layer allocation.
    protected static final int
            FULL_LAYER_SAVE_LAYER_STRATEGY = 0,
            NO_LAYER_SAVE_LAYER_STRATEGY = 1;

    // cache some objects for performance
    private static final int MAX_MC_POOL_SIZE = 32;

    // a cached identity matrix for resetMatrix() call
    private static final Matrix4 IDENTITY_MATRIX = Matrix4.identity();

    // the bottom-most device in the stack, only changed by init(). Image properties and the final
    // canvas pixels are determined by this device
    private final BaseDevice mBaseDevice;

    // keep track of the device clip bounds in the canvas' global space to reject draws before
    // invoking the top-level device.
    private final Rect2f mQuickRejectBounds = new Rect2f();

    // the surface we are associated with, may be null
    Surface mSurface;

    // local MCRec stack
    private MCRec[] mMCStack = new MCRec[MAX_MC_POOL_SIZE];
    // points to top of stack
    private int mMCIndex = 0;

    // value returned by getSaveCount()
    private int mSaveCount;

    // a temp rect that used with arguments
    private final Rect2f mTmpRect = new Rect2f();
    private final Matrix4 mTmpMatrix = new Matrix4();
    private final Paint mTmpPaint = new Paint();

    public Canvas(BaseDevice device) {
        Objects.requireNonNull(device);
        mSaveCount = 1;
        mMCStack[0] = new MCRec(device);
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
        return top().mDevice;
    }

    @Nonnull
    private MCRec push() {
        final int i = ++mMCIndex;
        MCRec[] stack = mMCStack;
        if (i == stack.length) {
            mMCStack = new MCRec[i + (i >> 1)];
            System.arraycopy(stack, 0, mMCStack, 0, i);
            stack = mMCStack;
        }
        MCRec rec = stack[i];
        if (rec == null) {
            stack[i] = rec = new MCRec();
        }
        return rec;
    }

    private void pop() {
        if (mMCIndex-- >= MAX_MC_POOL_SIZE) {
            mMCStack[mMCIndex + 1] = null;
        }
    }

    // points to top of stack
    @Nonnull
    private MCRec top() {
        return mMCStack[mMCIndex];
    }

    private void checkForDeferredSave() {
        if (top().mDeferredSaveCount > 0) {
            doSave();
        }
    }

    /**
     * Returns the depth of saved matrix/clip states on the Canvas' private stack.
     * This will equal save() calls minus restore() calls, and the number of save()
     * calls less the number of restore() calls plus one. The save count of a new
     * canvas is one.
     *
     * @return depth of save state stack
     */
    public int getSaveCount() {
        return mSaveCount;
    }

    /**
     * Saves the current matrix and clip onto a private stack.
     * <p>
     * Subsequent calls to translate, scale, rotate, skew, concat or clipRect,
     * clipPath will all operate as usual, but when the balancing call to
     * restore() is made, those calls will be forgotten, and the settings that
     * existed before the save() will be reinstated.
     * <p>
     * Saved Canvas state is put on a stack; multiple calls to save() should be balance
     * by an equal number of calls to restore(). Call restoreToCount() with the return
     * value of this method to restore this and subsequent saves.
     *
     * @return depth of saved stack to pass to restoreToCount() to balance this call
     */
    public int save() {
        mSaveCount++;
        top().mDeferredSaveCount++;
        return mSaveCount - 1;
    }

    private void doSave() {
        willSave();
        top().mDeferredSaveCount--;
        internalSave();
    }

    /**
     * This call balances a previous call to save(), and is used to remove all
     * modifications to the matrix/clip state since the last save call. The
     * state is removed from the stack. It is an error to call restore() more
     * or less times than save() was called in the final state.
     *
     * @throws IllegalStateException stack underflow
     */
    public final void restore() {
        if (top().mDeferredSaveCount > 0) {
            mSaveCount--;
            top().mDeferredSaveCount--;
        } else {
            // check for underflow
            if (mMCIndex > 0) {
                willRestore();
                mSaveCount--;
                internalRestore();
                didRestore();
            } else {
                throw new IllegalStateException("Stack underflow");
            }
        }
    }

    /**
     * Efficient way to pop any calls to save() that happened after the save
     * count reached saveCount. It is an error for saveCount to be less than 1.
     * <p>
     * Example:
     * <pre>
     * int count = canvas.save();
     * ... // more calls potentially to save()
     * canvas.restoreToCount(count);
     * // now the canvas is back in the same state it
     * // was before the initial call to save().
     * </pre>
     *
     * @param saveCount the depth of state stack to restore
     * @throws IllegalStateException stack underflow (i.e. saveCount is less than 1)
     */
    public final void restoreToCount(int saveCount) {
        if (saveCount < 1) {
            throw new IllegalStateException("Stack underflow");
        }
        int n = getSaveCount() - saveCount;
        for (int i = 0; i < n; ++i) {
            restore();
        }
    }

    private void internalSave() {
        // get before push stack
        MCRec rec = top();
        push().set(rec);

        topDevice().save();
    }

    private void internalRestore() {
        // now do the normal restore()
        pop();

        if (mMCIndex == -1) {
            // this was the last record, restored during the destruction of the Canvas
            return;
        }

        topDevice().restore(top().mMatrix);

        // Update the quick-reject bounds in case the restore changed the top device or the
        // removed save record had included modifications to the clip stack.
        computeQuickRejectBounds();
    }

    protected void willSave() {
    }

    protected void willRestore() {
    }

    protected void didRestore() {
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom) {
        return false;
    }

    @Override
    public void drawColor(int color, BlendMode mode) {
        // paint may be modified for recording canvas, so not impl in super class
        Paint paint = Paint.obtain();
        paint.setColor(color);
        paint.setBlendMode(mode);
        drawPaint(paint);
        paint.recycle();
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
     * Compute the clip's bounds based on all clipped Device's reported device bounds transformed
     * into the canvas' global space.
     */
    private void computeQuickRejectBounds() {
        BaseDevice device = topDevice();
        if (device.getClipType() == BaseDevice.CLIP_TYPE_EMPTY) {
            mQuickRejectBounds.setEmpty();
        } else {
            mQuickRejectBounds.set(device.getClipBounds());
            //FIXME
            //device.deviceToGlobal().mapRect(mQuickRejectBounds);
            // Expand bounds out by 1 in case we are anti-aliasing.  We store the
            // bounds as floats to enable a faster quick reject implementation.
            mQuickRejectBounds.inset(-1.0f, -1.0f);
        }
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
