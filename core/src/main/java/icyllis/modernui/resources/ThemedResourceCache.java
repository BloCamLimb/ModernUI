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

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.util.ArrayMap;
import icyllis.modernui.util.LongSparseArray;
import icyllis.modernui.resources.Resources.ThemeKey;
import icyllis.modernui.resources.Resources.Theme;

import java.lang.ref.WeakReference;

/**
 * Data structure used for caching data against themes.
 *
 * @param <T> type of data to cache
 */
abstract class ThemedResourceCache<T> {
    public static final int UNDEFINED_GENERATION = -1;
    private ArrayMap<ThemeKey, LongSparseArray<WeakReference<T>>> mThemedEntries;
    private LongSparseArray<WeakReference<T>> mUnthemedEntries;
    private LongSparseArray<WeakReference<T>> mNullThemedEntries;

    private int mGeneration;

    /**
     * Adds a new theme-dependent entry to the cache.
     *
     * @param key a key that uniquely identifies the entry
     * @param theme the theme against which this entry was inflated, or
     *              {@code null} if the entry has no theme applied
     * @param entry the entry to cache
     * @param generation The generation of the cache to compare against before storing
     */
    public void put(long key, @Nullable Theme theme, @NonNull T entry, int generation) {
        put(key, theme, entry, generation, true);
    }

    /**
     * Adds a new entry to the cache.
     *
     * @param key a key that uniquely identifies the entry
     * @param theme the theme against which this entry was inflated, or
     *              {@code null} if the entry has no theme applied
     * @param entry the entry to cache
     * @param generation The generation of the cache to compare against before storing
     * @param usesTheme {@code true} if the entry is affected theme changes,
     *                  {@code false} otherwise
     */
    public void put(long key, @Nullable Theme theme, @NonNull T entry, int generation,
                    boolean usesTheme) {
        if (entry == null) {
            return;
        }

        synchronized (this) {
            final LongSparseArray<WeakReference<T>> entries;
            if (!usesTheme) {
                entries = getUnthemedLocked(true);
            } else {
                entries = getThemedLocked(theme, true);
            }
            if (entries != null
                    && ((generation == mGeneration) || (generation == UNDEFINED_GENERATION)))  {
                entries.put(key, new WeakReference<>(entry));
            }
        }
    }

    /**
     * Returns the current generation of the cache
     *
     * @return The current generation
     */
    public int getGeneration() {
        return mGeneration;
    }

    /**
     * Returns an entry from the cache.
     *
     * @param key a key that uniquely identifies the entry
     * @param theme the theme where the entry will be used
     * @return a cached entry, or {@code null} if not in the cache
     */
    @Nullable
    public T get(long key, @Nullable Theme theme) {
        // The themed (includes null-themed) and unthemed caches are mutually
        // exclusive, so we'll give priority to whichever one we think we'll
        // hit first. Since most of the framework drawables are themed, that's
        // probably going to be the themed cache.
        synchronized (this) {
            final LongSparseArray<WeakReference<T>> themedEntries = getThemedLocked(theme, false);
            if (themedEntries != null) {
                final WeakReference<T> themedEntry = themedEntries.get(key);
                if (themedEntry != null) {
                    return themedEntry.get();
                }
            }

            final LongSparseArray<WeakReference<T>> unthemedEntries = getUnthemedLocked(false);
            if (unthemedEntries != null) {
                final WeakReference<T> unthemedEntry = unthemedEntries.get(key);
                if (unthemedEntry != null) {
                    return unthemedEntry.get();
                }
            }
        }

        return null;
    }


    /**
     * Prunes cache entries that have been invalidated by a configuration
     * change.
     *
     * @param configChanges a bitmask of configuration changes
     */
    public void onConfigurationChange(int configChanges) {
        synchronized (this) {
            pruneLocked(configChanges);
            mGeneration++;
        }
    }

    /**
     * Returns whether a cached entry has been invalidated by a configuration
     * change.
     *
     * @param entry a cached entry
     * @param configChanges a non-zero bitmask of configuration changes
     * @return {@code true} if the entry is invalid, {@code false} otherwise
     */
    protected abstract boolean shouldInvalidateEntry(@NonNull T entry, int configChanges);

    /**
     * Returns the cached data for the specified theme, optionally creating a
     * new entry if one does not already exist.
     *
     * @param t the theme for which to return cached data
     * @param create {@code true} to create an entry if one does not already
     *               exist, {@code false} otherwise
     * @return the cached data for the theme, or {@code null} if the cache is
     *         empty and {@code create} was {@code false}
     */
    @Nullable
    private LongSparseArray<WeakReference<T>> getThemedLocked(@Nullable Theme t, boolean create) {
        if (t == null) {
            if (mNullThemedEntries == null && create) {
                mNullThemedEntries = new LongSparseArray<>(1);
            }
            return mNullThemedEntries;
        }

        if (mThemedEntries == null) {
            if (create) {
                mThemedEntries = new ArrayMap<>(1);
            } else {
                return null;
            }
        }

        final ThemeKey key = t.getKey();
        LongSparseArray<WeakReference<T>> cache = mThemedEntries.get(key);
        if (cache == null && create) {
            cache = new LongSparseArray<>(1);

            mThemedEntries.put(key, cache);
        }

        return cache;
    }

    /**
     * Returns the theme-agnostic cached data.
     *
     * @param create {@code true} to create an entry if one does not already
     *               exist, {@code false} otherwise
     * @return the theme-agnostic cached data, or {@code null} if the cache is
     *         empty and {@code create} was {@code false}
     */
    @Nullable
    private LongSparseArray<WeakReference<T>> getUnthemedLocked(boolean create) {
        if (mUnthemedEntries == null && create) {
            mUnthemedEntries = new LongSparseArray<>(1);
        }
        return mUnthemedEntries;
    }

    /**
     * Prunes cache entries affected by configuration changes or where weak
     * references have expired.
     *
     * @param configChanges a bitmask of configuration changes, or {@code 0} to
     *                      simply prune missing weak references
     * @return {@code true} if the cache is completely empty after pruning
     */
    private boolean pruneLocked(int configChanges) {
        if (mThemedEntries != null) {
            for (int i = mThemedEntries.size() - 1; i >= 0; i--) {
                if (pruneEntriesLocked(mThemedEntries.valueAt(i), configChanges)) {
                    mThemedEntries.removeAt(i);
                }
            }
        }

        pruneEntriesLocked(mNullThemedEntries, configChanges);
        pruneEntriesLocked(mUnthemedEntries, configChanges);

        return mThemedEntries == null && mNullThemedEntries == null
                && mUnthemedEntries == null;
    }

    private boolean pruneEntriesLocked(@Nullable LongSparseArray<WeakReference<T>> entries,
                                       int configChanges) {
        if (entries == null) {
            return true;
        }

        for (int i = entries.size() - 1; i >= 0; i--) {
            final WeakReference<T> ref = entries.valueAt(i);
            if (ref == null || pruneEntryLocked(ref.get(), configChanges)) {
                entries.removeAt(i);
            }
        }

        return entries.isEmpty();
    }

    private boolean pruneEntryLocked(@Nullable T entry, int configChanges) {
        return entry == null || (configChanges != 0
                && shouldInvalidateEntry(entry, configChanges));
    }

    public synchronized void clear() {
        if (mThemedEntries != null) {
            mThemedEntries.clear();
        }

        if (mUnthemedEntries != null) {
            mUnthemedEntries.clear();
        }

        if (mNullThemedEntries != null) {
            mNullThemedEntries.clear();
        }
    }
}
