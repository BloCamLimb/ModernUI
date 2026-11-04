/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.resources;

import icyllis.arc3d.fastutil.objects.Object2LongOpenHashMap;
import icyllis.modernui.annotation.Immutable;
import icyllis.modernui.util.AttributeSet;
import icyllis.modernui.util.DisplayMetrics;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

@Immutable
public final class InlineStyle implements AttributeSet {

    final String[] keys;
    final int[] values;

    private InlineStyle(String[] keys, int[] values) {
        this.keys = keys;
        this.values = values;
    }

    @Override
    public int getAttributeCount() {
        return keys.length;
    }

    @Override
    public @NonNull String getAttributeName(int index) {
        return keys[index];
    }

    @Override
    public @NonNull String getAttributeValue(int index) {
        var v = TypedValue.coerceToString(type(index), data(index));
        assert v != null;
        return v;
    }

    @Nullable
    @Override
    public String getAttributeValue(@NonNull String name) {
        int i = Arrays.binarySearch(keys, name);
        return i >= 0 ? getAttributeValue(i) : null;
    }

    public void getAttributeValue(int index, @NonNull TypedValue outValue) {
        outValue.type = type(index);
        outValue.data = data(index);
        outValue.cookie = AssetManager.INVALID_COOKIE;
        outValue.flags = 0;
        outValue.density = DisplayMetrics.DENSITY_NONE;
        outValue.locale = null;
        outValue.object = null;
    }

    int type(int index) {
        return values[index * 2];
    }

    int data(int index) {
        return values[index * 2 + 1];
    }

    public static final class Builder {

        // attr -> (dataType << 32) | data
        private final Object2LongOpenHashMap<String> entries = new Object2LongOpenHashMap<>();

        public Builder() {
        }

        public Builder addNull(@NonNull String attr, boolean empty) {
            put(attr, TypedValue.TYPE_NULL,
                    empty ? TypedValue.DATA_NULL_EMPTY : TypedValue.DATA_NULL_ALT_UNDEFINED
            );
            return this;
        }

        public Builder addFloat(@NonNull String attr, float v) {
            put(attr, TypedValue.TYPE_FLOAT, Float.floatToRawIntBits(v));
            return this;
        }

        public Builder addDimension(@NonNull String attr,
                                    int value, @TypedValue.ComplexDimensionUnit int units) {
            put(attr, TypedValue.TYPE_DIMENSION,
                    TypedValue.createComplexDimension(value, units)
            );
            return this;
        }

        public Builder addDimension(@NonNull String attr,
                                    float value, @TypedValue.ComplexDimensionUnit int units) {
            put(attr, TypedValue.TYPE_DIMENSION,
                    TypedValue.createComplexDimension(value, units)
            );
            return this;
        }

        public Builder addBoolean(@NonNull String attr, boolean v) {
            put(attr, TypedValue.TYPE_INT_BOOLEAN, v ? 1 : 0);
            return this;
        }

        public Builder addColor(@NonNull String attr, int argb) {
            put(attr, TypedValue.TYPE_INT_COLOR_ARGB8, argb);
            return this;
        }

        public Builder addInt(@NonNull String attr, int v) {
            put(attr, TypedValue.TYPE_INT_DEC, v);
            return this;
        }

        public Builder addEnum(@NonNull String attr, int ord) {
            put(attr, TypedValue.TYPE_INT_DEC, ord);
            return this;
        }

        public Builder addFlags(@NonNull String attr, int flags) {
            put(attr, TypedValue.TYPE_INT_HEX, flags);
            return this;
        }

        private void put(@NonNull String attr, int type, int data) {
            if (attr.isEmpty()) {
                throw new IllegalArgumentException("Attribute name cannot be empty");
            }
            entries.put(attr, ((long) type << 32L) | (data & 0xFFFFFFFFL));
        }

        public @NonNull InlineStyle build() {
            var entries = this.entries;
            int len = entries.size();
            String[] keys = new String[len];
            int[] values = new int[len * 2];
            entries.keySet().toArray(keys);
            Arrays.sort(keys);
            for (int j = 0; j < len; j++) {
                long v = entries.getLong(keys[j]);
                values[j * 2] = (int) (v >>> 32);
                values[j * 2 + 1] = (int) (v);
            }
            return new InlineStyle(keys, values);
        }
    }
}
