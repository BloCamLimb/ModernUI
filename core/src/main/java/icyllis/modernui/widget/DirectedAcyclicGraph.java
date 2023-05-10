/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.widget;

import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A class which represents a simple directed acyclic graph.
 *
 * @param <T> Class for the data objects of this graph.
 * @see CoordinatorLayout
 */
final class DirectedAcyclicGraph<T> {

    private final Pools.Pool<ArrayList<T>> mListPool = Pools.newSimplePool(10);
    private final Object2ObjectOpenHashMap<T, ArrayList<T>> mGraph = new Object2ObjectOpenHashMap<>();

    private final ArrayList<T> mSortResult = new ArrayList<>();
    private final HashSet<T> mSortTmpMarked = new HashSet<>();

    /**
     * Add a node to the graph.
     *
     * <p>If the node already exists in the graph then this method is a no-op.</p>
     *
     * @param node the node to add
     */
    public void addNode(@Nonnull T node) {
        if (!mGraph.containsKey(node)) {
            mGraph.put(node, null);
        }
    }

    /**
     * Returns true if the node is already present in the graph, false otherwise.
     */
    public boolean contains(@Nonnull T node) {
        return mGraph.containsKey(node);
    }

    /**
     * Add an edge to the graph.
     *
     * <p>Both the given nodes should already have been added to the graph through
     * {@link #addNode(Object)}.</p>
     *
     * @param node         the parent node
     * @param incomingEdge the node which has is an incoming edge to {@code node}
     */
    public void addEdge(@Nonnull T node, @Nonnull T incomingEdge) {
        if (!mGraph.containsKey(node) || !mGraph.containsKey(incomingEdge)) {
            throw new IllegalArgumentException("All nodes must be present in the graph before"
                    + " being added as an edge");
        }

        ArrayList<T> edges = mGraph.get(node);
        if (edges == null) {
            // If edges is null, we should try and get one from the pool and add it to the graph
            edges = getEmptyList();
            mGraph.put(node, edges);
        }
        // Finally add the edge to the list
        edges.add(incomingEdge);
    }

    /**
     * Get any incoming edges from the given node.
     *
     * @return a new list containing any incoming edges, or {@code null} if there are none
     */
    @Nullable
    public List<T> getIncomingEdges(@Nonnull T node) {
        ArrayList<T> result = getIncomingEdgesInternal(node);
        if (result == null) {
            return null;
        } else {
            return new ArrayList<>(result);
        }
    }

    /**
     * Get any incoming edges from the given node.
     *
     * @return a list containing any incoming edges, or null if there are none.
     */
    @Nullable
    ArrayList<T> getIncomingEdgesInternal(@Nonnull T node) {
        return mGraph.get(node);
    }

    /**
     * Get any outgoing edges for the given node (i.e. nodes which have an incoming edge
     * from the given node).
     *
     * @return a new list containing any outgoing edges, or {@code null} if there are none
     */
    @Nullable
    public List<T> getOutgoingEdges(@Nonnull T node) {
        ArrayList<T> result = null;
        for (var entry : Object2ObjectMaps.fastIterable(mGraph)) {
            ArrayList<T> edges = entry.getValue();
            if (edges != null && edges.contains(node)) {
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Checks whether we have any outgoing edges for the given node (i.e. nodes which have
     * an incoming edge from the given node).
     *
     * @return <code>true</code> if the node has any outgoing edges, <code>false</code>
     * otherwise.
     */
    public boolean hasOutgoingEdges(@Nonnull T node) {
        for (var edges : mGraph.values()) {
            if (edges != null && edges.contains(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears the internal graph, and releases resources to pools.
     */
    public void clear() {
        for (var edges : mGraph.values()) {
            if (edges != null) {
                poolList(edges);
            }
        }
        mGraph.clear();
    }

    /**
     * Returns a topologically sorted list of the nodes in this graph. This uses the DFS algorithm
     * as described by Cormen et al. (2001). If this graph contains cyclic dependencies then this
     * method will throw a {@link RuntimeException}.
     *
     * <p>The resulting list will be ordered such that index 0 will contain the node at the bottom
     * of the graph. The node at the end of the list will have no dependencies on other nodes.</p>
     */
    @Nonnull
    public ArrayList<T> getSortedList() {
        mSortResult.clear();
        mSortTmpMarked.clear();

        // Start a DFS from each node in the graph
        for (var key : mGraph.keySet()) {
            dfs(key, mSortResult, mSortTmpMarked);
        }

        return mSortResult;
    }

    private void dfs(final T node, @Nonnull final ArrayList<T> result, final HashSet<T> tmpMarked) {
        if (result.contains(node)) {
            // We've already seen and added the node to the result list, skip...
            return;
        }
        if (tmpMarked.contains(node)) {
            throw new RuntimeException("This graph contains cyclic dependencies");
        }
        // Temporarily mark the node
        tmpMarked.add(node);
        // Recursively dfs all the node's edges
        final ArrayList<T> edges = mGraph.get(node);
        if (edges != null) {
            for (T edge : edges) {
                dfs(edge, result, tmpMarked);
            }
        }
        // Unmark the node from the temporary list
        tmpMarked.remove(node);
        // Finally add it to the result list
        result.add(node);
    }

    /**
     * Returns the size of the graph
     */
    int size() {
        return mGraph.size();
    }

    @Nonnull
    private ArrayList<T> getEmptyList() {
        ArrayList<T> list = mListPool.acquire();
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    private void poolList(@Nonnull ArrayList<T> list) {
        list.clear();
        mListPool.release(list);
    }
}
