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

package icyllis.arc3d.engine.graphene;

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.geom.BoundsManager;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * GPU hierarchical clipping.
 * <p>
 * We use scissor test and depth test to apply clip, antialiasing only works on
 * multisampled targets.
 */
public final class ClipStack {

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

    private final Device mDevice;
    private final Rect2i mDeviceBounds;
    private final Rect2f mDeviceBoundsF;

    public ClipStack(Device device) {
        mDevice = device;
        mDeviceBounds = new Rect2i(device.bounds());
        mDeviceBoundsF = new Rect2f(device.bounds());
        mSaves.add(new SaveRecord(device.bounds()));
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
        current.removeElements(mElements, mDevice);
        mSaves.pop();
        // Restore any remaining elements that were only invalidated by the now-removed save record.
        mSaves.element().restoreElements(mElements);
    }

    @UnmodifiableView
    public Collection<Element> elements() {
        return mElementsView;
    }

    private final ClipElement mTmpElement = new ClipElement();

    public void clipRect(@Nullable Matrix4c viewMatrix,
                         @Nonnull Rect2fc localRect,
                         int clipOp) {
        clip(mTmpElement.init(
                mDeviceBounds,
                localRect,
                false,
                viewMatrix,
                clipOp
        ));
    }

    // other clip shapes and clip shader are not supported
    // you can use post-processing effect to achieve that

    private void clip(ClipElement element) {
        if (mSaves.element().mState == STATE_EMPTY) {
            return;
        }

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
        if (!current.addElement(element, mElements, mDevice)) {
            if (wasDeferred) {
                // We made a new save record, but ended up not adding an element to the stack.
                // So instead of keeping an empty save record around, pop it off and restore the counter
                assert (elementCount == mElements.size());
                mSaves.pop();
                mSaves.element().pushSave();
            }
        }
    }

    public void getConservativeBounds(Rect2f out) {
        SaveRecord current = mSaves.element();
        if (current.mState == STATE_EMPTY) {
            out.setEmpty();
        } else if (current.mState == STATE_WIDE_OPEN) {
            out.set(mDeviceBounds);
        } else {
            if (current.op() == OP_DIFFERENCE) {
                subtract(mDeviceBoundsF, current.mInnerBounds, out, true);
            } else {
                assert (mDeviceBounds.contains(current.outerBounds()));
                out.set(current.mOuterBounds);
            }
        }
    }

    private final ClipDraw mTmpDraw = new ClipDraw();

