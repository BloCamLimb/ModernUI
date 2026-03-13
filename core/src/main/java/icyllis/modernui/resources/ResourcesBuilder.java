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
import icyllis.modernui.R;
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
import icyllis.modernui.resources.ResourceTypes.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * Not stable
 *
 * @hidden
 */
@ApiStatus.Internal
public class ResourcesBuilder {

    String mNamespace;

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

    Object2IntOpenHashMap<String> mGlobalStringTable = new Object2IntOpenHashMap<>();
    ArrayList<String> mGlobalStringArray = new ArrayList<>();
    ToIntFunction<String> mGlobalStringMapper = k -> {
        int index = mGlobalStringArray.size();
        mGlobalStringArray.add(k);
        return index;
    };

    Object2IntOpenHashMap<BiFunction<Resources, Resources.Theme, ?>> mFactoryTable = new Object2IntOpenHashMap<>();
    ArrayList<BiFunction<Resources, Resources.Theme, ?>> mFactoryArray = new ArrayList<>();
    ToIntFunction<BiFunction<Resources, Resources.Theme, ?>> mFactoryMapper = k -> {
        int index = mFactoryArray.size();
        mFactoryArray.add(k);
        return index;
    };

    Object2IntOpenHashMap<String> mLibraryTable = new Object2IntOpenHashMap<>();
    ArrayList<String> mLibraryArray = new ArrayList<>();
    ToIntFunction<String> mLibraryMapper = k -> {
        int index = mLibraryArray.size();
        mLibraryArray.add(k);
        return index;
    };

    HashMap<String, Style> mStyleTable = new HashMap<>();

