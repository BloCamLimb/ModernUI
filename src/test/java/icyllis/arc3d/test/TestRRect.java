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

import icyllis.arc3d.core.RRect;

import java.util.Random;

public class TestRRect {

    private static final Random random = new Random();

    private interface Rand {
        float f();
    }

    public static void main(String[] args) {
        testRun(TestRRect::rand_integer_small, "rand_integer_small");
        testRun(TestRRect::rand_integer_large, "rand_integer_large");
        testRun(TestRRect::rand_integer_huge, "rand_integer_huge");
        testRun(TestRRect::rand_float_small, "rand_float_small");
        testRun(TestRRect::rand_float_large, "rand_float_large");
        testRun(TestRRect::rand_float_huge, "rand_float_huge");
        testRun(TestRRect::rand_float_small_round, "rand_float_small_round");
        testRun(TestRRect::rand_float_large_round, "rand_float_large_round");
        testRun(TestRRect::rand_float_huge_round, "rand_float_huge_round");
        testRun(TestRRect::rand_float_small_ex, "rand_float_small_ex");
        testRun(TestRRect::rand_float_large_ex, "rand_float_large_ex");
        testRun(TestRRect::rand_float_huge_ex, "rand_float_huge_ex");

        RRect rrect = new RRect();
        rrect.setRectRadii(20, 20, 60, 60,
                new float[]{4,4,6,6,37,37,2,2});
        System.out.println(rrect.getType());
        System.out.println(rrect);
    }

    private static void testRun(Rand r, String tag) {
        RRect rrect = new RRect();
        long fail = 0;

        for (int i = 0; i < 10000; i++) {
            rrect.setRect(r.f(), r.f(), r.f(), r.f());
            assert rrect.isRect() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            rrect.setOval(r.f(), r.f(), r.f(), r.f());
            assert rrect.isOval() || rrect.isRect() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            float cx = r.f();
            float cy = r.f();
            float xrad = r.f();
            float yrad = r.f();
            rrect.setOval(cx - xrad, cy - yrad,
                    cx + xrad, cy + yrad);
            assert rrect.isOval() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            float cx = r.f();
            float cy = r.f();
            float rad = r.f();
            rrect.setOval(cx - rad, cy - rad,
                    cx + rad, cy + rad);
            assert rrect.isOval() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            float xrad = r.f();
            float yrad = r.f();
            rrect.setEllipse(r.f(), r.f(), xrad, yrad);
            assert rrect.isOval() || rrect.isRect() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            float rad = r.f();
            rrect.setEllipse(r.f(), r.f(), rad, rad);
            assert rrect.isOval() || rrect.isRect() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            rrect.setRectXY(r.f(), r.f(), r.f(), r.f(), r.f(), r.f());
            assert rrect.isRect() || rrect.isSimple() || rrect.isOval() || rrect.isEmpty() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            float cx = r.f();
            float cy = r.f();
            float xrad = r.f();
            float yrad = r.f();
            rrect.setRectXY(cx - xrad, cy - yrad,
                    cx + xrad, cy + yrad,
                    xrad, yrad);
            assert rrect.isRect() || rrect.isSimple() || rrect.isOval() || rrect.isEmpty() : rrect;
            if (!rrect.isOval() && !rrect.isEmpty()) {
                fail++;
            }
        }

        for (int i = 0; i < 10000; i++) {
            float cx = r.f();
            float cy = r.f();
            float rad = r.f();
            rrect.setRectXY(cx - rad, cy - rad,
                    cx + rad, cy + rad,
                    rad, rad);
            assert rrect.isRect() || rrect.isSimple() || rrect.isOval() || rrect.isEmpty() : rrect;
            if (!rrect.isOval() && !rrect.isEmpty()) {
                fail++;
            }
        }

        for (int i = 0; i < 10000; i++) {
            rrect.setNineSlice(r.f(), r.f(), r.f(), r.f(),
                    r.f(), r.f(), r.f(), r.f());
        }

        for (int i = 0; i < 10000; i++) {
            float xrad = r.f();
            float yrad = r.f();
            rrect.setNineSlice(r.f(), r.f(), r.f(), r.f(),
                    xrad, yrad, xrad, yrad);
            assert !rrect.isComplex() && !rrect.isNineSlice() : rrect;
        }

        for (int i = 0; i < 10000; i++) {
            float cx = r.f();
            float cy = r.f();
            float xrad = r.f();
            float yrad = r.f();
            rrect.setNineSlice(cx - xrad, cy - yrad,
                    cx + xrad, cy + yrad,
                    xrad, yrad, xrad, yrad);
            assert !rrect.isComplex() && !rrect.isNineSlice() : rrect;
            if (!rrect.isOval() && !rrect.isEmpty()) {
                fail++;
            }
        }

        for (int i = 0; i < 10000; i++) {
            float cx = r.f();
            float cy = r.f();
            float rad = r.f();
            rrect.setNineSlice(cx - rad, cy - rad,
                    cx + rad, cy + rad,
                    rad, rad, rad, rad);
            assert !rrect.isComplex() && !rrect.isNineSlice() : rrect;
            if (!rrect.isOval() && !rrect.isEmpty()) {
                fail++;
            }
        }

        rrect.setRect(2, 2, 2, 2);
        assert rrect.getType() == RRect.kEmpty_Type;

        rrect.setOval(2, 2, 2, 10);
        assert rrect.getType() == RRect.kEmpty_Type;

        System.out.printf("[%s] fail: %d%n", tag, fail);
    }

