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

public class CF1900A {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        int t = sc.nextInt();
        while (t-- != 0) {
            int n = sc.nextInt();
            String s = sc.next();
            int res = 0, c3 = 0;
            int last = -1;
            for (int i = 0; i <= n; i++) {
                if (i == n || s.charAt(i) == '#') {
                    if (last != -1) {
                        int diff = i - last;
                        if (diff < 3) {
                            res += diff;
                        } else {
                            c3++;
                        }
                    }
                    last = -1;
                } else if (last == -1) {
                    last = i;
                }
            }
            if (c3 > 0) {
                res = 2;
            }
            pw.println(res);
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
    }
}
