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

package icyllis.arcui.core;

import java.lang.annotation.*;

/**
 * Denotes that the annotated element represents a packed 32-bit ARGB color value,
 * un-premultiplied, {@code AARRGGBB}. If applied to an int array, every element
 * in the array represents a color integer. This matches the format of
 * {@link ImageInfo#COLOR_BGRA_8888}. Color ints are used to specify colors in paint
 * and in gradients.
 * <p>
 * Color that is premultiplied has the same component values as color that is
 * un-premultiplied if alpha is 255, fully opaque, although it may have the
 * component values in a different order.
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
