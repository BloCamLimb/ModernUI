/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.fastutil.longs.Long2ObjectOpenHashMap;
import icyllis.modernui.graphics.MathUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Internally used by Resources API.
 *
 * @hidden
 */
@ApiStatus.Internal
public final class ResourceCache<V> {

    private final ReentrantReadWriteLock mLock;
    private final Long2ObjectOpenHashMap<WeakReference<V>> mEntries;

    private int mNextFlushSize;

    public ResourceCache() {
        mEntries = new Long2ObjectOpenHashMap<>(3, 0.75f);
        mNextFlushSize = 16;
        mLock = new ReentrantReadWriteLock();
    }

    public @Nullable V get(long key) {
        WeakReference<V> ref;
        mLock.readLock().lock();
        try {
            ref = mEntries.get(key);
        } finally {
            mLock.readLock().unlock();
        }
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    public @NonNull V putIfAbsent(long key, @NonNull V newInstance) {
        mLock.writeLock().lock();
        try {
            WeakReference<V> existingRef = mEntries.get(key);
            if (existingRef != null) {
                V existingEntry = existingRef.get();
                if (existingEntry != null) {
                    // there's a race, reuse the existing instance, drop the new one
                    return existingEntry;
                }
            } else if (mEntries.size() > mNextFlushSize) {
                mEntries.values().removeIf(r -> r.refersTo(null));
                mNextFlushSize = Math.max(MathUtil.ceilPow2(mEntries.size()), 16);
                mEntries.trim(mNextFlushSize);
            }

            mEntries.put(key, new WeakReference<>(newInstance));
            return newInstance;
        } finally {
            mLock.writeLock().unlock();
        }
    }
}
