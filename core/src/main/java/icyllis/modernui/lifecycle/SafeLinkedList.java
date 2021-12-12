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

package icyllis.modernui.lifecycle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * LinkedList, which pretends to be a map and supports modifications during iterations.
 *
 * @param <T> the token type
 * @param <E> the element type, which is associated with the token
 */
public class SafeLinkedList<T, E extends Supplier<T>> implements Iterable<E> {

    private Node<E> mHead;
    private Node<E> mTail;
    // using WeakHashMap over List<WeakReference>, so we don't have to manually remove
    // WeakReferences that have null in them.
    private final WeakHashMap<SafeRemove<E>, Boolean> mIterators = new WeakHashMap<>();
    private int mSize = 0;

    public SafeLinkedList() {
    }

    @Nullable
    protected Node<E> find(T token) {
        Node<E> n = mHead;
        while (n != null) {
            if (n.mElement.get().equals(token)) {
                return n;
            }
            n = n.mNext;
        }
        return null;
    }

    /**
     * If the specified key is not already associated
     * with a value, associates it with the given value.
     *
     * @param e value to be associated with the specified key
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     */
    public E putIfAbsent(@Nonnull E e) {
        Node<E> node = find(e.get());
        if (node != null) {
            return node.mElement;
        }
        put(e);
        return null;
    }

    protected Node<E> put(@Nonnull E e) {
        final Node<E> l = mTail;
        final Node<E> node = new Node<>(l, e, null);
        mTail = node;
        if (l == null)
            mHead = node;
        else
            l.mNext = node;
        mSize++;
        return node;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     */
    @Nullable
    public E remove(@Nonnull T key) {
        Node<E> n = find(key);
        if (n == null) {
            return null;
        }
        mSize--;
        if (!mIterators.isEmpty()) {
            for (SafeRemove<E> iter : mIterators.keySet()) {
                iter.remove(n);
            }
        }

        if (n.mPrev != null) {
            n.mPrev.mNext = n.mNext;
        } else {
            mHead = n.mNext;
        }

        if (n.mNext != null) {
            n.mNext.mPrev = n.mPrev;
        } else {
            mTail = n.mPrev;
        }

        n.mNext = null;
        n.mPrev = null;
        return n.mElement;
    }

    /**
     * @return the number of elements in this map
     */
    public int size() {
        return mSize;
    }

    /**
     * @return an ascending iterator, which doesn't include new elements added during an
     * iteration.
     */
    @Nonnull
    @Override
    public Iterator<E> iterator() {
        var iterator = new AscendingIterator<>(mHead, mTail);
        mIterators.put(iterator, Boolean.FALSE);
        return iterator;
    }

    /**
     * @return a descending iterator, which doesn't include new elements added during an
     * iteration.
     */
    @Nonnull
    public Iterator<E> descendingIterator() {
        var iterator = new DescendingIterator<>(mTail, mHead);
        mIterators.put(iterator, Boolean.FALSE);
        return iterator;
    }

    /**
     * return an iterator with additions.
     */
    @Nonnull
    public Iterator<E> iteratorWithAdditions() {
        var iterator = new IteratorWithAdditions();
        mIterators.put(iterator, Boolean.FALSE);
        return iterator;
    }

    /**
     * @return eldest added or null
     */
    public E head() {
        return mHead == null ? null : mHead.mElement;
    }

    /**
     * @return newest added or null
     */
    public E tail() {
        return mTail == null ? null : mTail.mElement;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SafeLinkedList<?, ?> map)) {
            return false;
        }
        if (this.size() != map.size()) {
            return false;
        }
        Iterator<E> iterator1 = iterator();
        Iterator<?> iterator2 = map.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            E next1 = iterator1.next();
            Object next2 = iterator2.next();
            if ((next1 == null && next2 != null)
                    || (next1 != null && !next1.equals(next2))) {
                return false;
            }
        }
        return !iterator1.hasNext() && !iterator2.hasNext();
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (E e : this) {
            h += e.hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        Iterator<E> iterator = iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().toString());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    static class Node<E> {
        E mElement;
        Node<E> mNext;
        Node<E> mPrev;

        Node(Node<E> prev, E element, Node<E> next) {
            mElement = element;
            mNext = next;
            mPrev = prev;
        }
    }

    private static abstract class SafeIterator<E> implements Iterator<E>, SafeRemove<E> {

        Node<E> mExpectedEnd;
        Node<E> mNext;

        SafeIterator(Node<E> start, Node<E> expectedEnd) {
            mExpectedEnd = expectedEnd;
            mNext = start;
        }

        @Override
        public void remove(@Nonnull Node<E> node) {
            if (mExpectedEnd == node && node == mNext) {
                mNext = null;
                mExpectedEnd = null;
            }

            if (mExpectedEnd == node) {
                mExpectedEnd = backward(mExpectedEnd);
            }

            if (mNext == node) {
                mNext = nextNode();
            }
        }

        @Nullable
        private Node<E> nextNode() {
            if (mNext == mExpectedEnd || mExpectedEnd == null) {
                return null;
            }
            return forward(mNext);
        }

        @Override
        public boolean hasNext() {
            return mNext != null;
        }

        @Override
        public E next() {
            E result = mNext.mElement;
            mNext = nextNode();
            return result;
        }

        abstract Node<E> forward(@Nonnull Node<E> node);

        abstract Node<E> backward(@Nonnull Node<E> node);
    }

    static class AscendingIterator<E> extends SafeIterator<E> {

        AscendingIterator(Node<E> start, Node<E> expectedEnd) {
            super(start, expectedEnd);
        }

        @Override
        Node<E> forward(@Nonnull Node<E> node) {
            return node.mNext;
        }

        @Override
        Node<E> backward(@Nonnull Node<E> node) {
            return node.mPrev;
        }
    }

    private static class DescendingIterator<E> extends SafeIterator<E> {

        DescendingIterator(Node<E> start, Node<E> expectedEnd) {
            super(start, expectedEnd);
        }

        @Override
        Node<E> forward(@Nonnull Node<E> node) {
            return node.mPrev;
        }

        @Override
        Node<E> backward(@Nonnull Node<E> node) {
            return node.mNext;
        }
    }

    private class IteratorWithAdditions implements Iterator<E>, SafeRemove<E> {

        private Node<E> mCurrent;
        private boolean mBeforeHead = true;

        IteratorWithAdditions() {
        }

        @Override
        public void remove(@Nonnull Node<E> node) {
            if (node == mCurrent) {
                mCurrent = mCurrent.mPrev;
                mBeforeHead = mCurrent == null;
            }
        }

        @Override
        public boolean hasNext() {
            if (mBeforeHead) {
                return mHead != null;
            }
            return mCurrent != null && mCurrent.mNext != null;
        }

        @Override
        public E next() {
            if (mBeforeHead) {
                mBeforeHead = false;
                mCurrent = mHead;
            } else {
                mCurrent = mCurrent.mNext;
            }
            return mCurrent.mElement;
        }
    }

    interface SafeRemove<V> {

        void remove(@Nonnull Node<V> node);
    }
}
