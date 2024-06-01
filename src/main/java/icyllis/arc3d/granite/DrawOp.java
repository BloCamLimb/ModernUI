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
public final class DrawOp {
    // reference to our renderer instance
    public GeometryRenderer mRenderer;
    // the copied view matrix
    public Matrix4 mTransform;
    public Object mGeometry;
    // clip params
    public Rect2fc mDrawBounds;
    public Rect2fc mTransformedShapeBounds;
    public Rect2ic mScissorRect;
    // the packed draw order
    public long mDrawOrder;
    public float mStrokeRadius = -1; // >0: relative to transform; ==0: hairline, 1px in device space; <0: fill
    public float mJoinLimit;    // >0: miter join; ==0: bevel join; <0: round join
    public int mStrokeCap;
    @Nullable
    public PaintParams mPaintParams; // null implies depth-only draw (clipping)

    public boolean isClippedOut() {
        return mDrawBounds.isEmpty();
    }

    public float getInflationRadius() {
        if (mStrokeRadius < 0) {
            return 0;
        } else if (mStrokeRadius == 0) {
            return 1;
        }

        float multiplier = 1;
        if (mJoinLimit > 0) {
            multiplier = Math.max(multiplier, mJoinLimit);
        }
        if (mStrokeCap == Paint.CAP_SQUARE) {
            multiplier = Math.max(multiplier, MathUtil.SQRT2);
        }
        return mStrokeRadius * multiplier;
    }
}