    // Compute the bounds and the effective elements of the clip stack when applied to the draw
    // described by the provided transform, shape, and stroke.
    //
    // Applying clips to a draw is a mostly lazy operation except for what is returned:
    //  - The Clip's scissor is set to 'conservativeBounds()'.
    //  - The Clip stores the draw's clipped bounds, taking into account its transform, styling, and
    //    the above scissor.
    //  - The Clip also stores the draw's fill-style invariant clipped bounds which is used in atlas
    //    draws and may differ from the draw bounds.
    //
    // All clip elements that affect the draw will be returned in `outEffectiveElements` alongside
    // the bounds. This method does not have any side-effects and the per-clip element state has to
    // be explicitly updated by calling `updateClipStateForDraw()` which prepares the clip stack for
    // later rendering.
    //
    // The returned clip element list will be empty if the shape is clipped out or if the draw is
    // unaffected by any of the clip elements.
    public boolean prepareForDraw(DrawOp draw,
                                  Rect2f shapeBounds,
                                  boolean hasAABloat,
                                  List<Element> elementsForMask) {

        SaveRecord save = mSaves.element();
        if (save.mState == STATE_EMPTY) {
            // We know the draw is clipped out so don't bother computing the base draw bounds.
            return true;
        }

        if (!shapeBounds.isFinite()) {
            return true;
        }

        boolean infiniteBounds = false;

        // Some renderers make the drawn area larger than the geometry for anti-aliasing
        float rendererOutset = hasAABloat
                ? draw.mTransform.localAARadius(shapeBounds)
                : 0;

        Rect2f transformedShapeBounds = new Rect2f();
        boolean shapeInDeviceSpace = false;

        Rect2fc deviceBounds = mDeviceBoundsF;

        if (!Float.isFinite(rendererOutset)) {
            transformedShapeBounds.set(deviceBounds);
            infiniteBounds = true;
        } else {
            // Will be in device space once style/AA outsets and the localToDevice transform are
            // applied.
            transformedShapeBounds.set(shapeBounds);

            // Not hairline
            if (draw.mStrokeRadius != 0.0f || rendererOutset != 0.0f) {
                float localStyleOutset = draw.getInflationRadius() + rendererOutset;
                transformedShapeBounds.outset(localStyleOutset, localStyleOutset);

                // Not fill
                if (draw.mStrokeRadius > 0.0f || rendererOutset != 0.0f) {
                    // While this loses any shape type, the bounds remain local so hopefully tests are
                    // fairly accurate.
                    shapeBounds.set(transformedShapeBounds);
                }
            }

            draw.mTransform.mapRect(transformedShapeBounds);

            // Hairlines get an extra pixel *after* transforming to device space, unless the renderer
            // has already defined an outset
            if (draw.mStrokeRadius == 0.0 && rendererOutset == 0.0f) {
                transformedShapeBounds.outset(0.5f, 0.5f);
                // and the associated transform must be kIdentity since the bounds have been mapped by
                // localToDevice already.
                shapeBounds.set(transformedShapeBounds);
                shapeInDeviceSpace = true;
            }

            // Restrict bounds to the device limits.
            if (!transformedShapeBounds.intersect(deviceBounds)) {
                transformedShapeBounds.setEmpty();
            }
        }

        Rect2f drawBounds = new Rect2f();  // defined in device space
        if (infiniteBounds) {
            drawBounds.set(deviceBounds);
            shapeBounds.set(drawBounds);
            shapeInDeviceSpace = true;
        } else {
            drawBounds.set(transformedShapeBounds);
        }

        if (drawBounds.isEmpty()) {
            // clipped out
            return true;
        }
        if (save.mState == STATE_WIDE_OPEN) {
            draw.mDrawBounds = drawBounds;
            draw.mTransformedShapeBounds = transformedShapeBounds;
            draw.mScissorRect = mDeviceBounds;
            return false;
        }

        // We don't evaluate Simplify() on the SaveRecord and the draw because a reduced version of
        // Simplify is effectively performed in computing the scissor rect.
        // Given that, we can skip iterating over the clip elements when:
        //  - the draw's *scissored* bounds are empty, which happens when the draw was clipped out.
        //  - the scissored bounds are contained in our inner bounds, which happens if all we need to
        //    apply to the draw is the computed scissor rect.
        // TODO: The Clip's scissor is defined in terms of integer pixel coords, but if we move to
        // clip plane distances in the vertex shader, it can be defined in terms of the original float
        // coordinates.
        Rect2ic scissor = save.scissor(mDeviceBounds, drawBounds);
        if (!drawBounds.intersect(scissor)) {
            // clipped out
            return true;
        }
        if (!transformedShapeBounds.intersect(scissor)) {
            transformedShapeBounds.setEmpty(); // do we really need this?
        }
        if (!save.innerBounds().contains(drawBounds)) {

            // If we made it here, the clip stack affects the draw in a complex way so iterate each element.
            // A draw is a transformed shape that "intersects" the clip. We use empty inner bounds because
            // there's currently no way to re-write the draw as the clip's geometry, so there's no need to
            // check if the draw contains the clip (vice versa is still checked and represents an unclipped
            // draw so is very useful to identify).
            assert elementsForMask.isEmpty();

            mTmpDraw.init(
                    shapeInDeviceSpace ? Matrix4.identity() : draw.mTransform,
                    shapeBounds,
                    drawBounds
            );

            int i = mElements.size();
            for (ClipElement e : mElements) {
                --i;
                if (i < save.oldestElementIndex()) {
                    // All earlier elements have been invalidated by elements already processed
                    break;
                } else if (e.isInvalid()) {
                    continue;
                }

                switch (getClipGeometry(e, mTmpDraw)) {
                    case CLIP_GEOMETRY_EMPTY:
                        // This can happen for difference op elements that have a larger fInnerBounds than
                        // can be preserved at the next level.
                        elementsForMask.clear();
                        return true;
                    case CLIP_GEOMETRY_B_ONLY:
                        // We don't need to produce a mask for the element
                        break;
                    case CLIP_GEOMETRY_A_ONLY:
                        // Shouldn't happen for draws, fall through to regular element processing
                        assert false;
                    case CLIP_GEOMETRY_BOTH: {
                        // First apply using HW methods (scissor and window rects). When the inner and outer
                        // bounds match, nothing else needs to be done.
                        boolean fullyApplied = false;

                        /*if (e.op() == OP_INTERSECT) {
                            // The second test allows clipped draws that are scissored by multiple
                            // elements to remain scissor-only.
                            fullyApplied = e.innerBounds() == e.outerBounds() ||
                                    e.innerBounds().contains(scissor);
                        }*/

                        if (!fullyApplied) {
                            elementsForMask.add(e);
                        }

                        break;
                    }
                }
            }

        }

        draw.mDrawBounds = drawBounds;
        draw.mTransformedShapeBounds = transformedShapeBounds;
        draw.mScissorRect = scissor;
        return false;
    }

