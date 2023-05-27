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

package icyllis.modernui.util;

/**
 * A structure describing general information about a display, such as its
 * size, density, and font scaling.
 */
public class DisplayMetrics {

    // most desktop monitors have a physical PPI of 80 to 282;

    /**
     * Standard quantized DPI for low-density screens.
     */
    public static final int DENSITY_LOW = 72;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_LOW} (72dpi) and
     * {@link #DENSITY_MEDIUM} (96dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_MEDIUM} assets for them.
     */
    public static final int DENSITY_84 = 84;

    /**
     * Standard quantized DPI for medium-density screens.
     */
    public static final int DENSITY_MEDIUM = 96;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_MEDIUM} (96dpi) and
     * {@link #DENSITY_HIGH} (144dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_HIGH} assets for them.
     */
    public static final int DENSITY_108 = 108;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_MEDIUM} (96dpi) and
     * {@link #DENSITY_HIGH} (144dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_HIGH} assets for them.
     */
    public static final int DENSITY_120 = 120;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_MEDIUM} (96dpi) and
     * {@link #DENSITY_HIGH} (144dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_HIGH} assets for them.
     */
    public static final int DENSITY_132 = 132;

    /**
     * Standard quantized DPI for high-density screens.
     */
    public static final int DENSITY_HIGH = 144;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_HIGH} (144dpi) and
     * {@link #DENSITY_XHIGH} (192dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_XHIGH} assets for them.
     */
    public static final int DENSITY_156 = 156;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_HIGH} (144dpi) and
     * {@link #DENSITY_XHIGH} (192dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_XHIGH} assets for them.
     */
    public static final int DENSITY_168 = 168;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_HIGH} (144dpi) and
     * {@link #DENSITY_XHIGH} (192dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_XHIGH} assets for them.
     */
    public static final int DENSITY_180 = 180;

    /**
     * Standard quantized DPI for extra-high-density screens.
     */
    public static final int DENSITY_XHIGH = 192;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_204 = 204;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_216 = 216;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_228 = 228;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_240 = 240;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_252 = 252;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_264 = 264;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (192 dpi) and {@link #DENSITY_XXHIGH} (288 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_276 = 276;

    /**
     * Standard quantized DPI for extra-extra-high-density screens.
     */
    public static final int DENSITY_XXHIGH = 288;

    /**
     * The reference density used throughout the system.
     */
    public static final int DENSITY_DEFAULT = DENSITY_LOW;

    /**
     * Scaling factor to convert a density in DPI units to the density scale.
     */
    public static final float DENSITY_DEFAULT_SCALE = 1.0f / DENSITY_DEFAULT;

    /**
     * The absolute width of the available display size in pixels.
     */
    public int widthPixels;
    /**
     * The absolute height of the available display size in pixels.
     */
    public int heightPixels;
    /**
     * The logical density of the display.<br>This is a scaling factor for the
     * Density Independent Pixel unit, where one DIP is one pixel on an
     * approximately 72 dpi screen, providing the baseline of the system's
     * display. Thus on a 72 dpi screen this density value will be 1; on a
     * 96 dpi screen it would be 1.33; etc.
     *
     * <p>This value does not exactly follow the real screen size (as given by
     * {@link #xdpi} and {@link #ydpi}), but rather is used to scale the size of
     * the overall UI in steps based on gross changes in the display dpi.
     *
     * @see #DENSITY_DEFAULT
     */
    public float density;
    /**
     * The screen density expressed as dots-per-inch.<br>May be either
     * {@link #DENSITY_LOW}, {@link #DENSITY_MEDIUM}, or {@link #DENSITY_HIGH}.
     */
    public int densityDpi;
    /**
     * A scaling factor for fonts displayed on the display.<br>This is the same
     * as {@link #density}, except that it may be adjusted in smaller
     * increments at runtime based on a user preference for the font size.
     */
    public float scaledDensity;
    /**
     * The exact physical pixels per inch of the screen in the X dimension.
     */
    public float xdpi;
    /**
     * The exact physical pixels per inch of the screen in the Y dimension.
     */
    public float ydpi;

    public DisplayMetrics() {
    }

    public void setTo(DisplayMetrics o) {
        if (this == o) {
            return;
        }
        widthPixels = o.widthPixels;
        heightPixels = o.heightPixels;
        density = o.density;
        densityDpi = o.densityDpi;
        scaledDensity = o.scaledDensity;
        xdpi = o.xdpi;
        ydpi = o.ydpi;
    }

    public void setToDefaults() {
        widthPixels = 0;
        heightPixels = 0;
        density = 1.0f;
        densityDpi = DENSITY_DEFAULT;
        scaledDensity = density;
        xdpi = DENSITY_DEFAULT;
        ydpi = DENSITY_DEFAULT;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisplayMetrics && equals((DisplayMetrics) o);
    }

    /**
     * Returns true if these display metrics equal the other display metrics.
     *
     * @param other The display metrics with which to compare.
     * @return True if the display metrics are equal.
     */
    public boolean equals(DisplayMetrics other) {
        return equalsPhysical(other)
                && scaledDensity == other.scaledDensity;
    }

    /**
     * Returns true if the physical aspects of the two display metrics
     * are equal.  This ignores the scaled density, which is a logical
     * attribute based on the current desired font size.
     *
     * @param other The display metrics with which to compare.
     * @return True if the display metrics are equal.
     */
    public boolean equalsPhysical(DisplayMetrics other) {
        return other != null
                && widthPixels == other.widthPixels
                && heightPixels == other.heightPixels
                && density == other.density
                && densityDpi == other.densityDpi
                && xdpi == other.xdpi
                && ydpi == other.ydpi;
    }

    @Override
    public int hashCode() {
        return widthPixels * heightPixels * densityDpi;
    }

    @Override
    public String toString() {
        return "DisplayMetrics{density=" + density + ", width=" + widthPixels +
                ", height=" + heightPixels + ", scaledDensity=" + scaledDensity +
                ", xdpi=" + xdpi + ", ydpi=" + ydpi + "}";
    }
}
