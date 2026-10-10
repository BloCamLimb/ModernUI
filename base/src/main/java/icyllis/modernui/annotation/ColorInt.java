/*
 * ModernUI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.annotation;

import java.lang.annotation.*;

/**
 * Denotes that the annotated element represents a packed color
 * int, {@code 0xAARRGGBB}. If applied to an int array, every element
 * in the array represents a color integer. The color has
 * non-premultiplied alpha, 8-bit unsigned per channel, in sRGB color space.
 * <p>
 * Example:
 * <pre>{@code
 *  public abstract void setTextColor(@ColorInt int color);
 * }</pre>
 * <p>
 * Note: this packing format is exactly mapped to
 * {@link icyllis.modernui.graphics.Bitmap.Format#BGRA_8888_PACK32}.
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.LOCAL_VARIABLE, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface ColorInt {
}
