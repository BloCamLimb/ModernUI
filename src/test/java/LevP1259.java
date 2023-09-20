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

import java.util.Scanner;

// 39 -> 3+9=12 -> 1+2=3
public class LevP1259 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        long n;
        while ((n = scanner.nextLong()) != 0) find(n);
    }

    static void find(long n) {
        int res = 0;
        do {
            res += n % 10;
            n /= 10;
        } while (n != 0);
        if (res < 10) System.out.println(res);
        else find(res);
    }
}
