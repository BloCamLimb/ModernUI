/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleableRes;

import javax.xml.stream.XMLStreamReader;

public class ResourceUtils {

    @Nullable
    public static String findAttribute(@NonNull XMLStreamReader reader,
                                       @NonNull String name) {
        var res = reader.getAttributeValue(null, name);
        if (res != null) {
            return res.trim();
        }
        return null;
    }

    @Nullable
    public static String findNonEmptyAttribute(@NonNull XMLStreamReader reader,
                                               @NonNull String name) {
        var res = reader.getAttributeValue(null, name);
        if (res != null) {
            var trim = res.trim();
            if (!trim.isEmpty()) {
                return trim;
            }
        }
        return null;
    }

    @NonNull
    public static ResourceValues.Reference parseXmlAttributeName(@NonNull String s) {
        var name = s.trim();
        int start = 0;

        var ref = new ResourceValues.Reference();
        if (!name.isEmpty() && name.charAt(0) == '*') {
            ref.private_reference = true;
            start++;
        }

        String namespace;
        String entry;

        int i = name.indexOf(':', start);
        if (i >= 0) {
            namespace = name.substring(0, i);
            entry = name.substring(i + 1);
        } else {
            namespace = "";
            entry = name.substring(start);
        }
        ref.name = new ResourceId(namespace, Resource.getTypeName(Resource.TYPE_ATTR), entry);
        return ref;
    }

    /**
     * Find the index of the attribute in the sorted keys (namespace=>attribute pairs).
     * Returns -1 if not found.
     */
    public static int indexOfAttribute(@NonNull String[] keys,
                                       @NonNull String namespace, @NonNull String attribute) {
        assert (keys.length & 1) == 0;
        int low = 0;
        int high = (keys.length >> 1) - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = ResourceId.comparePair(keys[mid << 1], keys[(mid << 1) + 1],
                    namespace, attribute);
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return -1;
    }
}
