/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

//TODO work in process
public class AttributeMap {

    // int, float takes up 16 bytes
    private final Map<String, Object> map = new HashMap<>();

    public AttributeMap() {
    }

    public void putInt(@Nonnull String key, int value) {
        map.put(key, value);
    }

    public int getInt(@Nonnull String key, int defValue) {
        Object o = map.get(key);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return defValue;
    }

    public void putFloat(@Nonnull String key, float value) {
        map.put(key, value);
    }

    public float getFloat(@Nonnull String key, float defValue) {
        Object o = map.get(key);
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        return defValue;
    }

    public void put(@Nonnull String key, @Nonnull AttributeMap map) {
        if (map != this) {
            this.map.put(key, map);
        }
    }

    @Nullable
    public AttributeMap get(@Nonnull String key) {
        Object o = map.get(key);
        if (o instanceof AttributeMap) {
            return (AttributeMap) o;
        }
        return null;
    }
}
