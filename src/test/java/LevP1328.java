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
import java.util.Arrays;
import java.util.StringTokenizer;

public class LevP1328 {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out);
        int[] a = new int[20];
        int n = nextInt();
        while (n-- > 0) {
            int sum = nextInt() + nextInt() + nextInt();
            int v, m = 0;
            while ((v = nextInt()) != -1)
                a[m++] = -v;
            Arrays.sort(a, 0, m);
            pw.println(sum - a[0] - a[1] - a[2]);
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
