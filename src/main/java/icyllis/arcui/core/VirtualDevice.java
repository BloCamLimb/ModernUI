/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.core;

import javax.annotation.Nonnull;

/**
 * A virtual device draws nothing, but tracks device's clip.
 *
 * @author BloCamLimb
 */
public class VirtualDevice extends BaseDevice {

    // cache some objects for performance
    private static final int MAX_CLIP_POOL_SIZE = 16;

    private ClipState[] mClipStack = new ClipState[MAX_CLIP_POOL_SIZE];
    private int mClipIndex = 0;

    public VirtualDevice(int left, int top, int right, int bottom) {
        super(new ImageInfo(right - left, bottom - top));
        setOrigin(null, left, top);
        ClipState state = new ClipState();
        state.mClip.setRect(mBounds);
        mClipStack[0] = state;
    }

    public final void resetForNextPicture(int left, int top, int right, int bottom) {
        resize(right - left, bottom - top);
        setOrigin(null, left, top);
        for (int i = mClipIndex; i > 0; i--) {
            pop();
        }
        ClipState state = mClipStack[0];
        state.mClip.setRect(mBounds);
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
        if (mClipIndex-- >= MAX_CLIP_POOL_SIZE) {
            mClipStack[mClipIndex + 1] = null;
        }
    }

    @Nonnull
    private ConservativeClip clip() {
        return mClipStack[mClipIndex].mClip;
    }

    @Nonnull
    private ConservativeClip writableClip() {
        ClipState state = mClipStack[mClipIndex];
        if (state.mDeferredSaveCount > 0) {
            state.mDeferredSaveCount--;
            ClipState next = push();
            next.mClip.set(state.mClip);
            next.mDeferredSaveCount = 0;
            return next.mClip;
        } else {
            return state.mClip;
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
    public void clipRect(RectF rect, int clipOp, boolean doAA) {
        writableClip().opRect(rect, localToDevice(), clipOp, doAA);
    }

    @Override
    public void replaceClip(Rect rect) {
        writableClip().replace(rect, globalToDevice(), mBounds);
    }

    @Override
    public boolean clipIsAA() {
        return clip().isAA();
    }

    @Override
    public boolean clipIsWideOpen() {
        return clip().isRect() && getClipBounds().equals(mBounds);
    }

    @Override
    protected int getClipType() {
        ConservativeClip clip = clip();
        if (clip.isEmpty()) {
            return CLIP_TYPE_EMPTY;
        } else if (clip.isRect()) {
            return CLIP_TYPE_RECT;
        } else {
            return CLIP_TYPE_COMPLEX;
        }
    }

    @Override
    protected Rect getClipBounds() {
        return clip().getBounds();
    }

    @Override
    protected void drawPaint(Paint paint) {
    }

    private static final class ClipState {

        final ConservativeClip mClip = new ConservativeClip();
        int mDeferredSaveCount = 0;

        ClipState() {
        }
    }
}
