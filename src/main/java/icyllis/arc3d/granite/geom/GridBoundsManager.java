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

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.Rect2fc;
import icyllis.arc3d.granite.DrawOrder;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;

/**
 * A BoundsManager that tracks highest CompressedPaintersOrder over a uniform spatial grid.
 */
public final class GridBoundsManager extends BoundsManager {

    private final float mScaleX;
    private final float mScaleY;
    private final int mGridWidth;
    private final int mGridHeight;

    private final short[] mNodes;

    public GridBoundsManager(int deviceWidth, int deviceHeight,
                             int gridWidth, int gridHeight) {
        assert (deviceWidth > 0 && deviceHeight > 0);
        assert (gridWidth >= 1 && gridHeight >= 1);
        mScaleX = (float) gridWidth / deviceWidth;
        mScaleY = (float) gridHeight / deviceHeight;
        mGridWidth = gridWidth;
        mGridHeight = gridHeight;
        mNodes = new short[gridWidth * gridHeight];
    }

    @NonNull
    public static GridBoundsManager makeRes(int deviceWidth, int deviceHeight,
                                            int gridCellSize, int maxGridSize) {
        assert (deviceWidth > 0 && deviceHeight > 0);
        assert (gridCellSize >= 1);

        int gridWidth = (int) Math.ceil((double) deviceWidth / gridCellSize);
        if (maxGridSize > 0 && gridWidth > maxGridSize) {
            // We'd have too many sizes so clamp the grid resolution, leave the device size alone
            // since the grid cell size can't be preserved anyways.
            gridWidth = maxGridSize;
        } else {
            // Pad out the device size to keep cell size the same
            deviceWidth = gridWidth * gridCellSize;
        }

        int gridHeight = (int) Math.ceil((double) deviceHeight / gridCellSize);
        if (maxGridSize > 0 && gridHeight > maxGridSize) {
            gridHeight = maxGridSize;
        } else {
            deviceHeight = gridHeight * gridCellSize;
        }
        return new GridBoundsManager(deviceWidth, deviceHeight, gridWidth, gridHeight);
    }

    @Override
    public int getMostRecentDraw(Rect2fc bounds) {
        int l = MathUtil.clamp((int) (bounds.left()   * mScaleX), 0, mGridWidth  - 1);
        int t = MathUtil.clamp((int) (bounds.top()    * mScaleY), 0, mGridHeight - 1);
        int r = MathUtil.clamp((int) (bounds.right()  * mScaleX), 0, mGridWidth  - 1);
        int b = MathUtil.clamp((int) (bounds.bottom() * mScaleY), 0, mGridHeight - 1);
        int p = t * mGridWidth + l;
        int h = b - t;
        int w = r - l;
        int max = DrawOrder.MIN_VALUE;
        for (int y = 0; y <= h; ++y) {
            for (int x = 0; x <= w; ++x) {
                int v = mNodes[p + x] & 0xFFFF;
                if (v > max) {
                    max = v;
                }
            }
            p = p + mGridWidth;
        }

        return max;
    }

    @Override
    public void recordDraw(Rect2fc bounds, int order) {
        int l = MathUtil.clamp((int) (bounds.left()   * mScaleX), 0, mGridWidth  - 1);
        int t = MathUtil.clamp((int) (bounds.top()    * mScaleY), 0, mGridHeight - 1);
        int r = MathUtil.clamp((int) (bounds.right()  * mScaleX), 0, mGridWidth  - 1);
        int b = MathUtil.clamp((int) (bounds.bottom() * mScaleY), 0, mGridHeight - 1);
        int p = t * mGridWidth + l;
        int h = b - t;
        int w = r - l;

        for (int y = 0; y <= h; ++y) {
            for (int x = 0; x <= w; ++x) {
                int v = mNodes[p + x] & 0xFFFF;
                if (order > v) {
                    mNodes[p + x] = (short) order;
                }
            }
            p = p + mGridWidth;
        }
    }

    @Override
    public void clear() {
        Arrays.fill(mNodes, (short) DrawOrder.MIN_VALUE);
    }
}
