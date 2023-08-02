/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

/**
 * Utility class that provides auxiliary operations.
 */
public class MathUtil {

    // compile-time
    private static final boolean USE_SIN_TABLE = false;

    // 256 kB
    private static final float[] SIN_TABLE;

    public static final float PI = (float) Math.PI;
    public static final float PI2 = (float) (Math.PI * 2.0);
    public static final float PI3 = (float) (Math.PI * 3.0);
    public static final float PI4 = (float) (Math.PI * 4.0);
    public static final float PI_O_2 = (float) (Math.PI / 2.0);
    public static final float PI_O_3 = (float) (Math.PI / 3.0);
    public static final float PI_O_4 = (float) (Math.PI / 4.0);
    public static final float PI_O_6 = (float) (Math.PI / 6.0);

    public static final float EPS = 1.0e-5f;
    public static final float INV_EPS = 1.0e5f;

    // DEG_TO_RAD == 1.0 / RAD_TO_DEG
    public static final float DEG_TO_RAD = 0.01745329251994329576923690768489f;
    public static final float RAD_TO_DEG = 57.295779513082320876798154814105f;

    // SQRT2 == INV_SQRT2 * 2.0
    public static final float SQRT2 = 1.4142135623730951f;
    public static final float INV_SQRT2 = 0.7071067811865476f;

    static {
        if (USE_SIN_TABLE) {
            float[] v = new float[0x10000];
            for (int i = 0; i < 0x10000; i++)
                v[i] = (float) Math.sin(i * 9.587379924285257E-5);
            SIN_TABLE = v;
        } else {
            SIN_TABLE = null;
        }
    }

    // fast sin, error +- 0.000152, in radians
    private static float fsin(float a) {
        return SIN_TABLE[Math.round(a * 10430.378f) & 0xffff];
    }

    // fast cos, error +- 0.000152, in radians
    private static float fcos(float a) {
        return SIN_TABLE[(Math.round(a * 10430.378f) + 0x4000) & 0xffff];
    }

    // sin
    public static float sin(float a) {
        return (float) Math.sin(a);
    }

    // cos
    public static float cos(float a) {
        return (float) Math.cos(a);
    }

