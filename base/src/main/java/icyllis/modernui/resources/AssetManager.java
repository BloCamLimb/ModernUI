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

import com.ibm.icu.util.LocaleMatcher;
import icyllis.modernui.annotation.GuardedBy;
import icyllis.modernui.annotation.Immutable;
import icyllis.modernui.resources.ResourceTypes.*;
import icyllis.modernui.resources.ResourceMap.*;
import icyllis.modernui.system.LocaleList;
import icyllis.modernui.util.Log;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

/**
 * @hide
 * @hidden
 */
@Immutable
@ApiStatus.Internal
public final class AssetManager {

    public static final Marker MARKER = MarkerFactory.getMarker("AssetManager");

    public static final int kInvalidCookie = -1;
    public static final int kMaxIterations = 20;

    private static final ResolvedBag SENTINEL_BAG = new ResolvedBag(null, null, 0);

    private static final class FilteredNamespace {

        // this map cannot be empty
        final HashMap<String, FilteredType> filteredTypes;
        final int cookie;

        FilteredNamespace(HashMap<String, FilteredType> filteredTypes,
                          int cookie) {
            this.filteredTypes = filteredTypes;
            this.cookie = cookie;
        }
    }

    private static final class FilteredType {

        final TypeSpec typeSpec;
        // this is a pre-filtered list, a subset of typeSpec.typeEntries
        // this list can be empty
        final TypeEntry[] filteredTypeEntries;

        FilteredType(TypeSpec typeSpec,
                     TypeEntry[] filteredTypeEntries) {
            this.typeSpec = typeSpec;
            this.filteredTypeEntries = filteredTypeEntries;
        }
    }

    public static final class Builder {
        private final LinkedHashSet<AssetPack> mAssetPacks = new LinkedHashSet<>();

        private LocaleList mLocales = LocaleList.getEmptyLocaleList();

        // Smaller indices are given priority.
        public Builder addAssetPack(@NonNull AssetPack assetPack) {
            Objects.requireNonNull(assetPack);
            mAssetPacks.add(assetPack);
            return this;
        }

        // Smaller indices are given priority.
        public Builder setLocales(@NonNull LocaleList locales) {
            Objects.requireNonNull(locales);
            mLocales = locales;
            return this;
        }

        @NonNull
        public AssetManager build() {
            final AssetPack[] assetPacks = new AssetPack[mAssetPacks.size()];

            LocaleList locales = mLocales;
            // "und" should also fall into this category, but we don't use that in ModernUI
            boolean matchRootOnly = locales.isEmpty() ||
                    (locales.size() == 1 && locales.get(0).equals(Locale.ROOT));

            HashMap<String, ArrayList<FilteredNamespace>> groups = new HashMap<>();
            int cookie = 0;
            // first-pass: remove entries that cannot be matched by any input
            ArrayList<TypeEntry> tmpTypeEntries = new ArrayList<>();
            LocaleMatcher localeMatcher = null;
            for (var pack : mAssetPacks) {
                var resources = pack.getResources();
                for (var namespaceSpec : resources.getNamespaceSpecs()) {
                    if (namespaceSpec.typeSpecs.isEmpty()) {
                        continue;
                    }
                    HashMap<String, FilteredType> types = new HashMap<>();
                    for (var e : namespaceSpec.typeSpecs.entrySet()) {
                        tmpTypeEntries.clear();
                        for (var typeEntry : e.getValue().typeEntries) {
                            // any density, variant, numberingSystem could match;
                            // so we only filter LSR using likely subtags
                            boolean isRoot = typeEntry.config.baseLocale.equals(Locale.ROOT);
                            if (!matchRootOnly && !isRoot) {
                                if (localeMatcher == null) {
                                    localeMatcher = LocaleMatcher.builder()
                                            .build();
                                }
                                for (int locIdx = 0; locIdx < locales.size(); locIdx++) {
                                    var desiredLocale = locales.get(locIdx);
                                    if (localeMatcher.isMatch(
                                            desiredLocale, typeEntry.config.baseLocale)) {
                                        tmpTypeEntries.add(typeEntry);
                                        break;
                                    }
                                }
                            }
                            if (isRoot) {
                                tmpTypeEntries.add(typeEntry);
                            }
                        }
                        types.put(e.getKey(),
                                new FilteredType(e.getValue(),
                                        tmpTypeEntries.toArray(new TypeEntry[0])));
                    }

                    groups.computeIfAbsent(
                                    namespaceSpec.getNamespace(), __ -> new ArrayList<>())
                            .add(new FilteredNamespace(types, cookie));
                }
                assetPacks[cookie++] = pack;
            }
            // second-pass: cross-pack type entry dedup
            HashMap<String, HashSet<ResConfig>> globalDedup = new HashMap<>();
            for (var group : groups.entrySet()) {
                if (group.getValue().size() <= 1) {
                    // there's only one namespace spec
                    continue;
                }
                globalDedup.values().forEach(HashSet::clear);
                for (var ns : group.getValue()) {
                    for (var te : ns.filteredTypes.entrySet()) {
                        var dedup = globalDedup.computeIfAbsent(te.getKey(), __ -> new HashSet<>());
                        tmpTypeEntries.clear();
                        boolean didDedup = false;
                        for (var e : te.getValue().filteredTypeEntries) {
                            if (dedup.add(e.config)) {
                                tmpTypeEntries.add(e);
                            } else {
                                // there's already a type entry with the exact same config, this can't be
                                // any better than the previous one, so it's removed
                                didDedup = true;
                            }
                        }
                        if (didDedup) {
                            te.setValue(new FilteredType(te.getValue().typeSpec,
                                    tmpTypeEntries.toArray(new TypeEntry[0])));
                        }
                    }
                }
            }
            // finalize
            HashMap<String, FilteredNamespace[]> finalGroups = new HashMap<>(
                    (int) Math.ceil(groups.size() / 0.75), 0.75f);
            for (var e : groups.entrySet()) {
                finalGroups.put(e.getKey(), e.getValue().toArray(new FilteredNamespace[0]));
            }

            return new AssetManager(assetPacks, locales, finalGroups);
        }
    }

