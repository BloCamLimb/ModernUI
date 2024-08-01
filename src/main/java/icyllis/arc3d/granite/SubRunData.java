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

import icyllis.arc3d.core.*;

public class SubRunData implements Geometry {

    private final SubRunContainer.AtlasSubRun mSubRun;
    private final Matrix mSubRunToLocal;
    private final int mFilter;
    private final int mStartGlyphIndex;
    private final int mGlyphCount;

    public SubRunData(SubRunContainer.AtlasSubRun subRun,
                      Matrix4c localToDevice,
                      float originX, float originY,
                      int startGlyphIndex, int glyphCount) {
        mSubRun = subRun;
        var subRunToLocal = new Matrix();
        mFilter = subRun.getSubRunToLocalAndFilter(
                localToDevice,
                originX, originY,
                subRunToLocal);
        mSubRunToLocal = subRunToLocal;
        mStartGlyphIndex = startGlyphIndex;
        mGlyphCount = glyphCount;
    }

    public SubRunContainer.AtlasSubRun getSubRun() {
        return mSubRun;
    }

    public Matrixc getSubRunToLocal() {
        return mSubRunToLocal;
    }

    public int getFilter() {
        return mFilter;
    }

    public int getStartGlyphIndex() {
        return mStartGlyphIndex;
    }

    public int getGlyphCount() {
        return mGlyphCount;
    }

    public Rect2fc getBounds() {
        return mSubRun.getBounds();
    }

    public void getBounds(Rect2f dest) {
        mSubRun.getBounds().store(dest);
    }
}
