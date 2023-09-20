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

import java.io.*;
import java.util.StringTokenizer;

public class LevP1790 {

    static final int N = 100;

    public static void main(String[] args) {
        int[] parent = new int[N + 1], rank = new int[N + 1];
        int n = nextInt(), m = nextInt();
        for (int i = 1; i <= n; i++) {
            parent[i] = i;
            rank[i] = 1;
        }
        while (m-- > 0)
            union(parent, rank, nextInt(), nextInt());
        int comp = 0;
        for (int i = 1; comp < 2 && i <= n; i++)
            if (find(parent, i) == i) comp++;
        System.out.println(comp == 1 ? "Yes" : "No");
    }

    static int find(int[] parent, int i) {
        return parent[i] != i ? parent[i] = find(parent, parent[i]) : i;
    }

    static void union(int[] parent, int[] rank, int x, int y) {
        x = find(parent, x);
        y = find(parent, y);
        if (x == y) return; // connected
        int rx = rank[x], ry = rank[y];
        if (rx > ry) parent[y] = x;
        else {
            parent[x] = y;
            if (rx == ry) rank[y]++;
        }
    }

    static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    static StringTokenizer st;

    static String next() {
        while (st == null || !st.hasMoreElements()) {
            try {
                st = new StringTokenizer(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return st.nextToken();
    }

    static int nextInt() {
        return Integer.parseInt(next());
    }
}
