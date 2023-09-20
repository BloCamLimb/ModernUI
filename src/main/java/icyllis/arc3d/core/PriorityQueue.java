/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Similar to {@link java.util.PriorityQueue}, but supports {@link Accessor}.
 *
 * @param <E> the type of elements held in this queue
 */
@SuppressWarnings({"unchecked", "unused"})
public class PriorityQueue<E> extends AbstractQueue<E> {

    private static final int DEFAULT_INITIAL_CAPACITY = 11;

    /**
     * The heap array.
     */
    protected transient E[] mHeap;

    /**
     * The number of elements in this queue.
     */
    protected int mSize;

    /**
     * The type-specific comparator used in this queue.
     */
    protected Comparator<? super E> mComparator;

    /**
     * The type-specific index accessor used in this queue.
     */
    protected Accessor<? super E> mAccessor;

    public PriorityQueue() {
        this(DEFAULT_INITIAL_CAPACITY, null, null);
    }

    public PriorityQueue(int priority) {
        this(priority, null, null);
    }

    public PriorityQueue(Accessor<? super E> accessor) {
        this(DEFAULT_INITIAL_CAPACITY, null, accessor);
    }

    public PriorityQueue(int capacity, Accessor<? super E> accessor) {
        this(capacity, null, accessor);
    }

    public PriorityQueue(Comparator<? super E> comparator, Accessor<? super E> accessor) {
        this(DEFAULT_INITIAL_CAPACITY, comparator, accessor);
    }

    /**
     * Creates a {@code PriorityQueue} with a given capacity, comparator and index accessor.
     *
     * @param capacity   the initial capacity of this queue.
     * @param comparator the comparator used in this queue, or {@code null} for the natural order.
     * @param accessor   the index accessor used in this queue, or {@code null}.
     */
    public PriorityQueue(int capacity, Comparator<? super E> comparator, Accessor<? super E> accessor) {
        mHeap = (E[]) new Object[Math.max(1, capacity)];
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
        int i = mSize;
        if (i >= mHeap.length)
            grow(i + 1);
        siftUp(i, Objects.requireNonNull(e));
        mSize = i + 1;
        return true;
    }

    @Override
    public E peek() {
        return mHeap[0];
    }

    private int indexOf(Object o) {
        if (o != null) {
            if (mAccessor != null)
                return mAccessor.getIndex((E) o);
            final E[] es = mHeap;
            for (int i = 0, n = mSize; i < n; i++)
                if (o.equals(es[i]))
                    return i;
        }
        return -1;
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
        int i = indexOf(o);
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
        return indexOf(o) >= 0;
    }

