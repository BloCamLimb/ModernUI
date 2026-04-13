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

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.resources.ResourceTypes.*;
import icyllis.modernui.util.Log;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @hide
 * @hidden
 */
@ApiStatus.Internal
public class AssetManager {

    public static final Marker MARKER = MarkerFactory.getMarker("AssetManager");

    private static final Object sLock = new Object();

    private static final PackAssets[] sEmptyPackAssets = {};

    @GuardedBy("sLock")
    private static AssetManager sSystem;

    @GuardedBy("sLock")
    private static PackAssets[] sSystemPackAssets;
    @GuardedBy("sLock")
    private static Set<PackAssets> sSystemPackAssetsSet;

    @GuardedBy("sLock")
    private static void createSystemAssets() {
        if (sSystem != null) {
            return;
        }

        try {
            ResourcesBuilder resourcesBuilder = new ResourcesBuilder(R.ns);
            SystemTheme.addToResources(resourcesBuilder);
            PackAssets pack = resourcesBuilder.buildPack(new EmptyAssetsProvider());

            sSystemPackAssetsSet = Set.of(pack);
            sSystemPackAssets = new PackAssets[]{pack};
            sSystem = new AssetManager(true);
            sSystem.setPackAssets(sEmptyPackAssets, false);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create system AssetManager", e);
        }
    }

    public static final int kInvalidCookie = -1;
    public static final int kMaxIterations = 20;

    public static class ResolvedBag {
        public static final int COLUMN_TYPE = 0;
        public static final int COLUMN_DATA = 1;
        public static final int COLUMN_COOKIE = 2;
        public static final int VALUE_COLUMNS = 3;

        public int typeSpecFlags;

        /*
         * This holds (namespace, attribute name) pairs, sorted by namespace and then
         * by attribute name in lexicographic order.
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

        public String namespace(int index) {
            return keys[index << 1];
        }

        public String attribute(int index) {
            return keys[(index << 1) + 1];
        }

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
                    (values.length / VALUE_COLUMNS) == (keys.length >> 1);
            return keys != null ? keys.length >> 1 : 0;
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

    @Deprecated
    public AssetManager() {
    }

    private AssetManager(boolean sentinel) {

    }

    /**
     * Return a global shared asset manager that provides access to only
     * system assets (no application assets).
     */
    public static AssetManager getSystem() {
        synchronized (sLock) {
            createSystemAssets();
            return sSystem;
        }
    }

    public void setPackAssets(@NonNull PackAssets[] packs, boolean invalidateCaches) {
        PackAssets[] newPacks = new PackAssets[sSystemPackAssets.length + packs.length];

        System.arraycopy(sSystemPackAssets, 0, newPacks, 0, sSystemPackAssets.length);

        int newLength = sSystemPackAssets.length;
        for (PackAssets pack : packs) {
            if (!sSystemPackAssetsSet.contains(pack)) {
                newPacks[newLength++] = pack;
            }
        }

        if (newLength != newPacks.length) {
            newPacks = Arrays.copyOf(newPacks, newLength);
        }

        synchronized (this) {
            buildResTableLocked(newPacks);
            if (invalidateCaches) {
                invalidateCachesLocked(~0);
            }
        }
    }

    private void buildResTableLocked(@NonNull PackAssets[] packs) {
        packAssets = packs;

        packageGroups.clear();
        for (int i = 0; i < packAssets.length; i++) {
            var pack = packAssets[i];
            int cookie = i;

            var loadedResources = pack.getLoadedResources();

            for (var loadedPackage : loadedResources.getPackages()) {
                var pkgGroup = packageGroups.computeIfAbsent(
                        loadedPackage.getPackageName(), __ -> new PackageGroup());

                pkgGroup.packages.add(loadedPackage);
                pkgGroup.cookies.add(cookie);
            }
        }
    }

    private void invalidateCachesLocked(int diff) {

        if (diff == ~0) {
            cachedBags.clear();
            return;
        }

        cachedBags.values().removeIf(b -> (b.typeSpecFlags & diff) != 0);
    }

    public boolean getResource(@NonNull ResourceId resId,
                               @NonNull TypedValue outValue) {
        Object entry = findEntry(resId.namespace(), resId.type(), resId.entry(),
                outValue);
        return entry == outValue;
    }

