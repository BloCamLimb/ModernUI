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

import org.jetbrains.annotations.ApiStatus;

public class DisplayMetrics {

    // most desktop monitors have a physical DPI of 81;
    // most laptop monitors have a physical DPI of 141;
    // most phone screens have a physical DPI in 384..460;

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
     * Intermediate density for screens that sit between {@link #DENSITY_MEDIUM} (160dpi) and
     * {@link #DENSITY_HIGH} (240dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_HIGH} assets for them.
     */
    public static final int DENSITY_108 = 108;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_MEDIUM} (160dpi) and
     * {@link #DENSITY_HIGH} (240dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_HIGH} assets for them.
     */
    public static final int DENSITY_120 = 120;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_MEDIUM} (160dpi) and
     * {@link #DENSITY_HIGH} (240dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_HIGH} assets for them.
     */
    public static final int DENSITY_132 = 132;

    /**
     * Standard quantized DPI for high-density screens.
     */
    public static final int DENSITY_HIGH = 144;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_HIGH} (240dpi) and
     * {@link #DENSITY_XHIGH} (320dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_XHIGH} assets for them.
     */
    public static final int DENSITY_156 = 156;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_HIGH} (240dpi) and
     * {@link #DENSITY_XHIGH} (320dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_XHIGH} assets for them.
     */
    public static final int DENSITY_168 = 168;

    /**
     * Intermediate density for screens that sit between {@link #DENSITY_HIGH} (240dpi) and
     * {@link #DENSITY_XHIGH} (320dpi). This is not a density that applications should target,
     * instead relying on the system to scale their {@link #DENSITY_XHIGH} assets for them.
     */
    public static final int DENSITY_180 = 180;

    /**
     * Standard quantized DPI for extra-high-density screens.
     */
    public static final int DENSITY_XHIGH = 192;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (320 dpi) and {@link #DENSITY_XXHIGH} (480 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_204 = 204;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (320 dpi) and {@link #DENSITY_XXHIGH} (480 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_216 = 216;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (320 dpi) and {@link #DENSITY_XXHIGH} (480 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_240 = 240;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (320 dpi) and {@link #DENSITY_XXHIGH} (480 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_252 = 252;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (320 dpi) and {@link #DENSITY_XXHIGH} (480 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_264 = 264;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XHIGH} (320 dpi) and {@link #DENSITY_XXHIGH} (480 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXHIGH} assets for them.
     */
    public static final int DENSITY_270 = 270;

    /**
     * Standard quantized DPI for extra-extra-high-density screens.
     */
    public static final int DENSITY_XXHIGH = 288;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XXHIGH} (480 dpi) and {@link #DENSITY_XXXHIGH} (640 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXXHIGH} assets for them.
     */
    public static final int DENSITY_336 = 336;

    /**
     * Intermediate density for screens that sit somewhere between
     * {@link #DENSITY_XXHIGH} (480 dpi) and {@link #DENSITY_XXXHIGH} (640 dpi).
     * This is not a density that applications should target, instead relying
     * on the system to scale their {@link #DENSITY_XXXHIGH} assets for them.
     */
    public static final int DENSITY_360 = 360;

    /**
     * Standard quantized DPI for extra-extra-extra-high-density screens.  Applications
     * should not generally worry about this density; relying on XHIGH graphics
     * being scaled up to it should be sufficient for almost all cases.  A typical
     * use of this density would be 4K television screens -- 3840x2160, which
     * is 2x a traditional HD 1920x1080 screen which runs at DENSITY_XHIGH.
     */
    public static final int DENSITY_XXXHIGH = 384;

    /**
     * The reference density used throughout the system.
     */
    // default DPI is 72 for compatibility reasons
    public static final int DENSITY_DEFAULT = DENSITY_LOW;

    /**
     * Scaling factor to convert a density in DPI units to the density scale.
     */
    @ApiStatus.Internal
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
     * The logical density of the display.  This is a scaling factor for the
     * Density Independent Pixel unit, where one DIP is one pixel on an
     * approximately 160 dpi screen (for example a 240x320, 1.5"x2" screen),
     * providing the baseline of the system's display. Thus on a 160dpi screen
     * this density value will be 1; on a 120 dpi screen it would be .75; etc.
     *
     * <p>This value does not exactly follow the real screen size (as given by
     * {@link #xdpi} and {@link #ydpi}), but rather is used to scale the size of
     * the overall UI in steps based on gross changes in the display dpi.  For
     * example, a 240x320 screen will have a density of 1 even if its width is
     * 1.8", 1.3", etc. However, if the screen resolution is increased to
     * 320x480 but the screen size remained 1.5"x2" then the density would be
     * increased (probably to 1.5).
     *
     * @see #DENSITY_DEFAULT
     */
    public float density;
    /**
     * The screen density expressed as dots-per-inch.  May be either
     * {@link #DENSITY_LOW}, {@link #DENSITY_MEDIUM}, or {@link #DENSITY_HIGH}.
     */
    public int densityDpi;
    /**
     * A scaling factor for fonts displayed on the display.  This is the same
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
