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
import java.util.HashMap;
import java.util.function.Supplier;

/**
 * LinkedHashMap, which supports modifications during iterations.
 * Takes more memory that {@link SafeLinkedList}.
 *
 * @param <T> the token type
 * @param <E> the element type, which is associated with the token
 */
public class SafeLinkedHashMap<T, E extends Supplier<T>> extends SafeLinkedList<T, E> {

    private final HashMap<T, Node<E>> mHashMap = new HashMap<>();

    public SafeLinkedHashMap() {
    }

    @Override
    protected Node<E> find(T token) {
        return mHashMap.get(token);
    }

    @Override
    public E putIfAbsent(@Nonnull E e) {
        Node<E> node = find(e.get());
        if (node != null) {
            return node.mElement;
        }
        mHashMap.put(e.get(), put(e));
        return null;
    }

    @Override
    public E remove(@Nonnull T token) {
        E removed = super.remove(token);
        mHashMap.remove(token);
        return removed;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * token.
     */
    public boolean contains(@Nonnull T token) {
        return mHashMap.containsKey(token);
    }

    /**
     * Return an element added to prior to an element associated with the given token.
     *
     * @param token the token
     */
    @Nullable
    public E ceil(T token) {
        if (contains(token)) {
            Node<E> n = mHashMap.get(token).mPrev;
            return n == null ? null : n.mElement;
        }
        return null;
    }
}
