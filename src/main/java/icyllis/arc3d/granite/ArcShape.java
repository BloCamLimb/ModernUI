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

public class ArcShape implements Geometry {

    public float mCenterX;
    public float mCenterY;
    public float mRadius;
    public float mStartAngle;
    public float mSweepAngle;
    public float mHalfWidth;

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
}
