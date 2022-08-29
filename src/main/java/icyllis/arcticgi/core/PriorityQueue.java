/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Just like {@link java.util.PriorityQueue}, but supports {@link Accessor}.
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
@SuppressWarnings({"unchecked", "unused", "SuspiciousSystemArraycopy"})
public class PriorityQueue<E> extends AbstractQueue<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * Priority queue represented as a balanced binary heap: the two
     * children of queue[n] are queue[2*n+1] and queue[2*(n+1)].  The
     * priority queue is ordered by comparator, or by the elements'
     * natural ordering, if comparator is null: For each node n in the
     * heap and each descendant d of n, n <= d.  The element with the
     * lowest value is in queue[0], assuming the queue is nonempty.
     */
    transient E[] mHeap;

    /**
     * The number of elements in this queue.
     */
    int size;

    /**
     * The type-specific comparator used in this queue.
     */
    @Nullable
    private final Comparator<? super E> mComparator;

    /**
     * The type-specific index access used in this queue.
     */
    private final Accessor<? super E> mAccessor;

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity, a given index access
     * and using the natural order.
     *
     * @param accessor the index access used in this queue.
     */
    public PriorityQueue(Accessor<? super E> accessor) {
        this(DEFAULT_INITIAL_CAPACITY, null, accessor);
    }

    /**
     * Creates a {@code PriorityQueue} with a given capacity and index access.
     *
     * @param capacity the initial capacity of this queue.
     * @param accessor the index access used in this queue.
     */
    public PriorityQueue(int capacity, Accessor<? super E> accessor) {
        this(capacity, null, accessor);
    }

    /**
     * Creates a {@code PriorityQueue} with the default initial capacity, a given comparator
     * and index access.
     *
     * @param comparator the comparator used in this queue, or {@code null} for the natural order.
     * @param accessor   the index access used in this queue.
     */
    public PriorityQueue(@Nullable Comparator<? super E> comparator, Accessor<? super E> accessor) {
        this(DEFAULT_INITIAL_CAPACITY, comparator, accessor);
    }

    /**
     * Creates a new empty queue with a given capacity, comparator and index access.
     *
     * @param capacity   the initial capacity of this queue.
     * @param comparator the comparator used in this queue, or {@code null} for the natural order.
     * @param accessor   the index access used in this queue.
     */
    public PriorityQueue(int capacity, @Nullable Comparator<? super E> comparator, Accessor<? super E> accessor) {
        mHeap = (E[]) new Object[Math.max(capacity, 1)];
        mComparator = comparator;
        mAccessor = accessor;
    }

    /**
     * Increases the capacity of the array.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        int oldCapacity = mHeap.length;
        // Double size if small; else grow by 50%
        int newCapacity = oldCapacity + Math.max(minCapacity - oldCapacity,
                oldCapacity < 64 ? oldCapacity + 2 : oldCapacity >> 1);
        mHeap = Arrays.copyOf(mHeap, newCapacity);
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

    /**
     * Inserts the specified element into this priority queue.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws ClassCastException   if the specified element cannot be
     *                              compared with elements currently in this priority queue
     *                              according to the priority queue's ordering
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean offer(E e) {
        int i = size;
        if (i >= mHeap.length)
            grow(i + 1);
        siftUp(i, e);
        size = i + 1;
        return true;
    }

    @Override
    public E peek() {
        return mHeap[0];
    }

    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.  Returns {@code true} if and only if this queue contained
     * the specified element (or equivalently, if this queue changed as a
     * result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    @Override
    public boolean remove(Object o) {
        int i = mAccessor.getIndex((E) o);
        if (i == -1)
            return false;
        else {
            removeAt(i);
            return true;
        }
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
        return mAccessor.getIndex((E) o) >= 0;
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
    @Nonnull
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(mHeap, size);
    }

    /**
     * Returns an array containing all the elements in this queue; the
     * runtime type of the returned array is that of the specified array.
     * The returned array elements are in no particular order.
     * If the queue fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of the
     * specified array and the size of this queue.
     *
     * <p>If the queue fits in the specified array with room to spare
     * (i.e., the array has more elements than the queue), the element in
     * the array immediately following the end of the collection is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     * <p>
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing all the elements in this queue
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */
    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        final int n = size;
        if (a.length < n)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(mHeap, n, a.getClass());
        System.arraycopy(mHeap, 0, a, 0, n);
        if (a.length > n)
            a[n] = null;
        return a;
    }

    /**
     * Returns an iterator over the elements in this queue. The iterator
     * does not return the elements in any particular order.
     *
     * @return an iterator over the elements in this queue
     */
    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    private final class Itr implements Iterator<E> {

        /**
         * Index (into queue array) of element to be returned by
         * subsequent call to next.
         */
        private int cursor;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public E next() {
            if (cursor < size)
                return mHeap[cursor++];
            throw new NoSuchElementException();
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        final E[] es = mHeap;
        for (int i = 0, n = size; i < n; i++) {
            mAccessor.setIndex(es[i], -1);
            es[i] = null;
        }
        size = 0;
    }

    @Override
    public E poll() {
        final E[] es;
        final E result;

        if ((result = (es = mHeap)[0]) != null) {
            final int n;
            final E x = es[(n = --size)];
            es[n] = null;
            if (n > 0) {
                final Comparator<? super E> cmp;
                if ((cmp = mComparator) == null)
                    siftDownComparable(0, x, es, n, mAccessor);
                else
                    siftDownUsingComparator(0, x, es, n, cmp, mAccessor);
            }
        }
        return result;
    }

    /**
     * Removes the ith element from queue.
     */
    public void removeAt(int i) {
        // assert i >= 0 && i < size;
        final E[] es = mHeap;
        mAccessor.setIndex(es[i], -1);
        int s = --size;
        if (s == i) // removed last element
            es[i] = null;
        else {
            E moved = es[s];
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
    public E get(int i) {
        return mHeap[i];
    }

    /**
     * Sorts the queue into priority order. The queue is only guaranteed to remain in sorted order
     * until any other operation, other than at(), is performed.
     */
    public void sort() {
        final E[] es = mHeap;
        final int n = size;
        Arrays.sort(es, 0, n, mComparator);
        for (int i = 0; i < n; i++)
            mAccessor.setIndex(es[i], i);
    }

    /**
     * Establishes the heap invariant (described above) in the entire tree,
     * assuming nothing about the order of the elements prior to the call.
     * This classic algorithm due to Floyd (1964) is known to be O(size).
     */
    public void heap() {
        final E[] es = mHeap;
        int n = size, i = (n >>> 1) - 1;
        final Comparator<? super E> cmp;
        if ((cmp = mComparator) == null)
            for (; i >= 0; i--)
                siftDownComparable(i, es[i], es, n);
        else
            for (; i >= 0; i--)
                siftDownUsingComparator(i, es[i], es, n, cmp);
        for (int j = 0; j < n; j++)
            mAccessor.setIndex(es[j], j);
    }

    /**
     * Trims the underlying heap array so that it has exactly {@link #size()} elements.
     */
    public void trim() {
        final E[] es = mHeap;
        final int n = size;
        if (n >= es.length || n == 0)
            return;
        mHeap = Arrays.copyOf(mHeap, n);
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
        if (mComparator != null)
            siftUpUsingComparator(k, x, mHeap, mComparator, mAccessor);
        else
            siftUpComparable(k, x, mHeap, mAccessor);
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

    private static <T> void siftUpComparable(int k, T x, T[] es, Accessor<? super T> index) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (key.compareTo(e) >= 0)
                break;
            es[k] = e;
            index.setIndex(e, k);
            k = parent;
        }
        es[k] = x;
        index.setIndex(x, k);
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
                                                  Accessor<? super T> index) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (c.compare(x, e) >= 0)
                break;
            es[k] = e;
            index.setIndex(e, k);
            k = parent;
        }
        es[k] = x;
        index.setIndex(x, k);
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
        if (mComparator != null)
            siftDownUsingComparator(k, x, mHeap, size, mComparator, mAccessor);
        else
            siftDownComparable(k, x, mHeap, size, mAccessor);
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

    private static <T> void siftDownComparable(int k, T x, T[] es, int n,
                                               Accessor<? super T> accessor) {
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
            accessor.setIndex(c, k);
            k = child;
        }
        es[k] = x;
        accessor.setIndex(x, k);
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
                                                    Accessor<? super T> accessor) {
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
            accessor.setIndex(c, k);
            k = child;
        }
        es[k] = x;
        accessor.setIndex(x, k);
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
    @Nullable
    public Comparator<? super E> comparator() {
        return mComparator;
    }

    @Nonnull
    public Accessor<? super E> accessor() {
        return mAccessor;
    }

    /**
     * This allows us to store the index in each element to improve the performance of
     * inserting or removing elements. Without this, it will traverse the queue to find
     * the index.
     *
     * @param <E> the type of elements held in this queue
     */
    public interface Accessor<E> {

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
