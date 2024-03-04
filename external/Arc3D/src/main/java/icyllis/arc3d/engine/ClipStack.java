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
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * GPU hierarchical clipping.
 * <p>
 * We use scissor test and stencil test to apply clip, antialiasing only works on
 * multisampled targets.
 */
//TODO aa check? review
public final class ClipStack extends Clip {

    /**
     * Clip ops.
     */
    public static final int
            OP_DIFFERENCE = ClipOp.CLIP_OP_DIFFERENCE,  // target minus operand
            OP_INTERSECT = ClipOp.CLIP_OP_INTERSECT;    // target intersected with operand

    /**
     * Clip states.
     */
    public static final int
            STATE_EMPTY = 0,
            STATE_WIDE_OPEN = 1,
            STATE_DEVICE_RECT = 2,
            STATE_COMPLEX = 3;

    private final ArrayDeque<SaveRecord> mSaves = new ArrayDeque<>();
    private final ArrayDeque<ClipElement> mElements = new ArrayDeque<>();
    private final Collection<Element> mElementsView = Collections.unmodifiableCollection(mElements);

    private final Rect2i mDeviceBounds;
    private final boolean mMSAA;

    public ClipStack(Rect2ic deviceBounds, boolean msaa) {
        mDeviceBounds = new Rect2i(deviceBounds);
        mMSAA = msaa;
        mSaves.add(new SaveRecord(deviceBounds));
    }

    public int currentClipState() {
        return mSaves.element().mState;
    }

    public void save() {
        mSaves.element().pushSave();
    }

    public void restore() {
        SaveRecord current = mSaves.element();
        if (current.popSave()) {
            // This was just a deferred save being undone, so the record doesn't need to be removed yet
            return;
        }

        // When we remove a save record, we delete all elements >= its starting index and any masks
        // that were rasterized for it.
        current.removeElements(mElements);
        mSaves.pop();
        // Restore any remaining elements that were only invalidated by the now-removed save record.
        mSaves.element().restoreElements(mElements);
    }

    @UnmodifiableView
    public Collection<Element> elements() {
        return mElementsView;
    }

    private final ClipElement mTmpElement = new ClipElement();
    private final Rect2f mTmpOuter = new Rect2f();

    public void clipRect(@Nullable Matrixc viewMatrix,
                         @Nonnull Rect2fc localRect,
                         int clipOp) {
        clip(mTmpElement.init(
                localRect.left(), localRect.top(), localRect.right(), localRect.bottom(),
                viewMatrix, clipOp, false
        ));
    }

    public void clipRect(@Nullable Matrixc viewMatrix,
                         float left, float top, float right, float bottom,
                         int clipOp) {
        clip(mTmpElement.init(
                left, top, right, bottom,
                viewMatrix, clipOp, false
        ));
    }

    // other clip shapes and clip shader are not supported
    // you can use post-processing effect to achieve that

    private void clip(ClipElement element) {
        if (mSaves.element().mState == STATE_EMPTY) {
            return;
        }

        // Reduce the path to anything simpler, will apply the transform if it's a scale+translate
        // and ensures the element's bounds are clipped to the device (NOT the conservative clip bounds,
        // since those are based on the net effect of all elements while device bounds clipping happens
        // implicitly. During addElement, we may still be able to invalidate some older elements).
        element.simplify(mDeviceBounds, mMSAA, mTmpOuter);

        // An empty op means do nothing (for difference), or close the save record, so we try and detect
        // that early before doing additional unnecessary save record allocation.
        if (element.shape().isEmpty()) {
            if (element.clipOp() == OP_DIFFERENCE) {
                // If the shape is empty and we're subtracting, this has no effect on the clip
                return;
            }
            // else we will make the clip empty, but we need a new save record to record that change
            // in the clip state; fall through to below and updateForElement() will handle it.
        }

        boolean wasDeferred;
        SaveRecord current = mSaves.element();
        if (current.canBeUpdated()) {
            // Current record is still open, so it can be modified directly
            wasDeferred = false;
        } else {
            // Must un-defer the save to get a new record.
            boolean alive = current.popSave();
            assert alive;
            wasDeferred = true;
            current = new SaveRecord(current, mElements.size());
            mSaves.push(current);
        }

        int elementCount = mElements.size();
        if (!current.addElement(element, mElements)) {
            if (wasDeferred) {
                // We made a new save record, but ended up not adding an element to the stack.
                // So instead of keeping an empty save record around, pop it off and restore the counter
                assert (elementCount == mElements.size());
                mSaves.pop();
                mSaves.element().pushSave();
            }
        }
    }

