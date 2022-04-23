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

import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Alpha types.
 * <p>
 * Describes how to interpret the alpha component of a pixel. A pixel may
 * be opaque, or alpha, describing multiple levels of transparency.
 * <p>
 * In simple blending, alpha weights the source color and the destination
 * color to create a new color. If alpha describes a weight from zero to one:
 * <p>
 * result color = source color * alpha + destination color * (1 - alpha)
 * <p>
 * In practice alpha is encoded in two or more bits, where 1.0 equals all bits set.
 * <p>
 * RGB may have alpha included in each component value; the stored
 * value is the original RGB multiplied by alpha. Premultiplied color
 * components improve performance, but it will reduce the image quality.
 * The usual practice is to premultiply alpha in the GPU, since they were
 * converted into floating-point values.
 */
@MagicConstant(intValues = {
        AlphaType.UNKNOWN,
        AlphaType.OPAQUE,
        AlphaType.PREMULTIPLIED,
        AlphaType.STRAIGHT})
@Retention(RetentionPolicy.SOURCE)
public @interface AlphaType {

    int UNKNOWN = 0;          // uninitialized
    int OPAQUE = 1;           // pixel is opaque
    int PREMULTIPLIED = 2;    // pixel components are premultiplied by alpha
    int STRAIGHT = 3;         // pixel components are independent of alpha
}