    private AssetManager(@NonNull AssetPack @NonNull[] assetPacks,
                         @NonNull LocaleList locales,
                         @NonNull HashMap<String, FilteredNamespace[]> groups) {
        mAssetPacks = assetPacks;
        mGroups = groups;
        mLocales = locales;
    }

    @NonNull
    @Unmodifiable
    public List<@NonNull AssetPack> getAssetPacks() {
        //noinspection Java9CollectionFactory
        return Collections.unmodifiableList(Arrays.asList(mAssetPacks));
    }

    @NonNull
    public LocaleList getLocales() {
        return mLocales;
    }

    @Nullable
    public Asset getAsset(@NonNull String path) {
        return getNonAsset("assets/" + path);
    }

    @Nullable
    public Asset getNonAsset(@NonNull String path) {
        for (var pack : mAssetPacks) {
            Asset asset = pack.getAssets().getAsset(path);
            if (asset != null) {
                return asset;
            }
        }
        return null;
    }

    @Nullable
    public Asset getNonAsset(@NonNull String path, int cookie) {
        var pack = getPack(cookie);
        if (pack != null) {
            return pack.getAssets().getAsset(path);
        }
        return null;
    }

    public boolean getResource(@NonNull ResourceId resId,
                               int densityDpi,
                               @NonNull TypedValue outValue) {
        Object entry = findEntry(resId.namespace(), resId.type(), resId.entry(),
                densityDpi, outValue);
        return entry == outValue;
    }

    @Nullable
    public ResolvedBag getBag(@NonNull ResourceId resId) {
        ResolvedBag cached;
        mBagCacheLock.readLock().lock();
        try {
            cached = mCachedBags.get(resId);
        } finally {
            mBagCacheLock.readLock().unlock();
        }
        if (cached != SENTINEL_BAG) {
            return cached;
        }

        mBagCacheLock.writeLock().lock();
        try {
            return resolveBagLocked(resId, null, null);
        } finally {
            mBagCacheLock.writeLock().unlock();
        }
    }

    @Nullable
    private ResolvedBag resolveBagLocked(@NonNull ResourceId resId, List<ResourceId> childResIds,
                                         TypedValue value) {
        ResolvedBag cached = mCachedBags.get(resId);
        if (cached != SENTINEL_BAG) {
            return cached;
        }

        if (value == null) {
            value = new TypedValue();
        }

        Object entry = findEntry(resId.namespace(), resId.type(), resId.entry(),
                0, value);

        ByteBuffer map = null;

        if (entry instanceof ByteBuffer) {
            map = (ByteBuffer) entry;
        }

        if (map == null) {
            if (childResIds != null && !childResIds.isEmpty()) {
                // If the parent bag we are looking for does not exist or is not a bag, cache it.
                // Because the resId is declared within the package at this point.
                // Otherwise, the resId is likely random and not worth caching.
                mCachedBags.put(resId, null);
            }
            return null;
        }

        int entrySize = map.getShort(ResTable_entry.size) & 0xFFFF;
        int parentId = map.getInt(ResTable_entry.parent);
        int entryCount = map.getInt(ResTable_entry.count);
        int cookie = value.cookie;
        int typeFlags = value.flags;
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null; // impossible
        }

