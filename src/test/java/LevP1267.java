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

import java.util.Arrays;
import java.util.Scanner;

public class LevP1267 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int[] a = new int[3];
        while (scanner.hasNext()) {
            for (int i = 0; i < 3; i++)
                a[i] = scanner.nextInt();
            Arrays.sort(a);
            if (a[0] + a[1] > a[2]) {
                if (a[0] == a[1] && a[1] == a[2]) {
                    System.out.println("regular triangle");
                } else if (a[0] == a[1] || a[1] == a[2]) {
                    System.out.println("isosceles triangle");
                } else if (a[0] * a[0] + a[1] * a[1] == a[2] * a[2]) {
                    System.out.println("right triangle");
                } else {
                    System.out.println("triangle");
                }
            } else {
                System.out.println("not a triangle");
            }
        }
    }
}
