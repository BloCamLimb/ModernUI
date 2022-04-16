/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui.core;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Just like {@link java.util.PriorityQueue}, but supports {@link IndexAccess}.
 * <p>
 * An unbounded priority {@linkplain Queue queue} based on a priority heap.
 * The elements of the priority queue are ordered according to their
 * {@linkplain Comparable natural ordering}, or by a {@link Comparator}
 * provided at queue construction time, depending on which constructor is
 * used.  A priority queue does not permit {@code null} elements.
 * A priority queue relying on natural ordering also does not permit
 * insertion of non-comparable objects (doing so may result in
 * {@code ClassCastException}).
 *
 * <p>The <em>head</em> of this queue is the <em>least</em> element
 * with respect to the specified ordering.  If multiple elements are
 * tied for least value, the head is one of those elements -- ties are
 * broken arbitrarily.  The queue retrieval operations {@code poll},
 * {@code remove}, {@code peek}, and {@code element} access the
 * element at the head of the queue.
 *
 * <p>A priority queue is unbounded, but has an internal
 * <i>capacity</i> governing the size of an array used to store the
 * elements on the queue.  It is always at least as large as the queue
 * size.  As elements are added to a priority queue, its capacity
 * grows automatically.  The details of the growth policy are not
 * specified.
 *
 * @param <E> the type of elements held in this queue
 */
@SuppressWarnings({"unchecked", "unused"})
public class PriorityQueue<E> extends AbstractQueue<E> {

    /**
     * A static, final, empty array.
     */
    private static final Object[] EMPTY_ARRAY = {};

    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     */
    protected transient E[] heap = (E[]) EMPTY_ARRAY;

    /**
     * The number of elements in this queue.
     */
    protected int size;

    /**
     * The type-specific comparator used in this queue.
     */
    protected final Comparator<? super E> c;

    /**
     * The type-specific index access used in this queue.
     */
    protected final IndexAccess<? super E> ia;

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity and using the natural order.
     */
    public PriorityQueue() {
        this(11, null, null);
    }

