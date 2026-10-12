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

import icyllis.modernui.resources.ResourceTypes.*;
import icyllis.modernui.util.Log;
import it.unimi.dsi.fastutil.ints.IntArrayList;
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
import java.util.List;

/**
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public final class AssetManager {

    public static final Marker MARKER = MarkerFactory.getMarker("AssetManager");

    public static final int kInvalidCookie = -1;
    public static final int kMaxIterations = 20;

    public static class ResolvedBag {
        public static final int COLUMN_TYPE = 0;
        public static final int COLUMN_DATA = 1;
        public static final int COLUMN_COOKIE = 2;
        public static final int VALUE_COLUMNS = 3;

        public int typeSpecFlags;

        /*
         * This holds attribute names, sorted in lexicographic order.
         */
        public String[] keys;
        /*
            struct Value {
                uint32_t type;
                uint32_t data;
                int32_t cookie;
            }
         */
        public int[] values;

        public int type(int index) {
            return values[index * VALUE_COLUMNS + COLUMN_TYPE];
        }

        public int data(int index) {
            return values[index * VALUE_COLUMNS + COLUMN_DATA];
        }

        public int cookie(int index) {
            return values[index * VALUE_COLUMNS + COLUMN_COOKIE];
        }

        public int getEntryCount() {
            assert values == null ||
                    (values.length / VALUE_COLUMNS) == keys.length;
            return keys != null ? keys.length : 0;
        }
    }

    private static final ResolvedBag SENTINEL_BAG = new ResolvedBag();

    public static class PackageGroup {
        // The following three arrays are parallel arrays
        public final ArrayList<LoadedPackage> packages = new ArrayList<>();

        // Array of maps from type index to prefiltered collection of configurations
        // that match the current AssetManager configuration
        public final ArrayList<ArrayList<LoadedPackage.TypeEntry>[]> filteredConfigs = new ArrayList<>();

        public final IntArrayList cookies = new IntArrayList();
    }

    public static final class Builder {
        private final ArrayList<AssetPack> mAssetPacks = new ArrayList<>();
        private final ArrayList<ResourcesLoader> mLoaders = new ArrayList<>();

        private boolean mNoInit = false;

        // Smaller indices are given priority.
        public Builder addAssetPack(AssetPack assetPack) {
            mAssetPacks.add(assetPack);
            return this;
        }

        // Larger indices are given priority.
        public Builder addLoader(ResourcesLoader loader) {
            mLoaders.add(loader);
            return this;
        }

        public Builder setNoInit() {
            mNoInit = true;
            return this;
        }

        @NonNull
        public AssetManager build() {
            // Filter AssetPack so that assets provided by multiple loaders are only included once
            // in the AssetManager assets. The last appearance of the AssetPack dictates its load
            // order.
            final ArrayList<AssetPack> loaderAssetPacks = new ArrayList<>();
            final HashSet<AssetPack> uniqueLoaderAssetPacks = new HashSet<>();
            for (int i = mLoaders.size() - 1; i >= 0; i--) {
                final List<AssetPack> currentLoaderAssetPacks = mLoaders.get(i).getAssetPacks();
                for (int j = currentLoaderAssetPacks.size() - 1; j >= 0; j--) {
                    final AssetPack assetPack = currentLoaderAssetPacks.get(j);
                    if (uniqueLoaderAssetPacks.add(assetPack)) {
                        loaderAssetPacks.add(assetPack);
                    }
                }
            }

            final int totalAssetPackCount = mAssetPacks.size()
                    + loaderAssetPacks.size();
            final AssetPack[] assetPacks = new AssetPack[totalAssetPackCount];

            for (int i = 0, n = mAssetPacks.size(); i < n; i++) {
                assetPacks[i] = mAssetPacks.get(i);
            }

            for (int i = 0, n = loaderAssetPacks.size(); i < n; i++) {
                assetPacks[i + mAssetPacks.size()] =
                        loaderAssetPacks.get(i);
            }

            return new AssetManager(assetPacks, mNoInit,
                    mLoaders.isEmpty() ? null
                            : mLoaders.toArray(new ResourcesLoader[0]));
        }
    }

    private AssetManager(AssetPack @NonNull[] assetPacks, boolean preset,
            ResourcesLoader @Nullable[] loaders) {

        HashMap<String, PackageGroup> packageGroups = new HashMap<>();
        for (int i = 0; i < assetPacks.length; i++) {
            var pack = assetPacks[i];
            int cookie = i;

            var resources = pack.getResources();

            for (var loadedPackage : resources.getPackages()) {
                var pkgGroup = packageGroups.computeIfAbsent(
                        loadedPackage.getPackageName(), __ -> new PackageGroup());

                pkgGroup.packages.add(loadedPackage);
                pkgGroup.cookies.add(cookie);
            }
        }

        mLoaders = loaders;
        mAssetPacks = assetPacks;
        mPackageGroups = packageGroups;
    }

    @NonNull
    @Unmodifiable
    public List<AssetPack> getAssetPacks() {
        return Arrays.asList(mAssetPacks);
    }

    @NonNull
    @Unmodifiable
    public List<ResourcesLoader> getLoaders() {
        return mLoaders == null ? Collections.emptyList() : Arrays.asList(mLoaders);
    }

    private void invalidateCachesLocked(int diff) {

        if (diff == ~0) {
            mCachedBags.clear();
            return;
        }

        mCachedBags.values().removeIf(b -> (b.typeSpecFlags & diff) != 0);
    }

    @Nullable
    public Asset getNonAsset(String path) {
        for (var pack : mAssetPacks) {
            Asset asset = pack.getAssets().getAsset(path);
            if (asset != null) {
                return asset;
            }
        }
        return null;
    }

    @Nullable
    public Asset getNonAsset(String path, int cookie) {
        var pack = getPack(cookie);
        if (pack != null) {
            return pack.getAssets().getAsset(path);
        }
        return null;
    }

    public boolean getResource(@NonNull ResourceId resId,
                               @NonNull TypedValue outValue) {
        Object entry = findEntry(resId.namespace(), resId.type(), resId.entry(),
                outValue);
        return entry == outValue;
    }

    @Nullable
    public ResolvedBag getBag(@NonNull ResourceId resId) {
        synchronized (this) {
            return getBag(resId, null, null);
        }
    }

    @Nullable
    private ResolvedBag getBag(@NonNull ResourceId resId, List<ResourceId> childResIds,
                             TypedValue value) {
        ResolvedBag cached = mCachedBags.get(resId);
        if (cached != SENTINEL_BAG) {
            return cached;
        }

        if (value == null) {
            value = new TypedValue();
        }

        Object entry = findEntry(resId.namespace(), resId.type(), resId.entry(),
                value);

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
            parentResId = resources.lookupResourceId(resId, parentId, 0);
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
            parentBag = getBag(parentResId, childResIds, value);
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

                newBag = new ResolvedBag();
                newBag.keys = merger.getResultKeys();
                newBag.values = merger.getResultValues();
                // Combine flags from the parent and our own bag.
                newBag.typeSpecFlags = typeFlags | (parentBag != null ? parentBag.typeSpecFlags : 0);
            } else {
                // Child is empty, just set to parent
                newBag = new ResolvedBag();
                newBag.keys = parentBag.keys;
                newBag.values = parentBag.values;
                // Combine flags from the parent and our own bag.
                newBag.typeSpecFlags = typeFlags | parentBag.typeSpecFlags;
            }
        }
        mCachedBags.put(resId, newBag);
        return newBag;
    }

    @Nullable
    private Object findEntry(String namespace, String typeString, String entryString,
                             @NonNull TypedValue outValue) {
        if (namespace == null || namespace.isEmpty() ||
                typeString == null || typeString.isEmpty() ||
                entryString == null || entryString.isEmpty()) {
            return null;
        }

        PackageGroup packageGroup = mPackageGroups.get(namespace);
        if (packageGroup == null) {
            return null;
        }

        synchronized (this) {
            return findEntryInternal(packageGroup, typeString, entryString, outValue);
        }
    }

    @Nullable
    private Object findEntryInternal(@NonNull PackageGroup packageGroup,
                                     @NonNull String typeString, @NonNull String entryString,
                                     @NonNull TypedValue outValue) {
        int bestCookie = kInvalidCookie;
        LoadedPackage bestPackage = null;
        LoadedPackage.TypeEntry bestType = null;
        int bestOffset = 0;
        int typeFlags = 0;

        int packageCount = packageGroup.packages.size();
        for (int pi = 0; pi < packageCount; pi++) {
            LoadedPackage loadedPackage = packageGroup.packages.get(pi);
            int cookie = packageGroup.cookies.getInt(pi);

            LoadedPackage.TypeSpec typeSpec = loadedPackage.getTypeSpecByName(typeString);
            if (typeSpec == null) {
                continue;
            }

            int entryIndex = typeSpec.getEntryIndexByName(entryString);
            if (entryIndex == -1) {
                continue;
            }
            typeFlags |= typeSpec.getFlagsForEntryIndex(entryIndex);

            int typeEntryCount = typeSpec.typeEntries.length;
            for (int i = 0; i < typeEntryCount; i++) {
                LoadedPackage.TypeEntry typeEntry = typeSpec.typeEntries[i];

                int offset = loadedPackage.getEntryOffset(typeEntry, entryIndex);

                if (offset == ResTable_type.NO_ENTRY) {
                    continue;
                }

                bestCookie = cookie;
                bestPackage = loadedPackage;
                bestType = typeEntry;
                bestOffset = offset;

                break;
            }
        }

        if (bestCookie == kInvalidCookie) {
            return null;
        }

        Object bestEntry = bestPackage.getEntryFromOffset(bestType, bestOffset, outValue);
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
        return resources.lookupResourceId(null, data, typeId);
    }

    @Nullable
    String getAttributeIdForCookie(int cookie, int data) {
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.getKeyStringPool().getStringAt(data);
    }

    // do post-processing before exposing it to public API users,
    // freeze the 'object' field
    boolean postProcess(@NonNull TypedValue outValue) {
        switch (outValue.type & ResourceTypes.Res_value.DATA_TYPE_MASK) {
            case TypedValue.TYPE_STRING -> {
                if ((outValue.object = getPooledStringForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
                }
            }
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
    private final HashMap<String, PackageGroup> mPackageGroups;

    private final ResourcesLoader[] mLoaders;

    // Cached set of bags. We don't need the best lookup performance, use
    // OpenHashMap instead of HashMap to save memory (this map can be big)
    private final Object2ObjectOpenHashMap<ResourceId, ResolvedBag> mCachedBags = new Object2ObjectOpenHashMap<>();

    {
        mCachedBags.defaultReturnValue(SENTINEL_BAG);
    }
}
