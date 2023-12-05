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

public class CF1902B {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        int tc = sc.nextInt();
        while (tc-- != 0) {
            int n = sc.nextInt();
            long P = sc.nextLong();
            int l = sc.nextInt(), t = sc.nextInt();
            int used;
            int tasks = (n + 6) / 7;
            if ((tasks / 2) * (t * 2L + l) >= P) {
                used = (int) ((P + (t * 2L + l) - 1) / (t * 2L + l));
            } else {
                used = tasks / 2;
                P -= used * (t * 2L + l);
                assert P > 0;
                if (tasks % 2 == 1) {
                    used++;
                    P -= t + l;
                }
                if (P > 0) {
                    used += (int) ((P + l - 1) / l);
                }
            }
            assert used <= n;
            pw.println(n - used);
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

        int nextInt() {
            return Integer.parseInt(next());
        }

        long nextLong() {
            return Long.parseLong(next());
        }
    }
}
