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

public class NUIST2023B {

    static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    static StringTokenizer st;

    static int nextInt() {
        while (st == null || !st.hasMoreElements())
            try {
                st = new StringTokenizer(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        return Integer.parseInt(st.nextToken());
    }

    public static void main(String[] args) {
        int n = nextInt();
        int[][] delta = {{-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}};
        int[] wind = delta[nextInt() - 1];
        int[][] scene = new int[52][52];
        boolean result = false;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                scene[i][j] = nextInt();
            }
        }
        CYCLE:
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= n; j++) {
                if (scene[i][j] != 0 || scene[i + wind[0]][j + wind[1]] != 2) {
                    continue;
                }
                for (int[] d : delta) {
                    if (scene[i + d[0]][j + d[1]] == 1) {
                        result = true;
                        break CYCLE;
                    }
                }
            }
        }
        System.out.println(result ? "Yes" : "No");
    }
}
