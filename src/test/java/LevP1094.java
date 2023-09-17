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

public class LevP1094 {

    public static final int N = 10;
    public static final int[] DX = {-1, 1, 0, 0};
    public static final int[] DY = {0, 0, -1, 1};
    public static final char[] CHAR_MAP = {'N', 'S', 'W', 'E'};

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[][] map = new int[N][N];
        char[] ans = new char[N * N];
        CYCLE:
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n; j++)
                    map[i][j] = scanner.nextInt();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (map[i][j] != 0) continue;
                    map[i][j] = 1;
                    int ret = dfs(map, n, i, j, 0, ans);
                    if (ret != -1) {
                        System.out.printf("%d %d\n", i + 1, j + 1);
                        System.out.println(String.valueOf(ans, 0, ret));
                        continue CYCLE;
                    }
                    map[i][j] = 0;
                }
            }
        }
    }

    static int dfs(int[][] map, int n, int sx, int sy, int depth, char[] ans) {
        int sum = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                sum += map[i][j];
        if (sum == n * n) return depth;
        for (int dir = 0; dir < 4; dir++) {
            int move = 0;
            for (;;) {
                int x = sx + DX[dir] * (move + 1);
                int y = sy + DY[dir] * (move + 1);
                if (x < 0 || y < 0 || x >= n || y >= n) break;
                if (map[x][y] != 0) break;
                map[x][y] = 1;
                move++;
            }
            if (move != 0) {
                ans[depth] = CHAR_MAP[dir];
                int ret = dfs(map, n, sx + DX[dir] * move, sy + DY[dir] * move, depth + 1, ans);
                if (ret != -1) return ret;
                for (int j = move; j >= 1; j--)
                    map[sx + DX[dir] * j][sy + DY[dir] * j] = 0;
            }
        }
        return -1;
    }
}
