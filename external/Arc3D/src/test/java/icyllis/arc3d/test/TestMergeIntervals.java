/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import java.io.PrintWriter;
import java.util.*;

public class TestMergeIntervals {

    public static void main(String[] args) {
        var sc = new Scanner(System.in);
        var pw = new PrintWriter(System.out);
        int n = sc.nextInt();
        record Range(int st, int en) {
        }
        var ls = new ArrayList<Range>(n);
        for (int i = 0; i < n; i++)
            ls.add(new Range(sc.nextInt(), sc.nextInt()));
        ls.sort(Comparator.comparingInt(Range::st));
        int st = Integer.MIN_VALUE, en = Integer.MIN_VALUE;
        for (var e : ls)
            if (en < e.st) {
                if (st >= 0)
                    pw.println(st + " " + en);
                st = e.st;
                en = e.en;
            } else
                en = Math.max(en, e.en);
        if (st >= 0)
            pw.println(st + " " + en);
        pw.flush();
    }
}
