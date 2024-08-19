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

import it.unimi.dsi.fastutil.ints.IntArrays;

import java.util.Arrays;

public class TextureTracker {

    private int[] mLastBinding = IntArrays.EMPTY_ARRAY;

    // the array is not de-duplicated, so compare their contents
    public boolean setCurrentTextures(int[] textures) {
        if (textures.length != 0 && !Arrays.equals(mLastBinding, textures)) {
            mLastBinding = textures;
            return true;
        }
        // No binding change
        return false;
    }

    public void bindTextures(DrawCommandList commandList) {
        assert mLastBinding.length != 0;
        commandList.bindTextures(mLastBinding);
    }
}
