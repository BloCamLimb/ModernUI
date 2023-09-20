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

public class LevP1236 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] dp = new int[1001];
        while (scanner.hasNext()) {
            int m = scanner.nextInt(), n = scanner.nextInt();
            for (int i = 1, e = Math.max(m, n); i <= e; i++)
                dp[i] = 0;
            while (m-- > 0) for (int i = 1; i <= n; i++)
                dp[i] = Math.max(dp[i], dp[i - 1]) + scanner.nextInt();
            System.out.println(dp[n]);
        }
    }
}