    @Nullable
    public ResolvedBag getBag(@NonNull ResourceId resId) {
        return getBag(resId, null, null);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    @Nullable
    final ResolvedBag getBag(@NonNull ResourceId resId, List<ResourceId> childResIds,
                             TypedValue value) {
        ResolvedBag cached = cachedBags.get(resId);
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
                cachedBags.put(resId, null);
            }
            return null;
        }

        int entrySize = map.getShort(ResTable_entry.size) & 0xFFFF;
        int parentId = map.getInt(ResTable_entry.parent);
        int entryCount = map.getInt(ResTable_entry.count);
        int cookie = value.cookie;
        int typeFlags = value.flags;
        LoadedResources resources = getLoadedResources(cookie);
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
        if (parentResId == null || childResIds.contains(parentResId)) {
            // There is no parent or a circular parental dependency exist, meaning there is nothing to
            // inherit and we can do a simple copy of the entries in the map.
            int offset = entrySize;

            ResolvedBag bag = null;
            fill_entries://ttt
            if (entryCount > 0) {
                // Note that keys are already sorted using ResourceId.comparePair()
                String[] keys = new String[entryCount * 2];
                int[] values = new int[entryCount * ResolvedBag.VALUE_COLUMNS];
                for (int i = 0; i < entryCount; i++) {
                    int mapName = map.getInt(offset + ResTable_map.name);
                    String packageName = resources.lookupPackageName(mapName);
                    String attrName = resources.getKeyStringPool().getStringAt(
                            mapName & Res_value.KEY_INDEX_MASK);
                    if (packageName == null || attrName == null) {
                        // malformed, returns null bag
                        Log.LOGGER.error(MARKER, "Failed to find map name 0x{} from bag '{}'",
                                Integer.toHexString(mapName), resId);
                        break fill_entries;
                    }
                    keys[i * 2 + 0] = packageName;
                    keys[i * 2 + 1] = attrName;
                    values[i * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_TYPE] =
                            map.getShort(offset + ResTable_map.value + Res_value.type) & 0xFFFF;
                    values[i * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_DATA] =
                            map.getInt(offset + ResTable_map.value + Res_value.data);
                    values[i * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_COOKIE] = cookie;
                    offset += ResTable_map.SIZEOF;
                }
                bag = new ResolvedBag();
                bag.keys = keys;
                bag.values = values;
                bag.typeSpecFlags = typeFlags;
            }
            cachedBags.put(resId, bag);
            return bag;
        }

        ResolvedBag parentBag = getBag(parentResId, childResIds, value);
        if (parentBag == null) {
            Log.LOGGER.error(MARKER, "Failed to find parent '{}' of bag '{}'",
                    parentResId, resId);
            cachedBags.put(resId, null);
            return null;
        }

        ResolvedBag newBag = null;
        int parentCount = parentBag.getEntryCount();
        int maxCount = parentCount + entryCount;
        fill_entries://ttt
        if (maxCount > 0) {
            // allocate max possible array
            String[] newKeys = new String[maxCount * 2];
            int[] newValues = new int[maxCount * ResolvedBag.VALUE_COLUMNS];
            int newIndex = 0;
            String[] parentKeys = parentBag.keys;
            int[] parentValues = parentBag.values;
            int childIndex = 0;
            int childOffset = entrySize;
            int parentIndex = 0;

            // merge two sorted arrays (parent and child)

            while (childIndex < entryCount && parentIndex < parentCount) {
                int mapName = map.getInt(childOffset + ResTable_map.name);
                String childPackageName = resources.lookupPackageName(mapName);
                String attrName = resources.getKeyStringPool().getStringAt(
                        mapName & Res_value.KEY_INDEX_MASK);
                if (childPackageName == null || attrName == null) {
                    // malformed, returns null bag
                    Log.LOGGER.error(MARKER, "Failed to find map name 0x{} from bag '{}'",
                            Integer.toHexString(mapName), resId);
                    break fill_entries;
                }
                String childAttrName = attrName;
                String parentPackageName = parentKeys[parentIndex * 2 + 0];
                String parentAttrName = parentKeys[parentIndex * 2 + 1];

                int keyCompare = ResourceId.comparePair(childPackageName, childAttrName,
                        parentPackageName, parentAttrName);

                if (keyCompare <= 0) {
                    // Use the child key if it comes before the parent
                    // or is equal to the parent (overrides).
                    newKeys[newIndex * 2 + 0] = childPackageName;
                    newKeys[newIndex * 2 + 1] = childAttrName;
                    newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_TYPE] =
                            map.getShort(childOffset + ResTable_map.value + Res_value.type) & 0xFFFF;
                    newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_DATA] =
                            map.getInt(childOffset + ResTable_map.value + Res_value.data);
                    newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_COOKIE] = cookie;
                    childIndex++;
                    childOffset += ResTable_map.SIZEOF;
                } else {
                    newKeys[newIndex * 2 + 0] = parentPackageName;
                    newKeys[newIndex * 2 + 1] = parentAttrName;
                    System.arraycopy(parentValues, parentIndex * ResolvedBag.VALUE_COLUMNS,
                            newValues, newIndex * ResolvedBag.VALUE_COLUMNS, ResolvedBag.VALUE_COLUMNS);
                }


