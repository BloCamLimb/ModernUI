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

package icyllis.arc3d.granite.geom;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.granite.DrawOrder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

/**
 * A BoundsManager that tracks every draw and can exactly determine all queries
 * using a brute force search.
 */
public final class FullBoundsManager extends BoundsManager {

    // rects and orders are parallel, hold repeated L T R B values
    private final FloatArrayList mRects;
    // painter's orders
    private final ShortArrayList mOrders;

    public FullBoundsManager() {
        mRects = new FloatArrayList();
        mOrders = new ShortArrayList();
    }

    public FullBoundsManager(int capacity) {
        mRects = new FloatArrayList(capacity * 4);
        mOrders = new ShortArrayList(capacity);
    }

    @Override
    public int getMostRecentDraw(Rect2fc bounds) {
        assert mRects.size() == mOrders.size() * 4;
        assert (!bounds.isFinite() || (bounds.left() <= bounds.right() && bounds.top() <= bounds.bottom()));

        float al = bounds.left(), at = bounds.top(), ar = bounds.right(), ab = bounds.bottom();
        float[] r = mRects.elements();
        short[] orders = mOrders.elements();
        int limit = mOrders.size();
        int max = DrawOrder.MIN_VALUE;
        for (int i = 0, j = 0; j < limit; i += 4, j += 1) {
            // fast overlap check
            if (ar > r[i] && ab > r[i|1] && r[i|2] > al && r[i|3] > at) {
                max = Math.max(max, Short.toUnsignedInt(orders[j]));
            }
        }
        return max;
    }

    @Override
    public void recordDraw(Rect2fc bounds, int order) {
        assert (!bounds.isFinite() || (bounds.left() <= bounds.right() && bounds.top() <= bounds.bottom()));
        var r = mRects;
        r.add(bounds.left());
        r.add(bounds.top());
        r.add(bounds.right());
        r.add(bounds.bottom());
        mOrders.add((short) order);
    }

    @Override
    public void clear() {
        mRects.clear();
        mOrders.clear();
    }

    public int count() {
        return mOrders.size();
    }

    public void transferTo(BoundsManager other) {
        var tmp = new Rect2f();
        float[] r = mRects.elements();
        short[] orders = mOrders.elements();
        int limit = mOrders.size();
        for (int i = 0, j = 0; j < limit; i += 4, j += 1) {
            tmp.set(r[i], r[i|1], r[i|2], r[i|3]);
            other.recordDraw(tmp, orders[j]);
        }
        clear();
    }
}