    /**
     * Creates a {@code PriorityQueue} with a given capacity and using the natural order.
     *
     * @param capacity the initial capacity of this queue.
     */
    public PriorityQueue(int capacity) {
        this(capacity, null, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity and a given comparator.
     *
     * @param c the comparator used in this queue, or {@code null} for the natural order.
     */
    public PriorityQueue(@Nullable Comparator<? super E> c) {
        this(11, c, null);
    }

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity, a given index access
     * and using the natural order.
     *
     * @param ia the index access used in this queue, or {@code null}.
     */
    public PriorityQueue(@Nullable IndexAccess<? super E> ia) {
        this(11, null, ia);
    }

    /**
     * Creates a {@code PriorityQueue} with a given capacity and comparator.
     *
     * @param capacity the initial capacity of this queue.
     * @param c        the comparator used in this queue, or {@code null} for the natural order.
     */
    public PriorityQueue(int capacity, @Nullable Comparator<? super E> c) {
        this(capacity, c, null);
    }

    /**
     * Creates a {@code PriorityQueue} with a given capacity and index access.
     *
     * @param capacity the initial capacity of this queue.
     * @param ia       the index access used in this queue, or {@code null}.
     */
    public PriorityQueue(int capacity, @Nullable IndexAccess<? super E> ia) {
        this(capacity, null, ia);
    }

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity, a given comparator
     * and index access.
     *
     * @param c  the comparator used in this queue, or {@code null} for the natural order.
     * @param ia the index access used in this queue, or {@code null}.
     */
    public PriorityQueue(@Nullable Comparator<? super E> c, @Nullable IndexAccess<? super E> ia) {
        this(11, c, ia);
    }

    /**
     * Creates a new empty queue with a given capacity, comparator and index access.
     *
     * @param capacity the initial capacity of this queue.
     * @param c        the comparator used in this queue, or {@code null} for the natural order.
     * @param ia       the index access used in this queue, or {@code null}.
     */
    public PriorityQueue(int capacity, @Nullable Comparator<? super E> c, @Nullable IndexAccess<? super E> ia) {
        if (capacity > 0)
            heap = (E[]) new Object[capacity];
        this.c = c;
        this.ia = ia;
    }

    /**
     * Increases the capacity of the array.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        int oldCapacity = heap.length;
        // Double size if small; else grow by 50%
        int newCapacity = oldCapacity + Math.max(minCapacity - oldCapacity,
                oldCapacity < 64 ? oldCapacity + 2 : oldCapacity >> 1);
        heap = Arrays.copyOf(heap, newCapacity);
    }

    /**
     * Inserts the specified element into this priority queue.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws ClassCastException   if the specified element cannot be
     *                              compared with elements currently in this priority queue
     *                              according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean add(E e) {
        return offer(e);
    }

    @Override
    public boolean offer(E e) {
        int i = size;
        if (i >= heap.length)
            grow(i + 1);
        siftUp(i, e);
        size = i + 1;
        return true;
    }

    @Override
    public E peek() {
        return heap[0];
    }

    private int indexOf(Object o) {
        final E[] es = heap;
        for (int i = 0, n = size; i < n; i++)
            if (o.equals(es[i]))
                return i;
        return -1;
    }

    @Override
    public boolean remove(Object o) {
        int i = ia != null ? ia.getIndex((E) o) : indexOf(o);
        if (i == -1)
            return false;
        else {
            removeAt(i);
            return true;
        }
    }

    /**
     * Just like remove(), but do not check if it's in the queue.
     */
    public void delete(E e) {
        removeAt(ia != null ? ia.getIndex(e) : indexOf(e));
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    /**
     * Returns an array containing all the elements in this queue.
     * The elements are in no particular order.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * @return an array containing all the elements in this queue
     */
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(heap, size);
    }

    @Override
    public Iterator<E> iterator() {
        // we don't use it :p
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        final Object[] es = heap;
        for (int i = 0, n = size; i < n; i++)
            es[i] = null;
        size = 0;
    }

    @Override
    public E poll() {
        final E[] es;
        final E result;

        if ((result = (es = heap)[0]) != null) {
            final int n;
            final E x = es[(n = --size)];
            es[n] = null;
            if (n > 0) {
                final Comparator<? super E> cmp;
                if ((cmp = c) == null)
                    siftDownComparable(0, x, es, n);
                else
                    siftDownUsingComparator(0, x, es, n, cmp);
            }
        }
        return result;
    }

    /**
     * Removes the ith element from queue.
     */
    void removeAt(int i) {
        // assert i >= 0 && i < size;
        final Object[] es = heap;
        int s = --size;
        if (s == i) // removed last element
            es[i] = null;
        else {
            E moved = (E) es[s];
            es[s] = null;
            siftDown(i, moved);
            if (es[i] == moved)
                siftUp(i, moved);
        }
    }

    /**
     * Gets the item at index i in the priority queue (for i < size()). at(0) is equivalent
     * to peek(). Otherwise, there is no guarantee about ordering of elements in the queue.
     */
    public E elementAt(int i) {
        return heap[i];
    }

    /**
     * Sorts the queue into priority order.  The queue is only guaranteed to remain in sorted order
     * until any other operation, other than at(), is performed.
     */
    public void sort() {
        final E[] es = heap;
        final int n = size;
        Arrays.sort(es, 0, n, c);
        IndexAccess<? super E> idx = ia;
        if (idx != null)
            for (int i = 0; i < n; i++)
                idx.setIndex(es[i], i);
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * promoting x up the tree until it is greater than or equal to
     * its parent, or is the root.
     * <p>
     * To simplify and speed up coercions and comparisons, the
     * Comparable and Comparator versions are separated into different
     * methods that are otherwise identical. (Similarly for siftDown.)
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, E x) {
        if (c != null)
            if (ia != null)
                siftUpUsingComparator(k, x, heap, c, ia);
            else
                siftUpUsingComparator(k, x, heap, c);
        else if (ia != null)
            siftUpComparable(k, x, heap, ia);
        else
            siftUpComparable(k, x, heap);
    }

    private static <T> void siftUpComparable(int k, T x, T[] es) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (key.compareTo(e) >= 0)
                break;
            es[k] = e;
            k = parent;
        }
        es[k] = x;
    }

