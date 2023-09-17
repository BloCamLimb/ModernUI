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

import java.math.BigInteger;
import java.util.Scanner;

public class LevP1132 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        BigInteger b10 = BigInteger.valueOf(10), b9 = BigInteger.valueOf(9);
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            System.out.println(b10.pow(n).subtract(b9.pow(n)));
        }
    }
}
