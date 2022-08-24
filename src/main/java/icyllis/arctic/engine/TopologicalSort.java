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

package icyllis.arctic.engine;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Provides a depth-first-search topological sort algorithm for simple directed acyclic graphs.
 */
public final class TopologicalSort {

    /**
     * Topologically sort the nodes in 'graph'. For this sort, when node 'i' depends
     * on node 'j' it means node 'j' must appear in the result before node 'i'.
     * <p>
     * The graph to sort must be directed, must not allow self loops, and must not contain
     * cycles. Otherwise, {@link IllegalStateException} will be thrown and the contents of
     * 'graph' will be in some arbitrary state.
     *
     * @param graph    the directed acyclic graph to sort
     * @param accessor the data accessor
     * @param <T>      the node type of the graph
     * @throws IllegalStateException if the graph contains loops
     */
    public static <T> void topologicalSort(@Nonnull List<T> graph, @Nonnull Accessor<T> accessor) {
        assert checkAllUnmarked(graph, accessor);

        int index = 0;

        // Start a DFS from each node in the graph.
        for (T node : graph) {
            // Output this node after all the nodes it depends on have been output.
            index = dfsVisit(node, accessor, index);
        }

        assert index == graph.size();

        // Reorder the array given the output order.
        for (int i = 0, e = graph.size(); i < e; i++) {
            for (int j = accessor.getIndex(graph.get(i)); j != i; ) {
                T temp = graph.set(j, graph.get(i));
                graph.set(i, temp);
                j = accessor.getIndex(temp);
            }
        }

        assert cleanExit(graph, accessor);
    }

    /**
     * Recursively visit a node and all the other nodes it depends on.
     */
    private static <T> int dfsVisit(final T node, @Nonnull Accessor<T> accessor, int index) {
        if (accessor.isInResult(node)) {
            // If the node under consideration has been already been output it means it
            // (and all the nodes it depends on) are already in 'result'.
            return index;
        }
        if (accessor.isTempMarked(node)) {
            // There was a loop
            throw new IllegalStateException();
        }
        final List<T> edges = accessor.getEdges(node);
        if (edges != null && !edges.isEmpty()) {
            // Temporarily mark the node
            accessor.setTempMarked(node, true);
            // Recursively dfs all the node's edges
            for (T edge : edges) {
                index = dfsVisit(edge, accessor, index);
            }
            // Unmark the node from the temporary list
            accessor.setTempMarked(node, false);
        }
        // Mark this node as output
        accessor.setIndex(node, index);
        return index + 1;
    }

    private static <T> boolean checkAllUnmarked(@Nonnull List<T> graph, @Nonnull Accessor<T> accessor) {
        for (final T node : graph) {
            assert !accessor.isInResult(node);
            assert !accessor.isTempMarked(node);
        }
        return true;
    }

    private static <T> boolean cleanExit(@Nonnull List<T> graph, @Nonnull Accessor<T> accessor) {
        for (int i = 0, e = graph.size(); i < e; i++) {
            final T node = graph.get(i);
            assert accessor.getIndex(node) == i;
            assert accessor.isInResult(node);
            assert !accessor.isTempMarked(node);
        }
        return true;
    }

    /**
     * Direct access to node data. This removes the overhead of the iteration.
     *
     * @param <T> the node type of the graph
     */
    public interface Accessor<T> {

        /**
         * Stores the index into the node and {@link #isInResult(Object)} will return true.
         * The index may be retrieved by {@link #getIndex(Object)}.
         *
         * @param node  the node of the graph
         * @param index the index of the node in the result
         */
        void setIndex(T node, int index);

        /**
         * Retrieves the index previously stored into the node by {@link #setIndex(Object, int)}.
         *
         * @param node the node of the graph
         * @return the index of the node in the result
         */
        int getIndex(T node);

        /**
         * Returns whether the sort has already seen and added the node to the result.
         * This method should return false before each sort starts, and return true after
         * {@link #setIndex(Object, int)} is called in the sort.
         *
         * @param node the node of the graph
         * @return true if the node has result in the sort
         */
        boolean isInResult(T node);

        /**
         * Sets a transient state to indicate that it is visited during sorting, used to check
         * cyclic dependencies. The state may be retrieved by {@link #isTempMarked(Object)}.
         *
         * @param node   the node of the graph
         * @param marked if the node is temporarily marked or not
         */
        void setTempMarked(T node, boolean marked);

        /**
         * Retrieves the state previously set the node by {@link #setTempMarked(Object, boolean)}.
         *
         * @param node the node of the graph
         * @return if the node is temporarily marked or not
         */
        boolean isTempMarked(T node);

        /**
         * Returns any incoming edges (dependencies) from the given node.
         *
         * @param node the node of the graph
         * @return a list containing any incoming edges, or null if there are none
         */
        List<T> getEdges(T node);
    }
}