    public int updateForDraw(DrawOp draw,
                             List<Element> elementsForMask,
                             BoundsManager boundsManager,
                             int depth) {
        if (draw.isClippedOut()) {
            return DrawOrder.MIN_VALUE;
        }

        assert mSaves.element().mState != STATE_EMPTY;

        int maxClipOrder = DrawOrder.MIN_VALUE;
        for (Element element : elementsForMask) {
            ClipElement e = (ClipElement) element;
            int order = e.updateForDraw(boundsManager, draw.mDrawBounds, depth);
            maxClipOrder = Math.max(maxClipOrder, order);
        }

        return maxClipOrder;
    }

    public void recordDeferredClipDraws() {
        for (var e : mElements) {
            // When a Device requires all clip elements to be recorded, we have to iterate all elements,
            // and will draw clip shapes for elements that are still marked as invalid from the clip
            // stack, including those that are older than the current save record's oldest valid index,
            // because they could have accumulated draw usage prior to being invalidated, but weren't
            // flushed when they were invalidated because of an intervening save.
            e.drawClip(mDevice);
        }
    }

    public interface ClipGeometry {

        int op();

        Rect2fc shape();

        Matrix4c viewMatrix();

        Rect2fc outerBounds();

        boolean contains(ClipGeometry other);
    }

    public static boolean intersects(
            ClipGeometry A,
            ClipGeometry B) {
        if (!Rect2f.intersects(
                A.outerBounds(),
                B.outerBounds())) {
            return false;
        }

        if (A.viewMatrix().isAxisAligned() &&
                B.viewMatrix().isAxisAligned()) {
            // The two shape's coordinate spaces are different but both rect-stays-rect or simpler.
            // This means, though, that their outer bounds approximations are tight to their transormed
            // shape bounds. There's no point to do further tests given that and that we already found
            // that these outer bounds *do* intersect.
            return true;
        } else if (A.viewMatrix().equals(B.viewMatrix())) {
            // Since the two shape's local coordinate spaces are the same, we can compare shape
            // bounds directly for a more accurate intersection test. We intentionally do not go
            // further and do shape-specific intersection tests since these could have unknown
            // complexity (for paths) and limited utility (e.g. two round rects that are disjoint
            // solely from their corner curves).
            return A.shape().intersects(B.shape());
        }
        //TODO handle oriented box if non-perspective
        return true;
    }

    // This captures which of the two elements in (A op B) would be required when they are combined,
    // where op is intersect or difference.
    public static final int
            CLIP_GEOMETRY_EMPTY = 0,
            CLIP_GEOMETRY_A_ONLY = 1,
            CLIP_GEOMETRY_B_ONLY = 2,
            CLIP_GEOMETRY_BOTH = 3;

