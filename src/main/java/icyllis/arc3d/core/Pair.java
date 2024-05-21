/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable pair of two elements by default.
 *
 * @param <L> the left element type
 * @param <R> the right element type
 */
public class Pair<L, R> implements Map.Entry<L, R> {

    /**
     * Left object
     */
    L left;

    /**
     * Right object
     */
    R right;

    public Pair() {
    }

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    @Nonnull
    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    @Nonnull
    public static <L, R> Pair<L, R> of(Map.Entry<L, R> entry) {
        if (entry == null) {
            return new Pair<>();
        } else {
            return new Pair<>(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @return the key, may be null
     */
    @Override
    public final L getKey() {
        return left;
    }

    /**
     * @return the value, may be null
     */
    @Override
    public final R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the left element, may be null
     */
    public final L getLeft() {
        return left;
    }

    /**
     * @return the right element, may be null
     */
    public final R getRight() {
        return right;
    }

    /**
     * @return the first element, may be null
     */
    public final L getFirst() {
        return left;
    }

    /**
     * @return the second element, may be null
     */
    public final R getSecond() {
        return right;
    }

    /**
     * Same as {@link Map.Entry#hashCode()}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(left) ^ Objects.hashCode(right);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Map.Entry<?, ?> e) {
            return Objects.equals(left, e.getKey())
                    && Objects.equals(right, e.getValue());
        }
        return false;
    }

    /**
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "(" + left + ',' + right + ')';
    }
}