    @Override
    public void getConservativeBounds(Rect2i out) {
        SaveRecord current = mSaves.element();
        if (current.mState == STATE_EMPTY) {
            out.setEmpty();
        } else if (current.mState == STATE_WIDE_OPEN) {
            out.set(mDeviceBounds);
        } else {
            if (current.op() == OP_DIFFERENCE) {
                subtract(mDeviceBounds, current.mInnerBounds, out, true);
            } else {
                assert (mDeviceBounds.contains(current.outerBounds()));
                out.set(current.mOuterBounds);
            }
        }
    }

    private final Draw mTmpDraw = new Draw();
    private final ArrayList<Element> mElementsForMask = new ArrayList<>();

    @Override
    public int apply(SurfaceDrawContext sdc, boolean aa, ClipResult out, Rect2f bounds) {

        Draw draw = mTmpDraw.init(bounds, aa);
        if (!draw.mBounds.intersect(mDeviceBounds)) {
            return CLIPPED_OUT;
        }

        SaveRecord save = mSaves.element();
        // Early out if we know a priori that the clip is full 0s or full 1s.
        if (save.mState == STATE_EMPTY) {
            return CLIPPED_OUT;
        } else if (save.mState == STATE_WIDE_OPEN) {
            return NOT_CLIPPED;
        }

        // A refers to the entire clip stack, B refers to the draw
        switch (getClipGeometry(save, draw)) {
            case CLIP_GEOMETRY_EMPTY -> {
                return CLIPPED_OUT;
            }
            case CLIP_GEOMETRY_A_ONLY -> {
                // Shouldn't happen since draws don't report inner bounds
                assert false;
            }
            case CLIP_GEOMETRY_B_ONLY -> {
                return NOT_CLIPPED;
            }
            case CLIP_GEOMETRY_BOTH -> {
                // The draw is combined with the saved clip elements; the below logic tries to skip
                // as many elements as possible.
                assert (save.mState == STATE_DEVICE_RECT ||
                        save.mState == STATE_COMPLEX);
            }
        }

        // We can determine a scissor based on the draw and the overall stack bounds.
        final Rect2i scissorBounds;
        if (save.op() == OP_INTERSECT) {
            // Initially we keep this as large as possible; if the clip is applied solely with coverage
            // FPs then using a loose scissor increases the chance we can batch the draws.
            // We tighten it later if any form of mask or atlas element is needed.
            scissorBounds = new Rect2i(save.outerBounds());
        } else {
            assert save.op() == OP_DIFFERENCE;
            var diff = new Rect2i();
            if (Rect2i.subtract(draw.outerBounds(), save.innerBounds(), diff)) {
                scissorBounds = diff;
            } else {
                scissorBounds = new Rect2i(draw.outerBounds());
            }
        }

        // We mark this true once we have an element that wouldn't affect the scissored draw bounds,
        // but does affect the regular draw bounds.
        // In that case, the scissor is sufficient for clipping and we can skip the
        // element but definitely cannot then drop the scissor.
        boolean scissorIsNeeded = false;

        // Elements not skipped will be collected here and later applied by using the stencil buffer.
        List<Element> elementsForMask = mElementsForMask;

        int i = mElements.size();
        for (ClipElement e : mElements) {
            --i;
            if (i < save.oldestElementIndex()) {
                // All earlier elements have been invalidated by elements already processed
                break;
            } else if (e.isInvalid()) {
                continue;
            }

            switch (getClipGeometry(e, draw)) {
                case CLIP_GEOMETRY_EMPTY:
                    // This can happen for difference op elements that have a larger fInnerBounds than
                    // can be preserved at the next level.
                    elementsForMask.clear();
                    return CLIPPED_OUT;
                case CLIP_GEOMETRY_B_ONLY:
                    // We don't need to produce a mask for the element
                    break;
                case CLIP_GEOMETRY_A_ONLY:
                    // Shouldn't happen for draws, fall through to regular element processing
                    assert false;
                case CLIP_GEOMETRY_BOTH: {
                    // The element must apply coverage to the draw, enable the scissor to limit overdraw
                    scissorIsNeeded = true;

                    // First apply using HW methods (scissor and window rects). When the inner and outer
                    // bounds match, nothing else needs to be done.
                    boolean fullyApplied = false;

                    if (e.op() == OP_INTERSECT) {
                        // The second test allows clipped draws that are scissored by multiple
                        // elements to remain scissor-only.
                        fullyApplied = e.innerBounds() == e.outerBounds() ||
                                e.innerBounds().contains(scissorBounds);
                    }

                    if (!fullyApplied) {
                        elementsForMask.add(e);
                    }

                    break;
                }
            }
        }

        if (!scissorIsNeeded) {
            // More detailed analysis of the element shapes determined no clip is needed
            assert elementsForMask.isEmpty();
            return NOT_CLIPPED;
        }

        if (save.op() == OP_INTERSECT && !elementsForMask.isEmpty()) {
            boolean res = scissorBounds.intersect(draw.outerBounds());
            assert res;
        }

        if (!scissorBounds.contains(draw.outerBounds())) {
            out.addScissor(scissorBounds, bounds);
        }

        // render stencil mask
        if (!elementsForMask.isEmpty()) {

            //TODO do render stencil

            elementsForMask.clear();
        }

        assert out.hasClip();
        return CLIPPED;
    }

