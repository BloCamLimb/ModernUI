/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import java.io.PrintWriter;
import java.util.*;

public class TestSieveOfEratosthenes {

    public static void main(String[] args) {
        var pw = new PrintWriter(System.out);
        int n = 10000000;
        int[] arr = new int[n];
        Random random = new Random();
        for (int i = 0; i < n; i++) {
            arr[i] = random.nextInt(n);
        }
        int[] divisor = divisor(n);
        int[] count = new int[divisor.length];
        long time = System.nanoTime();
        pw.println(largestGCDSubsequence(arr, n, divisor, count));
        time = System.nanoTime() - time;
        pw.println(time);
        pw.flush();
    }

    static int[] divisor(int n) {
        int[] divisor = new int[n + 1];
        for (int i = 2, e = (int) Math.sqrt(n); i <= e; ++i) {
            if (divisor[i] == 0)
                for (int j = i << 1; j <= n; j += i)
                    divisor[j] = i;
        }
        for (int i = 1; i < n; ++i)
            if (divisor[i] == 0)
                divisor[i] = i;
        return divisor;
    }

    static int largestGCDSubsequence(int[] arr, int n, int[] divisor, int[] count) {
        int ans = 0;
        for (int i = 0; i < n; ++i) {
            int element = arr[i];
            while (element > 1) {
                int div = divisor[element];
                ans = Math.max(ans, ++count[div]);
                while (element % div == 0)
                    element /= div;
            }
        }
        return ans;
    }
}
