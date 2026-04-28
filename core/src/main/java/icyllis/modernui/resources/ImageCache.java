/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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
import icyllis.modernui.graphics.Image;
import icyllis.modernui.util.LongSparseArray;

import java.lang.ref.WeakReference;

final class ImageCache {

    private final LongSparseArray<WeakReference<Image.ConstantState>> mEntries
            = new LongSparseArray<>();

    private long mGeneration;

    public Image putAndGet(long key, @NonNull Image newImage, long generation,
                           boolean needNewInstance) {
        synchronized (this) {
            if (generation != mGeneration) {
                // not cached, just return the new instance
                return newImage;
            }
            LongSparseArray<WeakReference<Image.ConstantState>> entries = mEntries;

            WeakReference<Image.ConstantState> existingRef = entries.get(key);
            if (existingRef != null) {
                Image.ConstantState existingEntry = existingRef.get();
                if (existingEntry != null) {
                    // there's a race, reuse the existing instance and destroy the new one
                    newImage.close();
                    return needNewInstance ? existingEntry.newImage() : existingEntry.getImage();
                }
            }

            Image.ConstantState newEntry = newImage.getConstantState();
            entries.put(key, new WeakReference<>(newEntry));
            return needNewInstance ? newEntry.newImage() : newEntry.getImage();
        }
    }

    public Image getInstance(long key, boolean needNewInstance) {
        synchronized (this) {
            WeakReference<Image.ConstantState> ref = mEntries.get(key);
            if (ref != null) {
                Image.ConstantState entry = ref.get();
                if (entry != null) {
                    return needNewInstance ? entry.newImage() : entry.getImage();
                }
            }
        }
        return null;
    }

    public long getGeneration() {
        // read lock is not needed
        return mGeneration;
    }

    public void onConfigurationChange(int configChanges) {
        synchronized (this) {
            LongSparseArray<WeakReference<Image.ConstantState>> entries = mEntries;
            for (int i = entries.size() - 1; i >= 0; i--) {
                WeakReference<Image.ConstantState> ref = entries.valueAt(i);
                if (ref == null || pruneEntryLocked(ref.get(), configChanges)) {
                    entries.removeAt(i);
                }
            }
            mGeneration++;
        }
    }

    private boolean pruneEntryLocked(@Nullable Image.ConstantState entry, int configChanges) {
        if (entry == null) {
            return true;
        }
        boolean prune = Configuration.needNewResources(configChanges, entry.getChangingConfigurations());
        if (prune) {
            // destroy the shared instance
            entry.close();
        }
        return prune;
    }

    public void clear() {
        synchronized (this) {
            LongSparseArray<WeakReference<Image.ConstantState>> entries = mEntries;
            for (int i = entries.size() - 1; i >= 0; i--) {
                WeakReference<Image.ConstantState> ref = entries.valueAt(i);
                if (ref != null) {
                    Image.ConstantState entry = ref.get();
                    if (entry != null) {
                        // destroy the shared instance
                        entry.close();
                    }
                }
            }
            entries.clear();
        }
    }
}