    // tan
    public static float tan(float a) {
        return (float) Math.tan(a);
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isApproxZero(float a) {
        return Math.abs(a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isApproxZero(float a, float b) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isApproxZero(float a, float b, float c) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isApproxZero(float a, float b, float c, float d) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS && Math.abs(d) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isApproxEqual(float a, float b) {
        return Math.abs(b - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isApproxEqual(float a, float b, float c) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isApproxEqual(float a, float b, float c, float d) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isApproxEqual(float a, float b, float c, float d, float e) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS && Math.abs(e - a) < EPS;
    }

    // square root
    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    // asin
    public static float asin(float a) {
        return (float) Math.asin(a);
    }

    // acos
    public static float acos(float a) {
        return (float) Math.acos(a);
    }

    // atan2 (b, a)
    public static float atan2(float a, float b) {
        return (float) Math.atan2(a, b);
    }

    // hypot
    public static float hypot(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static int clamp(int x, int min, int max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static long clamp(long x, long min, long max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static float clamp(float x, float min, float max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static double clamp(double x, double min, double max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Median of three numbers.
     */
    public static float median(float a, float b, float c) {
        return clamp(c, Math.min(a, b), Math.max(a, b));
    }

    /**
     * Median of three numbers.
     */
    public static double median(double a, double b, double c) {
        return clamp(c, Math.min(a, b), Math.max(a, b));
    }

    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(float, float, float)} but without intermediate overflow or underflow.
     */
    public static float lerpStable(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(double, double, double)} but without intermediate overflow or underflow.
     */
    public static double lerpStable(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }

    /**
     * 2D bilinear interpolation between four values (a quad).
     */
    public static float biLerp(float q00, float q10, float q01, float q11, float tx, float ty) {
        return lerp(lerp(q00, q10, tx), lerp(q01, q11, tx), ty);
    }

    /**
     * 2D bilinear interpolation between four values (a quad).
     */
    public static double biLerp(double q00, double q10, double q01, double q11, double tx, double ty) {
        return lerp(lerp(q00, q10, tx), lerp(q01, q11, tx), ty);
    }

    /**
     * 3D trilinear interpolation between eight values (a cube).
     */
    public static float triLerp(float c000, float c100, float c010, float c110,
                                float c001, float c101, float c011, float c111,
                                float tx, float ty, float tz) {
        return lerp(lerp(lerp(c000, c100, tx), lerp(c010, c110, tx), ty),
                lerp(lerp(c001, c101, tx), lerp(c011, c111, tx), ty), tz);
    }

    /**
     * 3D trilinear interpolation between eight values (a cube).
     */
    public static double triLerp(double c000, double c100, double c010, double c110,
                                 double c001, double c101, double c011, double c111,
                                 double tx, double ty, double tz) {
        return lerp(lerp(lerp(c000, c100, tx), lerp(c010, c110, tx), ty),
                lerp(lerp(c001, c101, tx), lerp(c011, c111, tx), ty), tz);
    }

    /**
     * Aligns {@code a} up to 2 (half-word).
     */
    public static int align2(int a) {
        assert a >= 0 && a <= Integer.MAX_VALUE - 8;
        return (a + 1) & -2;
    }

    /**
     * Aligns {@code a} up to 4 (word).
     */
    public static int align4(int a) {
        assert a >= 0 && a <= Integer.MAX_VALUE - 8;
        return (a + 3) & -4;
    }

    /**
     * Aligns {@code a} up to 8 (double word).
     */
    public static int align8(int a) {
        assert a >= 0 && a <= Integer.MAX_VALUE - 8;
        return (a + 7) & -8;
    }

    /**
     * Aligns {@code a} up to 2 (half-word).
     */
    public static long align2(long a) {
        assert a >= 0 && a <= Long.MAX_VALUE - 16;
        return (a + 1) & -2;
    }

    /**
     * Aligns {@code a} up to 4 (word).
     */
    public static long align4(long a) {
        assert a >= 0 && a <= Long.MAX_VALUE - 16;
        return (a + 3) & -4;
    }

    /**
     * Aligns {@code a} up to 8 (double word).
     */
    public static long align8(long a) {
        assert a >= 0 && a <= Long.MAX_VALUE - 16;
        return (a + 7) & -8;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 2. Asserts {@code a >= 0}.
     */
    public static boolean isAlign2(int a) {
        assert a >= 0;
        return (a & 1) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 4. Asserts {@code a >= 0}.
     */
    public static boolean isAlign4(int a) {
        assert a >= 0;
        return (a & 3) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 8. Asserts {@code a >= 0}.
     */
    public static boolean isAlign8(int a) {
        assert a >= 0;
        return (a & 7) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 2. Asserts {@code a >= 0}.
     */
    public static boolean isAlign2(long a) {
        assert a >= 0;
        return (a & 1) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 4. Asserts {@code a >= 0}.
     */
    public static boolean isAlign4(long a) {
        assert a >= 0;
        return (a & 3) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 8. Asserts {@code a >= 0}.
     */
    public static boolean isAlign8(long a) {
        assert a >= 0;
        return (a & 7) == 0;
    }

    /**
     * Aligns {@code a} up to a power of two.
     */
    public static int alignTo(int a, int alignment) {
        assert alignment > 0 && (alignment & (alignment - 1)) == 0;
        return (a + alignment - 1) & -alignment;
    }

    public static int alignUp(int a, int alignment) {
        assert alignment > 0;
        int r = a % alignment;
        return r == 0 ? a : a + alignment - r;
    }

    public static int alignUpPad(int a, int alignment) {
        assert alignment > 0;
        return (alignment - a % alignment) % alignment;
    }

    public static int alignDown(int a, int alignment) {
        assert alignment > 0;
        return (a / alignment) * alignment;
    }

    /**
     * Returns {@code true} if {@code a} is a power of 2. Asserts {@code a > 0}.
     */
    public static boolean isPow2(int a) {
        assert a > 0 : "undefined";
        return (a & a - 1) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a power of 2. Asserts {@code a > 0}.
     */
    public static boolean isPow2(long a) {
        assert a > 0 : "undefined";
        return (a & a - 1) == 0;
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded up to the
     * next power of 2. Asserts {@code a > 0}. NextLog2.
     */
    public static int ceilLog2(int a) {
        assert a > 0 : "undefined";
        return Integer.SIZE - Integer.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded up to the
     * next power of 2. Asserts {@code a > 0}. NextLog2.
     */
    public static int ceilLog2(long a) {
        assert a > 0 : "undefined";
        return Long.SIZE - Long.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code a}.
     * Asserts {@code a > 0 && a <= 2^30}. NextPow2.
     */
    public static int ceilPow2(int a) {
        assert a > 0 && a <= (1 << (Integer.SIZE - 2)) : "undefined";
        return 1 << -Integer.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code a}.
     * Asserts {@code a > 0 && a <= 2^62}. NextPow2.
     */
    public static long ceilPow2(long a) {
        assert a > 0 && a <= (1L << (Long.SIZE - 2)) : "undefined";
        return 1L << -Long.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded down to the
     * previous power of 2. Asserts {@code a > 0}. PrevLog2.
     */
    public static int floorLog2(int a) {
        assert a > 0 : "undefined";
        return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(a);
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded down to the
     * previous power of 2. Asserts {@code a > 0}. PrevLog2.
     */
    public static int floorLog2(long a) {
        assert a > 0 : "undefined";
        return (Long.SIZE - 1) - Long.numberOfLeadingZeros(a);
    }

    /**
     * Returns the largest power of two less than or equal to {@code a}.
     * Asserts {@code a > 0}. PrevPow2.
     */
    public static int floorPow2(int a) {
        assert a > 0 : "undefined";
        return Integer.highestOneBit(a);
    }

    /**
     * Returns the largest power of two less than or equal to {@code a}.
     * Asserts {@code a > 0}. PrevPow2.
     */
    public static long floorPow2(long a) {
        assert a > 0 : "undefined";
        return Long.highestOneBit(a);
    }

    /**
     * Returns the log2 of the provided value, were that value to be rounded up to the next power of 2.
     * Returns 0 if value <= 0:<br>
     * Never returns a negative number, even if value is NaN.
     * <pre>
     * CeilLog2((-inf..1]) -> 0
     * CeilLog2((1..2]) -> 1
     * CeilLog2((2..4]) -> 2
     * CeilLog2((4..8]) -> 3
     * CeilLog2(+inf) -> 128
     * CeilLog2(NaN) -> 0
     * </pre>
     * NextLog2.
     */
    public static int ceilLog2(float v) {
        int exp = ((Float.floatToRawIntBits(v) + (1 << 23) - 1) >> 23) - 127;
        return exp & ~(exp >> 31);
    }

    public static int ceilLog4(float v) {
        return (ceilLog2(v) + 1) >> 1;
    }

    public static int ceilLog16(float v) {
        return (ceilLog2(v) + 3) >> 2;
    }

    protected MathUtil() {
        throw new UnsupportedOperationException();
    }
}