                if (keyCompare >= 0) {
                    parentIndex++;
                }
                newIndex++;
            }

            while (childIndex < entryCount) {
                int mapName = map.getInt(childOffset + ResTable_map.name);
                String childPackageName = resources.lookupPackageName(mapName);
                String attrName = resources.getKeyStringPool().getStringAt(
                        mapName & Res_value.KEY_INDEX_MASK);
                if (childPackageName == null || attrName == null) {
                    // malformed, returns null bag
                    Log.LOGGER.error(MARKER, "Failed to find map name 0x{} from bag '{}'",
                            Integer.toHexString(mapName), resId);
                    break fill_entries;
                }
                String childAttrName = attrName;
                newKeys[newIndex * 2 + 0] = childPackageName;
                newKeys[newIndex * 2 + 1] = childAttrName;
                newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_TYPE] =
                        map.getShort(childOffset + ResTable_map.value + Res_value.type) & 0xFFFF;
                newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_DATA] =
                        map.getInt(childOffset + ResTable_map.value + Res_value.data);
                newValues[newIndex * ResolvedBag.VALUE_COLUMNS + ResolvedBag.COLUMN_COOKIE] = cookie;
                childIndex++;
                childOffset += ResTable_map.SIZEOF;
                newIndex++;
            }

            if (parentIndex < parentCount) {
                int numToCopy = parentCount - parentIndex;
                System.arraycopy(parentValues, parentIndex * ResolvedBag.VALUE_COLUMNS,
                        newValues, newIndex * ResolvedBag.VALUE_COLUMNS, numToCopy * ResolvedBag.VALUE_COLUMNS);
                System.arraycopy(parentKeys, parentIndex * 2,
                        newKeys, newIndex * 2, numToCopy * 2);
                newIndex += numToCopy;
            }

            assert newIndex <= maxCount;
            if (newIndex < maxCount) {
                // trim to size
                newKeys = Arrays.copyOf(newKeys, newIndex * 2);
                newValues = Arrays.copyOf(newValues, newIndex * ResolvedBag.VALUE_COLUMNS);
            }

            newBag = new ResolvedBag();
            newBag.keys = newKeys;
            newBag.values = newValues;
            // Combine flags from the parent and our own bag.
            newBag.typeSpecFlags = typeFlags | parentBag.typeSpecFlags;
        }
        cachedBags.put(resId, newBag);
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

        PackageGroup packageGroup = packageGroups.get(namespace);
        if (packageGroup == null) {
            return null;
        }

        return findEntryInternal(packageGroup, typeString, entryString, outValue);
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

    public PackAssets getPackAssets(int cookie) {
        if (cookie < 0 || cookie >= packAssets.length) {
            return null;
        }
        return packAssets[cookie];
    }

    public LoadedResources getLoadedResources(int cookie) {
        PackAssets assets = getPackAssets(cookie);
        if (assets == null) {
            return null;
        }
        return assets.getLoadedResources();
    }

    private PackAssets[] packAssets;
    private final HashMap<String, PackageGroup> packageGroups = new HashMap<>();

    // Cached set of bags. We don't need the best lookup performance, use
    // OpenHashMap instead of HashMap to save memory (this map can be big)
    Object2ObjectOpenHashMap<ResourceId, ResolvedBag> cachedBags = new Object2ObjectOpenHashMap<>();

    {
        cachedBags.defaultReturnValue(SENTINEL_BAG);
    }
}
