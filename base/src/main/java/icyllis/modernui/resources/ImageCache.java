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

import icyllis.arc3d.fastutil.ints.Int2ObjectOpenHashMap;
import icyllis.modernui.graphics.Image;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;

public final class ImageCache implements AutoCloseable {

    private final ReentrantReadWriteLock mLock;
    private Int2ObjectOpenHashMap<WeakReference<Image>> mEntries;

    public ImageCache() {
        mEntries = new Int2ObjectOpenHashMap<>();
        mLock = new ReentrantReadWriteLock();
    }

    public @NonNull Image putAndGet(int key, @NonNull Image newImage,
                                    @NonNull UnaryOperator<Image> deriveOp) {
        mLock.writeLock().lock();
        try {
            if (mEntries == null) {
                return newImage;
            }
            WeakReference<Image> existingRef = mEntries.get(key);
            if (existingRef != null) {
                Image existingEntry = existingRef.get();
                if (existingEntry != null) {
                    // there's a race, reuse the existing instance and destroy the new one
                    newImage.close();
                    return deriveOp.apply(existingEntry);
                }
            }

            mEntries.put(key, new WeakReference<>(newImage));
            return deriveOp.apply(newImage);
        } finally {
            mLock.writeLock().unlock();
        }
    }

    public @Nullable Image getInstance(int key,
                                       @NonNull UnaryOperator<Image> deriveOp) {
        mLock.readLock().lock();
        try {
            if (mEntries == null) {
                return null;
            }
            WeakReference<Image> ref = mEntries.get(key);
            if (ref != null) {
                Image entry = ref.get();
                if (entry != null) {
                    return deriveOp.apply(entry);
                }
            }
        } finally {
            mLock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void close() {
        mLock.writeLock().lock();
        try {
            for (var ref : mEntries.values()) {
                if (ref != null) {
                    Image entry = ref.get();
                    if (entry != null) {
                        // destroy the shared instance
                        entry.close();
                    }
                }
            }
            mEntries = null;
        } finally {
            mLock.writeLock().unlock();
        }
    }
}
