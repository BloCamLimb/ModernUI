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

import java.io.*;
import java.math.BigInteger;
import java.util.StringTokenizer;

public class CF1850E {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        long t = sc.nextLong();
        while (t-- != 0) {
            long n = sc.nextLong();
            long c = sc.nextLong();
            long sum = 0;
            long inc = 0;
            for (int i = 0; i < n; ++i) {
                long a = sc.nextLong();
                sum += a * a;
                inc += a * 4;
            }
            var b = BigInteger.valueOf(inc);
            long sq = b.multiply(b).add(
                    BigInteger.valueOf(16 * n).multiply(BigInteger.valueOf(c - sum))
            ).sqrt().longValue();
            long ans = (sq - inc) / (8 * n);
            pw.println(ans);
        }
        pw.flush();
    }

    static class Scanner {

        final BufferedReader br;
        StringTokenizer st;

        Scanner(InputStream in) {
            br = new BufferedReader(new InputStreamReader(in));
        }

        String next() {
            while (st == null || !st.hasMoreElements()) {
                try {
                    st = new StringTokenizer(br.readLine());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return st.nextToken();
        }

        long nextLong() {
            return Long.parseLong(next());
        }
    }
}