    private static float rand_integer_small() {
        return random.nextInt(-1000, 1000);
    }

    private static float rand_integer_large() {
        return random.nextInt(-50000, 50000);
    }

    private static float rand_integer_huge() {
        return random.nextInt(-30000000, 30000000);
    }

    private static float rand_float_small() {
        return random.nextFloat(-1000, 1000);
    }

    private static float rand_float_large() {
        return random.nextFloat(-50000, 50000);
    }

    private static float rand_float_huge() {
        return random.nextFloat(-30000000, 30000000);
    }

    private static float rand_float_small_round() {
        return Math.round(random.nextFloat(-1000, 1000) * 4F) / 4F;
    }

    private static float rand_float_large_round() {
        return Math.round(random.nextFloat(-50000, 50000) * 4F) / 4F;
    }

    private static float rand_float_huge_round() {
        return Math.round(random.nextFloat(-30000000, 30000000) * 4F) / 4F;
    }

    private static float rand_float_small_ex() {
        return switch (random.nextInt(10)) {
            case 0 -> 0;
            case 1 -> Float.POSITIVE_INFINITY;
            case 2 -> Float.NEGATIVE_INFINITY;
            case 3 -> Float.NaN;
            case 4 -> Float.MAX_VALUE;
            case 5 -> Float.MIN_VALUE;
            case 6 -> Float.MIN_NORMAL;
            case 7 -> Float.MIN_NORMAL * 0.25f;
            default -> random.nextFloat(-1000, 1000);
        };
    }

    private static float rand_float_large_ex() {
        return switch (random.nextInt(10)) {
            case 0 -> 0;
            case 1 -> Float.POSITIVE_INFINITY;
            case 2 -> Float.NEGATIVE_INFINITY;
            case 3 -> Float.NaN;
            case 4 -> Float.MAX_VALUE;
            case 5 -> Float.MIN_VALUE;
            case 6 -> Float.MIN_NORMAL;
            case 7 -> Float.MIN_NORMAL * 0.25f;
            default -> random.nextFloat(-50000, 50000);
        };
    }

    private static float rand_float_huge_ex() {
        return switch (random.nextInt(10)) {
            case 0 -> 0;
            case 1 -> Float.POSITIVE_INFINITY;
            case 2 -> Float.NEGATIVE_INFINITY;
            case 3 -> Float.NaN;
            case 4 -> Float.MAX_VALUE;
            case 5 -> Float.MIN_VALUE;
            case 6 -> Float.MIN_NORMAL;
            case 7 -> Float.MIN_NORMAL * 0.25f;
            default -> random.nextFloat(-30000000, 30000000);
        };
    }
}
