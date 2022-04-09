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

import java.lang.annotation.*;

/**
 * A Color may an integer ({@code AARRGGBB}) or an instance of Color class (Color4f).
 */
public class Color {

    /**
     * Denotes that the annotated element represents a packed color
     * int, {@code AARRGGBB}. If applied to an int array, every element
     * in the array represents a color integer.
     * <p>
     * Example:
     * <pre>{@code
     *  public abstract void setColor(@ColorInt int color);
     * }</pre>
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
    public @interface ColorInt {
    }

    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be \([0..255]\), but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     *
     * @param alpha Alpha component \([0..255]\) of the color
     * @param red   Red component \([0..255]\) of the color
     * @param green Green component \([0..255]\) of the color
     * @param blue  Blue component \([0..255]\) of the color
     */
    @ColorInt
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
