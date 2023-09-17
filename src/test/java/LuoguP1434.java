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

import java.util.Scanner;

// LevP1029
public class LuoguP1434 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[][] map = new int[100][100], dist = new int[100][100];
        int r = scanner.nextInt(), c = scanner.nextInt();
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                map[i][j] = scanner.nextInt();
        int ans = 0;
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                ans = Math.max(ans, dfs(map, dist, r, c, i, j));
        System.out.println(ans);
    }

    static int dfs(int[][] map, int[][] dist, int r, int c, int sx, int sy) {
        if (dist[sx][sy] != 0) return dist[sx][sy];
        dist[sx][sy] = 1;
        for (int i = 0; i < 4; i++) {
            int x = i == 0 ? sx - 1 : i == 1 ? sx + 1 : sx;
            int y = i == 2 ? sy - 1 : i == 3 ? sy + 1 : sy;
            if (x < 0 || y < 0 || x >= r || y >= c) continue;
            if (map[x][y] >= map[sx][sy]) continue;
            dist[sx][sy] = Math.max(dist[sx][sy], dfs(map, dist, r, c, x, y) + 1);
        }
        return dist[sx][sy];
    }
}
