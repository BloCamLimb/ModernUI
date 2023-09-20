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
import java.util.BitSet;
import java.util.StringTokenizer;

public class LevP1276 {

    static final int N = 10000;

    public static void main(String[] args) {
        BitSet composites = new BitSet(N + 1);
        { // Euler's Sieve
            int[] primes = new int[N + 1];
            int p = 0;
            for (int i = 2; i <= N; i++) {
                if (!composites.get(i))
                    primes[p++] = i;
                for (int j = 0; j < p; j++) {
                    int k = i * primes[j];
                    if (k > N) break;
                    composites.set(k);
                    if (i % primes[j] == 0) break;
                }
            }
        }
        PrintWriter pw = new PrintWriter(System.out);
        int t = nextInt();
        while (t-- > 0) {
            int a = nextInt(), b = nextInt(), cnt = 0;
            for (int i = a; i <= b; i++) {
                if (!composites.get(i)) { // is prime
                    int num = i, sum = 0;
                    do {
                        sum += num % 10;
                        num /= 10;
                    } while (num != 0);
                    if (!composites.get(sum)) cnt++;  // sum of digits is also prime
                }
            }
            pw.println(cnt);
        }
        pw.flush();
    }

    static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    static StringTokenizer st;

    static String next() {
        while (st == null || !st.hasMoreElements()) {
            try {
                st = new StringTokenizer(br.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return st.nextToken();
    }

    static int nextInt() {
        return Integer.parseInt(next());
    }
}
