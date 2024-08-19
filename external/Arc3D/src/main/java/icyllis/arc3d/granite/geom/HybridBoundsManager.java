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

import icyllis.arc3d.core.Rect2fc;

/**
 * A BoundsManager that first relies on {@link FullBoundsManager} for N draw calls, and then switches
 * to the {@link GridBoundsManager} if it exceeds its limit. For low N, the brute force approach is
 * surprisingly efficient, has the highest accuracy, and very low memory overhead. Once the draw
 * call count is large enough, the grid's lower performance complexity outweigh its memory cost and
 * reduced accuracy.
 */
public final class HybridBoundsManager extends BoundsManager {

    private final int mDeviceWidth;
    private final int mDeviceHeight;
    private final int mGridCellSize;
    private final int mMaxBruteForceN;
    private final int mMaxGridSize;

    // point to either mFullManager or mGridManager
    private BoundsManager mCurrentManager;

    private final FullBoundsManager mFullManager;
    private       GridBoundsManager mGridManager;

    public HybridBoundsManager(int deviceWidth, int deviceHeight,
                               int gridCellSize, int maxBruteForceN, int maxGridSize) {
        assert (deviceWidth >= 1 && deviceHeight >= 1 &&
                gridCellSize >= 1 && maxBruteForceN >= 1);
        mDeviceWidth = deviceWidth;
        mDeviceHeight = deviceHeight;
        mGridCellSize = gridCellSize;
        mMaxBruteForceN = maxBruteForceN;
        mMaxGridSize = maxGridSize;
        mFullManager = new FullBoundsManager(maxBruteForceN);
        mCurrentManager = mFullManager;
    }

    @Override
    public int getMostRecentDraw(Rect2fc bounds) {
        return mCurrentManager.getMostRecentDraw(bounds);
    }

    @Override
    public void recordDraw(Rect2fc bounds, int order) {
        if (mCurrentManager == mFullManager &&
                mFullManager.count() >= mMaxBruteForceN) {
            // We need to switch from the brute force manager to the grid manager
            if (mGridManager == null) {
                mGridManager = GridBoundsManager.makeRes(
                        mDeviceWidth, mDeviceHeight,
                        mGridCellSize, mMaxGridSize
                );
            }
            mCurrentManager = mGridManager;
            // Fill out the grid manager with the recorded draws in the brute force manager,
            // and clear the brute force manager
            mFullManager.transferTo(mCurrentManager);
            assert mFullManager.count() == 0;
        }
        mCurrentManager.recordDraw(bounds, order);
    }

    @Override
    public void clear() {
        if (mCurrentManager == mGridManager) {
            // Reset the grid manager so it's ready to use next frame, but don't delete it.
            mGridManager.clear();
            // Assume brute force manager was reset when we swapped to the grid originally
            mCurrentManager = mFullManager;
        } else {
            assert mCurrentManager == mFullManager;
            mFullManager.clear();
        }
    }
}
