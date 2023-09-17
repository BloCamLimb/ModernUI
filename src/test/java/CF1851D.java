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
import java.util.BitSet;
import java.util.StringTokenizer;

public class CF1851D {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        long t = sc.nextLong();
        BitSet bs = new BitSet(200000);
        while (t-- != 0) {
            bs.clear();
            long n = sc.nextLong();
            long strange_number = -1;
            boolean hard_fail = false;
            long last_value = 0;
            for (int i = 1; i < n; i++) {
                long temp = sc.nextLong();
                long value = temp - last_value;
                last_value = temp;
                if (value > 3 * n) {
                    hard_fail = true;
                } else if (value > n) {
                    if (strange_number == -1) {
                        strange_number = value;
                    } else {
                        hard_fail = true;
                    }
                } else {
                    int key = (int) value - 1;
                    if (bs.get(key)) {
                        if (strange_number == -1) {
                            strange_number = value;
                        } else {
                            hard_fail = true;
                        }
                    } else {
                        bs.set(key);
                    }
                }
            }
            boolean ans = false;
            if (!hard_fail) {
                if (strange_number == -1) {
                    ans = true;
                } else {
                    int v1 = -1, v2 = -1;
                    for (int i = 0; i < n; i++) {
                        if (!bs.get(i)) {
                            if (v1 == -1) {
                                v1 = i;
                            } else if (v2 == -1) {
                                v2 = i;
                            } else {
                                break;
                            }
                        }
                    }
                    if (v1 + v2 + 2 == strange_number) {
                        ans = true;
                    }
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

        long nextLong() {
            return Long.parseLong(next());
        }
    }
}
