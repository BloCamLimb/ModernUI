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
import org.intellij.lang.annotations.MagicConstant;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @hide
 * @hidden
 */
@Immutable
@ApiStatus.Internal
public final class AssetManager {

    public static final Marker MARKER = MarkerFactory.getMarker("AssetManager");

    public static final int INVALID_COOKIE = -1;

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
            // remove entries that cannot be matched by any input
            ArrayList<TypeEntry> tmpTypeEntries = new ArrayList<>();
            LocaleMatcher localeMatcher = null;
            for (var pack : mAssetPacks) {
                var resources = pack.getResources();
                for (var namespaceSpec : resources.getNamespaceSpecs()) {
                    if (namespaceSpec.typeSpecs.length == 0) {
                        continue;
                    }
                    HashMap<String, FilteredType> types = new HashMap<>();
                    for (var typeSpec : namespaceSpec.typeSpecs) {
                        tmpTypeEntries.clear();
                        for (var typeEntry : typeSpec.typeEntries) {
                            // any density could match; so we only pre-filter locale
                            boolean isRoot = typeEntry.locale.equals(Locale.ROOT);
                            if (!matchRootOnly && !isRoot) {
                                if (localeMatcher == null) {
                                    localeMatcher = LocaleMatcher.builder()
                                            .build();
                                }
                                for (int locIdx = 0; locIdx < locales.size(); locIdx++) {
                                    var desiredLocale = locales.get(locIdx);
                                    if (localeMatcher.isMatch(
                                            desiredLocale, typeEntry.locale)) {
                                        tmpTypeEntries.add(typeEntry);
                                        break;
                                    }
                                }
                            }
                            if (isRoot) {
                                tmpTypeEntries.add(typeEntry);
                            }
                        }
                        types.put(typeSpec.typeName,
                                new FilteredType(typeSpec,
                                        tmpTypeEntries.toArray(new TypeEntry[0])));
                    }

                    groups.computeIfAbsent(
                                    namespaceSpec.namespace, __ -> new ArrayList<>())
                            .add(new FilteredNamespace(types, cookie));
                }
                assetPacks[cookie++] = pack;
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

    public static final int
            IS_NOTHING = 0,
            IS_VALUE = 1,
            IS_BAG = 2;

    // IS_VALUE: 'type', 'data', 'cookie', 'flags', 'density', 'locale' are updated
    // IS_BAG: 'cookie', 'flags', 'density', 'locale' are updated
    // 'object' is never updated
    @MagicConstant(intValues = {IS_NOTHING, IS_VALUE, IS_BAG})
    public int getValue(@NonNull ResourceId resId,
                        int density,
                        @NonNull TypedValue outValue) {
        Object entry = findEntry(resId, density, outValue);
        if (entry == null) {
            return IS_NOTHING;
        }
        return entry == outValue ? IS_VALUE : IS_BAG;
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
            return getBagLocked(resId, null, null);
        } finally {
            mBagCacheLock.writeLock().unlock();
        }
    }

