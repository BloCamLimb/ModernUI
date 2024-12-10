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

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/**
 * The NoPixelsDevice draws nothing, but tracks device's clip.
 * Used for deferred rendering.
 */
public class NoPixelsDevice extends Device {

    // cache some objects and their references for performance
    private static final int CLIP_POOL_SIZE = 16;

    private ConservativeClip[] mClipStack = new ConservativeClip[CLIP_POOL_SIZE];
    private int mClipIndex = 0;

    public NoPixelsDevice(@NonNull Rect2ic bounds) {
        this(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
    }

    public NoPixelsDevice(int left, int top, int right, int bottom) {
        super(new ImageInfo(right - left, bottom - top));
        setOrigin(null, left, top);
        var clip = new ConservativeClip();
        clip.setRect(getBounds());
        mClipStack[0] = clip;
    }

    //TODO should be reviewed if there's picture support
    public final void resetForNextPicture(int left, int top, int right, int bottom) {
        resize(right - left, bottom - top);
        setOrigin(null, left, top);
        for (int i = mClipIndex; i > 0; i--) {
            pop();
        }
        var clip = mClipStack[0];
        clip.setRect(getBounds());
        clip.mDeferredSaveCount = 0;
    }

    @NonNull
    private ConservativeClip push() {
        final int i = ++mClipIndex;
        ConservativeClip[] stack = mClipStack;
        if (i == stack.length) {
            mClipStack = stack = Arrays.copyOf(stack, i + (i >> 1));
        }
        var clip = stack[i];
        if (clip == null) {
            stack[i] = clip = new ConservativeClip();
        }
        return clip;
    }

    private void pop() {
        final int i = mClipIndex--;
        if (i >= CLIP_POOL_SIZE) {
            mClipStack[i] = null;
        }
    }

    @NonNull
    private ConservativeClip getClip() {
        return mClipStack[mClipIndex];
    }

    @NonNull
    private ConservativeClip getWritableClip() {
        var current = mClipStack[mClipIndex];
        if (current.mDeferredSaveCount > 0) {
            current.mDeferredSaveCount--;
            var next = push();
            next.set(current);
            next.mDeferredSaveCount = 0;
            return next;
        } else {
            return current;
        }
    }

    @Override
    public void pushClipStack() {
        mClipStack[mClipIndex].mDeferredSaveCount++;
    }

    @Override
    public void popClipStack() {
        var clip = mClipStack[mClipIndex];
        if (clip.mDeferredSaveCount > 0) {
            clip.mDeferredSaveCount--;
        } else {
            pop();
        }
    }

    @Override
    public void clipRect(Rect2fc rect, int clipOp, boolean doAA) {
        getWritableClip().op(rect, getLocalToDevice(), clipOp, doAA, true);
    }

    public void replaceClip(Rect2ic globalRect) {
        final Rect2i deviceRect = new Rect2i();
        getGlobalToDevice().mapRect(globalRect, deviceRect);
        var clip = getWritableClip();
        if (!deviceRect.intersect(getBounds())) {
            clip.setEmpty();
        } else {
            clip.setRect(deviceRect);
        }
    }

    @Override
    public boolean isClipAA() {
        return getClip().mIsAA;
    }

    @Override
    public boolean isClipEmpty() {
        return getClipBounds().isEmpty();
    }

    @Override
    public boolean isClipRect() {
        return getClip().mIsRect && !isClipEmpty();
    }

    @Override
    public boolean isClipWideOpen() {
        return getClip().mIsRect && getClipBounds().equals(getBounds());
    }

    @Override
    public void getClipBounds(@NonNull Rect2i bounds) {
        bounds.set(getClipBounds());
    }

    @Override
    protected Rect2ic getClipBounds() {
        return getClip().getBounds();
    }

    @Override
    public void drawPaint(Paint paint) {
    }

    @Override
    public void drawPoints(int mode, float[] pts, int offset, int count, Paint paint) {
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1, @Paint.Cap int cap, float width, Paint paint) {
    }

    @Override
    public void drawRect(Rect2fc r, Paint paint) {
    }

    @Override
    public void drawRoundRect(RoundRect rr, Paint paint) {
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, Paint paint) {
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, int cap, float width, Paint paint) {
    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle,
                        float sweepAngle, Paint paint) {
    }

    @Override
    public void drawChord(float cx, float cy, float radius, float startAngle,
                          float sweepAngle, Paint paint) {
    }

    @Override
    public void drawImageRect(@RawPtr Image image, Rect2fc src, Rect2fc dst,
                              SamplingOptions sampling, Paint paint, int constraint) {
    }

    @Override
    protected void onDrawGlyphRunList(Canvas canvas, GlyphRunList glyphRunList, Paint paint) {
    }

    @Override
    public void drawVertices(Vertices vertices, @SharedPtr Blender blender, Paint paint) {
        RefCnt.move(blender);
    }

    /**
     * The ConservativeClip computes the maximum rectangular bounds of the actual clipping region
     * for quick rejection. This can skip operations that have no rendering results.
     */
    private static final class ConservativeClip {

        private final Rect2i mClipBounds = new Rect2i();
        private int mDeferredSaveCount = 0;
        private boolean mIsAA = false;
        private boolean mIsRect = true;

        ConservativeClip() {
        }

        public void set(ConservativeClip clip) {
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

        public void setRect(Rect2ic r) {
            mClipBounds.set(r);
            mIsRect = true;
            mIsAA = false;
        }

        public void op(Rect2fc localBounds, Matrix4c localToDevice,
                       int op, boolean isAA, boolean isRect) {
            mIsAA |= isAA;
            boolean isDeviceRect = isRect && localToDevice.isAxisAligned();
            if (op == ClipOp.CLIP_OP_INTERSECT) {
                if (!localBounds.isEmpty()) {
                    final Rect2i deviceRect = new Rect2i();
                    if (isAA) {
                        localToDevice.mapRectOut(localBounds, deviceRect);
                    } else {
                        localToDevice.mapRect(localBounds, deviceRect);
                    }
                    if (!mClipBounds.intersect(deviceRect)) {
                        mClipBounds.setEmpty();
                    }
                } else {
                    mClipBounds.setEmpty();
                }
                // A rectangular clip remains rectangular if the intersection is a rect
                mIsRect &= isDeviceRect;
            } else if (isDeviceRect) {
                // Conservatively, we can leave the clip bounds unchanged and respect the difference op.
                // But, if we're subtracting out an axis-aligned rectangle that fully spans our existing
                // clip on an axis, we can shrink the clip bounds.
                assert op == ClipOp.CLIP_OP_DIFFERENCE;
                final Rect2i deviceRect = new Rect2i();
                if (isAA) {
                    localToDevice.mapRectIn(localBounds, deviceRect);
                } else {
                    localToDevice.mapRect(localBounds, deviceRect);
                }
                final Rect2i difference = new Rect2i();
                if (Rect2i.subtract(mClipBounds, deviceRect, difference)) {
                    mClipBounds.set(difference);
                } else {
                    // Difference can only shrink the current clip.
                    // Leaving clip unchanged conservatively fulfills the contract.
                    mIsRect = false;
                }
            } else {
                // A non-rect shape was applied
                mIsRect = false;
            }
        }
    }
}
