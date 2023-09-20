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

// CF932E
// solve sum i=1..n C(n,i)*i^k
// input n,k (1<=n<=1e9, 1<=k<=1e5)
// time limit: 1s
public class LevP1839 {

    public static final int
            G = 332748118,
            P = 998244353;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        solve(scanner.nextInt(), scanner.nextInt());
    }

    // sum i=1..k S2(k,i) * falling_fact(n,i) * 2^(n-i)
    // complexity: Omega(klogk) + Omega( k*( O(1) + O(logn) ) )
    static void solve(int n, int k) {
        int lim = 1 << -Integer.numberOfLeadingZeros((k << 1) - 1);
        long[] a, b;
        if (k <= 2) {
            a = new long[]{0, 1, 1, 0};
        } else if (k == 4) {
            a = new long[]{0, 1, 7, 6, 1, 0, 0, 0};
        } else {
            a = new long[lim];
            b = new long[lim];
            long[] fac = new long[100005], ifac = new long[100005];
            fac[0] = ifac[0] = 1;
            for (int i = 1; i <= 100000; i++)
                fac[i] = fac[i - 1] * i % P;
            ifac[100000] = qpow(fac[100000], P - 2);
            for (int i = 99999; i > 0; i--)
                ifac[i] = ifac[i + 1] * (i + 1) % P;
            for (int i = 0; i <= k; i++) {
                a[i] = (i & 1) != 0 ? (P - ifac[i]) : ifac[i];
                b[i] = (qpow(i, k) * ifac[i]) % P;
            }
            polymul(a, b, lim);
        }
        long ans = 0, ff = 1; // falling factorial
        for (int i = 0, e = Math.min(k, n); // for k > n, ff is 0, so en here
             i <= e; i++) {
            long v = a[i] * ff % P;
            ff = ff * (n - i) % P;
            v = v * qpow(2, n - i) % P;
            ans = (ans + v) % P;
        }
        System.out.println(ans);
    }

    static void polymul(long[] a, long[] b, int lim) {
        int[] rev = new int[lim];
        for (int i = 0; i < lim; i++)
            rev[i] = (rev[i >> 1] >> 1) | ((i & 1) != 0 ? lim >> 1 : 0);
        ntt(a, rev, lim, 1);
        ntt(b, rev, lim, 1);
        for (int i = 0; i < lim; i++)
            a[i] = a[i] * b[i] % P;
        ntt(a, rev, lim, -1);
        long inv = qpow(lim, P - 2);
        for (int i = 0; i < lim; i++)
            a[i] = a[i] * inv % P;
    }

    static void ntt(long[] a, int[] rev, int lim, int opt) {
        for (int i = 0; i < lim; i++)
            if (i < rev[i]) swap(a, i, rev[i]);
        for (int m = 2; m <= lim; m <<= 1) {
            int k = m >> 1;
            long nw = qpow(opt == 1 ? 3 : G, (P - 1) / m);
            for (int i = 0; i < lim; i += m) {
                long w = 1;
                for (int j = 0; j < k; j++, w = w * nw % P) {
                    long x = a[i + j], y = w * a[i + j + k] % P;
                    a[i + j] = (x + y) % P;
                    a[i + j + k] = (x - y + P) % P;
                }
            }
        }
    }

    static long qpow(long a, long b) {
        long i = 1;
        for (; b != 0; b >>= 1, a = a * a % P)
            if ((b & 1) != 0) i = i * a % P;
        return i;
    }

    static void swap(long[] a, int i, int j) {
        long t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