    public interface Geometry {

        int op();

        Rect2ic outerBounds();

        boolean contains(Geometry other);
    }

    // This captures which of the two elements in (A op B) would be required when they are combined,
    // where op is intersect or difference.
    public static final int
            CLIP_GEOMETRY_EMPTY = 0,
            CLIP_GEOMETRY_A_ONLY = 1,
            CLIP_GEOMETRY_B_ONLY = 2,
            CLIP_GEOMETRY_BOTH = 3;

    public static int getClipGeometry(
            Geometry A,
            Geometry B) {

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

    // All data describing a geometric modification to the clip
    public static class Element {

        final Rect2f mRect;
        final Matrix mViewMatrix;
        int mClipOp;
        boolean mAA;

        Element() {
            mRect = new Rect2f();
            mViewMatrix = new Matrix();
        }

        Element(Rect2fc rect, Matrixc viewMatrix, int clipOp, boolean aa) {
            mRect = new Rect2f(rect);
            mViewMatrix = new Matrix(viewMatrix);
            mClipOp = clipOp;
            mAA = aa;
        }

        // local rect
        // do not modify
        public Rect2fc shape() {
            return mRect;
        }

        // local to device
        // do not modify
        public Matrixc viewMatrix() {
            return mViewMatrix;
        }

        public int clipOp() {
            return mClipOp;
        }

        public boolean aa() {
            return mAA;
        }

        @Override
        public String toString() {
            return "Element{" +
                    "mRect=" + mRect +
                    ", mViewMatrix=" + mViewMatrix +
                    ", mClipOp=" + (mClipOp == OP_INTERSECT ? "Intersect" : "Difference") +
                    ", mAA=" + mAA +
                    '}';
        }
    }

    // Implements the geometric Element data with logic for containment and bounds testing.
    static class ClipElement extends Element implements Geometry {

        // cached inverse of fLocalToDevice for contains() optimization
        final Matrix mInverseViewMatrix = new Matrix();

        // Device space bounds, rounded in or out to pixel boundaries and accounting for any
        // uncertainty around anti-aliasing and rasterization snapping.
        final Rect2i mInnerBounds = new Rect2i();
        final Rect2i mOuterBounds = new Rect2i();

        // Elements are invalidated by SaveRecords as the record is updated with new elements that
        // override old geometry. An invalidated element stores the index of the first element of
        // the save record that invalidated it. This makes it easy to undo when the save record is
        // popped from the stack, and is stable as the current save record is modified.
        int mInvalidatedByIndex = -1;

        public ClipElement() {
        }

        public ClipElement(Rect2fc rect, Matrixc viewMatrix, int clipOp, boolean aa) {
            super(rect, viewMatrix, clipOp, aa);
            if (!viewMatrix.invert(mInverseViewMatrix)) {
                // If the transform can't be inverted, it means that two dimensions are collapsed to 0 or
                // 1 dimension, making the device-space geometry effectively empty.
                mRect.setEmpty();
            }
        }

        public ClipElement(ClipElement e) {
            super(e.shape(), e.viewMatrix(), e.clipOp(), e.aa());
            mInverseViewMatrix.set(e.mInverseViewMatrix);
            mInnerBounds.set(e.mInnerBounds);
            mOuterBounds.set(e.mOuterBounds);
            mInvalidatedByIndex = e.mInvalidatedByIndex;
        }

        public ClipElement init(float left, float top, float right, float bottom,
                                Matrixc viewMatrix, int clipOp, boolean aa) {
            mRect.set(left, top, right, bottom);
            if (viewMatrix != null) {
                mViewMatrix.set(viewMatrix);
            } else {
                mViewMatrix.setIdentity();
            }
            mClipOp = clipOp;
            mAA = aa;
            if (viewMatrix != null) {
                if (!viewMatrix.invert(mInverseViewMatrix)) {
                    // If the transform can't be inverted, it means that two dimensions are collapsed to 0 or
                    // 1 dimension, making the device-space geometry effectively empty.
                    mRect.setEmpty();
                    mInverseViewMatrix.setIdentity();
                }
            } else {
                mInverseViewMatrix.setIdentity();
            }
            mInnerBounds.setEmpty();
            mOuterBounds.setEmpty();
            mInvalidatedByIndex = -1;
            return this;
        }

        public void set(ClipElement e) {
            mRect.set(e.shape());
            mViewMatrix.set(e.viewMatrix());
            mClipOp = e.clipOp();
            mAA = e.aa();
            mInverseViewMatrix.set(e.mInverseViewMatrix);
            mInnerBounds.set(e.mInnerBounds);
            mOuterBounds.set(e.mOuterBounds);
            mInvalidatedByIndex = e.mInvalidatedByIndex;
        }

        // As new elements are pushed on to the stack, they may make older elements redundant.
        // The old elements are marked invalid so they are skipped during clip application, but may
        // become active again when a save record is restored.
        public boolean isInvalid() {
            return mInvalidatedByIndex >= 0;
        }

        public void markInvalid(SaveRecord current) {
            assert (!isInvalid());
            mInvalidatedByIndex = current.firstActiveElementIndex();
        }

        public void restoreValid(SaveRecord current) {
            if (current.firstActiveElementIndex() < mInvalidatedByIndex) {
                mInvalidatedByIndex = -1;
            }
        }

        public boolean combine(ClipElement other, SaveRecord current) {
            // To reduce the number of possibilities, only consider intersect+intersect. Difference and
            // mixed op cases could be analyzed to simplify one of the shapes, but that is a rare
            // occurrence and the math is much more complicated.
            if (other.mClipOp != OP_INTERSECT || mClipOp != OP_INTERSECT) {
                return false;
            }

            // At the moment, only rect+rect or rrect+rrect are supported (although rect+rrect is
            // treated as a degenerate case of rrect+rrect).
            boolean shapeUpdated = false;

            if (Matrix.equals(mViewMatrix, other.mInverseViewMatrix)) {
                if (!mRect.intersect(other.mRect)) {
                    // By floating point, it turns out the combination should be empty
                    mRect.setEmpty();
                    markInvalid(current);
                    return true;
                }
                shapeUpdated = true;
            }

            if (shapeUpdated) {
                // This logic works under the assumption that both combined elements were intersect, so we
                // don't do the full bounds computations like in simplify().
                assert (mClipOp == OP_INTERSECT && other.mClipOp == OP_INTERSECT);
                boolean res = mOuterBounds.intersect(other.mOuterBounds);
                assert res;
                if (!mInnerBounds.intersect(other.mInnerBounds)) {
                    mInnerBounds.setEmpty();
                }
                return true;
            } else {
                return false;
            }
        }

        public void updateForElement(ClipElement added, SaveRecord current) {
            if (isInvalid()) {
                // Already doesn't do anything, so skip this element
                return;
            }

            // 'A' refers to this element, 'B' refers to 'added'.
            switch (getClipGeometry(this, added)) {
                case CLIP_GEOMETRY_EMPTY:
                    // Mark both elements as invalid to signal that the clip is fully empty
                    markInvalid(current);
                    added.markInvalid(current);
                    break;

                case CLIP_GEOMETRY_A_ONLY:
                    // This element already clips more than 'added', so mark 'added' is invalid to skip it
                    added.markInvalid(current);
                    break;

                case CLIP_GEOMETRY_B_ONLY:
                    // 'added' clips more than this element, so mark this as invalid
                    markInvalid(current);
                    break;

                case CLIP_GEOMETRY_BOTH:
                    // Else the bounds checks think we need to keep both, but depending on the combination
                    // of the ops and shape kinds, we may be able to do better.
                    if (added.combine(this, current)) {
                        // 'added' now fully represents the combination of the two elements
                        markInvalid(current);
                    }
                    break;
            }
        }

        public int op() {
            return mClipOp;
        }

        public Rect2ic innerBounds() {
            return mInnerBounds;
        }

        // reference to unmodifiable rect
        public Rect2ic outerBounds() {
            return mOuterBounds;
        }

        public boolean contains(Geometry g) {
            if (g instanceof Draw d) {
                return contains(d);
            } else if (g instanceof SaveRecord s) {
                return contains(s);
            } else {
                return contains((ClipElement) g);
            }
        }

        public boolean contains(Draw d) {
            if (mInnerBounds.contains(d.outerBounds())) {
                return true;
            } else {
                // If the draw is non-AA, use the already computed outer bounds so we don't need to use
                // device-space outsetting inside shape_contains_rect.
                Rect2fc queryBounds = d.mAA ? d.bounds() : d.mTmpBounds;
                return rect_contains_rect(mRect, mViewMatrix, mInverseViewMatrix,
                        queryBounds, Matrix.identity(), /* mixed-aa */ false);
            }
        }

        public boolean contains(SaveRecord s) {
            if (mInnerBounds.contains(s.mOuterBounds)) {
                return true;
            }

            return rect_contains_rect(mRect, mViewMatrix, mInverseViewMatrix,
                    new Rect2f(s.mOuterBounds), Matrix.identity(), false);
        }

        public boolean contains(ClipElement e) {
            // This is similar to how ClipElement checks containment for a Draw, except that both the tester
            // and testee have a transform that needs to be considered.
            if (mInnerBounds.contains(e.mOuterBounds)) {
                return true;
            }

            boolean mixedAA = mAA != e.mAA;

            return rect_contains_rect(mRect, mViewMatrix, mInverseViewMatrix,
                    e.mRect, e.mViewMatrix, mixedAA);
        }

        // a.contains(b) where a's local space is defined by 'aToDevice', and b's possibly separate local
        // space is defined by 'bToDevice'. 'a' and 'b' geometry are provided in their local spaces.
        // Automatically takes into account if the anti-aliasing policies differ. When the policies match,
        // we assume that coverage AA or GPU's non-AA rasterization will apply to A and B equivalently, so
        // we can compare the original shapes. When the modes are mixed, we outset B in device space first.
        static boolean rect_contains_rect(Rect2fc a, Matrixc aToDevice, Matrixc deviceToA,
                                          Rect2fc b, Matrixc bToDevice, boolean mixedAAMode) {
            if (!mixedAAMode && Matrix.equals(aToDevice, bToDevice)) {
                // A and B are in the same coordinate space, so don't bother mapping
                return a.contains(b);
            } else if (bToDevice.isIdentity() && aToDevice.isAxisAligned()) {
                // Optimize the common case of draws (B, with identity matrix) and axis-aligned shapes,
                // instead of checking the four corners separately.
                Rect2f bInA = new Rect2f(b);
                if (mixedAAMode) {
                    bInA.inset(-0.5f, -0.5f);
                }
                boolean res = deviceToA.mapRect(bInA);
                assert res;
                return a.contains(bInA);
            }

            // Test each corner for contains; since a is convex, if all 4 corners of b's bounds are
            // contained, then the entirety of b is within a.
            Quad deviceQuad = new Quad(b, bToDevice);
            if (deviceQuad.w0() < 5e-5f ||
                    deviceQuad.w1() < 5e-5f ||
                    deviceQuad.w2() < 5e-5f ||
                    deviceQuad.w3() < 5e-5f) {
                // Something in B actually projects behind the W = 0 plane and would be clipped to infinity,
                // so it's extremely unlikely that A can contain B.
                return false;
            }

            float[] p = new float[2];
            for (int i = 0; i < 4; ++i) {
                deviceQuad.point(i, p);
                deviceToA.mapPoint(p);
                if (!a.contains(p[0], p[1])) {
                    return false;
                }
            }

            return true;
        }

        public void simplify(Rect2ic deviceBounds, boolean msaa, Rect2f outer) {
            // Then simplify the base shape, if it becomes empty, no need to update the bounds
            mRect.sort();
            if (mRect.isEmpty()) {
                return;
            }

            boolean axisAligned = mViewMatrix.mapRect(mRect, outer);
            if (!outer.intersect(deviceBounds)) {
                // A non-empty shape is offscreen, so treat it as empty
                mRect.setEmpty();
                return;
            }

            // Except for axis-aligned clip rects, upgrade to AA when forced. We skip axis-aligned clip
            // rects because a non-AA axis aligned rect can always be set as just a scissor test or window
            // rect, avoiding an expensive stencil mask generation.
            if (msaa && !axisAligned) {
                mAA = true;
            }

            // Except for non-AA axis-aligned rects, the outer bounds is the rounded-out device-space
            // mapped bounds of the shape.
            getPixelBounds(outer, mAA, true, mOuterBounds);

            if (axisAligned) {
                // The actual geometry can be updated to the device-intersected bounds and we can
                // know the inner bounds
                mRect.set(outer);
                mViewMatrix.setIdentity();
                mInverseViewMatrix.setIdentity();

                if (!mAA && outer.width() >= 1.f && outer.height() >= 1.f) {
                    outer.round(mOuterBounds);
                    mInnerBounds.set(mOuterBounds);
                } else {
                    getPixelBounds(outer, mAA, false, mInnerBounds);
                }
            }

            if (mOuterBounds.isEmpty()) {
                // This can happen if we have non-AA shapes smaller than a pixel that do not cover a pixel
                // center. We could round out, but rasterization would still result in an empty clip.
                mRect.setEmpty();
            }
        }

        private int clipType() {
            if (mRect.isEmpty()) {
                return STATE_EMPTY;
            } else {
                return mClipOp == OP_INTERSECT && mViewMatrix.isIdentity()
                        ? STATE_DEVICE_RECT : STATE_COMPLEX;
            }
        }
    }

    static void subtract(Rect2ic a, Rect2ic b, Rect2i out, boolean exact) {
        Rect2i diff = new Rect2i();
        if (Rect2i.subtract(a, b, diff) || !exact) {
            // Either A-B is exactly the rectangle stored in diff, or we don't need an exact answer
            // and can settle for the subrect of A excluded from B (which is also 'diff')
            out.set(diff);
        } else {
            // For our purposes, we want the original A when A-B cannot be exactly represented
            out.set(a);
        }
    }

    static final class SaveRecord implements Geometry {

        // Inner bounds is always contained in outer bounds, or it is empty. All bounds will be
        // contained in the device bounds.
        private final Rect2i mInnerBounds; // Inside is full coverage (stack op == intersect) or 0 cov (diff)
        private final Rect2i mOuterBounds; // Outside is 0 coverage (op == intersect) or full cov (diff)

        final int mStartingElementIndex;  // First element owned by this save record
        int mOldestValidIndex; // Index of oldest element that remains valid for this record

        // Number of save() calls without modifications (yet)
        private int mDeferredSaveCount;

        private int mState;
        private int mOp;

        SaveRecord(Rect2ic deviceBounds) {
            mInnerBounds = new Rect2i(deviceBounds);
            mOuterBounds = new Rect2i(deviceBounds);
            mStartingElementIndex = 0;
            mOldestValidIndex = 0;
            mState = STATE_WIDE_OPEN;
            mOp = OP_INTERSECT;
        }

        SaveRecord(SaveRecord prior,
                   int startingElementIndex) {
            mInnerBounds = new Rect2i(prior.mInnerBounds);
            mOuterBounds = new Rect2i(prior.mOuterBounds);
            mStartingElementIndex = startingElementIndex;
            mOldestValidIndex = prior.mOldestValidIndex;
            mState = prior.mState;
            mOp = prior.mOp;
            // If the prior record never needed a mask, this one will insert into the same index
            // (that's okay since we'll remove it when this record is popped off the stack).
            assert (startingElementIndex >= prior.mStartingElementIndex);
        }

        public int op() {
            return mOp;
        }

        public Rect2ic outerBounds() {
            return mOuterBounds;
        }

        public Rect2ic innerBounds() {
            return mInnerBounds;
        }

        public boolean contains(Geometry g) {
            if (g instanceof Draw draw) {
                return contains(draw);
            } else {
                return contains((ClipElement) g);
            }
        }

        public boolean contains(Draw draw) {
            return mInnerBounds.contains(draw.outerBounds());
        }

        public boolean contains(ClipElement element) {
            return mInnerBounds.contains(element.outerBounds());
        }

        public int firstActiveElementIndex() {
            return mStartingElementIndex;
        }

        public int oldestElementIndex() {
            return mOldestValidIndex;
        }

        public boolean canBeUpdated() {
            return mDeferredSaveCount == 0;
        }

        // Deferred save manipulation
        public void pushSave() {
            assert (mDeferredSaveCount >= 0);
            mDeferredSaveCount++;
        }

        // Returns true if the record should stay alive. False means the ClipStack must delete it
        public boolean popSave() {
            mDeferredSaveCount--;
            assert (mDeferredSaveCount >= -1);
            return mDeferredSaveCount >= 0;
        }

        // Return true if the element was added to 'elements', or otherwise affected the save record
        // (e.g. turned it empty).
        public boolean addElement(ClipElement toAdd, ArrayDeque<ClipElement> elements) {
            // Validity check the element's state first; if the shape class isn't empty, the outer bounds
            // shouldn't be empty; if the inner bounds are not empty, they must be contained in outer.
            assert ((toAdd.shape().isEmpty() || !toAdd.mOuterBounds.isEmpty()) &&
                    (toAdd.mInnerBounds.isEmpty() || toAdd.mOuterBounds.contains(toAdd.mInnerBounds)));
            // And we shouldn't be adding an element if we have a deferred save
            assert (canBeUpdated());

            if (mState == STATE_EMPTY) {
                // The clip is already empty, and we only shrink, so there's no need to record this element.
                return false;
            } else if (toAdd.shape().isEmpty()) {
                // An empty difference op should have been detected earlier, since it's a no-op
                assert (toAdd.clipOp() == OP_INTERSECT);
                mState = STATE_EMPTY;
                return true;
            }

            // In this invocation, 'A' refers to the existing stack's bounds and 'B' refers to the new
            // element.
            switch (getClipGeometry(this, toAdd)) {
                case CLIP_GEOMETRY_EMPTY:
                    // The combination results in an empty clip
                    mState = STATE_EMPTY;
                    return true;

                case CLIP_GEOMETRY_A_ONLY:
                    // The combination would not be any different than the existing clip
                    return false;

                case CLIP_GEOMETRY_B_ONLY:
                    // The combination would invalidate the entire existing stack and can be replaced with
                    // just the new element.
                    replaceWithElement(toAdd, elements);
                    return true;

                case CLIP_GEOMETRY_BOTH:
                    // The new element combines in a complex manner, so update the stack's bounds based on
                    // the combination of its and the new element's ops (handled below)
                    break;
            }

            if (mState == STATE_WIDE_OPEN) {
                // When the stack was wide open and the clip effect was kBoth, the "complex" manner is
                // simply to keep the element and update the stack bounds to be the element's intersected
                // with the device.
                replaceWithElement(toAdd, elements);
                return true;
            }

            // Some form of actual clip element(s) to combine with.
            if (mOp == OP_INTERSECT) {
                if (toAdd.op() == OP_INTERSECT) {
                    // Intersect (stack) + Intersect (toAdd)
                    //  - Bounds updates is simply the paired intersections of outer and inner.
                    boolean res = mOuterBounds.intersect(toAdd.outerBounds());
                    assert res;
                    if (!mInnerBounds.intersect(toAdd.innerBounds())) {
                        // NOTE: this does the right thing if either rect is empty, since we set the
                        // inner bounds to empty here
                        mInnerBounds.setEmpty();
                    }
                } else {
                    // Intersect (stack) + Difference (toAdd)
                    //  - Shrink the stack's outer bounds if the difference op's inner bounds completely
                    //    cuts off an edge.
                    //  - Shrink the stack's inner bounds to completely exclude the op's outer bounds.
                    subtract(mOuterBounds, toAdd.innerBounds(), mOuterBounds, /* exact */ true);
                    subtract(mInnerBounds, toAdd.outerBounds(), mInnerBounds, /* exact */ false);
                }
            } else {
                if (toAdd.op() == OP_INTERSECT) {
                    // Difference (stack) + Intersect (toAdd)
                    //  - Bounds updates are just the mirror of Intersect(stack) + Difference(toAdd)
                    Rect2i oldOuter = new Rect2i(mOuterBounds);
                    subtract(toAdd.outerBounds(), mInnerBounds, mOuterBounds, /* exact */ true);
                    subtract(toAdd.innerBounds(), oldOuter, mInnerBounds,     /* exact */ false);
                } else {
                    // Difference (stack) + Difference (toAdd)
                    //  - The updated outer bounds is the union of outer bounds and the inner becomes the
                    //    largest of the two possible inner bounds
                    mOuterBounds.join(toAdd.outerBounds());
                    if (toAdd.innerBounds().width() * toAdd.innerBounds().height() >
                            mInnerBounds.width() * mInnerBounds.height()) {
                        mInnerBounds.set(toAdd.innerBounds());
                    }
                }
            }

            // If we get here, we're keeping the new element and the stack's bounds have been updated.
            // We ought to have caught the cases where the stack bounds resemble an empty or wide open
            // clip, so assert that's the case.
            assert (!mOuterBounds.isEmpty() &&
                    (mInnerBounds.isEmpty() || mOuterBounds.contains(mInnerBounds)));

            return appendElement(toAdd, elements);
        }

        private boolean appendElement(ClipElement toAdd, ArrayDeque<ClipElement> elements) {
            // Update past elements to account for the new element
            int i = elements.size() - 1;

            // After the loop, elements between [max(youngestValid, startingIndex)+1, count-1] can be
            // removed from the stack (these are the active elements that have been invalidated by the
            // newest element; since it's the active part of the stack, no restore() can bring them back).
            int youngestValid = mStartingElementIndex - 1;
            // After the loop, elements between [0, oldestValid-1] are all invalid. The value of oldestValid
            // becomes the save record's new fLastValidIndex value.
            int oldestValid = elements.size();
            // After the loop, this is the earliest active element that was invalidated. It may be
            // older in the stack than earliestValid, so cannot be popped off, but can be used to store
            // the new element instead of allocating more.
            ClipElement oldestActiveInvalid = null;
            int oldestActiveInvalidIndex = elements.size();

            for (ClipElement existing : elements) {
                if (i < mOldestValidIndex) {
                    break;
                }
                // We don't need to pass the actual index that toAdd will be saved to; just the minimum
                // index of this save record, since that will result in the same restoration behavior later.
                existing.updateForElement(toAdd, this);

                if (toAdd.isInvalid()) {
                    if (existing.isInvalid()) {
                        // Both new and old invalid implies the entire clip becomes empty
                        mState = STATE_EMPTY;
                        return true;
                    } else {
                        // The new element doesn't change the clip beyond what the old element already does
                        return false;
                    }
                } else if (existing.isInvalid()) {
                    // The new element cancels out the old element. The new element may have been modified
                    // to account for the old element's geometry.
                    if (i >= mStartingElementIndex) {
                        // Still active, so the invalidated index could be used to store the new element
                        oldestActiveInvalid = existing;
                        oldestActiveInvalidIndex = i;
                    }
                } else {
                    // Keep both new and old elements
                    oldestValid = i;
                    if (i > youngestValid) {
                        youngestValid = i;
                    }
                }

                --i;
            }

            // Post-iteration validity check
            assert (oldestValid == elements.size() ||
                    (oldestValid >= mOldestValidIndex && oldestValid < elements.size()));
            assert (youngestValid == mStartingElementIndex - 1 ||
                    (youngestValid >= mStartingElementIndex && youngestValid < elements.size()));
            assert (oldestActiveInvalid == null || (oldestActiveInvalidIndex >= mStartingElementIndex &&
                    oldestActiveInvalidIndex < elements.size()));

            // Update final state
            assert (oldestValid >= mOldestValidIndex);
            mOldestValidIndex = Math.min(oldestValid, oldestActiveInvalidIndex);
            mState = oldestValid == elements.size() ? toAdd.clipType() : STATE_COMPLEX;
            if (mOp == OP_DIFFERENCE && toAdd.op() == OP_INTERSECT) {
                // The stack remains in difference mode only as long as all elements are difference
                mOp = OP_INTERSECT;
            }

            int targetCount = youngestValid + 1;
            if (oldestActiveInvalid == null || oldestActiveInvalidIndex >= targetCount) {
                // toAdd will be stored right after youngestValid
                targetCount++;
                oldestActiveInvalid = null;
            }
            while (elements.size() > targetCount) {
                assert (oldestActiveInvalid != elements.peek()); // shouldn't delete what we'll reuse
                elements.pop();
            }
            if (oldestActiveInvalid != null) {
                oldestActiveInvalid.set(toAdd);
            } else if (elements.size() < targetCount) {
                elements.push(new ClipElement(toAdd));
            } else {
                elements.element().set(toAdd);
            }

            return true;
        }

        private void replaceWithElement(ClipElement toAdd, ArrayDeque<ClipElement> elements) {
            // The aggregate state of the save record mirrors the element
            mInnerBounds.set(toAdd.mInnerBounds);
            mOuterBounds.set(toAdd.mOuterBounds);

            mOp = toAdd.clipOp();
            mState = toAdd.clipType();

            // All prior active element can be removed from the stack: [startingIndex, count - 1]
            int targetCount = mStartingElementIndex + 1;
            while (elements.size() > targetCount) {
                elements.pop();
            }
            if (elements.size() < targetCount) {
                elements.push(new ClipElement(toAdd));
            } else {
                elements.element().set(toAdd);
            }
        }

        public void removeElements(ArrayDeque<ClipElement> elements) {
            while (elements.size() > mStartingElementIndex) {
                elements.pop();
            }
        }

        public void restoreElements(ArrayDeque<ClipElement> elements) {
            // Presumably this SaveRecord is the new top of the stack, and so it owns the elements
            // from its starting index to restoreCount - 1. Elements from the old save record have
            // been destroyed already, so their indices would have been >= restoreCount, and any
            // still-present element can be un-invalidated based on that.
            int i = elements.size() - 1;
            for (ClipElement e : elements) {
                if (i < mOldestValidIndex) {
                    break;
                }
                e.restoreValid(this);
                --i;
            }
        }
    }

    static class Draw implements Geometry {

        final Rect2f mOriginalBounds = new Rect2f();
        final Rect2i mBounds = new Rect2i();
        final Rect2f mTmpBounds = new Rect2f();

        boolean mAA;

        public Draw init(Rect2fc drawBounds, boolean aa) {
            getPixelBounds(drawBounds, aa, true, mBounds);
            mAA = aa;
            // Be slightly more forgiving on whether or not a draw is inside a clip element.
            mOriginalBounds.set(drawBounds);
            mOriginalBounds.inset(kBoundsTolerance, kBoundsTolerance);
            if (mOriginalBounds.isEmpty()) {
                mOriginalBounds.set(drawBounds);
            }
            mTmpBounds.set(mBounds);
            return this;
        }

        @Override
        public int op() {
            return OP_INTERSECT;
        }

        @Override
        public Rect2ic outerBounds() {
            return mBounds;
        }

        @Override
        public boolean contains(Geometry other) {
            // Draw does not have inner bounds so cannot contain anything.
            assert other instanceof SaveRecord || other instanceof ClipElement;
            return false;
        }

        public Rect2fc bounds() {
            return mOriginalBounds;
        }
    }
}
