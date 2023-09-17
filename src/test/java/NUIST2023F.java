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

public class NUIST2023F {

    static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    static int nextInt() {
        try {
            return Integer.parseInt(br.readLine());
        } catch (IOException e) {
            return 0;
        }
    }

    static long nextULong() {
        try {
            return Long.parseUnsignedLong(br.readLine());
        } catch (IOException e) {
            return 0;
        }
    }

    public static void main(String[] args) {
        PrintWriter w = new PrintWriter(System.out);
        int t = nextInt();
        while (t-- != 0) {
            long a = nextULong(), r = 1;
            do ++r;
            while (Long.remainderUnsigned(a, r) == 0);
            w.println(r);
        }
        w.flush();
    }
}
