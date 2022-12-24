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

/**
 * The conservative clip computes the maximum rectangular bounds of the actual clipping region
 * for quick reject (a preprocessing before transferring drawing commands to GPU).
 */
public final class ConservativeClip {

    private static final ThreadLocal<Rect> sRect = ThreadLocal.withInitial(Rect::new);

    private final Rect mBounds = new Rect();
    private boolean mIsRect = true;
    private boolean mIsAA = false;

    public ConservativeClip() {
    }

    private void applyOpParams(int op, boolean aa, boolean rect) {
        mIsAA |= aa;
        mIsRect &= (op == ClipStack.OP_INTERSECT && rect);
    }

    public void set(ConservativeClip clip) {
        mBounds.set(clip.mBounds);
        mIsRect = clip.mIsRect;
        mIsAA = clip.mIsAA;
    }

    public boolean isEmpty() {
        return mBounds.isEmpty();
    }

    public boolean isRect() {
        return mIsRect;
    }

    public boolean isAA() {
        return mIsAA;
    }

    // do not modify
    public Rect getBounds() {
        return mBounds;
    }

    public void setEmpty() {
        mBounds.setEmpty();
        mIsRect = true;
        mIsAA = false;
    }

    public void setRect(int left, int top, int right, int bottom) {
        mBounds.set(left, top, right, bottom);
        mIsRect = true;
        mIsAA = false;
    }

    public void setRect(Rect r) {
        setRect(r.left, r.top, r.right, r.bottom);
    }

    public void replace(final Rect globalRect, final Matrix3 globalToDevice, final Rect deviceBounds) {
        final Rect deviceRect = sRect.get();
        globalToDevice.mapRect(globalRect, deviceRect);
        if (!deviceRect.intersect(deviceBounds)) {
            setEmpty();
        } else {
            setRect(deviceRect);
        }
    }

    public void opRect(final RectF localRect, final Matrix4 localToDevice, int clipOp, boolean doAA) {
        applyOpParams(clipOp, doAA, localToDevice.isScaleTranslate());
        switch (clipOp) {
            case ClipStack.OP_INTERSECT:
                break;
            case ClipStack.OP_DIFFERENCE:
                // Difference can only shrink the current clip.
                // Leaving clip unchanged conservatively fulfills the contract.
                return;
            default:
                throw new IllegalArgumentException();
        }
        final Rect deviceRect = sRect.get();
        if (doAA) {
            localToDevice.mapRectOut(localRect, deviceRect);
        } else {
            localToDevice.mapRect(localRect, deviceRect);
        }
        opRect(deviceRect, clipOp);
    }

    public void opRect(final Rect deviceRect, int clipOp) {
        applyOpParams(clipOp, false, true);

        if (clipOp == ClipStack.OP_INTERSECT) {
            if (!mBounds.intersect(deviceRect)) {
                mBounds.setEmpty();
            }
            return;
        }

        if (clipOp == ClipStack.OP_DIFFERENCE) {
            if (mBounds.isEmpty()) {
                return;
            }
            if (deviceRect.isEmpty() || !Rect.intersects(mBounds, deviceRect)) {
                return;
            }
            if (deviceRect.contains(mBounds)) {
                mBounds.setEmpty();
                return;
            }
        }

        throw new IllegalArgumentException();
    }
}
