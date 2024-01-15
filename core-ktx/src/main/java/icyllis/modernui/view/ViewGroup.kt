/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.view

/**
 * Returns the view at [index].
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
operator fun ViewGroup.get(index: Int): View =
    getChildAt(index) ?: throw IndexOutOfBoundsException("Index: $index, Size: $childCount")

/** Performs the given action on each view in this view group. */
inline fun ViewGroup.forEach(action: (view: View) -> Unit) {
    for (index in 0..<childCount) {
        action(getChildAt(index))
    }
}

/** Returns a [MutableIterator] over the views in this view group. */
operator fun ViewGroup.iterator(): MutableIterator<View> = object : MutableIterator<View> {
    private var index = 0
    override fun hasNext() = index < childCount
    override fun next() = getChildAt(index++) ?: throw IndexOutOfBoundsException()
    override fun remove() = removeViewAt(--index)
}

/**
 * Returns a [Sequence] over the immediate child views in this view group.
 *
 * @see View.allViews
 * @see ViewGroup.descendants
 */
val ViewGroup.children: Sequence<View>
    get() = object : Sequence<View> {
        override fun iterator() = this@children.iterator()
    }

/**
 * Returns a [Sequence] over the child views in this view group recursively.
 *
 * This performs a depth-first traversal. A view with no children will return a zero-element
 * sequence.
 *
 * For example, to efficiently filter views within the hierarchy using a predicate:
 *
 * ```
 * fun ViewGroup.findViewTreeIterator(predicate: (View) -> Boolean): Sequence<View> {
 *     return sequenceOf(this)
 *         .plus(descendantsTree)
 *         .filter { predicate(it) }
 * }
 * ```
 *
 * @see View.allViews
 * @see ViewGroup.children
 * @see View.ancestors
 */
val ViewGroup.descendants: Sequence<View>
    get() = Sequence {
        TreeIterator(children.iterator()) { child ->
            (child as? ViewGroup)?.children?.iterator()
        }
    }

/**
 * Lazy iterator for iterating through an abstract hierarchy.
 *
 * @param rootIterator Iterator for root elements of hierarchy
 * @param getChildIterator Function which returns a child iterator for the current item if the
 * current item has a child or `null` otherwise
 */
internal class TreeIterator<T>(
    rootIterator: Iterator<T>,
    private val getChildIterator: ((T) -> Iterator<T>?)
) : Iterator<T> {
    private val stack = mutableListOf<Iterator<T>>()
    private var iterator: Iterator<T> = rootIterator
    override fun hasNext(): Boolean {
        return iterator.hasNext()
    }
    override fun next(): T {
        val item = iterator.next()
        prepareNextIterator(item)
        return item
    }
    /**
     * Calculates next iterator for [item].
     */
    private fun prepareNextIterator(item: T) {
        // If current item has a child, then get the child iterator and save the current iterator to
        // the stack. Otherwise, if current iterator has no more elements then restore the parent
        // iterator from the stack.
        val childIterator = getChildIterator(item)
        if (childIterator != null && childIterator.hasNext()) {
            stack.add(iterator)
            iterator = childIterator
        } else {
            while (!iterator.hasNext() && stack.isNotEmpty()) {
                iterator = stack.last()
                stack.removeLast()
            }
        }
    }
}