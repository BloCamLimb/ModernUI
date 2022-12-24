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

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A virtual device draws nothing, but tracks device's clip.
 */
public final class VirtualDevice extends BaseDevice {

    private final Deque<ClipState> mClipStack = new ArrayDeque<>(8);
    private final Pool<ClipState> mClipRecPool = Pools.simple(16);

    public VirtualDevice(int left, int top, int right, int bottom) {
        super(new ImageInfo(right - left, bottom - top));
        setOrigin(null, left, top);
        resetClipStack();
    }

    public void resetForNextPicture(int left, int top, int right, int bottom) {
        mInfo.resize(right - left, bottom - top);
        setOrigin(null, left, top);
        resetClipStack();
    }

    private ConservativeClip clip() {
        return mClipStack.element().mClip;
    }

    private ConservativeClip writableClip() {
        ClipState state = mClipStack.element();
        if (state.mDeferredSaveCount > 0) {
            state.mDeferredSaveCount--;
            ClipState next = mClipRecPool.acquire();
            if (next == null) {
                next = new ClipState();
            }
            next.mClip.set(state.mClip);
            next.mDeferredSaveCount = 0;
            return next.mClip;
        } else {
            return state.mClip;
        }
    }

    private void resetClipStack() {
        if (mClipStack.isEmpty()) {
            ClipState next = mClipRecPool.acquire();
            if (next == null) {
                next = new ClipState();
            }
            mClipStack.push(next);
        } else {
            for (int i = 0; i < mClipStack.size() - 1; i++) {
                mClipRecPool.release(mClipStack.poll());
            }
        }
        ClipState state = mClipStack.element();
        state.mClip.setRect(mBounds);
        state.mDeferredSaveCount = 0;
    }

    @Override
    protected void onSave() {
        mClipStack.element().mDeferredSaveCount++;
    }

    @Override
    protected void onRestore() {
        ClipState state = mClipStack.element();
        if (state.mDeferredSaveCount > 0) {
            state.mDeferredSaveCount--;
        } else {
            mClipStack.pop();
            mClipRecPool.release(state);
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
    }
}
