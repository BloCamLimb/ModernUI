/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2025 BloCamLimb <pocamelards@gmail.com>
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

import icyllis.arc3d.sketch.RRect;
import icyllis.arc3d.core.Rect2f;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BlurredBox {

    public float mLeft;
    public float mTop;
    public float mRight;
    public float mBottom;

    public float mRadius;

    public float mBlurRadius;
    public float mNoiseAlpha;

    public BlurredBox() {
    }

    public BlurredBox(RRect rect) {
        mLeft = rect.left();
        mTop = rect.top();
        mRight = rect.right();
        mBottom = rect.bottom();
    }

    public void getBounds(Rect2f dest) {
        dest.set(mLeft, mTop, mRight, mBottom);
        dest.outset(mBlurRadius, mBlurRadius);
    }
}
