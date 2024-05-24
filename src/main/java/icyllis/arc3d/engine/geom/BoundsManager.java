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

package icyllis.arc3d.engine.geom;

import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.engine.graphene.DrawOrder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class BoundsManager {
    //TODO

    // mRects and mOrders are parallel, repeated L T R B values
    private final FloatArrayList mRects = new FloatArrayList();
    // painter's orders
    private final ShortArrayList mOrders = new ShortArrayList();

    public int getMostRecentDraw(Rect2fc bounds) {
        assert mRects.size() == mOrders.size() * 4;
        float[] r = mRects.elements();
        short[] orders = mOrders.elements();
        int limit = mRects.size();
        int max = DrawOrder.MIN_VALUE;
        for (int i = 0, j = 0; i < limit; i += 4, j++) {
            if (bounds.intersects(r[i], r[i|1], r[i|2], r[i|3])) {
                max = Math.max(max, Short.toUnsignedInt(orders[j]));
            }
        }
        return max;
    }

    public void recordDraw(Rect2fc bounds, int paintersOrder) {
        mRects.add(bounds.left());
        mRects.add(bounds.top());
        mRects.add(bounds.right());
        mRects.add(bounds.bottom());
        mOrders.add((short) paintersOrder);
    }

    public void clear() {
        mRects.clear();
        mOrders.clear();
    }
}