    @Nullable
    private ResolvedBag getBagLocked(@NonNull ResourceId resId,
                                     List<ResourceId> childResIds,
                                     TypedValue value) {
        ResolvedBag cached = mCachedBags.get(resId);
        if (cached != SENTINEL_BAG) {
            return cached;
        }

        if (value == null) {
            value = new TypedValue();
        }

        Object entry = findEntry(resId, 0, value);

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
            parentResId = resources.lookupResourceId(parentId);
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
            parentBag = getBagLocked(parentResId, childResIds, value);
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

    // we have already done pre-filtering in Builder.build()
    @Nullable
    private Object findEntry(@NonNull ResourceId id,
                             int desiredDensity, @NonNull TypedValue outValue) {
        var group = mGroups.get(id.namespace());
        if (group == null) {
            return null;
        }

        Locale desiredLocale = mLocales.isEmpty() ? Locale.ROOT :
                mLocales.size() == 1 ? mLocales.get(0) : null;

        int bestCookie = INVALID_COOKIE;
        TypeEntry bestType = null;
        int bestOffset = 0;
        int typeSpecFlags = 0;
        boolean fullyMatchedLocale = false;

        LocaleMatcher.Builder localeMatcher = null;

        for (FilteredNamespace ns : group) {
            FilteredType ts = ns.filteredTypes.get(id.type());
            if (ts == null) {
                continue;
            }
            TypeSpec typeSpec = ts.typeSpec;
            int entryIndex = typeSpec.getEntryIndexByName(id.entry());
            if (entryIndex == -1) {
                continue;
            }
            typeSpecFlags |= typeSpec.getFlagsForEntryIndex(entryIndex);

            if (fullyMatchedLocale && bestType.density == desiredDensity) {
                // exact match, just accumulate flags above
                continue;
            }

            for (TypeEntry type : ts.filteredTypeEntries) {
                // do this first to ensure bestType is valid
                int offset = ResourceMap.getEntryOffset(type, entryIndex);

                if (offset == ResTable_type.NO_ENTRY) {
                    continue;
                }

                // true: this entry starts a new locale bucket, it replaces the best unconditionally
                boolean newBucket = false;

                if (!fullyMatchedLocale) {
                    if (desiredLocale != null &&
                            desiredLocale.equals(type.locale)) {
                        // exact match, no need for the matcher anymore
                        fullyMatchedLocale = true;
                        localeMatcher = null;
                        newBucket = true;
                    } else if (localeMatcher != null) {
                        // already collecting candidates for the second pass,
                        // density is irrelevant in this pass
                        if (!type.locale.equals(Locale.ROOT)) {
                            localeMatcher.addSupportedLocale(type.locale);
                        }
                        continue;
                    } else if (bestType == null) {
                        // first candidate (ROOT or not)
                        newBucket = true;
                    } else if (bestType.locale.equals(type.locale)) {
                        // same locale bucket, only density decides below
                    } else if (bestType.locale.equals(Locale.ROOT)) {
                        // any non-ROOT is better than ROOT
                        newBucket = true;
                    } else if (type.locale.equals(Locale.ROOT)) {
                        // existing is non-ROOT, ROOT can never be better
                        continue;
                    } else {
                        // two different non-ROOT locales: defer to the matcher in the second pass.
                        // bestLocale is guaranteed non-null and non-ROOT here.
                        localeMatcher = LocaleMatcher.builder();
                        localeMatcher.addSupportedLocale(bestType.locale);
                        localeMatcher.addSupportedLocale(type.locale);
                        continue;
                    }
                } else {
                    // only density matters now
                    if (!bestType.locale.equals(type.locale)) {
                        continue;
                    }
                }

                if (!newBucket) {
                    if (bestType.density == desiredDensity) {
                        // density already fully matched in this bucket
                        continue;
                    }
                    if (type.density != desiredDensity &&
                            !isBetterThan(type.density, bestType.density, desiredDensity)) {
                        continue;
                    }
                }

                bestCookie = ns.cookie;
                bestType = type;
                bestOffset = offset;

                if (fullyMatchedLocale && type.density == desiredDensity) {
                    // exact match
                    break;
                }
            }
        }

        // run the second pass if needed
        if (localeMatcher != null) {
            Locale targetLocale = localeMatcher.build()
                    .getBestLocale(mLocales);
            bestCookie = INVALID_COOKIE;
            bestType = null;

            // we only match density here, because we know the target locale bucket
            CYCLE:
            for (FilteredNamespace ns : group) {
                FilteredType ts = ns.filteredTypes.get(id.type());
                if (ts == null) {
                    continue;
                }
                TypeSpec typeSpec = ts.typeSpec;
                int entryIndex = typeSpec.getEntryIndexByName(id.entry());
                if (entryIndex == -1) {
                    continue;
                }

                for (TypeEntry type : ts.filteredTypeEntries) {

                    if (!targetLocale.equals(type.locale)) {
                        continue;
                    }

                    if (bestType != null &&
                            !isBetterThan(type.density, bestType.density, desiredDensity)) {
                        continue;
                    }

                    // do this last
                    int offset = ResourceMap.getEntryOffset(type, entryIndex);

                    if (offset == ResTable_type.NO_ENTRY) {
                        continue;
                    }

                    bestCookie = ns.cookie;
                    bestType = type;
                    bestOffset = offset;

                    if (type.density == desiredDensity) {
                        // exact match
                        break CYCLE;
                    }
                }
            }

            // there must be a match in the second pass
            assert bestCookie != INVALID_COOKIE;
        }

        if (bestCookie == INVALID_COOKIE) {
            return null;
        }

        Object bestEntry = ResourceMap.getEntryFromOffset(bestType, bestOffset, outValue);
        if (bestEntry == null) {
            return null;
        }

        outValue.cookie = bestCookie;
        outValue.flags = typeSpecFlags;
        outValue.density = bestType.density;
        outValue.locale = bestType.locale;

        return bestEntry;
    }

    // is this density better than other density to match the desired density?
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isBetterThan(int thisDensity,
                                        int otherDensity,
                                        int desiredDensity) {
        if (thisDensity != otherDensity) {
            int h = thisDensity;
            int l = otherDensity;
            boolean thisHigher = true;
            if (l > h) {
                var __ = l;l = h;h = __;
                thisHigher = false;
            }

            if (h == desiredDensity) {
                return thisHigher;
            } else if (l >= desiredDensity) {
                // requested value lower than both l and h, give l
                return !thisHigher;
            } else {
                // otherwise give h
                return thisHigher;
            }
        }

        return false;
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
    ResourceId getReferenceIdForCookie(int cookie, int data) {
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.lookupResourceId(data);
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
    RuntimeFactory getFactoryForCookie(int cookie, int data) {
        ResourceMap resources = getResources(cookie);
        if (resources == null) {
            return null;
        }
        return resources.getFactory(data);
    }

    // do post-processing before exposing it to public API users,
    // freeze the 'object' field
    boolean postProcess(@NonNull TypedValue outValue) {
        if (outValue.cookie == INVALID_COOKIE) {
            // inline value, not from resource
            return true;
        }
        switch (outValue.type) {
            case TypedValue.TYPE_REFERENCE -> {
                if ((outValue.object = getReferenceIdForCookie(outValue.cookie, outValue.data)) == null) {
                    return false;
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
