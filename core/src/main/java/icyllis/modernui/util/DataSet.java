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

package icyllis.modernui.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * A DataSet (sometimes known as DataStore) encapsulates a mapping from String keys
 * to values of various primitive types. The base class can safely be persisted to
 * and restored from local disk or network. A DataSet can be used as a node of other
 * DataSets to construct a tree structure. The I/O uses {@link java.io.DataInput} and
 * {@link java.io.DataOutput} where Strings are coded in Java modified UTF-8 format.
 */
//TODO work in process
public class DataSet {

    // int, float takes up 16 bytes
    private final Map<String, Object> mMap = new Object2ObjectOpenHashMap<>();

    public DataSet() {
    }

    public void putInt(@Nonnull String key, int value) {
        mMap.put(key, value);
    }

    public int getInt(@Nonnull String key, int defValue) {
        Object o = mMap.get(key);
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return defValue;
    }

    public void putFloat(@Nonnull String key, float value) {
        mMap.put(key, value);
    }

    public float getFloat(@Nonnull String key, float defValue) {
        Object o = mMap.get(key);
        if (o instanceof Number) {
            return ((Number) o).floatValue();
        }
        return defValue;
    }

    public void put(@Nonnull String key, @Nonnull DataSet map) {
        if (map != this) {
            this.mMap.put(key, map);
        }
    }

    @Nullable
    public DataSet get(@Nonnull String key) {
        Object o = mMap.get(key);
        if (o instanceof DataSet) {
            return (DataSet) o;
        }
        return null;
    }
}
