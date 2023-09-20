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

import java.io.PrintWriter;
import java.util.*;

// date=13 && friday
public class LevP1329 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        Calendar cal = Calendar.getInstance(Locale.ROOT);
        cal.set(Calendar.DATE, 13);
        while (sc.hasNext()) {
            int year = sc.nextInt();
            cal.set(Calendar.YEAR, year);
            int count = 0;
            for (int i = 0; i < 12; i++) {
                cal.set(Calendar.MONTH, i);
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                    count++;
                }
            }
            pw.print(year);
            pw.print(' ');
            pw.println(count);
        }
        pw.flush();
    }
}
