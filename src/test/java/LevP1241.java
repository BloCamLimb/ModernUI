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
import java.util.Scanner;

// ****A*BC*DEF*G****** -> ABCDEFG******
public class LevP1241 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        while (sc.hasNext()) {
            String s = sc.next();
            int i = s.length() - 1;
            for (; i >= 0; i--)
                if (s.charAt(i) != '*')
                    break;
            for (int j = 0; j < i; j++)
                if (s.charAt(j) != '*')
                    pw.print(s.charAt(j));
            pw.println(s.substring(i));
        }
        pw.flush();
    }
}