    // ClipElement <-> ClipElement
    // ClipElement <-> ClipDraw
    // SaveRecord <-> ClipElement
    public static int getClipGeometry(
            ClipGeometry A,
            ClipGeometry B) {

        if (A.op() == OP_INTERSECT) {

            if (B.op() == OP_INTERSECT) {

                // Intersect (A) + Intersect (B)
                if (!intersects(A, B)) {
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
                if (!intersects(A, B)) {
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
                if (!intersects(A, B)) {
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

        // owned memory
        final Rect2f mShape;
        final Matrix4 mViewMatrix;
        int mClipOp;

        Element() {
            mShape = new Rect2f();
            mViewMatrix = new Matrix4();
        }

        Element(Rect2fc shape, Matrix4c viewMatrix, int clipOp) {
            mShape = new Rect2f(shape);
            mViewMatrix = new Matrix4(viewMatrix);
            mClipOp = clipOp;
        }

        // local rect
        // do not modify
        public Rect2fc shape() {
            return mShape;
        }

        // local to device
        // do not modify
        public Matrix4c viewMatrix() {
            return mViewMatrix;
        }

        public int clipOp() {
            return mClipOp;
        }

        @Override
        public String toString() {
            return "Element{" +
                    "mShape=" + mShape +
                    ", mViewMatrix=" + mViewMatrix +
                    ", mClipOp=" + (mClipOp == OP_INTERSECT ? "Intersect" : "Difference") +
                    '}';
        }
    }

    // Implements the geometric Element data with logic for containment and bounds testing.
    static final class ClipElement extends Element implements ClipGeometry {

        boolean mInverseFill;

        // cached inverse of fLocalToDevice for contains() optimization
        final Matrix4 mInverseViewMatrix = new Matrix4();

        // Device space bounds. These bounds are not snapped to pixels with the assumption that if
        // a relation (intersects, contains, etc.) is true for the bounds it will be true for the
        // rasterization of the coordinates that produced those bounds.
        final Rect2f mInnerBounds = new Rect2f();
        final Rect2f mOuterBounds = new Rect2f();

        // State tracking how this clip element needs to be recorded into the draw context. As the
        // clip stack is applied to additional draws, the clip's Z and usage bounds grow to account
        // for it; its compressed painter's order is selected the first time a draw is affected.
        final Rect2f mUsageBounds = new Rect2f();
        int mPaintersOrder = DrawOrder.MIN_VALUE;
        int mMaxDepth = DrawOrder.MIN_VALUE;

        // Elements are invalidated by SaveRecords as the record is updated with new elements that
        // override old geometry. An invalidated element stores the index of the first element of
        // the save record that invalidated it. This makes it easy to undo when the save record is
        // popped from the stack, and is stable as the current save record is modified.
        int mInvalidatedByIndex = -1;

        public ClipElement() {
        }

        public ClipElement(ClipElement e) {
            super(e.shape(), e.viewMatrix(), e.clipOp());
            mInverseFill = e.mInverseFill;
            mInverseViewMatrix.set(e.mInverseViewMatrix);
            mInnerBounds.set(e.mInnerBounds);
            mOuterBounds.set(e.mOuterBounds);
            mUsageBounds.set(e.mUsageBounds);
            mPaintersOrder = e.mPaintersOrder;
            mMaxDepth = e.mMaxDepth;
            mInvalidatedByIndex = e.mInvalidatedByIndex;
        }

        public void set(ClipElement e) {
            mShape.set(e.shape());
            mViewMatrix.set(e.viewMatrix());
            mClipOp = e.clipOp();
            mInverseFill = e.mInverseFill;
            mInverseViewMatrix.set(e.mInverseViewMatrix);
            mInnerBounds.set(e.mInnerBounds);
            mOuterBounds.set(e.mOuterBounds);
            mUsageBounds.set(e.mUsageBounds);
            mPaintersOrder = e.mPaintersOrder;
            mMaxDepth = e.mMaxDepth;
            mInvalidatedByIndex = e.mInvalidatedByIndex;
        }

        // init and simplify
        public ClipElement init(Rect2ic deviceBounds,
                                Rect2fc shape,
                                boolean inverseFill,
                                Matrix4c viewMatrix,
                                int clipOp) {
            mShape.set(shape);
            mViewMatrix.set(viewMatrix);
            mClipOp = clipOp;
            mUsageBounds.setEmpty();
            mPaintersOrder = DrawOrder.MIN_VALUE;
            mMaxDepth = DrawOrder.MIN_VALUE;
            mInvalidatedByIndex = -1;

            if (!viewMatrix.invert(mInverseViewMatrix)) {
                // If the transform can't be inverted, it means that two dimensions are collapsed to 0 or
                // 1 dimension, making the device-space geometry effectively empty.
                mShape.setEmpty();
                mInverseViewMatrix.setIdentity();
            }

            // Make sure the shape is not inverted. An inverted shape is equivalent to a non-inverted shape
            // with the clip op toggled.
            if (inverseFill) {
                mClipOp = (mClipOp == ClipOp.CLIP_OP_INTERSECT) ? ClipOp.CLIP_OP_DIFFERENCE : ClipOp.CLIP_OP_INTERSECT;
            }

            mInnerBounds.setEmpty();
            mViewMatrix.mapRect(mShape, mOuterBounds);
            if (!mOuterBounds.intersect(deviceBounds)) {
                mOuterBounds.setEmpty();
            }

            if (!mOuterBounds.isEmpty() &&
                    mViewMatrix.isAxisAligned()) {
                // The actual geometry can be updated to the device-intersected bounds and we can
                // know the inner bounds
                mShape.set(mOuterBounds);
                mViewMatrix.setIdentity();
                mInverseViewMatrix.setIdentity();
                mInnerBounds.set(mOuterBounds);
            }

            if (mOuterBounds.isEmpty()) {
                // This can happen if we have non-AA shapes smaller than a pixel that do not cover a pixel
                // center. We could round out, but rasterization would still result in an empty clip.
                mShape.setEmpty();
                mInnerBounds.setEmpty();
            }

            // Now that mClipOp and mShape are canonical, set the shape's fill type to match how it needs to be
            // drawn as a depth-only shape everywhere that is clipped out (intersect is thus inverse-filled)
            mInverseFill = (mClipOp == ClipOp.CLIP_OP_INTERSECT);

            assert (mShape.isEmpty() || deviceBounds.contains(mOuterBounds));
            assert validate();

            return this;
        }

        public boolean hasPendingDraw() {
            return mPaintersOrder != DrawOrder.MIN_VALUE;
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
            // NOTE: We don't draw the accumulated clip usage when the element is marked invalid. Some
            // invalidated elements are part of earlier save records so can become re-active after a restore
            // in which case they should continue to accumulate. Invalidated elements that are part of the
            // active save record are removed at the end of the stack modification, which is when they are
            // explicitly drawn.
        }

        public void restoreValid(SaveRecord current) {
            if (current.firstActiveElementIndex() < mInvalidatedByIndex) {
                mInvalidatedByIndex = -1;
            }
        }

        public boolean combine(ClipElement other, SaveRecord current) {
            // Don't combine elements that have collected draw usage, since that changes their geometry.
            if (hasPendingDraw() || other.hasPendingDraw()) {
                return false;
            }
            // To reduce the number of possibilities, only consider intersect+intersect. Difference and
            // mixed op cases could be analyzed to simplify one of the shapes, but that is a rare
            // occurrence and the math is much more complicated.
            if (other.mClipOp != OP_INTERSECT || mClipOp != OP_INTERSECT) {
                return false;
            }

            // At the moment, only rect+rect or rrect+rrect are supported (although rect+rrect is
            // treated as a degenerate case of rrect+rrect).
            boolean shapeUpdated = false;
            //TODO support roundrect
            if (mViewMatrix.equals(other.mViewMatrix)) {
                if (!mShape.intersect(other.mShape)) {
                    // By floating point, it turns out the combination should be empty
                    mShape.setEmpty();
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
                assert res; // Inner bounds can become empty, but outer bounds should not be able to.
                if (!mInnerBounds.intersect(other.mInnerBounds)) {
                    mInnerBounds.setEmpty();
                }
                mInverseFill = true;
                assert validate();
                return true;
            } else {
                return false;
            }
        }

        // 'added' represents a new op added to the element stack. Its combination with this element
        // can result in a number of possibilities:
        //  1. The entire clip is empty (signaled by both this and 'added' being invalidated).
        //  2. The 'added' op supercedes this element (this element is invalidated).
        //  3. This op supercedes the 'added' element (the added element is marked invalidated).
        //  4. Their combination can be represented by a single new op (in which case this
        //     element should be invalidated, and the combined shape stored in 'added').
        //  5. Or both elements remain needed to describe the clip (both are valid and unchanged).
        //
        // The calling element will only modify its invalidation index since it could belong
        // to part of the inactive stack (that might be restored later). All merged state/geometry
        // is handled by modifying 'added'.
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

        // Updates usage tracking to incorporate the bounds and Z value for the new draw call.
        // If this element hasn't affected any prior draws, it will use the bounds manager to
        // assign itself a compressed painters order for later rendering.
        //
        // This method assumes that this element affects the draw in a complex way, such that
        // calling `testForDraw()` on the same draw would return `DrawInfluence::kIntersect`. It is
        // assumed that `testForDraw()` was called beforehand to ensure that this is the case.
        //
        // Assuming that this element does not clip out the draw, returns the painters order the
        // draw must sort after.
        public int updateForDraw(BoundsManager boundsManager,
                                 Rect2fc drawBounds,
                                 int drawDepth) {
            assert (!isInvalid());
            assert (!drawBounds.isEmpty());

            if (!hasPendingDraw()) {
                // No usage yet so we need an order that we will use when drawing to just the depth
                // attachment. It is sufficient to use the next CompressedPaintersOrder after the
                // most recent draw under this clip's outer bounds. It is necessary to use the
                // entire clip's outer bounds because the order has to be determined before the
                // final usage bounds are known and a subsequent draw could require a completely
                // different portion of the clip than this triggering draw.
                //
                // Lazily determining the order has several benefits to computing it when the clip
                // element was first created:
                //  - Elements that are invalidated by nested clips before draws are made do not
                //    waste time in the BoundsManager.
                //  - Elements that never actually modify a draw (e.g. a defensive clip) do not
                //    waste time in the BoundsManager.
                //  - A draw that triggers clip usage on multiple elements will more likely assign
                //    the same order to those elements, meaning their depth-only draws are more
                //    likely to batch in the final DrawPass.
                //
                // However, it does mean that clip elements can have the same order as each other,
                // or as later draws (e.g. after the clip has been popped off the stack). Any
                // overlap between clips or draws is addressed when the clip is drawn by selecting
                // an appropriate DisjointStencilIndex value. Stencil-aside, this order assignment
                // logic, max Z tracking, and the depth test during rasterization are able to
                // resolve everything correctly even if clips have the same order value.
                // See go/clip-stack-order for a detailed analysis of why this works.
                mPaintersOrder = boundsManager.getMostRecentDraw(mOuterBounds) + 1;
                mUsageBounds.set(drawBounds);
                mMaxDepth = drawDepth;
            } else {
                // Earlier draws have already used this element so we cannot change where the
                // depth-only draw will be sorted to, but we need to ensure we cover the new draw's
                // bounds and use a Z value that will clip out its pixels as appropriate.
                mUsageBounds.join(drawBounds);
                mMaxDepth = Math.max(mMaxDepth, drawDepth);
            }

            return mPaintersOrder;
        }

        // Record a depth-only draw to the given device, restricted to the portion of the clip that
        // is actually required based on prior recorded draws. Resets usage tracking for subsequent
        // passes.
        public void drawClip(Device device) {
            assert validate();

            // Skip elements that have not affected any draws
            if (!hasPendingDraw()) {
                assert (mUsageBounds.isEmpty());
                return;
            }

            assert (!mUsageBounds.isEmpty());
            // For clip draws, the usage bounds is the scissor.
            var scissor = new Rect2i();
            mUsageBounds.roundOut(scissor);
            var drawBounds = new Rect2f(mOuterBounds);
            if (drawBounds.intersect(scissor)) {
                long order = DrawOrder.makeFromDepthAndPaintersOrder(
                        mMaxDepth + 1, mPaintersOrder
                );
                DrawOp draw = new DrawOp();
                draw.mTransform = mViewMatrix.clone();
                draw.mGeometry = new Rect2f(mShape);
                draw.mDrawBounds = drawBounds;
                draw.mTransformedShapeBounds = drawBounds;
                draw.mScissorRect = scissor;
                draw.mDrawOrder = order;
                // An element's clip op is encoded in the shape's fill type. Inverse fills are intersect ops
                // and regular fills are difference ops. This means fShape is already in the right state to
                // draw directly.
                assert ((mClipOp == ClipOp.CLIP_OP_DIFFERENCE && !mInverseFill) ||
                        (mClipOp == ClipOp.CLIP_OP_INTERSECT && mInverseFill));
                device.drawClipShape(draw, mInverseFill);
            }

            // After the clip shape is drawn, reset its state. If the clip element is being popped off the
            // stack or overwritten because a new clip invalidated it, this won't matter. But if the clips
            // were drawn because the Device had to flush pending work while the clip stack was not empty,
            // subsequent draws will still need to be clipped to the elements. In this case, the usage
            // accumulation process will begin again and automatically use the Device's post-flush Z values
            // and BoundsManager state.
            mUsageBounds.setEmpty();
            mPaintersOrder = DrawOrder.MIN_VALUE;
            mMaxDepth = DrawOrder.MIN_VALUE;
        }

        public int op() {
            return mClipOp;
        }

        public Rect2fc innerBounds() {
            return mInnerBounds;
        }

        // reference to unmodifiable rect
        public Rect2fc outerBounds() {
            return mOuterBounds;
        }

        public boolean contains(ClipGeometry g) {
            if (mInnerBounds.contains(g.outerBounds())) {
                return true;
            }
            if (!mOuterBounds.contains(g.outerBounds())) {
                return false;
            }

            if (mViewMatrix.equals(g.viewMatrix())) {
                // A and B are in the same coordinate space, so don't bother mapping
                return mShape.contains(g.shape());
            }

            if (mViewMatrix.isAxisAligned() && g.viewMatrix().isAxisAligned()) {
                // Optimize the common case of draws (B, with identity matrix) and axis-aligned shapes,
                // instead of checking the four corners separately.
                Rect2f localBounds = new Rect2f(g.shape());
                g.viewMatrix().mapRect(localBounds);
                mInverseViewMatrix.mapRect(localBounds);
                return mShape.contains(localBounds);
            }

            //TODO port from rect_contains_rect for convex

            return false;
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

        private int clipType() {
            if (mShape.isEmpty()) {
                return STATE_EMPTY;
            } else {
                return mClipOp == OP_INTERSECT && mViewMatrix.isIdentity()
                        ? STATE_DEVICE_RECT : STATE_COMPLEX;
            }
        }

        private boolean validate() {
            assert ((mShape.isEmpty() || !mOuterBounds.isEmpty()) &&
                    (mInnerBounds.isEmpty() || mOuterBounds.contains(mInnerBounds)));
            assert ((mClipOp == ClipOp.CLIP_OP_DIFFERENCE && !mInverseFill) ||
                    (mClipOp == ClipOp.CLIP_OP_INTERSECT && mInverseFill));
            assert (!hasPendingDraw() || !mUsageBounds.isEmpty());
            return true;
        }
    }

    static void subtract(Rect2fc a, Rect2fc b, Rect2f out, boolean exact) {
        Rect2f diff = new Rect2f();
        if (Rect2f.subtract(a, b, diff) || !exact) {
            // Either A-B is exactly the rectangle stored in diff, or we don't need an exact answer
            // and can settle for the subrect of A excluded from B (which is also 'diff')
            out.set(diff);
        } else {
            // For our purposes, we want the original A when A-B cannot be exactly represented
            out.set(a);
        }
    }

    static final class SaveRecord implements ClipGeometry {

        // Inner bounds is always contained in outer bounds, or it is empty. All bounds will be
        // contained in the device bounds.
        private final Rect2f mInnerBounds; // Inside is full coverage (stack op == intersect) or 0 cov (diff)
        private final Rect2f mOuterBounds; // Outside is 0 coverage (op == intersect) or full cov (diff)

        final int mStartingElementIndex;  // First element owned by this save record
        int mOldestValidIndex; // Index of oldest element that remains valid for this record

        // Number of save() calls without modifications (yet)
        private int mDeferredSaveCount;

        private int mState;
        private int mOp;

        SaveRecord(Rect2ic deviceBounds) {
            mInnerBounds = new Rect2f(deviceBounds);
            mOuterBounds = new Rect2f(deviceBounds);
            mStartingElementIndex = 0;
            mOldestValidIndex = 0;
            mState = STATE_WIDE_OPEN;
            mOp = OP_INTERSECT;
        }

        SaveRecord(SaveRecord prior,
                   int startingElementIndex) {
            mInnerBounds = new Rect2f(prior.mInnerBounds);
            mOuterBounds = new Rect2f(prior.mOuterBounds);
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

        @Override
        public Rect2fc shape() {
            return mOuterBounds;
        }

        @Override
        public Matrix4c viewMatrix() {
            return Matrix4.identity();
        }

        public Rect2fc outerBounds() {
            return mOuterBounds;
        }

        public Rect2fc innerBounds() {
            return mInnerBounds;
        }

        public boolean contains(ClipGeometry g) {
            assert g instanceof ClipElement || g instanceof ClipDraw;
            return mInnerBounds.contains(g.outerBounds());
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
        public boolean addElement(ClipElement toAdd, ArrayDeque<ClipElement> elements, Device device) {
            // Validity check the element's state first; if the shape class isn't empty, the outer bounds
            // shouldn't be empty; if the inner bounds are not empty, they must be contained in outer.
            assert (toAdd.validate());
            // And we shouldn't be adding an element if we have a deferred save
            assert (canBeUpdated());

            if (mState == STATE_EMPTY) {
                // The clip is already empty, and we only shrink, so there's no need to record this element.
                return false;
            } else if (toAdd.shape().isEmpty()) {
                // An empty difference op should have been detected earlier, since it's a no-op
                assert (toAdd.clipOp() == OP_INTERSECT);
                mState = STATE_EMPTY;
                removeElements(elements, device);
                return true;
            }

            // In this invocation, 'A' refers to the existing stack's bounds and 'B' refers to the new
            // element.
            switch (getClipGeometry(this, toAdd)) {
                case CLIP_GEOMETRY_EMPTY:
                    // The combination results in an empty clip
                    mState = STATE_EMPTY;
                    removeElements(elements, device);
                    return true;

                case CLIP_GEOMETRY_A_ONLY:
                    // The combination would not be any different than the existing clip
                    return false;

                case CLIP_GEOMETRY_B_ONLY:
                    // The combination would invalidate the entire existing stack and can be replaced with
                    // just the new element.
                    replaceWithElement(toAdd, elements, device);
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
                replaceWithElement(toAdd, elements, device);
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
                    Rect2f oldOuter = new Rect2f(mOuterBounds);
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

            return appendElement(toAdd, elements, device);
        }

        private boolean appendElement(ClipElement toAdd, ArrayDeque<ClipElement> elements, Device device) {
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
                elements.pop().drawClip(device);
            }
            if (oldestActiveInvalid != null) {
                oldestActiveInvalid.drawClip(device);
                oldestActiveInvalid.set(toAdd);
            } else if (elements.size() < targetCount) {
                elements.push(new ClipElement(toAdd));
            } else {
                elements.element().drawClip(device);
                elements.element().set(toAdd);
            }

            return true;
        }

        private void replaceWithElement(ClipElement toAdd, ArrayDeque<ClipElement> elements, Device device) {
            // The aggregate state of the save record mirrors the element
            mInnerBounds.set(toAdd.mInnerBounds);
            mOuterBounds.set(toAdd.mOuterBounds);

            mOp = toAdd.clipOp();
            mState = toAdd.clipType();

            // All prior active element can be removed from the stack: [startingIndex, count - 1]
            int targetCount = mStartingElementIndex + 1;
            while (elements.size() > targetCount) {
                elements.pop().drawClip(device);
            }
            if (elements.size() < targetCount) {
                elements.push(new ClipElement(toAdd));
            } else {
                elements.element().drawClip(device);
                elements.element().set(toAdd);
            }

            assert (elements.size() == mStartingElementIndex + 1);

            // This invalidates all older elements that are owned by save records lower in the clip stack.
            mOldestValidIndex = mStartingElementIndex;
        }

        public void removeElements(ArrayDeque<ClipElement> elements,
                                   Device device) {
            while (elements.size() > mStartingElementIndex) {
                // Since the element is being deleted now, it won't be in the ClipStack when the Device
                // calls recordDeferredClipDraws(). Record the clip's draw now (if it needs it).
                elements.pop().drawClip(device);
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

        // return value must be immutable, deviceBounds is immutable
        public Rect2ic scissor(Rect2ic deviceBounds, Rect2fc drawBounds) {
            // This should only be called when the clip stack actually has something non-trivial to evaluate
            // It is effectively a reduced version of Simplify() dealing only with device-space bounds and
            // returning the intersection results.
            assert (mState != STATE_EMPTY && mState != STATE_WIDE_OPEN);
            assert (deviceBounds.contains(drawBounds)); // This should have already been handled.
            if (mOp == OP_INTERSECT) {
                // kIntersect nominally uses the save record's outer bounds as the scissor. However, if the
                // draw is contained entirely within those bounds, it doesn't have any visual effect so
                // switch to using the device bounds as the canonical scissor to minimize state changes.
                if (mOuterBounds.contains(drawBounds)) {
                    // device bounds never change
                    return deviceBounds;
                } else {
                    // This automatically detects the case where the draw does not intersect the clip.
                    var res = new Rect2i();
                    mOuterBounds.roundOut(res);
                    return res;
                }
            } else {
                // kDifference nominally uses the draw's bounds minus the save record's inner bounds as the
                // scissor. However, if the draw doesn't intersect the clip at all then it doesn't have any
                // visual effect and we can switch to the device bounds as the canonical scissor.
                if (!mOuterBounds.intersects(drawBounds)) {
                    return deviceBounds;
                } else {
                    // This automatically detects the case where the draw is contained in inner bounds and
                    // would be entirely clipped out.
                    var diff = new Rect2f();
                    var res = new Rect2i();
                    if (Rect2f.subtract(drawBounds, mInnerBounds, diff)) {
                        diff.roundOut(res);
                    } else {
                        drawBounds.roundOut(res);
                    }
                    return res;
                }
            }
        }
    }

    static final class ClipDraw implements ClipGeometry {

        final Matrix4 mViewMatrix = new Matrix4();
        final Rect2f mShape = new Rect2f();
        final Rect2f mDrawBounds = new Rect2f();

        public ClipDraw init(Matrix4 viewMatrix,
                             Rect2fc shape,
                             Rect2fc drawBounds) {
            mViewMatrix.set(viewMatrix);
            mShape.set(shape);
            mDrawBounds.set(drawBounds);
            return this;
        }

        @Override
        public int op() {
            return OP_INTERSECT;
        }

        @Override
        public Rect2fc shape() {
            return mShape;
        }

        @Override
        public Matrix4 viewMatrix() {
            return mViewMatrix;
        }

        @Override
        public Rect2fc outerBounds() {
            return mDrawBounds;
        }

        @Override
        public boolean contains(ClipGeometry other) {
            // Draw does not have inner bounds so cannot contain anything.
            assert other instanceof SaveRecord || other instanceof ClipElement;
            return false;
        }
    }
}
