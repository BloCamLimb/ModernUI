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
 * Interface for managing a pool of objects.
 *
 * @param <T> The pooled type.
 */
public interface Pool<T> {

    /**
     * @return an instance from the pool if such, null otherwise.
     */
    @Nullable
    T acquire();

    /**
     * Release an instance to the pool.
     *
     * @param instance the instance to release.
     * @return {@code true} if the instance was put in the pool, otherwise the pool is full
     * @throws IllegalStateException if the instance is already in the pool.
     */
    boolean release(@Nonnull T instance);
}
