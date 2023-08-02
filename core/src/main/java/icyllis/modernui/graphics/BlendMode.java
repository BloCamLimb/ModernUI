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

package icyllis.modernui.graphics;

/**
 * Blends are operators that take in two colors (source, destination) and return a new color.
 * Many of these operate the same on all 4 components: red, green, blue, alpha. For these,
 * we just document what happens to one component, rather than naming each one separately.
 *
 * @since 3.0
 */
public enum BlendMode {
    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_CLEAR.png" />
     * <figcaption>Destination pixels covered by the source are cleared to 0.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = 0</p>
     * <p>C<sub>out</sub> = 0</p>
     */
    CLEAR,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_SRC.png" />
     * <figcaption>The source pixels replace the destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub></p>
     */
    SRC,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_DST.png" />
     * <figcaption>The source pixels are discarded, leaving the destination intact.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub></p>
     */
    DST,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_SRC_OVER.png" />
     * <figcaption>The source pixels are drawn over the destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    SRC_OVER,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_DST_OVER.png" />
     * <figcaption>The source pixels are drawn behind the destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> + (1 - a<sub>dst</sub>) * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    DST_OVER,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_SRC_IN.png" />
     * <figcaption>Keeps the source pixels that cover the destination pixels,
     * discards the remaining source and destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * a<sub>dst</sub></p>
     */
    SRC_IN,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_DST_IN.png" />
     * <figcaption>Keeps the destination pixels that cover source pixels,
     * discards the remaining source and destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> * a<sub>src</sub></p>
     */
    DST_IN,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_SRC_OUT.png" />
     * <figcaption>Keeps the source pixels that do not cover destination pixels.
     * Discards source pixels that cover destination pixels. Discards all
     * destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>dst</sub>) * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    SRC_OUT,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_DST_OUT.png" />
     * <figcaption>Keeps the destination pixels that are not covered by source pixels.
     * Discards destination pixels that are covered by source pixels. Discards all
     * source pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    DST_OUT,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_SRC_ATOP.png" />
     * <figcaption>Discards the source pixels that do not cover destination pixels.
     * Draws remaining source pixels over destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = a<sub>dst</sub> * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    SRC_ATOP,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_DST_ATOP.png" />
     * <figcaption>Discards the destination pixels that are not covered by source pixels.
     * Draws remaining destination pixels over source pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub></p>
     * <p>C<sub>out</sub> = a<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    DST_ATOP,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_XOR.png" />
     * <figcaption>Discards the source and destination pixels where source pixels
     * cover destination pixels. Draws remaining source pixels.</figcaption>
     * </p>
     * <p>
     * a<sub>out</sub> = (1 - a<sub>dst</sub>) * a<sub>src</sub> + (1 - a<sub>src</sub>) * a<sub>dst</sub>
     * </p>
     * <p>C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    XOR,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_PLUS.png" />
     * <figcaption>Adds the source pixels to the destination pixels and saturates
     * the result.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>src</sub> + a<sub>dst</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>src</sub> + C<sub>dst</sub>, 1))</p>
     */
    PLUS,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_MODULATE.png" />
     * <figcaption>Multiplies the source and destination pixels.</figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub></p>
     */
    MULTIPLY,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_SCREEN.png" />
     * <figcaption>
     * Adds the source and destination pixels, then subtracts the
     * source pixels multiplied by the destination.
     * </figcaption>
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - C<sub>src</sub> * C<sub>dst</sub></p>
     */
    SCREEN,

    /**
     * <p>
     * <img src="https://developer.android.com/reference/android/images/graphics/blendmode_OVERLAY.png" />
     * <figcaption>
     * Multiplies or screens the source and destination depending on the
     * destination color.
     * </figcaption>
     * </p>
     */
    OVERLAY,

    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    HARD_LIGHT,
    SOFT_LIGHT,
    DIFFERENCE,
    EXCLUSION,
    HUE,
    SATURATION,
    COLOR,
    LUMINOSITY;

    static final BlendMode[] VALUES = values();
}
