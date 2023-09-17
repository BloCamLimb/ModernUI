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

public class LevP1219 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++)
            a[i] = scanner.nextInt();
        System.out.println(n - lengthOfLIS(a, n));
    }

    // longest non-decreasing subsequence
    static int lengthOfLIS(int[] a, int n) {
        if (n <= 1) return n;
        int[] tail = new int[n];
        int length = 1;
        tail[0] = a[0];
        for (int i = 1; i < n; i++) {
            int v = a[i], idx = upperBound(tail, length, v);
            if (idx == length) tail[length++] = v;
            else tail[idx] = v;
        }
        return length;
    }

    static int upperBound(int[] a, int end, int key) {
        int low = 0, high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (a[mid] > key) high = mid - 1;
            else low = mid + 1;
        }
        return low;
    }
}
