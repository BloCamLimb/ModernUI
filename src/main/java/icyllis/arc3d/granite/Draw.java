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
 */
public final class Draw implements AutoCloseable {
    // reference to our renderer instance
    public GeometryRenderer mRenderer;
    // the copied view matrix
    public Matrix4c mTransform;
    public Geometry mGeometry;
    /**
     * Clip params (immutable), set by {@link ClipStack}.
     */
    public Rect2fc mDrawBounds;
    public Rect2fc mTransformedShapeBounds;
    public Rect2ic mScissorRect;
    /**
     * Precomputed AA radius, set by {@link ClipStack}.
     */
    public float mAARadius;
    /**
     * Packed draw order, see {@link DrawOrder}.
     */
    public long mDrawOrder;
    /**
     * Stroke params.
     */
    // half width of stroke
    public float mStrokeRadius = -1; // >0: relative to transform; ==0: hairline, 1px in device space; <0: fill
    public float mJoinLimit;        // >0: miter join; ==0: bevel join; <0: round join
    public byte mStrokeCap;
    public byte mStrokeAlign;
    @Nullable
    public PaintParams mPaintParams; // null implies depth-only draw (clipping mask)

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
     * @see Stroke#getInflationRadius(float, int, int, float)
     */
    public float getInflationRadius() {
        if (mStrokeRadius < 0) { // fill
            return 0;
        } else if (mStrokeRadius == 0) { // hairline
            return 1;
        }

        float multiplier = 1;
        if (mJoinLimit > 0) { // miter join
            multiplier = Math.max(multiplier, mJoinLimit);
        }
        if (mStrokeCap == Paint.CAP_SQUARE) {
            multiplier = Math.max(multiplier, MathUtil.SQRT2);
        }
        return mStrokeRadius * multiplier;
    }

    public int getDepth() {
        return DrawOrder.getDepth(mDrawOrder);
    }

    public float getDepthAsFloat() {
        return DrawOrder.getDepthAsFloat(mDrawOrder);
    }
}
