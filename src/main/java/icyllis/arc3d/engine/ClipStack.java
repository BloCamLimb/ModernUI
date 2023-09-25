/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
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

    @Override
    public int apply(SurfaceDrawContext sdc, ClipResult out, Rect2f bounds) {
        return 0;
    }

    public static final class Clip {

        final Rect2i mShape = new Rect2i();

        // model view matrix
        final Matrix4 mMatrix = Matrix4.identity();
    }

    public interface GeometrySource {

        int op();

        Rect2i outerBounds();

        boolean contains(GeometrySource other);
    }

    // This captures which of the two elements in (A op B) would be required when they are combined,
    // where op is intersect or difference.
    public static final int
            CLIP_GEOMETRY_EMPTY = 0,
            CLIP_GEOMETRY_A_ONLY = 1,
            CLIP_GEOMETRY_B_ONLY = 2,
            CLIP_GEOMETRY_BOTH = 3;

    public static int getClipGeometry(
            GeometrySource A,
            GeometrySource B) {

        if (A.op() == OP_INTERSECT) {

            if (B.op() == OP_INTERSECT) {

                // Intersect (A) + Intersect (B)
                if (!Rect2i.intersects(
                        A.outerBounds(),
                        B.outerBounds())) {
                    // Regions with non-zero coverage are disjoint, so intersection = empty
                    return CLIP_GEOMETRY_EMPTY;
                }

                if (B.contains(A)) {
                    // B's full coverage region contains entirety of A, so intersection = A
                    return CLIP_GEOMETRY_A_ONLY;
                }

                if (A.contains(B)) {
                    // A's full coverage region contains entirety of B, so intersection = B
                    return CLIP_GEOMETRY_B_ONLY;
                }

                {
                    // The shapes intersect in some non-trivial manner
                    return CLIP_GEOMETRY_BOTH;
                }
            }

            if (B.op() == OP_DIFFERENCE) {

                // Intersect (A) + Difference (B)
                if (!Rect2i.intersects(
                        A.outerBounds(),
                        B.outerBounds())) {
                    // A only intersects B's full coverage region, so intersection = A
                    return CLIP_GEOMETRY_A_ONLY;
                }

                if (B.contains(A)) {
                    // B's zero coverage region completely contains A, so intersection = empty
                    return CLIP_GEOMETRY_EMPTY;
                }

                {
                    // Intersection cannot be simplified. Note that the combination of a intersect
                    // and difference op in this order cannot produce kBOnly
                    return CLIP_GEOMETRY_BOTH;
                }
            }
        }

        if (A.op() == OP_DIFFERENCE) {

            if (B.op() == OP_INTERSECT) {

                // Difference (A) + Intersect (B) - the mirror of Intersect(A) + Difference(B),
                // but combining is commutative so this is equivalent barring naming.
                if (!Rect2i.intersects(
                        B.outerBounds(),
                        A.outerBounds())) {
                    // B only intersects A's full coverage region, so intersection = B
                    return CLIP_GEOMETRY_B_ONLY;
                }

                if (A.contains(B)) {
                    // A's zero coverage region completely contains B, so intersection = empty
                    return CLIP_GEOMETRY_EMPTY;
                }

                {
                    // Cannot be simplified
                    return CLIP_GEOMETRY_BOTH;
                }
            }

            if (B.op() == OP_DIFFERENCE) {

                // Difference (A) + Difference (B)
                if (A.contains(B)) {
                    // A's zero coverage region contains B, so B doesn't remove any extra
                    // coverage from their intersection.
                    return CLIP_GEOMETRY_A_ONLY;
                }

                if (B.contains(A)) {
                    // Mirror of the above case, intersection = B instead
                    return CLIP_GEOMETRY_B_ONLY;
                }

                {
                    // Intersection of the two differences cannot be simplified. Note that for
                    // this op combination it is not possible to produce kEmpty.
                    return CLIP_GEOMETRY_BOTH;
                }
            }
        }

        throw new IllegalStateException();
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

    public Rect2i getConservativeBounds() {
        var current = currentSaveRecord();
        if (current.mState == STATE_EMPTY) {
            return null;
        } else if (current.mState == STATE_WIDE_OPEN) {
            return mDeviceBounds;
        } else {
            {
                return current.mOuterBounds;
            }
        }
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
