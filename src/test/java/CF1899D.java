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
import java.util.HashMap;
import java.util.StringTokenizer;

public class CF1899D {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        var map = new HashMap<Integer, Count>();
        int t = sc.nextInt();
        while (t-- != 0) {
            map.clear();
            for (int i = 0, n = sc.nextInt(); i < n; ++i) {
                ++map.computeIfAbsent(sc.nextInt(), __ -> new Count()).value;
            }
            long res = 0;
            Count c1 = map.get(1), c2 = map.get(2);
            if (c1 != null && c2 != null) {
                res = (long) c1.value * c2.value;
            }
            for (var count : map.values()) {
                long c = count.value;
                if (c <= 1) continue;
                res += c * (c - 1) / 2;
            }
            pw.println(res);
        }
        pw.flush();
    }

    static class Count {
        int value;
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
