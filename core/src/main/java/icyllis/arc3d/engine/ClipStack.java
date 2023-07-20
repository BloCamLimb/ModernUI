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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;

import java.util.ArrayList;

/**
 * GPU hierarchical clipping.
 */
public final class ClipStack extends Clip {

    /**
     * Clip ops.
     */
    public static final byte
            OP_DIFFERENCE = 0,  // target minus operand
            OP_INTERSECT = 1;   // target intersected with operand

    /**
     * Clip states.
     */
    public static final byte
            STATE_EMPTY = 0,
            STATE_WIDE_OPEN = 1,
            STATE_DEVICE_RECT = 2,
            STATE_DEVICE_ROUND_RECT = 3,
            STATE_COMPLEX = 4;

    private final ArrayList<SaveRecord> mSaves = new ArrayList<>();

    private final Rect2i mDeviceBounds;
    private final boolean mMSAA;

    public static final class Clip {

        final Rect2i mShape = new Rect2i();

        // model view matrix
        final Matrix4 mMatrix = Matrix4.identity();
    }

    public static class Element {
        Rect2f mRect;
        Matrix mViewMatrix;
        int mClipOp;
        boolean mAA;

        Element(Rect2f rect, Matrix viewMatrix, int clipOp, boolean AA) {
            mRect = rect;
            mViewMatrix = viewMatrix;
            mClipOp = clipOp;
            mAA = AA;
        }
    }

    private static class RawElement extends Element {
        Matrix mInverseViewMatrix = Matrix.identity();

        Rect2i mInnerBounds;
        Rect2i mOuterBounds;

        int mInvalidatedByIndex = -1;

        public RawElement(Rect2f rect, Matrix viewMatrix, int clipOp, boolean AA) {
            super(rect, viewMatrix, clipOp, AA);
            if (!viewMatrix.invert(mInverseViewMatrix)) {
                mRect.setEmpty();
            }
        }
    }

    public static final class SaveRecord {

        private final Rect2i mInnerBounds;
        private final Rect2i mOuterBounds;


        // Number of save() calls without modifications (yet)
        private int mDeferredSaveCount;

        private int mState;
        private int mOp;

        SaveRecord(Rect2i deviceBounds) {
            mInnerBounds = new Rect2i(deviceBounds);
            mOuterBounds = new Rect2i(deviceBounds);
            mState = STATE_WIDE_OPEN;
            mOp = OP_DIFFERENCE;
        }
    }

    public ClipStack(Rect2i deviceBounds, boolean msaa) {
        mDeviceBounds = new Rect2i(deviceBounds);
        mMSAA = msaa;
        mSaves.add(new SaveRecord(deviceBounds));
    }

    private SaveRecord currentSaveRecord() {
        return mSaves.get(mSaves.size() - 1);
    }

    public void save() {
        currentSaveRecord().mDeferredSaveCount++;
    }

    public void restore() {
    }

    private final Rect2f mTmpRect1 = new Rect2f();

    public void clipRect(Matrix viewMatrix,
                         Rect2f localRect,
                         int clipOp) {
        if (currentSaveRecord().mState == STATE_EMPTY) {
            return;
        }
        if (localRect.isEmpty()) {
            return;
        }

        boolean axisAligned = viewMatrix.mapRect(localRect, mTmpRect1);
        if (!mTmpRect1.intersect(mDeviceBounds)) {
            return;
        }
        boolean aa = false;
        if (mMSAA && !axisAligned) {
            aa = true;
        }
    }
}
