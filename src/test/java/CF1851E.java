/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

import java.io.*;
import java.util.*;

public class CF1851E {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        int t = sc.nextInt();
        int[] prices = new int[200001];
        int[] costs = new int[200001];
        while (t-- != 0) {
            int n = sc.nextInt();
            int k = sc.nextInt();
            for (int i = 1; i <= n; i++) {
                prices[i] = sc.nextInt();
            }
            Arrays.fill(costs, 1, n + 1, -1);
            Node[] DAG = new Node[n];
            Arrays.setAll(DAG, i -> new Node(i + 1));
            for (int i = 1; i <= k; i++) {
                costs[sc.nextInt()] = 0;
            }
            for (int i = 1; i <= n; i++) {
                int m = sc.nextInt();
                if (costs[i] != -1) {
                    while (m-- != 0) {
                        sc.nextInt();
                    }
                } else if (m > 0) {
                    int[] dep = new int[m];
                    for (int j = 0; j < m; j++) {
                        dep[j] = sc.nextInt() - 1;
                    }
                    DAG[i - 1].dependencies = dep;
                }
            }
            topologicalSort(DAG);
            for (Node node : DAG) {
                int type = node.type;
                if (costs[type] == -1) {
                    if (node.dependencies != null) {
                        long c = 0;
                        for (int edge : node.dependencies) {
                            c += costs[edge + 1];
                        }
                        costs[type] = (int) Math.min(c, prices[type]);
                    } else {
                        costs[type] = prices[type];
                    }
                }
            }
            for (int i = 1; i <= n; i++) {
                pw.print(costs[i]);
                pw.print(' ');
            }
            pw.print('\n');
        }
        pw.flush();
    }

    static class Node {

        int type;
        int[] dependencies;
        int index = -1;

        public Node(int type) {
            this.type = type;
        }
    }

    public static void topologicalSort(Node[] graph) {

        int index = 0;

        // Start a DFS from each node in the graph.
        for (Node node : graph) {
            // Output this node after all the nodes it depends on have been output.
            index = dfsVisit(graph, node, index);
        }

        assert index == graph.length;

        // Reorder the array given the output order.
        for (int i = 0, e = graph.length; i < e; i++) {
            for (int j = graph[i].index; j != i; ) {
                Node temp = graph[j];
                graph[j] = graph[i];
                graph[i] = temp;
                j = temp.index;
            }
        }
    }

    /**
     * Recursively visit a node and all the other nodes it depends on.
     */
    private static <T> int dfsVisit(Node[] graph, final Node node, int index) {
        if (node.index != -1) {
            return index;
        }
        final int[] dep = node.dependencies;
        if (dep != null) {
            for (int edge : dep) {
                index = dfsVisit(graph, graph[edge], index);
            }
        }
        node.index = index;
        return index + 1;
    }

    static class Scanner {

        final BufferedReader br;
        StringTokenizer st;

        Scanner(InputStream in) {
            br = new BufferedReader(new InputStreamReader(in));
        }

        String next() {
            while (st == null || !st.hasMoreElements()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        int nextInt() {
            return Integer.parseInt(next());
        }
    }
}