    /**
     * Returns an array containing all of the elements in this queue.
     * The elements are in no particular order.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    @Nonnull
    @Override
    public Object[] toArray() {
        return Arrays.copyOf(mHeap, mSize);
    }

    /**
     * Returns an array containing all of the elements in this queue; the
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
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */
    @Nonnull
    @Override
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public <T> T[] toArray(@Nonnull T[] a) {
        final int size = mSize;
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(mHeap, size, a.getClass());
        System.arraycopy(mHeap, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
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
        private int mCursor;

        @Override
        public boolean hasNext() {
            return mCursor < mSize;
        }

        @Override
        public E next() {
            if (mCursor < mSize)
                return mHeap[mCursor++];
            throw new NoSuchElementException();
        }
    }

    @Override
    public int size() {
        return mSize;
    }

    /**
     * Removes all of the elements from this priority queue.
     * The queue will be empty after this call returns.
     */
    @Override
    public void clear() {
        final E[] es = mHeap;
        if (mAccessor != null) {
            for (int i = 0, n = mSize; i < n; i++) {
                mAccessor.setIndex(es[i], -1);
                es[i] = null;
            }
        } else {
            for (int i = 0, n = mSize; i < n; i++)
                es[i] = null;
        }
        mSize = 0;
    }

    @Override
    public E poll() {
        final E[] es = mHeap;
        final E result = es[0];
        if (result != null) {
            final int n = --mSize;
            final E x = es[n];
            es[n] = null;
            if (n > 0)
                siftDown(0, x);
        }
        return result;
    }

    /**
     * Removes the ith element from queue.
     */
    public void removeAt(int i) {
        Objects.checkIndex(i, mSize);
        final E[] es = mHeap;
        int s = --mSize;
        if (s == i) { // removed last element
            if (mAccessor != null)
                mAccessor.setIndex(es[i], -1);
            es[i] = null;
        } else {
            E moved = es[s];
            if (mAccessor != null)
                mAccessor.setIndex(moved, -1);
            es[s] = null;
            siftDown(i, moved);
            if (es[i] == moved)
                siftUp(i, moved);
        }
    }

    /**
     * Gets the ith element in priority queue. {@code elementAt(0)} is equivalent
     * to {@link #peek()}. Otherwise, there is no guarantee about ordering of elements in the queue.
     */
    public E elementAt(int i) {
        return mHeap[Objects.checkIndex(i, mSize)];
    }

    /**
     * Sorts the queue into priority order. The queue is only guaranteed to remain in sorted order
     * until any other operation, other than {@link #elementAt(int)}, is performed.
     */
    public void sort() {
        final int n = mSize;
        if (n > 1) {
            final E[] es = mHeap;
            Arrays.sort(es, 0, n, mComparator);
            final Accessor<? super E> access = mAccessor;
            if (access != null)
                for (int i = 0; i < n; i++)
                    access.setIndex(es[i], i);
        }
    }

    /**
     * Makes the current array into a heap.
     */
    public void heap() {
        final E[] es = mHeap;
        int n = mSize, i = (n >>> 1) - 1;
        if (mComparator == null)
            for (; i >= 0; i--)
                siftDownComparable(i, es[i], es, n);
        else
            for (; i >= 0; i--)
                siftDownUsingComparator(i, es[i], es, n, mComparator);
        final Accessor<? super E> access = mAccessor;
        if (access != null)
            for (i = 0; i < n; i++)
                access.setIndex(es[i], i);
    }

    /**
     * Trims the underlying heap array so that it has exactly {@link #size()} elements.
     */
    public void trim() {
        final E[] es = mHeap;
        final int n = mSize;
        if (n < es.length)
            mHeap = Arrays.copyOf(es, n);
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
            if (mAccessor != null)
                siftUpUsingComparator(k, x, mHeap, mComparator, mAccessor);
            else
                siftUpUsingComparator(k, x, mHeap, mComparator);
        else if (mAccessor != null)
            siftUpComparable(k, x, mHeap, mAccessor);
        else
            siftUpComparable(k, x, mHeap);
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

    private static <T> void siftUpComparable(int k, T x, T[] es, Accessor<? super T> access) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (key.compareTo(e) >= 0)
                break;
            es[k] = e;
            access.setIndex(e, k);
            k = parent;
        }
        es[k] = x;
        access.setIndex(x, k);
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
                                                  Accessor<? super T> access) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            T e = es[parent];
            if (c.compare(x, e) >= 0)
                break;
            es[k] = e;
            access.setIndex(e, k);
            k = parent;
        }
        es[k] = x;
        access.setIndex(x, k);
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
            if (mAccessor != null)
                siftDownUsingComparator(k, x, mHeap, mSize, mComparator, mAccessor);
            else
                siftDownUsingComparator(k, x, mHeap, mSize, mComparator);
        else if (mAccessor != null)
            siftDownComparable(k, x, mHeap, mSize, mAccessor);
        else
            siftDownComparable(k, x, mHeap, mSize);
    }

    private static <T> void siftDownComparable(int k, T x, T[] es, int n) {
        assert n > 0;
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
                                               Accessor<? super T> access) {
        assert n > 0;
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
            access.setIndex(c, k);
            k = child;
        }
        es[k] = x;
        access.setIndex(x, k);
    }

    private static <T> void siftDownUsingComparator(int k, T x, T[] es, int n, Comparator<? super T> cmp) {
        assert n > 0;
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
                                                    Accessor<? super T> access) {
        assert n > 0;
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
            access.setIndex(c, k);
            k = child;
        }
        es[k] = x;
        access.setIndex(x, k);
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
        return mComparator;
    }

    public Accessor<? super E> accessor() {
        return mAccessor;
    }

    /**
     * This allows us to store the index into the element itself to improve the performance
     * of inserting or removing elements. Without this mechanism, it will iterate through
     * the queue to find the index.
     *
     * @param <E> the type of elements held in this queue
     */
    public interface Accessor<E> {

        /**
         * Stores the new index into the element.
         * An index of -1 means the element is removed from the queue.
         *
         * @param e     an element of the queue
         * @param index the new index of the element in the queue, or -1
         */
        void setIndex(E e, int index);

        /**
         * Retrieves the index previously stored into the element.
         * An index of -1 means the element is removed from the queue.
         *
         * @param e an element of the queue
         * @return the index of the element in the queue, or -1
         */
        int getIndex(E e);
    }
}
