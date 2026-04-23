/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated element represents a packed color
 * long, {@code 0xAAAABBBBGGGGRRRR}. If applied to a long array,
 * every element in the array represents a color long. The color has
 * non-premultiplied alpha, 16-bit floating-point per channel, in
 * extended sRGB nonlinear color space.
 * <p>
 * Example:
 * <pre>{@code
 *  public abstract void setTextColor(@ColorLong long color);
 * }</pre>
 * <p>
 * Note: this packing format is exactly mapped to
 * {@link icyllis.modernui.graphics.Bitmap.Format#RGBA_F16}
 * on little-endian machines.
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.LOCAL_VARIABLE, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface ColorLong {
}
