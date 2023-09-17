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

import java.io.PrintWriter;
import java.util.Scanner;
import java.util.regex.Pattern;

// evaluate 1+1=2
public class LevP1341 {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PrintWriter pw = new PrintWriter(System.out);
        Pattern p = Pattern.compile("[+=]");
        while (sc.hasNext()) {
            int n = sc.nextInt();
            boolean result = true;
            while (n-- > 0) {
                String expr = sc.next();
                if (result) {
                    String[] codes = p.split(expr);
                    result = Integer.parseInt(codes[0]) + Integer.parseInt(codes[1]) == Integer.parseInt(codes[2]);
                }
            }
            pw.println(result ? "Accepted" : "Wrong Answer");
        }
        pw.flush();
    }
}