    public ResourcesBuilder(@NonNull String namespace) {
        mNamespace = namespace;
        mTypeStringTable.defaultReturnValue(-1);
        mKeyStringTable.defaultReturnValue(-1);
        mGlobalStringTable.defaultReturnValue(-1);
        mFactoryTable.defaultReturnValue(-1);

        storeTypeString("style");
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

    int storeGlobalString(String o) {
        return mGlobalStringTable.computeIfAbsent(o, mGlobalStringMapper);
    }

    int storeFactory(BiFunction<Resources, Resources.Theme,?> factory) {
        return mFactoryTable.computeIfAbsent(factory, mFactoryMapper);
    }

    int storeNamespace(String namespace) {
        return mLibraryTable.computeIfAbsent(namespace, mLibraryMapper);
    }

    int genId(String namespace, String entry) {
        int packageId = namespace.equals(mNamespace) ? 0 : storeNamespace(namespace) + 1;
        return (packageId << Res_value.PACKAGE_ID_SHIFT) | storeKeyString(entry);
    }

    public Style newStyle(@NonNull @StyleRes String name,
                          @Nullable @StyleRes ResourceId parent) {
        assert parent == null || parent.type().equals("style");
        Style builder = new Style(name, parent);
        if (mStyleTable.putIfAbsent(name, builder) == null) {
            return builder;
        }
        throw new IllegalStateException("style is already defined");
    }

    public Style newStyle(@NonNull @StyleRes ResourceId id,
                          @Nullable @StyleRes ResourceId parent) {
        assert id.namespace().equals(mNamespace) && id.type().equals("style");
        return newStyle(id.entry(), parent);
    }

    /**
     * @throws IllegalStateException the runtime limit is exceeded
     * @return
     */
    public ResourcesProvider build() {
        LoadedResources resources = new LoadedResources();

        mGlobalStringTable = null;
        resources.globalStringPool = new ResStringPool(mGlobalStringArray.toArray(new String[0]));
        mGlobalStringArray = null;

        mFactoryTable = null;
        resources.factoryPool = mFactoryArray.toArray(new BiFunction[0]);
        mFactoryArray = null;

        ArrayList<Style> styles = new ArrayList<>(mStyleTable.values());
        mStyleTable = null;

        Object2IntOpenHashMap<String> entryIndexTable = new Object2IntOpenHashMap<>(styles.size());
        entryIndexTable.defaultReturnValue(-1);
        int valuesSize = 0;
        int offsetsSize = 4 * styles.size();
        for (int i = 0; i < styles.size(); i++) {
            var style = styles.get(i);
            style.mEntries.sort(Style.STYLE_ENTRY_COMPARATOR);
            // entry data
            valuesSize += ResTable_entry.SIZEOF_EXT + ResTable_map.SIZEOF * style.mEntries.size();
            entryIndexTable.put(style.mName, i);
        }

        int[] styleOffsets = new int[styles.size()];

        // allocate heap buffer
        ByteBuffer data = ByteBuffer.allocate(offsetsSize + valuesSize)
                .order(ByteOrder.nativeOrder());
        int offset = offsetsSize;
        for (int i = 0; i < styles.size(); i++) {
            var style = styles.get(i);
            styleOffsets[i] = offset - offsetsSize;
            data.putShort(offset + ResTable_entry.size, (short) ResTable_entry.SIZEOF_EXT);
            data.putShort(offset + ResTable_entry.flags, (short) ResTable_entry.FLAG_COMPLEX);

            int parentId = -1;
            if (style.mParent != null) {
                parentId = genId(style.mParent.namespace(), style.mParent.entry());
            }

            data.putInt(offset + ResTable_entry.parent, parentId);
            data.putInt(offset + ResTable_entry.count, style.mEntries.size());
            offset += ResTable_entry.SIZEOF_EXT;

            var entries = style.mEntries;
            for (int j = 0; j < entries.size(); j++) {
                var entry = entries.get(j);
                int mapName = genId(entry.ns, entry.attr);
                data.putInt(offset + ResTable_map.name, mapName);
                data.putShort(offset + ResTable_map.value + Res_value.type, (short) entry.dataType);
                data.putInt(offset + ResTable_map.value + Res_value.data, entry.data);
                offset += ResTable_map.SIZEOF;
            }
        }
        data.asIntBuffer().put(0, styleOffsets);
        assert offset == offsetsSize + valuesSize;

        LoadedPackage.TypeEntry typeEntry = new LoadedPackage.TypeEntry();
        typeEntry.id = (byte) (storeTypeString("style") + 1);
        typeEntry.entryCount = styles.size();
        typeEntry.entriesStart = ResTable_type.SIZEOF + offsetsSize;
        typeEntry.data = data;

        LoadedPackage.TypeSpec typeSpec = new LoadedPackage.TypeSpec(0, Collections.singletonList(typeEntry));
        typeSpec.id = typeEntry.id;
        typeSpec.entryIndexTable = entryIndexTable;

        resources.typeStringPool = new ResStringPool(mTypeStringArray.toArray(new String[0]));
        mTypeStringArray = null;
        resources.keyStringPool = new ResStringPool(mKeyStringArray.toArray(new String[0]));
        mKeyStringArray = null;

        resources.packageLibs[0] = mNamespace;
        for (int i = 0; i < mLibraryArray.size(); i++) {
            resources.packageLibs[i + 1] = mLibraryArray.get(i);
        }

        LoadedPackage loadedPackage = new LoadedPackage(mNamespace);
        loadedPackage.typeSpecs = new HashMap<>();
        loadedPackage.typeSpecs.put("style", typeSpec);

        resources.packages = new LoadedPackage[]{loadedPackage};

        PackAssets pack = new PackAssets();
        pack.mAssetsProvider = new EmptyAssetsProvider();
        pack.loadedResources = resources;

        ResourcesProvider result = new ResourcesProvider();
        result.mPackAssets = pack;

        return result;
    }

    public class Style {

        String mName;
        ResourceId mParent;

        record Entry(String ns, String attr, int dataType, int data) {
        }

        final ArrayList<Entry> mEntries = new ArrayList<>();

        static final Comparator<Entry> STYLE_ENTRY_COMPARATOR = (o1, o2) ->
                ResourceId.comparePair(o1.ns, o1.attr, o2.ns, o2.attr);

        Style(String name, ResourceId parent) {
            mName = name;
            mParent = parent;
        }

        public void addNull(@AttrRes String attr, boolean empty) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_NULL,
                    empty ? TypedValue.DATA_NULL_EMPTY : TypedValue.DATA_NULL_UNDEFINED)
            );
        }

        public void addDimension(@AttrRes String attr, float value, int units) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_DIMENSION,
                    TypedValue.createComplexDimension(value, units))
            );
        }

        public void addAttribute(@AttrRes String attr, @NonNull @AttrRes String target) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_ATTRIBUTE,
                    storeKeyString(target)));
        }

        public void addAttribute(@AttrRes String attr, @NonNull @AttrRes ResourceId target) {
            assert target.type().equals("attr") && target.namespace().equals(Resources.DEFAULT_NAMESPACE);
            addAttribute(attr, target.entry());
        }

        public void addReference(@AttrRes ResourceId attr, @Nullable @AnyRes ResourceId target) {
            assert attr.type().equals("attr") && attr.namespace().equals(Resources.DEFAULT_NAMESPACE);
            addReference(attr.entry(), target);
        }

        public void addReference(@AttrRes String attr, @Nullable @AnyRes ResourceId target) {
            assert target == null || target.namespace().equals(Resources.DEFAULT_NAMESPACE);
            attr = dedupKeyString(attr);
            int typeId = target == null ? 0 : storeTypeString(target.type()) + 1;
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_REFERENCE | (typeId << ResourceTypes.Res_value.DATA_TYPE_ID_SHIFT),
                    target == null ? 0 : storeKeyString(target.entry())));
        }

        public void addBoolean(@AttrRes String attr, boolean v) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_INT_BOOLEAN, v ? 1 : 0));
        }

        public void addColor(@AttrRes String attr, int argb) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_INT_COLOR_ARGB8, argb));
        }

        public void addInteger(@AttrRes String attr, int v) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_INT_DEC, v));
        }

        public void addEnum(@AttrRes String attr, int ord) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_INT_DEC, ord));
        }

        public void addFlags(@AttrRes String attr, int flags) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_INT_HEX, flags));
        }

        public void addColor(@AttrRes String attr, @NonNull BiFunction<Resources, Resources.Theme, ColorStateList> factory) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_FACTORY,
                    storeFactory(factory)));
        }

        public void addDrawable(@AttrRes String attr, @NonNull BiFunction<Resources, Resources.Theme, Drawable> factory) {
            attr = dedupKeyString(attr);
            mEntries.add(new Entry(R.ns, attr, TypedValue.TYPE_FACTORY,
                    storeFactory(factory)));
        }

        ResourcesBuilder getThemeBuilder() {
            return ResourcesBuilder.this;
        }
    }
}
