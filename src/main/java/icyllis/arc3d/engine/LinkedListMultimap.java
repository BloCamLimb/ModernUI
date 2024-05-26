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

package icyllis.arc3d.engine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Implementation of {@code Multimap} that uses an {@code LinkedList} to store the values for a given
 * key. A {@link HashMap} associates each key with an {@link LinkedList} of values. Empty
 * {@code LinkedList} values will be automatically removed.
 *
 * @see HashMap
 * @see LinkedList
 */
@NotThreadSafe
public class LinkedListMultimap<K, V> extends HashMap<K, LinkedList<V>> {

    private V mTmpValue;

    private final BiFunction<K, LinkedList<V>, LinkedList<V>> mPollFirstEntry =
            (k, list) -> {
                mTmpValue = list.pollFirst();
                return list.isEmpty() ? null : list;
            };
    private final BiFunction<K, LinkedList<V>, LinkedList<V>> mPollLastEntry =
            (k, list) -> {
                mTmpValue = list.pollLast();
                return list.isEmpty() ? null : list;
            };

    private final BiFunction<K, LinkedList<V>, LinkedList<V>> mRemoveFirstEntry =
            (k, list) -> list.removeFirstOccurrence(mTmpValue) && list.isEmpty() ? null : list;
    private final BiFunction<K, LinkedList<V>, LinkedList<V>> mRemoveLastEntry =
            (k, list) -> list.removeLastOccurrence(mTmpValue) && list.isEmpty() ? null : list;

    public LinkedListMultimap() {
    }

    public LinkedListMultimap(@Nonnull Map<? extends K, ? extends LinkedList<V>> other) {
        super(other);
    }

    public void addFirstEntry(@Nonnull K k, @Nonnull V v) {
        computeIfAbsent(k, __ -> new LinkedList<>())
                .addFirst(Objects.requireNonNull(v));
    }

    public void addLastEntry(@Nonnull K k, @Nonnull V v) {
        computeIfAbsent(k, __ -> new LinkedList<>())
                .addLast(Objects.requireNonNull(v));
    }

    @Nullable
    public V pollFirstEntry(@Nonnull K k) {
        assert (mTmpValue == null);
        computeIfPresent(k, mPollFirstEntry);
        V v = mTmpValue;
        mTmpValue = null;
        return v;
    }

    @Nullable
    public V pollLastEntry(@Nonnull K k) {
        assert (mTmpValue == null);
        computeIfPresent(k, mPollLastEntry);
        V v = mTmpValue;
        mTmpValue = null;
        return v;
    }

    @Nullable
    public V peekFirstEntry(@Nonnull K k) {
        var list = get(k);
        // we always remove empty linked lists, so getFirst() not peekFirst()
        return list != null ? list.getFirst() : null;
    }

    @Nullable
    public V peekLastEntry(@Nonnull K k) {
        var list = get(k);
        // we always remove empty linked lists, so getFirst() not peekFirst()
        return list != null ? list.getLast() : null;
    }

    public void removeFirstEntry(@Nonnull K k, @Nonnull V v) {
        assert (mTmpValue == null);
        mTmpValue = v;
        computeIfPresent(k, mRemoveFirstEntry);
        assert (mTmpValue == v);
        mTmpValue = null;
    }

    public void removeLastEntry(@Nonnull K k, @Nonnull V v) {
        assert (mTmpValue == null);
        mTmpValue = v;
        computeIfPresent(k, mRemoveLastEntry);
        assert (mTmpValue == v);
        mTmpValue = null;
    }
}
