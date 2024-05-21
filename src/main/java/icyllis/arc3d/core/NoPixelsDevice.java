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

package icyllis.arc3d.core;

import javax.annotation.Nonnull;

/**
 * The NoPixelsDevice draws nothing, but tracks device's clip.
 * Used for deferred rendering.
 */
public class NoPixelsDevice extends Device {

    // cache some objects and their references for performance
    private static final int CLIP_POOL_SIZE = 16;

    private ClipState[] mClipStack = new ClipState[CLIP_POOL_SIZE];
    private int mClipIndex = 0;

    private final Rect2i mTmpBounds = new Rect2i();

    public NoPixelsDevice(@Nonnull Rect2i bounds) {
        this(bounds.mLeft, bounds.mTop, bounds.mRight, bounds.mBottom);
    }

    public NoPixelsDevice(int left, int top, int right, int bottom) {
        super(new ImageInfo(right - left, bottom - top));
        setOrigin(null, left, top);
        ClipState state = new ClipState();
        state.setRect(mBounds);
        mClipStack[0] = state;
    }

    public final void resetForNextPicture(int left, int top, int right, int bottom) {
        resize(right - left, bottom - top);
        setOrigin(null, left, top);
        for (int i = mClipIndex; i > 0; i--) {
            pop();
        }
        ClipState state = mClipStack[0];
        state.setRect(mBounds);
        state.mDeferredSaveCount = 0;
    }

    @Nonnull
    private ClipState push() {
        final int i = ++mClipIndex;
        ClipState[] stack = mClipStack;
        if (i == stack.length) {
            mClipStack = new ClipState[i + (i >> 1)];
            System.arraycopy(stack, 0, mClipStack, 0, i);
            stack = mClipStack;
        }
        ClipState state = stack[i];
        if (state == null) {
            stack[i] = state = new ClipState();
        }
        return state;
    }

    private void pop() {
        if (mClipIndex-- >= CLIP_POOL_SIZE) {
            mClipStack[mClipIndex + 1] = null;
        }
    }

    @Nonnull
    private ClipState clip() {
        return mClipStack[mClipIndex];
    }

    @Nonnull
    private ClipState writableClip() {
        ClipState state = mClipStack[mClipIndex];
        if (state.mDeferredSaveCount > 0) {
            state.mDeferredSaveCount--;
            ClipState next = push();
            next.set(state);
            next.mDeferredSaveCount = 0;
            return next;
        } else {
            return state;
        }
    }

    @Override
    protected void onSave() {
        mClipStack[mClipIndex].mDeferredSaveCount++;
    }

    @Override
    protected void onRestore() {
        ClipState state = mClipStack[mClipIndex];
        if (state.mDeferredSaveCount > 0) {
            state.mDeferredSaveCount--;
        } else {
            pop();
        }
    }

    @Override
    public void clipRect(Rect2f rect, int clipOp, boolean doAA) {
        writableClip().opRect(rect, getLocalToDevice(), clipOp, doAA);
    }

    @Override
    protected void onReplaceClip(Rect2i globalRect) {
        final Rect2i deviceRect = mTmpBounds;
        getGlobalToDevice().mapRect(globalRect, deviceRect);
        final ClipState clip = writableClip();
        if (!deviceRect.intersect(mBounds)) {
            clip.setEmpty();
        } else {
            clip.setRect(deviceRect);
        }
    }

    @Override
    public boolean clipIsAA() {
        return clip().mIsAA;
    }

    @Override
    public boolean clipIsWideOpen() {
        return clip().mIsRect && getClipBounds().equals(mBounds);
    }

    @Override
    protected int getClipType() {
        final ClipState clip = clip();
        if (clip.mClipBounds.isEmpty()) {
            return CLIP_TYPE_EMPTY;
        } else if (clip.mIsRect) {
            return CLIP_TYPE_RECT;
        } else {
            return CLIP_TYPE_COMPLEX;
        }
    }

    @Override
    protected Rect2ic getClipBounds() {
        return clip().getBounds();
    }

    @Override
    public void drawPaint(Paint paint) {
    }

    @Override
    public void drawRect(Rect2f r, Paint paint) {
    }

    /**
     * The ConservativeClip computes the maximum rectangular bounds of the actual clipping region
     * for quick rejection. This can skip operations that have no rendering results.
     */
    private static final class ClipState {

        private final Rect2i mClipBounds = new Rect2i();
        private int mDeferredSaveCount = 0;
        private boolean mIsAA = false;
        private boolean mIsRect = true;

        ClipState() {
        }

        private void applyOpParams(int op, boolean aa, boolean rect) {
            mIsAA |= aa;
            mIsRect &= (op == ClipOp.CLIP_OP_INTERSECT && rect);
        }

        public void set(ClipState clip) {
            mClipBounds.set(clip.mClipBounds);
            mIsRect = clip.mIsRect;
            mIsAA = clip.mIsAA;
        }

        // do not modify
        public Rect2ic getBounds() {
            return mClipBounds;
        }

        public void setEmpty() {
            mClipBounds.setEmpty();
            mIsRect = true;
            mIsAA = false;
        }

        public void setRect(int left, int top, int right, int bottom) {
            mClipBounds.set(left, top, right, bottom);
            mIsRect = true;
            mIsAA = false;
        }

        public void setRect(Rect2i r) {
            setRect(r.mLeft, r.mTop, r.mRight, r.mBottom);
        }

        public void opRect(final Rect2f localRect, final Matrix4 localToDevice, int clipOp, boolean doAA) {
            applyOpParams(clipOp, doAA, localToDevice.isScaleTranslate());
            switch (clipOp) {
                case ClipOp.CLIP_OP_INTERSECT:
                    break;
                case ClipOp.CLIP_OP_DIFFERENCE:
                    // Difference can only shrink the current clip.
                    // Leaving clip unchanged conservatively fulfills the contract.
                    return;
                default:
                    throw new IllegalArgumentException();
            }
            final Rect2i deviceRect = new Rect2i();
            if (doAA) {
                localToDevice.mapRectOut(localRect, deviceRect);
            } else {
                localToDevice.mapRect(localRect, deviceRect);
            }
            opRect(deviceRect, clipOp);
        }

        public void opRect(final Rect2i deviceRect, int clipOp) {
            applyOpParams(clipOp, false, true);

            if (clipOp == ClipOp.CLIP_OP_INTERSECT) {
                if (!mClipBounds.intersect(deviceRect)) {
                    mClipBounds.setEmpty();
                }
                return;
            }

            if (clipOp == ClipOp.CLIP_OP_DIFFERENCE) {
                if (mClipBounds.isEmpty()) {
                    return;
                }
                if (deviceRect.isEmpty() || !Rect2i.intersects(mClipBounds, deviceRect)) {
                    return;
                }
                if (deviceRect.contains(mClipBounds)) {
                    mClipBounds.setEmpty();
                    return;
                }
            }

            throw new IllegalArgumentException();
        }
    }
}
