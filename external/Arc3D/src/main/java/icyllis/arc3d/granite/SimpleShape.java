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

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.sketch.Paint;
import icyllis.arc3d.sketch.Point;
import icyllis.arc3d.sketch.RRect;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class SimpleShape extends RRect {

    /**
     * Extends RRect::Type, adds three types of stroked lines.
     * <p>
     * LeftTop becomes the start point and RightBottom becomes the end point.
     * RadiusUlx becomes half the line width.
     */
    public static final int
            kLine_Type = kLast_Type + 1,        // butt cap or square cap, and LTBR is projected
            kLineRound_Type = kLast_Type + 2;   // round cap, and LTBR is projected

    public SimpleShape() {
    }

    public SimpleShape(RRect other) {
        super(other);
    }

    public SimpleShape(Rect2fc other) {
        super(other);
    }

    public void setLine(float x0, float y0, float x1, float y1,
                        float radius, boolean round) {
        mLeft = x0;
        mTop = y0;
        mRight = x1;
        mBottom = y1;
        mRadii[0] = radius;
        mRadii[1] = radius;
        mType = round ? kLineRound_Type : kLine_Type;
    }

    public void setLine(float x0, float y0, float x1, float y1,
                        @Paint.Cap int cap, float width) {
        float radius = width * 0.5f;
        if (cap != Paint.CAP_BUTT) {
            double x = x1 - x0;
            double y = y1 - y0;
            double dmag = Math.sqrt(x * x + y * y);
            double dscale = 1.0 / dmag;
            float newX = (float) (x * dscale);
            float newY = (float) (y * dscale);
            if (Point.isDegenerate(newX, newY)) {
                setEmpty();
                return;
            }
            x1 += newX * radius;
            y1 += newY * radius;
            x0 -= newX * radius;
            y0 -= newY * radius;
        }
        setLine(x0, y0, x1, y1, radius, cap == Paint.CAP_ROUND);
    }

    @Override
    public void getBounds(Rect2f dest) {
        super.getBounds(dest);
        if (mType == kLine_Type || mType == kLineRound_Type) {
            dest.sort();
            float outset = getSimpleRadiusX();
            dest.outset(outset, outset);
        }
    }
}
