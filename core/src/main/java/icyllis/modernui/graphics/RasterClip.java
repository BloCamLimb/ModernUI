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
import icyllis.modernui.math.Rect;
import icyllis.modernui.math.RectF;

import javax.annotation.Nullable;

/**
 * Conservative clipping computes the maximum rectangular bounds of the actual clipping region
 * for quick check.
 */
class ConservativeClip {

    private static final ThreadLocal<Rect> sRect = ThreadLocal.withInitial(Rect::new);
    private static final ThreadLocal<RectF> sRectF = ThreadLocal.withInitial(RectF::new);

    final Rect mBounds = new Rect();
    boolean mIsRect = true;
    boolean mIsAA = false;

    Rect mClipRestrictionRect = null;

    final void applyClipRestriction(Region.Op op, Rect bounds) {
        if (op.compareTo(Region.Op.UNION) >= 0 && mClipRestrictionRect != null
                && !mClipRestrictionRect.isEmpty()) {
            if (!bounds.intersect(mClipRestrictionRect)) {
                bounds.setEmpty();
            }
        }
    }

    final void applyOpParams(Region.Op op, boolean aa, boolean rect) {
        mIsAA |= aa;
        mIsRect &= (op == Region.Op.INTERSECT && rect);
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

    public Rect getBounds() {
        return mBounds;
    }

    public void setEmpty() {
        mBounds.setEmpty();
        mIsRect = true;
        mIsAA = false;
    }

    public void setRect(Rect r) {
        mBounds.set(r);
        mIsRect = true;
        mIsAA = false;
    }

    public void setClipRestrictionRect(@Nullable Rect clipRestrictionRect) {
        mClipRestrictionRect = clipRestrictionRect;
    }

    public void opRectF(final RectF localRect, final Matrix4 ctm, Region.Op op, boolean doAA) {
        applyOpParams(op, doAA, ctm.isScaleTranslate());
        switch (op) {
            case INTERSECT, UNION, REPLACE:
                break;
            case DIFFERENCE:
                // Difference can only shrink the current clip.
                // Leaving clip unchanged conservatively fullfills the contract.
                return;
            case REVERSE_DIFFERENCE:
                // To reverse, we swap in the bounds with a replace op.
                // As with difference, leave it unchanged.
                op = Region.Op.REPLACE;
                break;
            case XOR:
                // Be conservative, based on (A XOR B) always included in (A union B),
                // which is always included in (bounds(A) union bounds(B))
                op = Region.Op.UNION;
                break;
            default:
                throw new IllegalArgumentException();
        }
        final RectF fr = sRectF.get();
        fr.set(localRect);
        ctm.transform(fr);
        final Rect r = sRect.get();
        if (doAA) {
            fr.roundOut(r);
        } else {
            fr.round(r);
        }
        opRect(r, op);
    }

    public void opRect(final Rect devRect, Region.Op op) {
        applyOpParams(op, false, true);

        if (op == Region.Op.INTERSECT) {
            if (!mBounds.intersect(devRect)) {
                mBounds.setEmpty();
            }
            return;
        }

        //TODO
        throw new UnsupportedOperationException();
    }
}

public class RasterClip {
}
