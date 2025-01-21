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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Implementation of {@code Multimap} that uses an {@code LinkedList} to store the values for a given
 * key. A {@code HashMap} associates each key with an {@code LinkedList} of values. Empty
 * {@code LinkedList} values will be automatically removed.
 */
@NotThreadSafe
public class LinkedListMultimap<K, V> extends Object2ObjectOpenHashMap<K, LinkedListMultimap.ListNode<V>> {

    private V outerValue;

    static class ListNode<V> {
        V item;
        ListNode<V> next;

        ListNode(V value, ListNode<V> next) {
            this.item = value;
            this.next = next;
        }
    }

    private final BiFunction<K, ListNode<V>, ListNode<V>> insertEntry = (k, list) -> {
        if (list != null) {
            list.next = new ListNode<>(list.item, list.next);
            list.item = outerValue;
        } else {
            return new ListNode<>(outerValue, null);
        }
        return list;
    };

    private final BiFunction<K, ListNode<V>, ListNode<V>> removeEntry = (k, list) -> {
        V value = outerValue;
        ListNode<V> head = list;
        ListNode<V> prev = null;
        do {
            if (list.item == value) {
                return internalRemoveEntry(prev, list, head);
            }
            prev = list;
            list = list.next;
        } while (list != null);
        return head;
    };

    public LinkedListMultimap() {
    }

    @Nullable
    public V find(@NonNull K k) {
        var list = get(k);
        return list != null ? list.item : null;
    }

    @Nullable
    public V find(@NonNull K k, @NonNull Predicate<V> test) {
        for (var list = get(k); list != null; list = list.next) {
            if (test.test(list.item)) {
                return list.item;
            }
        }
        return null;
    }

    public void insertEntry(@NonNull K k, @NonNull V v) {
        assert (outerValue == null);
        outerValue = v;
        compute(k, insertEntry);
        assert (outerValue == v);
        outerValue = null;
    }

    public void removeEntry(@NonNull K k, @NonNull V v) {
        assert (outerValue == null);
        outerValue = v;
        computeIfPresent(k, removeEntry);
        assert (outerValue == v);
        outerValue = null;
    }

    private ListNode<V> internalRemoveEntry(ListNode<V> prev, ListNode<V> curr, ListNode<V> head) {
        if (curr.next != null) {
            ListNode<V> next = curr.next;
            curr.item = next.item;
            curr.next = next.next;
            // 'next' is gone
        } else if (prev != null) {
            assert prev.next == curr;
            prev.next = null;
            // 'curr' is gone
        } else {
            assert head == curr;
            // 'head' is gone
            return null;
        }
        return head;
    }

    @Deprecated
    public void addLastEntry(@NonNull K k, @NonNull V v) {
    }

    @Deprecated
    @Nullable
    public V pollFirstEntry(@NonNull K k) {
        return null;
    }
}
