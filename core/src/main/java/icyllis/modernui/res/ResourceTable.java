/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.res;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Build resource files from raw assets.
 */
//TODO this will take a long time to finish
public class ResourceTable {

    private static final Object2IntOpenHashMap<String> sIdMap = new Object2IntOpenHashMap<>();

    public static int lookup(String pack, String type, String name, boolean onlyPublic) {
        String key = name + type + pack + (onlyPublic ? "1" : "0");
        return sIdMap.getInt(key);
    }
}