    private static <T> void siftUpComparable(int k, T x, T[] es, IndexAccess<? super T> ia) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (key.compareTo(e) >= 0)
                break;
            es[k] = e;
            ia.setIndex(e, k);
            k = parent;
        }
        es[k] = x;
        ia.setIndex(x, k);
    }

    private static <T> void siftUpUsingComparator(int k, T x, T[] es, Comparator<? super T> c) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (c.compare(x, e) >= 0)
                break;
            es[k] = e;
            k = parent;
        }
        es[k] = x;
    }

    private static <T> void siftUpUsingComparator(int k, T x, T[] es, Comparator<? super T> c,
                                                  IndexAccess<? super T> ia) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (c.compare(x, e) >= 0)
                break;
            es[k] = e;
            ia.setIndex(e, k);
            k = parent;
        }
        es[k] = x;
        ia.setIndex(x, k);
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by
     * demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     *
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, E x) {
        if (c != null)
            if (ia != null)
                siftDownUsingComparator(k, x, heap, size, c, ia);
            else
                siftDownUsingComparator(k, x, heap, size, c);
        else if (ia != null)
            siftDownComparable(k, x, heap, size, ia);
        else
            siftDownComparable(k, x, heap, size);
    }

    private static <T> void siftDownComparable(int k, T x, T[] es, int n) {
        // assert n > 0;
        Comparable<? super T> key = (Comparable<? super T>) x;
        int half = n >>> 1;           // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            T c = es[child];
            int right = child + 1;
            if (right < n &&
                    ((Comparable<? super T>) c).compareTo(es[right]) > 0)
                c = es[child = right];
            if (key.compareTo(c) <= 0)
                break;
            es[k] = c;
            k = child;
        }
        es[k] = x;
    }

    private static <T> void siftDownComparable(int k, T x, T[] es, int n, IndexAccess<? super T> ia) {
        // assert n > 0;
        Comparable<? super T> key = (Comparable<? super T>) x;
        int half = n >>> 1;           // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            T c = es[child];
            int right = child + 1;
            if (right < n &&
                    ((Comparable<? super T>) c).compareTo(es[right]) > 0)
                c = es[child = right];
            if (key.compareTo(c) <= 0)
                break;
            es[k] = c;
            ia.setIndex(c, k);
            k = child;
        }
        es[k] = x;
        ia.setIndex(x, k);
    }

    private static <T> void siftDownUsingComparator(int k, T x, T[] es, int n, Comparator<? super T> cmp) {
        // assert n > 0;
        int half = n >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            T c = es[child];
            int right = child + 1;
            if (right < n && cmp.compare(c, es[right]) > 0)
                c = es[child = right];
            if (cmp.compare(x, c) <= 0)
                break;
            es[k] = c;
            k = child;
        }
        es[k] = x;
    }

    private static <T> void siftDownUsingComparator(int k, T x, T[] es, int n, Comparator<? super T> cmp,
                                                    IndexAccess<? super T> ia) {
        // assert n > 0;
        int half = n >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            T c = es[child];
            int right = child + 1;
            if (right < n && cmp.compare(c, es[right]) > 0)
                c = es[child = right];
            if (cmp.compare(x, c) <= 0)
                break;
            es[k] = c;
            ia.setIndex(c, k);
            k = child;
        }
        es[k] = x;
        ia.setIndex(x, k);
    }

    /**
     * Returns the comparator used to order the elements in this
     * queue, or {@code null} if this queue is sorted according to
     * the {@linkplain Comparable natural ordering} of its elements.
     *
     * @return the comparator used to order this queue, or
     * {@code null} if this queue is sorted according to the
     * natural ordering of its elements
     */
    public Comparator<? super E> comparator() {
        return c;
    }

    public IndexAccess<? super E> indexAccess() {
        return ia;
    }

    /**
     * This allows us to store the index in each element to improve the performance of
     * inserting or removing elements. Without this, it will traverse the queue to find
     * the index.
     *
     * @param <E> the type of elements held in this queue
     */
    public interface IndexAccess<E> {

        /**
         * Stores the index into the element. The index must be the same with
         * {@link #getIndex(Object)} later.
         *
         * @param e     the element of the queue
         * @param index the index of the element in the queue
         */
        void setIndex(E e, int index);

        /**
         * Retrieves the index that is previously stored into the element.
         * The index must be the same with {@link #setIndex(Object, int)}.
         *
         * @param e the element of the queue
         * @return the index of the element in the queue
         */
        int getIndex(E e);
    }
}
