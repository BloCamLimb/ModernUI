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

import java.util.Arrays;
import java.util.Scanner;

// RE in OJ, use C++ version
public class LevP1256 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] a = new int[1000], b = new int[1000];
        while (scanner.hasNext()) {
            int m = scanner.nextInt(), n = scanner.nextInt(), c = scanner.nextInt();
            for (int i = 0; i < m; i++)
                a[i] = scanner.nextInt();
            for (int i = 0; i < n; i++)
                b[i] = scanner.nextInt();
            Arrays.sort(a, 0, m);
            Arrays.sort(b, 0, n);
            int i = 0, j = 0, ans = 0;
            while (i < m && j < n) {
                if (a[i] - b[j] > c) j++;
                else if (b[j] - a[i] > c) i++;
                else {
                    i++;
                    j++;
                    ans++;
                }
            }
            System.out.println(ans);
        }
    }
}
