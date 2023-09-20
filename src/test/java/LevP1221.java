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

import java.util.Scanner;

// disjoint set, union-find
public class LevP1221 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] parent = new int[30000], rank = new int[30000];
        while (scanner.hasNext()) {
            int n = scanner.nextInt(), m = scanner.nextInt();
            if ((n | m) == 0) break;
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 1;
            }
            while (m-- > 0) {
                int k = scanner.nextInt();
                int first = scanner.nextInt();
                for (int i = 1; i < k; i++)
                    union(parent, rank, first, scanner.nextInt());
            }
            int ans = 1, victim = find(parent, 0);
            for (int i = 1; i < n; i++)
                if (find(parent, i) == victim) ans++;
            System.out.println(ans);
        }
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
}
