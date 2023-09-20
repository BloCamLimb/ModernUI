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

public class CF1749C {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int t = scanner.nextInt();
        int[] elements = new int[128], temp = new int[128];
        while (t-- > 0) {
            int n = scanner.nextInt();
            for (int i = 0; i < n; i++) elements[i] = scanner.nextInt();
            System.out.println(find(elements, temp, n));
        }
    }

    public static int find(int[] elements, int[] temp, int n) {
        Arrays.sort(elements, 0, n);
        int low = 0, high = n;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            System.arraycopy(elements, 0, temp, 0, n);
            int k = 0, pos = n - 1;
            for (int i = 0; i <= mid; i++) {
                for (; pos >= 0; pos--) {
                    if (temp[pos] <= mid - i) {
                        k++;
                        temp[pos] = Integer.MAX_VALUE;
                        break;
                    }
                }
                if (pos <= i) break;
                temp[i] = Integer.MAX_VALUE;
            }
            if (k < mid) high = mid - 1;
            else low = mid + 1;
        }
        return high;
    }
}
