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
import icyllis.modernui.annotation.AnyRes;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.util.ColorStateList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * Not stable
 *
 * @hidden
 */
@ApiStatus.Internal
public class ThemeBuilder {

    Object2IntOpenHashMap<String> mTypeStringTable = new Object2IntOpenHashMap<>();
    ArrayList<String> mTypeStringArray = new ArrayList<>();
    ToIntFunction<String> mTypeStringMapper = k -> {
        int index = mTypeStringArray.size();
        mTypeStringArray.add(k);
        return index;
    };

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

    HashMap<String, Style> mStyleTable = new HashMap<>();
    Object2IntOpenHashMap<String> mStyleToOffset = new Object2IntOpenHashMap<>();

    public ThemeBuilder() {
        mTypeStringTable.defaultReturnValue(-1);
        mKeyStringTable.defaultReturnValue(-1);
        mGlobalObjectTable.defaultReturnValue(-1);
        mStyleToOffset.defaultReturnValue(-1);
    }

    int storeTypeString(String type) {
        return mTypeStringTable.computeIfAbsent(type, mTypeStringMapper);
    }

    int storeKeyString(String key) {
        return mKeyStringTable.computeIfAbsent(key, mKeyStringMapper);
    }

    String dedupKeyString(String key) {
        return mKeyStringArray.get(storeKeyString(key));
    }

    int storeGlobalObject(Object o) {
        return mGlobalObjectTable.computeIfAbsent(o, mGlobalObjectMapper);
    }

    public Style newStyle(@NonNull @StyleRes String name,
                          @NonNull @StyleRes String parent) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException();
        }
        name = dedupKeyString(name);
        if (!parent.isEmpty()) {
            parent = dedupKeyString(parent);
        }
        Style builder = new Style(name, parent);
        if (mStyleTable.putIfAbsent(name, builder) == null) {
            return builder;
        }
        throw new IllegalStateException("style is already defined");
    }

    public Resources build() {
        Resources resources = new Resources();

        resources.mTypeStrings = mTypeStringArray.toArray(new String[0]);
        mTypeStringArray = null;
        resources.mKeyStrings = mKeyStringArray.toArray(new String[0]);
        mKeyStringArray = null;

        mGlobalObjectTable = null;
        resources.mGlobalObjects = mGlobalObjectArray.toArray();
        mGlobalObjectArray = null;

        ArrayList<Style> styles = new ArrayList<>(mStyleTable.values());
        TopologicalSort.topologicalSort(styles, Style.TOPOSORT_TRAIT);

        int dataCount = 0;
        for (int i = 0; i < styles.size(); i++) {
            var style = styles.get(i);
            if (!style.mParent.isEmpty() && mStyleTable.get(style.mParent) == null) {
                throw new IllegalStateException("parent style not found");
            }
            style.mEntries.sort(Style.STYLE_ENTRY_COMPARATOR);
            dataCount += Resources.MAP_ENTRY_HEADER_COLUMNS + Resources.MAP_COLUMNS * style.mEntries.size();
        }

        mStyleTable = null;

        int[] data = new int[dataCount];
        int index = 0;
        for (int i = 0; i < styles.size(); i++) {
            var style = styles.get(i);
            mStyleToOffset.put(style.mName, index);
            data[index + Resources.MAP_ENTRY_PARENT] = !style.mParent.isEmpty() ?
                    mKeyStringTable.getInt(style.mParent) : -1;
            data[index + Resources.MAP_ENTRY_COUNT] = style.mEntries.size();
            index += Resources.MAP_ENTRY_ENTRIES;
            var entries = style.mEntries;
            for (int j = 0; j < entries.size(); j++) {
                var entry = entries.get(j);
                data[index + Resources.MAP_NAME] = mKeyStringTable.getInt(entry.attr);
                data[index + Resources.MAP_DATA_TYPE] = entry.dataType;
                data[index + Resources.MAP_DATA] = entry.data;
                index += Resources.MAP_COLUMNS;
            }
        }
        assert index == dataCount;

        resources.mData = data;
        mKeyStringTable = null;

        Object2IntMap.Entry<String>[] entries = mStyleToOffset.object2IntEntrySet().toArray(new Object2IntMap.Entry[0]);
        Arrays.sort(entries, Map.Entry.comparingByKey());
        String[] styleKeys = new String[entries.length];
        int[] styleOffsets = new int[entries.length];
        for (int i = 0; i < entries.length; i++) {
            styleKeys[i] = entries[i].getKey();
            styleOffsets[i] = entries[i].getIntValue();
        }
        resources.mStyleKeys = styleKeys;
        resources.mStyleOffsets = styleOffsets;
        mStyleToOffset = null;

        return resources;
    }

    public class Style {

        static final TopologicalSort.Access<Style> TOPOSORT_TRAIT = new TopologicalSort.Access<>() {
            @Override
            public void setIndex(Style node, int index) {
                node.mIndex = index;
            }

            @Override
            public int getIndex(Style node) {
                return node.mIndex;
            }

            @Override
            public void setTempMarked(Style node, boolean marked) {
                node.mTmpMarked = marked;
            }

            @Override
            public boolean isTempMarked(Style node) {
                return node.mTmpMarked;
            }

            @Override
            public Collection<Style> getIncomingEdges(Style node) {
                if (!node.mParent.isEmpty()) {
                    Style parent = node.getThemeBuilder().mStyleTable.get(node.mParent);
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

        static final Comparator<Entry> STYLE_ENTRY_COMPARATOR = Comparator.comparing(Entry::attr);

        int mIndex = -1;
        boolean mTmpMarked;

        Style(String name, String parent) {
            mName = name;
            mParent = parent;
        }

        public void addDimension(@AttrRes String attr, float value, int units) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_DIMENSION,
                    TypedValue.createComplexDimension(value, units))
            );
        }

        public void addAttribute(@AttrRes String attr, @NonNull @AttrRes String target) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_ATTRIBUTE,
                    storeKeyString(target)));
        }

        public void addReference(@AttrRes String attr, @Nullable @AnyRes ResourceId target) {
            assert target == null || target.namespace().equals(Resources.DEFAULT_NAMESPACE);
            attr = dedupKeyString(attr);
            int typeId = target == null ? 0 : storeTypeString(target.type()) + 1;
            mEntries.add(new Entry(attr, TypedValue.TYPE_REFERENCE | (typeId << ResourceTypes.Res_value.TYPE_ID_SHIFT),
                    target == null ? 0 : storeKeyString(target.entry())));
        }

        public void addBoolean(@AttrRes String attr, boolean v) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_BOOLEAN, v ? 1 : 0));
        }

        public void addColor(@AttrRes String attr, int argb) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_COLOR_ARGB8, argb));
        }

        public void addInteger(@AttrRes String attr, int v) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_DEC, v));
        }

        public void addEnum(@AttrRes String attr, int ord) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_DEC, ord));
        }

        public void addFlags(@AttrRes String attr, int flags) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_HEX, flags));
        }

        public void addColor(@AttrRes String attr, @NonNull BiFunction<Resources, Resources.Theme, ColorStateList> factory) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_FACTORY,
                    storeGlobalObject(factory)));
        }

        public void addDrawable(@AttrRes String attr, @NonNull BiFunction<Resources, Resources.Theme, Drawable> factory) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(attr, TypedValue.TYPE_FACTORY,
                    storeGlobalObject(factory)));
        }

        ThemeBuilder getThemeBuilder() {
            return ThemeBuilder.this;
        }
    }
}
