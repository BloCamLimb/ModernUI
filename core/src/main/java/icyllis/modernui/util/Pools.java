/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

/**
 * Helper class for creating pools of objects.
 */
public final class Pools {

    private Pools() {
    }

    /**
     * Creates a simple (non-synchronized) pool of objects.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    @NonNull
    public static <T> Pool<T> simple(int maxPoolSize) {
        return new SimplePool<>(maxPoolSize);
    }

    /**
     * Creates a synchronized pool of objects. Note that the lock is self.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    @NonNull
    public static <T> Pool<T> concurrent(int maxPoolSize) {
        return new SynchronizedPool<>(maxPoolSize);
    }

    /**
     * Simple (non-synchronized) pool of objects.
     *
     * @param <T> The pooled type.
     */
    private static class SimplePool<T> implements Pool<T> {

        private final T[] mPool;

        private int mPoolSize;

        @SuppressWarnings("unchecked")
        private SimplePool(int maxPoolSize) {
            if (maxPoolSize <= 0)
                throw new IllegalArgumentException("The max pool size must be > 0");
            mPool = (T[]) new Object[maxPoolSize];
        }

        @Nullable
        @Override
        public T acquire() {
            if (mPoolSize == 0)
                return null;
            final int i = --mPoolSize;
            T instance = mPool[i];
            mPool[i] = null;
            return instance;
        }

        @Override
        public boolean release(@NonNull T instance) {
            if (mPoolSize == mPool.length)
                return false;
            for (int i = mPoolSize - 1; i >= 0; i--)
                if (mPool[i] == instance)
                    throw new IllegalStateException("Already in the pool!");
            mPool[mPoolSize++] = instance;
            return true;
        }
    }

    /**
     * Synchronized pool of objects.
     *
     * @param <T> The pooled type.
     */
    private static class SynchronizedPool<T> extends SimplePool<T> {

        private SynchronizedPool(int maxPoolSize) {
            super(maxPoolSize);
        }

        @Nullable
        @Override
        public T acquire() {
            synchronized (this) {
                return super.acquire();
            }
        }

        @Override
        public boolean release(@NonNull T element) {
            synchronized (this) {
                return super.release(element);
            }
        }
    }
}
