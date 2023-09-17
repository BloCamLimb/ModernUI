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

import java.util.Scanner;

public class LevP1214 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            int n = scanner.nextInt();
            for (int i = 1; i < 10; i++) {
                if ((i << 1) > n) break;
                for (int j = 0; j < 10; j++) {
                    if (((i + j) << 1) > n) break;
                    for (int k = 0; k < 10; k++) {
                        int sum = ((i + j) << 1) + k;
                        if (sum > n) break;
                        if (sum == n) System.out.printf("%c%c%c%c%c\n", i + '0', j + '0', k + '0', j + '0', i + '0');
                    }
                }
            }
            for (int i = 1; i < 10; i++) {
                if ((i << 1) > n) break;
                for (int j = 0; j < 10; j++) {
                    if (((i + j) << 1) > n) break;
                    for (int k = 0; k < 10; k++) {
                        int sum = ((i + j + k) << 1);
                        if (sum > n) break;
                        if (sum == n) System.out.printf("%c%c%c%c%c%c\n", i + '0', j + '0', k + '0', k + '0', j + '0', i + '0');
                    }
                }
            }
            System.out.println();
        }
    }
}
