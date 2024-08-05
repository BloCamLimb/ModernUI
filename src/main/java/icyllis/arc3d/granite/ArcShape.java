/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Rect2f;

public class ArcShape {

    /**
     * The arc is a closed shape, paint's cap is ignored, this cap determines the shape
     * of the arc itself. Butt -> Ring, Round -> Arc, Square -> Horseshoe.
     */
    public static final int
            kArc_Type = 0,          // butt cap
            kArcRound_Type = 1,
            kArcSquare_Type = 2,
            kPie_Type = 3,          // circular sector
            kChord_Type = 4;        // circular segment
    public static final int
            kTypeCount = 5;

    public float mCenterX;
    public float mCenterY;
    public float mRadius;
    public float mStartAngle;
    public float mSweepAngle;
    /**
     * Valid only for open arc.
     */
    public float mHalfWidth;
    public int mType;

    public ArcShape() {
    }

    public ArcShape(float centerX, float centerY, float radius, float startAngle, float sweepAngle, float halfWidth) {
        mCenterX = centerX;
        mCenterY = centerY;
        mRadius = radius;
        mStartAngle = startAngle;
        mSweepAngle = sweepAngle;
        mHalfWidth = halfWidth;
    }

    public static boolean isOpenArc(int type) {
        return switch (type) {
            case kArc_Type, kArcRound_Type, kArcSquare_Type -> true;
            default -> false;
        };
    }

    public void getBounds(Rect2f dest) {
        dest.set(
                mCenterX - mRadius, mCenterY - mRadius,
                mCenterX + mRadius, mCenterY + mRadius
        );
        if (isOpenArc(mType)) {
            float outset = mType == kArcSquare_Type ? MathUtil.SQRT2 * mHalfWidth : mHalfWidth;
            dest.outset(outset, outset);
        }
    }
}
