/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui;

/**
 * Utility class that provides auxiliary operations.
 */
public final class MathUtil {

    public static final float PI = (float) Math.PI;
    public static final float PI_OVER_2 = (float) (Math.PI / 2);
    public static final float PI_OVER_3 = (float) (Math.PI / 3);
    public static final float PI_OVER_4 = (float) (Math.PI / 4);
    public static final float PI_OVER_6 = (float) (Math.PI / 6);
    public static final float TWO_PI = (float) (Math.PI * 2);
    public static final float THREE_PI = (float) (Math.PI * 3);
    public static final float FOUR_PI = (float) (Math.PI * 4);

    public static final float EPS = 1.0e-6f;
    public static final float DEG_TO_RAD = 0.017453292519943295f;
    public static final float RAD_TO_DEG = 1.0f / DEG_TO_RAD;

    public static final float SQRT_OF_TWO = (float) Math.sqrt(2.0);

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a) {
        return Math.abs(a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a, float b) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a, float b, float c) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean approxZero(float a, float b, float c, float d) {
        return Math.abs(a) < EPS && Math.abs(b) < EPS && Math.abs(c) < EPS && Math.abs(d) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b) {
        return Math.abs(b - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b, float c) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b, float c, float d) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean approxEqual(float a, float b, float c, float d, float e) {
        return Math.abs(b - a) < EPS && Math.abs(c - a) < EPS && Math.abs(d - a) < EPS && Math.abs(e - a) < EPS;
    }

    /**
     * If v compares less than lo, returns lo; otherwise if hi compares less than v,
     * returns hi; otherwise returns v.
     *
     * @return v clamped between lo and hi, inclusively.
     */
    public static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    /**
     * If v compares less than lo, returns lo; otherwise if hi compares less than v,
     * returns hi; otherwise returns v.
     *
     * @return v clamped between lo and hi, inclusively.
     */
    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    /**
     * @return the min component of vec4
     */
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * @return the max component of vec4
     */
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * @return linear interpolation
     */
    public static float lerp(float f, float st, float en) {
        return st + f * (en - st);
    }

    private MathUtil() {
    }
}
