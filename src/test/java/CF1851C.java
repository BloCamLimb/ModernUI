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
import java.util.StringTokenizer;

public class CF1851C {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        int t = sc.nextInt();
        int[] c = new int[200000];
        while (t-- != 0) {
            int n = sc.nextInt();
            int k = sc.nextInt();
            for (int i = 0; i < n; i++) {
                c[i] = sc.nextInt();
            }
            boolean ans;
            if (c[0] == c[n - 1]) {
                if (k < 2) {
                    ans = true;
                } else {
                    int c0 = c[0];
                    int target = k - 2;
                    for (int i = 1; i < n - 1 && target != 0; i++) {
                        if (c[i] == c0) {
                            target--;
                        }
                    }
                    ans = target == 0;
                }
            } else {
                int c0 = c[0];
                int target = k - 1;
                int i;
                for (i = 1; i < n - 1 && target != 0; i++) {
                    if (c[i] == c0) {
                        target--;
                    }
                }
                ans = target == 0;
                if (ans) {
                    int c1 = c[n - 1];
                    target = k - 1;
                    for (int j = n - 2; j >= i && target != 0; j--) {
                        if (c[j] == c1) {
                            target--;
                        }
                    }
                    ans = target == 0;
                }
            }

            pw.println(ans?"YES":"NO");
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
