/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.engine.TopologicalSort;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.TypedValue;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

public class ThemeBuilder {

    Resources mResources;

    Object2IntOpenHashMap<String> mKeyStringTable = new Object2IntOpenHashMap<>();
    ArrayList<String> mKeyStringArray = new ArrayList<>();
    ToIntFunction<String> mKeyStringMapper = k -> {
        int index = mKeyStringArray.size();
        mKeyStringArray.add(k);
        return index;
    };

    Object2IntOpenHashMap<Object> mGlobalObjectTable = new Object2IntOpenHashMap<>();
    ArrayList<Object> mGlobalObjectArray = new ArrayList<>();
    ToIntFunction<Object> mGlobalObjectMapper = k -> {
        int index = mGlobalObjectArray.size();
        mGlobalObjectArray.add(k);
        return index;
    };

    HashMap<String, StyleBuilder> mStyleTable = new HashMap<>();
    Object2IntOpenHashMap<String> mStyleToOffset = new Object2IntOpenHashMap<>();

    public ThemeBuilder(Resources resources) {
        mResources = resources;

        mKeyStringTable.defaultReturnValue(-1);
        mGlobalObjectTable.defaultReturnValue(-1);
        mStyleToOffset.defaultReturnValue(-1);
    }

    String storeKeyString(String key) {
        return mKeyStringArray.get(mKeyStringTable.computeIfAbsent(key, mKeyStringMapper));
    }

    int storeGlobalObject(Object o) {
        return mGlobalObjectTable.computeIfAbsent(o, mGlobalObjectMapper);
    }

    public StyleBuilder newStyle(@StyleRes String style, @StyleRes String parent) {
        style = storeKeyString(style);
        parent = storeKeyString(parent);
        StyleBuilder builder = new StyleBuilder(style, parent);
        if (mStyleTable.putIfAbsent(style, builder) == null) {
            return builder;
        }
        throw new IllegalStateException("style is already defined");
    }

    public Resources.Theme build() {
        Resources.Theme theme = mResources.newTheme();

        theme.mKeyStrings = mKeyStringArray.toArray(new String[0]);
        mKeyStringArray = null;

        mGlobalObjectTable = null;
        theme.mGlobalObjects = mGlobalObjectArray.toArray();
        mGlobalObjectArray = null;

        ArrayList<StyleBuilder> styles = new ArrayList<>(mStyleTable.values());
        TopologicalSort.topologicalSort(styles, StyleBuilder.ACCESS);

        int dataCount = 0;
        Comparator<StyleBuilder.Entry> entryComparator = Comparator.comparingInt(
                s -> mKeyStringTable.getInt(s.attr)
        );
        for (int i = 0; i < styles.size(); i++) {
            var style = styles.get(i);
            if (!style.mParent.isEmpty() && mStyleTable.get(style.mParent) == null) {
                throw new IllegalStateException("parent style not found");
            }
            style.mEntries.sort(entryComparator);
            dataCount += Resources.Theme.MAP_ENTRY_HEADER_COLUMNS + Resources.Theme.MAP_COLUMNS * style.mEntries.size();
        }

        mStyleTable = null;

        int[] data = new int[dataCount];
        int index = 0;
        for (int i = 0; i < styles.size(); i++) {
            var style = styles.get(i);
            mStyleToOffset.put(style.mName, index);
            data[index + Resources.Theme.MAP_ENTRY_PARENT] = mKeyStringTable.getInt(style.mParent);
            data[index + Resources.Theme.MAP_ENTRY_COUNT] = style.mEntries.size();
            index += Resources.Theme.MAP_ENTRY_ENTRIES;
            var entries = style.mEntries;
            for (int j = 0; j < entries.size(); j++) {
                var entry = entries.get(j);
                data[index + Resources.Theme.MAP_NAME] = mKeyStringTable.getInt(entry.attr);
                data[index + Resources.Theme.MAP_DATA_TYPE] = entry.dataType;
                data[index + Resources.Theme.MAP_DATA] = entry.data;
                index += Resources.Theme.MAP_COLUMNS;
            }
        }
        assert index == dataCount;

        theme.mData = data;
        mKeyStringTable = null;

        Object2IntMap.Entry<String>[] entries = mStyleToOffset.object2IntEntrySet().toArray(new Object2IntMap.Entry[0]);
        Arrays.sort(entries, Map.Entry.comparingByKey());
        String[] styleKeys = new String[entries.length];
        int[] styleOffsets = new int[entries.length];
        for (int i = 0; i < entries.length; i++) {
            styleKeys[i] = entries[i].getKey();
            styleOffsets[i] = entries[i].getIntValue();
        }
        theme.mStyleKeys = styleKeys;
        theme.mStyleOffsets = styleOffsets;
        mStyleToOffset = null;

        return theme;
    }

    public class StyleBuilder {

        static final TopologicalSort.Access<StyleBuilder> ACCESS = new TopologicalSort.Access<>() {
            @Override
            public void setIndex(StyleBuilder node, int index) {
                node.mIndex = index;
            }

            @Override
            public int getIndex(StyleBuilder node) {
                return node.mIndex;
            }

            @Override
            public void setTempMarked(StyleBuilder node, boolean marked) {
                node.mTmpMarked = marked;
            }

            @Override
            public boolean isTempMarked(StyleBuilder node) {
                return node.mTmpMarked;
            }

            @Override
            public Collection<StyleBuilder> getIncomingEdges(StyleBuilder node) {
                if (!node.mParent.isEmpty()) {
                    StyleBuilder parent = node.getThemeBuilder().mStyleTable.get(node.mParent);
                    if (parent != null) {
                        return List.of(parent);
                    }
                }
                return List.of();
            }
        };

        @StyleRes
        String mName;
        @StyleRes
        String mParent;

        record Entry(@AttrRes String attr, int dataType, int data) {
        }
        final ArrayList<Entry> mEntries = new ArrayList<>();

        int mIndex;
        boolean mTmpMarked;

        StyleBuilder(String name, String parent) {
            mName = name;
            mParent = parent;
        }

        public void addDimension(@AttrRes String attr, float value, int units) {
            attr = storeKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_DIMENSION,
                    TypedValue.createComplexDimension(value, units))
            );
        }

        public void addDrawable(@AttrRes String attr, Drawable.ConstantState cs) {
            attr = storeKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_OBJECT,
                    storeGlobalObject(cs)));
        }

        ThemeBuilder getThemeBuilder() {
            return ThemeBuilder.this;
        }
    }
}
