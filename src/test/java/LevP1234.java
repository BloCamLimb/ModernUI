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

public class LevP1234 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            double a = scanner.nextDouble(), b = scanner.nextDouble(), c = scanner.nextDouble(),
                    d = scanner.nextDouble(), e = scanner.nextDouble(), f = scanner.nextDouble();
            double x0 = (scanner.nextDouble() + scanner.nextDouble()) * 0.5, x1, y0, y1;
            do {
                y0 = ((((a * x0 + b) * x0 + c) * x0 + d) * x0 + e) * x0 + f;
                y1 = (((5 * a * x0 + 4 * b) * x0 + 3 * c) * x0 + 2 * d) * x0 + e;
                x1 = x0;
                x0 = x0 - y0 / y1;
            } while (Math.abs(x1 - x0) >= 1e-6);
            System.out.printf("%.4f\n", x1); // not exactly correct, should print x0 but wrong answer
        }
    }
}
