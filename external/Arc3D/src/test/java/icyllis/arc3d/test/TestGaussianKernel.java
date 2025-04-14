/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TestGaussianKernel {

    public static void main(String[] args) throws IOException {
        int radius = 15;
        double[] kernel = createKernel(radius, radius / 3.0);
        double value = 1.0;
        List<String> lines = new ArrayList<>();

        for (int r = -radius, j = 0; ; j += radius * 2) {

            double c = (double) r / (radius * 1.25);
            double approx = (r > 0
                    ? Math.pow(1 - c, 3.0)
                    : 2 - Math.pow(1 + c, 3.0)) * 0.5;

            System.out.printf("radius: %d, value: %f, approx: %f error: %f%n", r, value, approx, Math.abs(value-approx));
            lines.add(String.format("%d,%f", r, value));

            if (++r > radius) break;
            for (int k = 0; k < radius * 2; k++) {
                value -= kernel[j + k];
            }
        }

        Files.write(Path.of("test_gaussian_kernel.csv"), lines, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    public static double[] createKernel(int radius, double sigma) {
        int size = radius * 2;
        double[] kernel = new double[size * size];
        double base = -0.5 / (sigma * sigma);
        double factor = 1.0 / (sigma * Math.sqrt(2.0 * Math.PI));
        double wsum = 0.0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                double dx = Math.abs(j + 0.5 - radius);
                double dy = Math.abs(i + 0.5 - radius);
                double r = Math.sqrt(dx * dx + dy * dy);
                double w = Math.exp(r * r * base) * factor;
                kernel[i * size + j] = w;
                wsum += w;
            }
        }
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] /= wsum;
        }
        return kernel;
    }
}