        // Keep track of ids that have already been seen to prevent infinite loops caused by circular
        // dependencies between bags.
        if (childResIds == null) {
            childResIds = new ArrayList<>();
        }
        childResIds.add(resId);

        ResourceId parentResId = null;
        if (parentId != -1) {
            parentResId = resources.lookupParentId(resId, parentId);
            if (parentResId == null) {
                // parent id is malformed, no need to cache
                return null;
            }
        }

        ResolvedBag parentBag = null;
        if (parentResId == null || childResIds.contains(parentResId)) {
            // There is no parent or a circular parental dependency exist, meaning there is nothing to
            // inherit and we can do a simple copy of the entries in the map.
        } else {
            parentBag = resolveBagLocked(parentResId, childResIds, value);
            if (parentBag == null) {
                Log.LOGGER.error(MARKER, "Failed to find parent '{}' of bag '{}'",
                        parentResId, resId);
                mCachedBags.put(resId, null);
                return null;
            }
        }

        ResolvedBag newBag = null;
        int parentCount = parentBag != null ? parentBag.getEntryCount() : 0;
        int maxCount = parentCount + entryCount;
        fill_entries://ttt
        if (maxCount > 0) {
            if (entryCount > 0) {
                AttributeMerger merger = new AttributeMerger();
                merger.reset(parentBag != null ? parentBag.keys : null, 0, parentCount,
                        parentBag != null ? parentBag.values : null,
                        ResolvedBag.VALUE_COLUMNS, entryCount);
                int[] childValues = new int[ResolvedBag.VALUE_COLUMNS];
                childValues[ResolvedBag.COLUMN_COOKIE] = cookie;
                int childOffset = entrySize;

                for (int childIndex = 0; childIndex < entryCount; childIndex++) {
                    int mapName = map.getInt(childOffset + ResTable_map.name);
                    childValues[ResolvedBag.COLUMN_TYPE] =
                            map.getShort(childOffset + ResTable_map.value + Res_value.type) & 0xFFFF;
                    childValues[ResolvedBag.COLUMN_DATA] =
                            map.getInt(childOffset + ResTable_map.value + Res_value.data);

                    String attrName = resources.getKeyStringPool()
                            .getStringAt(mapName);
                    if (attrName == null) {
                        // malformed, returns null bag
                        Log.LOGGER.error(MARKER, "Failed to find map name 0x{} from bag '{}'",
                                Integer.toHexString(mapName), resId);
                        break fill_entries;
                    }
                    try {
                        merger.append(attrName, childValues);
                    } catch (IllegalArgumentException e) {
                        // malformed, returns null bag
                        Log.LOGGER.error(MARKER, "Failed to append attribute '{}' to bag '{}'",
                                attrName, resId, e);
                        break fill_entries;
                    }
                    childOffset += ResTable_map.SIZEOF;
                }

                newBag = new ResolvedBag(
                        merger.getResultKeys(),
                        merger.getResultValues(),
                        // Combine flags from the parent and our own bag.
                        typeFlags | (parentBag != null ? parentBag.typeSpecFlags : 0)
                );
            } else {
                // Child is empty, just set to parent
                newBag = new ResolvedBag(
                        parentBag.keys,
                        parentBag.values,
                        // Combine flags from the parent and our own bag.
                        typeFlags | parentBag.typeSpecFlags
                );
            }
        }
        mCachedBags.put(resId, newBag);
        return newBag;
    }

    @Nullable
    private Object findEntry(String namespace, String typeString, String entryString,
                             int desiredDensity, @NonNull TypedValue outValue) {
        if (namespace == null || namespace.isEmpty() ||
                typeString == null || typeString.isEmpty() ||
                entryString == null || entryString.isEmpty()) {
            return null;
        }

        var group = mGroups.get(namespace);
        if (group == null) {
            return null;
        }

        return findEntryInternal(group, typeString, entryString, desiredDensity, outValue);
    }

    @Nullable
    private Object findEntryInternal(@NonNull FilteredNamespace @NonNull[] group,
                                     @NonNull String typeString, @NonNull String entryString,
                                     int desiredDensity, @NonNull TypedValue outValue) {
        int bestCookie = kInvalidCookie;
        TypeEntry bestType = null;
        int bestOffset = 0;
        int typeFlags = 0;

        for (var ns : group) {
            var type = ns.filteredTypes.get(typeString);
            if (type == null) {
                continue;
            }
            TypeSpec typeSpec = type.typeSpec;
            int entryIndex = typeSpec.getEntryIndexByName(entryString);
            if (entryIndex == -1) {
                continue;
            }
            typeFlags |= typeSpec.getFlagsForEntryIndex(entryIndex);

            for (var typeEntry : type.filteredTypeEntries) {

                int offset = ResourceMap.getEntryOffset(typeEntry, entryIndex);

                if (offset == ResTable_type.NO_ENTRY) {
                    continue;
                }

                bestCookie = ns.cookie;
                bestType = typeEntry;
                bestOffset = offset;

                break;
            }
        }

        if (bestCookie == kInvalidCookie) {
            return null;
        }

        Object bestEntry = ResourceMap.getEntryFromOffset(bestType, bestOffset, outValue);
        if (bestEntry == null) {
            return null;
        }

        outValue.cookie = bestCookie;
        outValue.flags = typeFlags;

        return bestEntry;
    }

    @Nullable
    CharSequence getPooledStringForCookie(int cookie, int id) {
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.getGlobalStringPool().getSequenceAt(id);
    }

    @Nullable
    ResourceId getReferenceIdForCookie(int cookie, int typeId, int data) {
        assert typeId > 0;
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.lookupReferenceId(typeId, data);
    }

    @Nullable
    String getAttributeIdForCookie(int cookie, int data) {
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.getKeyStringPool().getStringAt(data);
    }

    @Nullable
    BiFunction<?, ?, ?> getFactoryForCookie(int cookie, int data) {
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.getFactory(data);
    }

    // do post-processing before exposing it to public API users,
    // freeze the 'object' field
    boolean postProcess(@NonNull TypedValue outValue) {
        switch (outValue.type & ResourceTypes.Res_value.DATA_TYPE_MASK) {
            case TypedValue.TYPE_REFERENCE -> {
                int typeId =
                        (outValue.type & ResourceTypes.Res_value.DATA_TYPE_ID_MASK) >>> ResourceTypes.Res_value.DATA_TYPE_ID_SHIFT;
                if (typeId != 0) {
                    // erase higher 8 bits
                    outValue.type = TypedValue.TYPE_REFERENCE;
                    if ((outValue.object = getReferenceIdForCookie(outValue.cookie, typeId, outValue.data)) == null) {
                        return false;
                    }
                } else {
                    outValue.object = null;
                }
            }
            case TypedValue.TYPE_ATTRIBUTE -> {
                if ((outValue.object = getAttributeIdForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
                }
            }
            case TypedValue.TYPE_STRING -> {
                if ((outValue.object = getPooledStringForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
                }
            }
            case TypedValue.TYPE_FACTORY -> {
                if ((outValue.object = getFactoryForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
                }
            }
            default -> outValue.object = null;
        }
        return true;
    }

    @Nullable
    public AssetPack getPack(int cookie) {
        if (cookie < 0 || cookie >= mAssetPacks.length) {
            return null;
        }
        return mAssetPacks[cookie];
    }

    @Nullable
    public ResourceMap getResources(int cookie) {
        AssetPack assets = getPack(cookie);
        if (assets == null) {
            return null;
        }
        return assets.getResources();
    }

    private final AssetPack[] mAssetPacks;
    private final LocaleList mLocales;
    // Map namespace to a list of pre-filtered namespace specs, the value cannot be empty array
    private final HashMap<String, FilteredNamespace[]> mGroups;

    private final ReentrantReadWriteLock mBagCacheLock = new ReentrantReadWriteLock();
    // Cached set of bags. Since lookup miss rate is very low, use
    // OpenHashMap instead of HashMap to save memory (this map can be big)
    @GuardedBy("mBagCacheLock")
    private final Object2ObjectOpenHashMap<ResourceId, ResolvedBag> mCachedBags = new Object2ObjectOpenHashMap<>();

    {
        mCachedBags.defaultReturnValue(SENTINEL_BAG);
    }
}
