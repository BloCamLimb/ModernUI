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

public class LevP1233 {

    public static final int N = 60;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[][] dp = new int[N + 1][N + 1];
        for (int i = 1; i <= N; i++)
            dp[i][1] = 1;
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 1; i <= n; i++)
                for (int j = 2; j <= i; j++)
                    dp[i][j] = dp[i - 1][j - 1] + dp[i - j][j];
            int ans = 0;
            for (int i = 2; i <= n; i++)
                ans += dp[n][i];
            System.out.println(ans);
        }
    }
}
