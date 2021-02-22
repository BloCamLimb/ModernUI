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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    @Nonnull
    public static <T> Pool<T> simple(int maxPoolSize) {
        return new SimplePool<>(maxPoolSize);
    }

    /**
     * Creates a synchronized pool of objects.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    @Nonnull
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
            if (mPoolSize <= 0)
                return null;
            final int lastPooledIndex = mPoolSize - 1;
            T instance = mPool[lastPooledIndex];
            mPool[lastPooledIndex] = null;
            mPoolSize--;
            return instance;
        }

        @Override
        public boolean release(@Nonnull T instance) {
            if (isInPool(instance))
                throw new IllegalStateException("Already in the pool!");
            if (mPoolSize >= mPool.length)
                return false;
            mPool[mPoolSize] = instance;
            mPoolSize++;
            return true;
        }

        private boolean isInPool(@Nonnull T instance) {
            for (int i = 0; i < mPoolSize; i++)
                if (mPool[i] == instance)
                    return true;
            return false;
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
        public boolean release(@Nonnull T element) {
            synchronized (this) {
                return super.release(element);
            }
        }
    }
}
