/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;

import javax.annotation.Nullable;

/**
 * Represents a recorded draw operation.
 * <p>
 * Draw contains multiple groups of data, each of which will be initialized step by step.
 */
public final class Draw implements AutoCloseable {

    /**
     * Pointer to the renderer instance, managed by {@link RendererProvider}.
     */
    public GeometryRenderer mRenderer;

    /**
     * This matrix transforms geometry's local space to device space.
     */
    public Matrix4c mTransform;
    public Object mGeometry;
    /**
     * Clip params (immutable), set by {@link ClipStack}.
     * <p>
     * DrawBounds: Tight bounds of the draw in device space, including any padding/outset for stroking and expansion
     * due to inverse fill and intersected with the scissor.
     * <p>
     * TransformedShapeBounds: Clipped bounds of the shape in device space, including any padding/outset for stroking,
     * intersected with the scissor and ignoring the fill rule. For a regular fill this is identical
     * to DrawBounds. For an inverse fill, this is a subset of DrawBounds.
     * <p>
     * ScissorRect: The scissor rectangle obtained by restricting the bounds of the clip stack that affects the
     * draw to the device bounds. The scissor must contain DrawBounds and must already be
     * intersected with the device bounds.
     */
    public Rect2fc mDrawBounds;
    public Rect2fc mTransformedShapeBounds;
    public Rect2ic mScissorRect;
    /**
     * Precomputed local AA radius if {@link GeometryRenderer#outsetBoundsForAA()} is true,
     * set by {@link ClipStack}.
     */
    public float mAARadius;
    /**
     * Packed draw order, see {@link DrawOrder}.
     */
    public long mDrawOrder;
    /**
     * Stroke params.
     */
    public float mHalfWidth = -1;   // >0: relative to transform; ==0: hairline, 1px in device space; <0: fill
    public float mJoinLimit = -1;   // >0: miter join; ==0: bevel join; <0: round join
    // Paint::Cap
    public byte mStrokeCap;
    // Paint::Align
    public byte mStrokeAlign;

    /**
     * Paint params, null implies depth-only draw (i.e. clipping mask).
     */
    @Nullable
    public PaintParams mPaintParams;

    @Override
    public void close() {
        if (mPaintParams != null) {
            mPaintParams.close();
        }
        mPaintParams = null;
    }

    public boolean isClippedOut() {
        return mDrawBounds.isEmpty();
    }

    /**
     * Returns true if the geometry is stroked instead of filled.
     */
    public boolean isStroke() {
        return mHalfWidth >= 0.f;
    }

    public boolean isMiterJoin() {
        return mJoinLimit > 0.f;
    }

    public boolean isBevelJoin() {
        return mJoinLimit == 0.f;
    }

    public boolean isRoundJoin() {
        return mJoinLimit < 0.f;
    }

    public float getMiterLimit() {
        return Math.max(0.f, mJoinLimit);
    }

    /**
     * @see StrokeRec#getInflationRadius()
     */
    public float getInflationRadius() {
        if (mHalfWidth < 0) { // fill
            return 0;
        } else if (mHalfWidth == 0) { // hairline
            return 1;
        }

        float multiplier = 1;
        if (mJoinLimit > 0) { // miter join
            multiplier = Math.max(multiplier, mJoinLimit);
        }
        if (mStrokeAlign == Paint.ALIGN_CENTER) {
            if (mStrokeCap == Paint.CAP_SQUARE) {
                multiplier = Math.max(multiplier, MathUtil.SQRT2);
            }
        } else {
            multiplier *= 2.0f;
        }
        return mHalfWidth * multiplier;
    }

    /**
     * Returns the painter's depth as unsigned integer.
     */
    public int getDepth() {
        return DrawOrder.getDepth(mDrawOrder);
    }

    public float getDepthAsFloat() {
        return DrawOrder.getDepthAsFloat(mDrawOrder);
    }
}
