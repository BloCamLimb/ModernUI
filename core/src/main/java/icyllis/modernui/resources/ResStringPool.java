/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.resources;

import icyllis.modernui.util.SparseArray;

public class ResStringPool {

    private CharSequence[] mCache;
    private SparseArray<CharSequence> mSparseCache;

    public ResStringPool(CharSequence[] input) {
        mCache = input;
    }

    public CharSequence getSequenceAt(int idx) {
        if (idx < 0 || idx > mCache.length) {
            return null;
        }
        return mCache[idx];
    }

    public String getStringAt(int idx) {
        CharSequence csq = getSequenceAt(idx);
        if (csq != null) {
            return csq.toString();
        }
        return null;
    }
}
