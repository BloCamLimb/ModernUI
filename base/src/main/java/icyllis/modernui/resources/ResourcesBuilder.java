/*
 * ModernUI.
 * Copyright (C) 2025-2026 BloCamLimb. All rights reserved.
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

import icyllis.modernui.annotation.AnyRes;
import icyllis.modernui.annotation.AttrRes;
import icyllis.modernui.annotation.StyleRes;
import icyllis.modernui.graphics.drawable.Drawable;
import icyllis.modernui.resources.ResourceTypes.ResTable_entry;
import icyllis.modernui.resources.ResourceTypes.ResTable_map;
import icyllis.modernui.resources.ResourceTypes.ResTable_type;
import icyllis.modernui.resources.ResourceTypes.Res_value;
import icyllis.modernui.util.ColorStateList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * This class allows to build an asset pack at runtime. The resources it creates
 * are all under a single namespace.
 */
// will move this to 'packager' module, so AssetPack is generated in advance to reduce runtime overhead;
// for simple use case, use InlineStyle instead
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

    // type -> entry -> (dataType << 32) | data
    HashMap<String, Object2LongOpenHashMap<String>> mValueTable = new HashMap<>();

    public ResourcesBuilder(@NonNull String namespace) {
        mNamespace = namespace;
        mTypeStringTable.defaultReturnValue(-1);
        mKeyStringTable.defaultReturnValue(-1);
        mGlobalStringTable.defaultReturnValue(-1);
        mFactoryTable.defaultReturnValue(-1);
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

    int storeFactory(BiFunction<Resources, Resources.Theme, ?> factory) {
        return mFactoryTable.computeIfAbsent(factory, mFactoryMapper);
    }

    int storeNamespace(String namespace) {
        return mLibraryTable.computeIfAbsent(namespace, mLibraryMapper);
    }

    int genId(String namespace, String entry) {
        int packageId = namespace.equals(mNamespace) ? 0 : storeNamespace(namespace) + 1;
        return (packageId << Res_value.PACKAGE_ID_SHIFT) | storeKeyString(entry);
    }

    /**
     * @return the namespace for all resources
     */
    public String getNamespace() {
        return mNamespace;
    }

    /**
     * Find an existing style builder.
     *
     * @param name the style name
     * @return the style builder for the style, if any
     * @see #newStyle(String, ResourceId)
     */
    @Nullable
    public Style getStyle(@NonNull @StyleRes String name) {
        return mStyleTable.get(name);
    }

    /**
     * Adds a style resource, returns the style builder to fill attribute values.
     * <p>
     * The final resource id is {@code new ResourceId(getNamespace(), "style", name)}.
     *
     * @param name   the style name
     * @param parent the parent of the style, if any
     * @return the style builder for the style
     */
    public Style newStyle(@NonNull @StyleRes String name,
                          @Nullable @StyleRes ResourceId parent) {
        assert parent == null || parent.type().equals("style");
        Style builder = mStyleTable.get(name);
        if (builder == null) {
            builder = new Style(name, parent);
            mStyleTable.put(name, builder);
            return builder;
        }
        if (Objects.equals(builder.mParent, parent)) {
            return builder;
        }
        throw new IllegalStateException(
                String.format("style %s is already defined with parent %s", name, builder.mParent));
    }

    /**
     * Adds a style resource, returns the style builder to fill attribute values.
     * <p>
     * For the resource id, its namespace must be {@link #getNamespace()},
     * and its type must be {@code "style"}.
     *
     * @param id     the style resource id
     * @param parent the parent of the style, if any
     * @return the style builder for the style
     * @see #newStyle(String, ResourceId)
     */
    public Style newStyle(@NonNull @StyleRes ResourceId id,
                          @Nullable @StyleRes ResourceId parent) {
        assert id.namespace().equals(mNamespace) && id.type().equals("style");
        return newStyle(id.entry(), parent);
    }

    @NonNull
    Object2LongOpenHashMap<String> getType(@NonNull String type) {
        return mValueTable.computeIfAbsent(type, __ -> new Object2LongOpenHashMap<>());
    }

    /**
     * Adds a string resource.
     *
     * @param id    the string resource id
     * @param value the resource value
     */
    public void addString(@NonNull @AnyRes ResourceId id,
                          @NonNull String value) {
        assert id.namespace().equals(mNamespace);
        getType(id.type()).put(id.entry(),
                ((long) Res_value.TYPE_STRING << 32) | storeGlobalString(value));
    }

    /**
     * Creates the asset pack. The builder cannot be reused.
     *
     * @param assetProvider the provider for additional assets, will be closed by the return value
     * @return a newly-created asset pack
     * @throws IllegalStateException the runtime limit is exceeded
     */
    public AssetPack build(@NonNull AssetProvider assetProvider) {
        ResourceMap resources = new ResourceMap();

        mGlobalStringTable = null;
        resources.globalStringPool = new ResStringPool(mGlobalStringArray.toArray(new String[0]));
        mGlobalStringArray = null;

        mFactoryTable = null;
        resources.factoryPool = mFactoryArray.toArray(new BiFunction[0]);
        mFactoryArray = null;

        ResourceMap.TypeSpec styleTypeSpec;
        buildStyles: {
            ArrayList<Style> styles = new ArrayList<>(mStyleTable.values());
            mStyleTable = null;
            if (styles.isEmpty()) {
                styleTypeSpec = null;
                break buildStyles;
            }

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
                    int mapName = storeKeyString(entry.attr);
                    data.putInt(offset + ResTable_map.name, mapName);
                    data.putShort(offset + ResTable_map.value + Res_value.type, (short) entry.dataType);
                    data.putInt(offset + ResTable_map.value + Res_value.data, entry.data);
                    offset += ResTable_map.SIZEOF;
                }
            }
            data.asIntBuffer().put(0, styleOffsets);
            assert offset == offsetsSize + valuesSize;

            ResourceMap.TypeEntry typeEntry = new ResourceMap.TypeEntry();
            typeEntry.id = (byte) (storeTypeString("style") + 1);
            typeEntry.entryCount = styles.size();
            typeEntry.entriesStart = ResTable_type.SIZEOF + offsetsSize;
            typeEntry.config = new ResConfig();
            typeEntry.data = data;

            styleTypeSpec = new ResourceMap.TypeSpec(0, Collections.singletonList(typeEntry));
            styleTypeSpec.id = typeEntry.id;
            styleTypeSpec.entryIndexTable = entryIndexTable;
        }

        HashMap<String, ResourceMap.TypeSpec> typeSpecs = new HashMap<>();

        if (styleTypeSpec != null) {
            typeSpecs.put("style", styleTypeSpec);
        }

        for (var typeE : mValueTable.entrySet()) {
            String typeName = typeE.getKey();
            Object2LongOpenHashMap<String> typeValues = typeE.getValue();

            if (typeValues.isEmpty()) {
                continue;
            }

            Object2IntOpenHashMap<String> entryIndexTable = new Object2IntOpenHashMap<>(typeValues.size());
            entryIndexTable.defaultReturnValue(-1);
            int valuesSize = ResTable_entry.SIZEOF * typeValues.size();
            int offsetsSize = 4 * typeValues.size();
            int i = 0;

            int[] valueOffsets = new int[typeValues.size()];

            // allocate heap buffer
            ByteBuffer data = ByteBuffer.allocate(offsetsSize + valuesSize)
                    .order(ByteOrder.nativeOrder());
            int offset = offsetsSize;
            for (var valueE : typeValues.object2LongEntrySet()) {
                entryIndexTable.put(valueE.getKey(), i);

                long packedValue = valueE.getLongValue();

                valueOffsets[i] = offset - offsetsSize;
                data.putShort(offset + ResTable_entry.dataType, (short) (packedValue >>> 32));
                data.putShort(offset + ResTable_entry.flags, (short) 0);
                data.putInt(offset + ResTable_entry.data, (int) packedValue);

                offset += ResTable_entry.SIZEOF;
            }
            data.asIntBuffer().put(0, valueOffsets);
            assert offset == offsetsSize + valuesSize;

            ResourceMap.TypeEntry typeEntry = new ResourceMap.TypeEntry();
            typeEntry.id = (byte) (storeTypeString(typeName) + 1);
            typeEntry.entryCount = typeValues.size();
            typeEntry.entriesStart = ResTable_type.SIZEOF + offsetsSize;
            typeEntry.config = new ResConfig();
            typeEntry.data = data;

            var typeSpec = new ResourceMap.TypeSpec(0, Collections.singletonList(typeEntry));
            typeSpec.id = typeEntry.id;
            typeSpec.entryIndexTable = entryIndexTable;
            typeSpecs.put(typeName, typeSpec);
        }

        resources.typeStringPool = new ResStringPool(mTypeStringArray.toArray(new String[0]));
        mTypeStringArray = null;
        resources.keyStringPool = new ResStringPool(mKeyStringArray.toArray(new String[0]));
        mKeyStringArray = null;

        resources.namespaces[0] = mNamespace;
        for (int i = 0; i < mLibraryArray.size(); i++) {
            resources.namespaces[i + 1] = mLibraryArray.get(i);
        }

        ResourceMap.NamespaceSpec namespaceSpec = new ResourceMap.NamespaceSpec(mNamespace);
        namespaceSpec.typeSpecs = typeSpecs;

        resources.namespaceSpecs = new ResourceMap.NamespaceSpec[]{namespaceSpec};

        return new AssetPack(assetProvider, resources);
    }

    /**
     * A style under construction.
     */
    public class Style {

        String mName;
        ResourceId mParent;

        record Entry(String attr, int dataType, int data) {
        }

        final ArrayList<Entry> mEntries = new ArrayList<>();

        static final Comparator<Entry> STYLE_ENTRY_COMPARATOR = Comparator.comparing(
                Entry::attr
        );

        Style(String name, ResourceId parent) {
            mName = name;
            mParent = parent;
        }

        public void addNull(@NonNull String attr, boolean empty) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_NULL,
                    empty ? TypedValue.DATA_NULL_EMPTY : TypedValue.DATA_NULL_UNDEFINED)
            );
        }

        public void addDimension(@NonNull String attr,
                                 float value, @TypedValue.ComplexDimensionUnit int units) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_DIMENSION,
                    TypedValue.createComplexDimension(value, units))
            );
        }

        public void addAttribute(@NonNull String attr, @NonNull String target) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_ATTRIBUTE,
                    storeKeyString(target)));
        }

        /**
         * @deprecated namespace and type are not used in attribute name anymore, this will be removed in 4.0
         */
        @Deprecated
        public void addAttribute(@NonNull String attr, @NonNull @AttrRes ResourceId target) {
            assert target.type().equals("attr");
            addAttribute(attr, target.entry());
        }

        public void addReference(@NonNull String attr, @Nullable @AnyRes ResourceId target) {
            int typeId = target == null ? 0 : storeTypeString(target.type()) + 1;
            mEntries.add(new Entry(attr,
                    TypedValue.TYPE_REFERENCE | (typeId << ResourceTypes.Res_value.DATA_TYPE_ID_SHIFT),
                    target == null ? -1 : genId(target.namespace(), target.entry())));
        }

        /**
         * @deprecated namespace and type are not used in attribute name anymore, this will be removed in 4.0
         */
        @Deprecated
        public void addReference(@NonNull @AttrRes ResourceId attr, @Nullable @AnyRes ResourceId target) {
            assert attr.type().equals("attr");
            addReference(attr.entry(), target);
        }

        public void addBoolean(@NonNull String attr, boolean v) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_BOOLEAN, v ? 1 : 0));
        }

        public void addColor(@NonNull String attr, int argb) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_COLOR_ARGB8, argb));
        }

        public void addInteger(@NonNull String attr, int v) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_DEC, v));
        }

        public void addEnum(@NonNull String attr, int ord) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_DEC, ord));
        }

        public void addFlags(@NonNull String attr, int flags) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_INT_HEX, flags));
        }

        public void addColor(@NonNull String attr,
                             @NonNull BiFunction<Resources, Resources.Theme, ColorStateList> factory) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_FACTORY,
                    storeFactory(factory)));
        }

        public void addDrawable(@NonNull String attr,
                                @NonNull BiFunction<Resources, Resources.Theme, Drawable> factory) {
            mEntries.add(new Entry(attr, TypedValue.TYPE_FACTORY,
                    storeFactory(factory)));
        }

        ResourcesBuilder getThemeBuilder() {
            return ResourcesBuilder.this;
        }
    }
}
