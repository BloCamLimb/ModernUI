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

import java.util.Objects;

/**
 * Helper class for creating pools of objects.
 */
public final class Pools {

    /**
     * Interface for managing a pool of objects.
     *
     * @param <T> The pooled type.
     */
    public interface Pool<T> {

        /**
         * @return An instance from the pool if such, null otherwise.
         */
        @Nullable
        T acquire();

        /**
         * Release an instance to the pool.
         *
         * @param instance The instance to release.
         * @return Whether the instance was put in the pool.
         * @throws IllegalStateException If the instance is already in the pool.
         */
        boolean release(@NonNull T instance);
    }

    private Pools() {
    }

    /**
     * Creates a simple (non-synchronized) pool of objects.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    @NonNull
    public static <T> Pool<T> newSimplePool(int maxPoolSize) {
        return new SimplePool<>(maxPoolSize);
    }

    /**
     * Creates a synchronized pool of objects.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    @NonNull
    public static <T> Pool<T> newSynchronizedPool(int maxPoolSize) {
        return new SynchronizedPool<>(maxPoolSize);
    }

    /**
     * Creates a synchronized pool of objects.
     *
     * @param maxPoolSize The max pool size.
     * @throws IllegalArgumentException If the max pool size is less than zero.
     */
    @NonNull
    public static <T> Pool<T> newSynchronizedPool(int maxPoolSize, @NonNull Object lock) {
        return new SynchronizedPool<>(maxPoolSize, lock);
    }

    /**
     * Simple (non-synchronized) pool of objects.
     *
     * @param <T> The pooled type.
     */
    public static class SimplePool<T> implements Pool<T> {

        private final T[] mPool;
        private int mPoolSize;

        /**
         * Creates a new instance.
         *
         * @param maxPoolSize The max pool size.
         * @throws IllegalArgumentException If the max pool size is less than zero.
         */
        @SuppressWarnings("unchecked")
        public SimplePool(int maxPoolSize) {
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
    public static class SynchronizedPool<T> extends SimplePool<T> {

        private final Object mLock;

        /**
         * Creates a new instance.
         *
         * @param maxPoolSize The max pool size.
         * @throws IllegalArgumentException If the max pool size is less than zero.
         */
        public SynchronizedPool(int maxPoolSize) {
            super(maxPoolSize);
            mLock = this;
        }

        /**
         * Creates a new instance.
         *
         * @param maxPoolSize The max pool size.
         * @throws IllegalArgumentException If the max pool size is less than zero.
         */
        public SynchronizedPool(int maxPoolSize, @NonNull Object lock) {
            super(maxPoolSize);
            mLock = Objects.requireNonNull(lock);
        }

        @Nullable
        @Override
        public T acquire() {
            synchronized (mLock) {
                return super.acquire();
            }
        }

        @Override
        public boolean release(@NonNull T element) {
            synchronized (mLock) {
                return super.release(element);
            }
        }
    }
}
