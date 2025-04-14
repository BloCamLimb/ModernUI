/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.granite.geom;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;

public class EdgeAAQuad {

    // see Canvas
    public static final int
            kLeft   = 0b0001,
            kTop    = 0b0010,
            kRight  = 0b0100,
            kBottom = 0b1000,

            kNone   = 0b0000,
            kAll    = 0b1111;

    public final float x0,x1,x2,x3;
    public final float y0,y1,y2,y3;
    private final byte mEdgeFlags;
    private final boolean mIsRect;

    public EdgeAAQuad(Rect2fc rect, int edgeFlags) {
        x0=rect.left();
        x1=rect.right();
        x2=rect.right();
        x3=rect.left();
        y0=rect.top();
        y1=rect.top();
        y2=rect.bottom();
        y3=rect.bottom();
        mEdgeFlags = (byte) edgeFlags;
        mIsRect = true;
    }

    public EdgeAAQuad(float[] points, int edgeFlags) {
        x0=points[0];
        x1=points[2];
        x2=points[4];
        x3=points[6];
        y0=points[1];
        y1=points[3];
        y2=points[5];
        y3=points[7];
        mEdgeFlags = (byte) edgeFlags;
        mIsRect = false;
    }

    public int edgeFlags() {
        return mEdgeFlags & 0xFF;
    }

    public boolean isRect() {
        return mIsRect;
    }

    public void getBounds(Rect2f dest) {
        if (mIsRect) {
            dest.set(x0, y0, x2, y2);
        } else {
            dest.set(x0, y0, x1, y1);
            dest.sort();
            float l,t,r,b;
            if (x2 > x3) {
                l=x3;
                r=x2;
            } else {
                l=x2;
                r=x3;
            }
            if (y2 > y3) {
                t=y3;
                b=y2;
            } else {
                t=y2;
                b=y3;
            }
            dest.joinNoCheck(l,t,r,b);
        }
    }
}
