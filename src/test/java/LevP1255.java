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

public class LevP1255 {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out);
        int t = nextInt();
        while (t-- > 0) {
            int n = nextInt(), v = nextInt();
            // Kadane’s Algorithm
            int cur = v, max = v, p = 0, st = 0, en = 0;
            for (int i = 1; i < n; i++) {
                v = nextInt();
                if (cur < 0) {
                    cur = v;
                    p = i;
                } else {
                    cur += v;
                }
                if (cur > max) {
                    max = cur;
                    st = p;
                    en = i;
                }
            }
            pw.print(max);pw.print(' ');pw.print(st + 1);pw.print(' ');pw.print(en + 1);pw.println();
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
