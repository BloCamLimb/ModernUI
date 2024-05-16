/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.core;

import javax.annotation.Nonnull;

/**
 * Blend modes, all the blend equations apply to premultiplied colors.
 * Source color is blend color or paint color, destination color is
 * base color or canvas color.
 * <p>
 * Non-clamped blend modes &le; {@link #MODULATE}, plus {@link #SCREEN} are
 * Porter-Duff blend modes. They can be directly implemented by GPU hardware.
 * The others are advanced blend equations, which need to be implemented in
 * fragment shaders, or require an extension.
 */
public enum BlendMode implements Blender {
    /**
     * <p>
     * Destination pixels covered by the source are cleared to 0.
     * </p>
     * <p>a<sub>out</sub> = 0</p>
     * <p>C<sub>out</sub> = 0</p>
     */
    CLEAR,

    /**
     * <p>
     * The source pixels replace the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub></p>
     */
    SRC,

    /**
     * <p>
     * The source pixels are discarded, leaving the destination intact.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub></p>
     */
    DST,

    /**
     * <p>
     * The source pixels are drawn over the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    SRC_OVER,

    /**
     * <p>
     * The source pixels are drawn behind the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> + (1 - a<sub>dst</sub>) * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    DST_OVER,

    /**
     * <p>
     * Keeps the source pixels that cover the destination pixels,
     * discards the remaining source and destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * a<sub>dst</sub></p>
     */
    SRC_IN,

    /**
     * <p>
     * Keeps the destination pixels that cover source pixels,
     * discards the remaining source and destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> * a<sub>src</sub></p>
     */
    DST_IN,

    /**
     * <p>
     * Keeps the source pixels that do not cover destination pixels.
     * Discards source pixels that cover destination pixels. Discards all
     * destination pixels.
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>dst</sub>) * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    SRC_OUT,

    /**
     * <p>
     * Keeps the destination pixels that are not covered by source pixels.
     * Discards destination pixels that are covered by source pixels. Discards all
     * source pixels.
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    DST_OUT,

    /**
     * <p>
     * Discards the source pixels that do not cover destination pixels.
     * Draws remaining source pixels over destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = a<sub>dst</sub> * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    SRC_ATOP,

    /**
     * <p>
     * Discards the destination pixels that are not covered by source pixels.
     * Draws remaining destination pixels over source pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub></p>
     * <p>C<sub>out</sub> = a<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    DST_ATOP,

    /**
     * <p>
     * Discards the source and destination pixels where source pixels
     * cover destination pixels. Draws remaining source pixels.
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>dst</sub>) * a<sub>src</sub> + (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    XOR,

    /**
     * <p>
     * Adds the source pixels to the destination pixels, without alpha blending.
     * For floating-point textures, color components may be greater than 1.0.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub></p>
     *
     * @see #PLUS_CLAMPED
     * @see #LINEAR_DODGE
     */
    PLUS,

    /**
     * <p>
     * Adds the source pixels to the destination pixels and saturates
     * the result, without alpha blending. For unsigned fixed-point textures,
     * this is the same as {@link #PLUS}. This is a special blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>src</sub> + a<sub>dst</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>src</sub> + C<sub>dst</sub>, 1))</p>
     *
     * @see #PLUS
     * @see #LINEAR_DODGE
     */
    PLUS_CLAMPED,

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels, without alpha blending.
     * For floating-point textures, color components may be less than 0.0.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> - a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> - C<sub>src</sub></p>
     *
     * @see #MINUS_CLAMPED
     * @see #SUBTRACT
     */
    MINUS,

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels and saturates
     * the result, without alpha blending. For unsigned fixed-point textures,
     * this is the same as {@link #MINUS}. This is a special blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>dst</sub> - a<sub>src</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>dst</sub> - C<sub>src</sub>, 1))</p>
     *
     * @see #MINUS
     * @see #SUBTRACT
     */
    MINUS_CLAMPED,

    /**
     * <p>
     * Multiplies the source and destination pixels, without alpha blending.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub></p>
     *
     * @see #MULTIPLY
     */
    MODULATE,

    /**
     * <p>
     * Multiplies the source and destination pixels.
     * This is {@link #MODULATE} with alpha blending. If both the source and
     * destination are opaque, then this is the same as {@link #MODULATE}.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 -
     * a<sub>src</sub>) * C<sub>dst</sub></p>
     *
     * @see #MODULATE
     */
    MULTIPLY,

    /**
     * <p>
     * Adds the source and destination pixels, then subtracts the
     * source pixels multiplied by the destination.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - C<sub>src</sub> * C<sub>dst</sub></p>
     */
    SCREEN,

    /**
     * <p>
     * Multiplies or screens the source and destination depending on the
     * destination color. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> &le; 0.5 * a<sub>dst</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> - 2 * (a<sub>src</sub> - C<sub>src</sub>) *
     * (a<sub>dst</sub> - C<sub>dst</sub>) + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) *
     * C<sub>dst</sub>
     * </p>
     */
    OVERLAY,

    /**
     * <p>
     * Retains the smallest component of the source and
     * destination pixels. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>
     * C<sub>out</sub> = min(C<sub>src</sub> / a<sub>src</sub>, C<sub>dst</sub> / a<sub>dst</sub>) *
     * a<sub>src</sub> * a<sub>dst</sub> +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    DARKEN,

    /**
     * <p>
     * Retains the largest component of the source and
     * destination pixel. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>
     * C<sub>out</sub> = max(C<sub>src</sub> / a<sub>src</sub>, C<sub>dst</sub> / a<sub>dst</sub>) *
     * a<sub>src</sub> * a<sub>dst</sub> +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    LIGHTEN,

    /**
     * <p>
     * Makes destination brighter to reflect source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> &le; 0:<br>
     * C<sub>out</sub> = C<sub>src</sub> * (1 - a<sub>dst</sub>)
     * </p>
     *
     * <p>if C<sub>src</sub> &ge; a<sub>src</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * min(a<sub>dst</sub>, C<sub>dst</sub> * a<sub>src</sub> /
     * (a<sub>src</sub> - C<sub>src</sub>)) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    COLOR_DODGE,

    /**
     * <p>
     * Makes destination darker to reflect source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> &ge; a<sub>dst</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if C<sub>src</sub> &le; 0:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * (1 - a<sub>src</sub>)
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * (a<sub>dst</sub> - min(a<sub>dst</sub>, (a<sub>dst</sub> -
     * C<sub>dst</sub>) * a<sub>src</sub> / C<sub>dst</sub>)) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    COLOR_BURN,

    /**
     * <p>
     * Makes destination lighter or darker, depending on source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> &le; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> - 2 * (a<sub>src</sub> - C<sub>src</sub>) *
     * (a<sub>dst</sub> - C<sub>dst</sub>) + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) *
     * C<sub>dst</sub>
     * </p>
     */
    HARD_LIGHT,

    /**
     * <p>
     * Makes destination lighter or darker, depending on source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> &le; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * C<sub>dst</sub> * (a<sub>src</sub> - 2 * C<sub>src</sub>) /
     * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> + C<sub>dst</sub> *
     * (2 * C<sub>src</sub> + 1 - a<sub>src</sub>)
     * </p>
     *
     * <p>if C<sub>dst</sub> &le; 0.25 * a<sub>dst</sub>:<br>
     * C<sub>out</sub> = (a<sub>dst</sub> * a<sub>dst</sub> * (C<sub>src</sub> + C<sub>dst</sub> *
     * (6 * C<sub>src</sub> - 3 * a<sub>src</sub> + 1)) + 12 * a<sub>dst</sub> * C<sub>dst</sub> * C<sub>dst</sub> *
     * (a<sub>src</sub> - 2 * C<sub>src</sub>) - 16 * C<sub>dst</sub> * C<sub>dst</sub> * C<sub>dst</sub> *
     * (a<sub>src</sub> - 2 * C<sub>src</sub>) - a<sub>dst</sub> * a<sub>dst</sub> * a<sub>dst</sub> *
     * C<sub>src</sub>) / a<sub>dst</sub> * a<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * (a<sub>src</sub> - 2 * C<sub>src</sub> + 1) + C<sub>src</sub> *
     * (1 - a<sub>dst</sub>) - sqrt(C<sub>dst</sub> * a<sub>dst</sub>) * (a<sub>src</sub> - 2 * C<sub>src</sub>)
     * </p>
     */
    SOFT_LIGHT,

    /**
     * <p>
     * Subtracts darker from lighter with higher contrast.
     * This is an advanced blend equation.
     * </p>
     * <p>
     * a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     * <p>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - 2 * min(C<sub>src</sub> *
     * a<sub>dst</sub>, C<sub>dst</sub> * a<sub>src</sub>)
     * </p>
     */
    DIFFERENCE,

    /**
     * <p>
     * Subtracts darker from lighter with lower contrast.
     * This is an advanced blend equation.
     * </p>
     * <p>
     * a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     * <p>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - 2 * C<sub>src</sub> * C<sub>dst</sub>
     * </p>
     */
    EXCLUSION,

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels and saturates
     * the result, with alpha blending. If both the source and destination are
     * opaque, then this is the same as {@link #MINUS_CLAMPED}. This is a custom
     * blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> / a<sub>dst</sub> - C<sub>src</sub> / a<sub>src</sub> &ge; 0:<br>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - 2 * C<sub>src</sub> * a<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * @see #MINUS
     * @see #MINUS_CLAMPED
     */
    SUBTRACT,

    /**
     * <p>
     * Divides the destination pixels by the source pixels and saturates the result.
     * For negative and NaN values, the result color is black (XOR). This is a custom
     * blend equation.
     * </p>
     * <p>
     * a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     * <p>
     * C<sub>out</sub> = pin((C<sub>dst</sub> * a<sub>src</sub>) / (C<sub>src</sub> * a<sub>dst</sub>), 0, 1) *
     * a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    DIVIDE,

    /**
     * <p>
     * Lightens the destination pixels to reflect the source pixels while also increasing contrast.
     * This is {@link #PLUS_CLAMPED} with alpha blending. If both the source and
     * destination are opaque, then this is the same as {@link #PLUS_CLAMPED}.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &le; 1:<br>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * @see #PLUS
     * @see #PLUS_CLAMPED
     */
    LINEAR_DODGE,

    /**
     * <p>
     * Darkens the destination pixels to reflect the source pixels while also increasing contrast.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &gt; 1:<br>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    LINEAR_BURN,

    /**
     * <p>
     * Burns or dodges colors by changing contrast, depending on the blend color.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> &le; 0:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * (1 - a<sub>src</sub>)
     * </p>
     *
     * <p>if C<sub>src</sub> &lt; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * (a<sub>dst</sub> - min(a<sub>dst</sub>, (a<sub>dst</sub> -
     * C<sub>dst</sub>) * a<sub>src</sub> / (2 * C<sub>dst</sub>))) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if C<sub>src</sub> &ge; a<sub>src</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * min(a<sub>dst</sub>, C<sub>dst</sub> * a<sub>src</sub> /
     * (2 * (a<sub>src</sub> - C<sub>src</sub>))) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    VIVID_LIGHT,

    /**
     * <p>
     * Burns or dodges colors by changing brightness, depending on the blend color.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &gt; 2:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &le; 1:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * a<sub>dst</sub> + C<sub>dst</sub> * a<sub>src</sub> -
     * a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    LINEAR_LIGHT,

    /**
     * <p>
     * Conditionally replaces destination pixels with source pixels depending on the brightness of the source pixels.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> - C<sub>dst</sub> / a<sub>dst</sub> &gt; 1 &amp;&amp;
     * C<sub>src</sub> &lt; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> - C<sub>dst</sub> / a<sub>dst</sub> &gt; 1 &amp;&amp;
     * C<sub>src</sub> &ge; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub> +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> - C<sub>dst</sub> / a<sub>dst</sub> &le; 1 &amp;&amp;
     * C<sub>src</sub> * a<sub>dst</sub> &lt; 0.5 * C<sub>dst</sub> * a<sub>src</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * a<sub>src</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    PIN_LIGHT,

    /**
     * <p>
     * Adds two images together, setting each color channel value to either 0 or 1.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &lt; 1:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    HARD_MIX,

    /**
     * <p>
     * Similar to {@link #DARKEN}, but darkens on the composite channel, instead of
     * separate RGB color channels. It compares the source and destination color,
     * and keep the one with lower luminosity between the two.
     * </p>
     * <p>if lum(C<sub>src</sub>) &le; lum(C<sub>dst</sub>):<br>
     * Equivalent to {@link #SRC_OVER}
     * </p>
     *
     * <p>otherwise:<br>
     * Equivalent to {@link #DST_OVER}
     * </p>
     */
    DARKER_COLOR,

    /**
     * <p>
     * Similar to {@link #LIGHTEN}, but lightens on the composite channel, instead of
     * separate RGB color channels. It compares the source and destination color,
     * and keep the one with higher luminosity between the two.
     * </p>
     * <p>if lum(C<sub>src</sub>) &ge; lum(C<sub>dst</sub>):<br>
     * Equivalent to {@link #SRC_OVER}
     * </p>
     *
     * <p>otherwise:<br>
     * Equivalent to {@link #DST_OVER}
     * </p>
     */
    LIGHTER_COLOR,

    /**
     * <p>
     * Replaces hue of destination with hue of source, leaving saturation
     * and luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    HUE,

    /**
     * <p>
     * Replaces saturation of destination saturation hue of source, leaving hue and
     * luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    SATURATION,

    /**
     * <p>
     * Replaces hue and saturation of destination with hue and saturation of source,
     * leaving luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    COLOR,

    /**
     * <p>
     * Replaces luminosity of destination with luminosity of source, leaving hue and
     * saturation unchanged. This is an advanced blend equation.
     * </p>
     */
    LUMINOSITY;

    /**
     * Name alias of {@link #LINEAR_DODGE}.
     */
    public static final BlendMode ADD = LINEAR_DODGE;

    private static final BlendMode[] VALUES = values();
    /**
     * Number of blend modes, runtime constant.
     */
    public static final int COUNT = VALUES.length;

    /**
     * Returns the value at the given index.
     * The return value does not guarantee binary compatibility.
     *
     * @param index the {@link BlendMode#ordinal()}
     * @return the blend mode
     */
    @Nonnull
    public static BlendMode modeAt(int index) {
        return VALUES[index];
    }

    @Override
    public BlendMode asBlendMode() {
        return this;
    }

    /**
     * 25 blend modes after {@link #MULTIPLY} are advanced.
     * <p>
     * Advanced blend modes are not directly supported by 3D API, they will be implemented
     * with custom fragment shader code and DST read (via texture barrier, input attachment,
     * or image copy). Advanced blend modes should be avoided for operations other than
     * layer compositing.
     */
    public boolean isAdvanced() {
        return ordinal() >= MULTIPLY.ordinal();
    }

    /**
     * Applies this blend mode with RGBA colors. src, dst and out store premultiplied
     * R,G,B,A components from index 0 to 3. src, dst and out can be the same pointer.
     * src and dst are read-only, out may be written multiple times.
     * <p>
     * This method is mainly used for blending solid colors without using shader code.
     * <p>
     * This method is final to avoid generating too many anonymous classes.
     */
    public final void apply(@Size(4) float[] src,
                            @Size(4) float[] dst,
                            @Size(4) float[] out) {
        //@formatter:off
        switch (this) {
            case CLEAR -> {
                for (int i = 0; i < 4; i++) {
                    out[i] = 0;
                }
            }
            case SRC            -> System.arraycopy(src, 0, out, 0, 4); // benchmark result
            case DST            -> System.arraycopy(dst, 0, out, 0, 4); // benchmark result
            case SRC_OVER       -> blend_src_over       (src,dst,out);
            case DST_OVER       -> blend_dst_over       (src,dst,out);
            case SRC_IN         -> blend_src_in         (src,dst,out);
            case DST_IN         -> blend_dst_in         (src,dst,out);
            case SRC_OUT        -> blend_src_out        (src,dst,out);
            case DST_OUT        -> blend_dst_out        (src,dst,out);
            case SRC_ATOP       -> blend_src_atop       (src,dst,out);
            case DST_ATOP       -> blend_dst_atop       (src,dst,out);
            case XOR            -> blend_xor            (src,dst,out);
            case PLUS           -> blend_plus           (src,dst,out);
            case PLUS_CLAMPED   -> blend_plus_clamped   (src,dst,out);
            case MINUS          -> blend_minus          (src,dst,out);
            case MINUS_CLAMPED  -> blend_minus_clamped  (src,dst,out);
            case MODULATE       -> blend_modulate       (src,dst,out);
            case MULTIPLY       -> blend_multiply       (src,dst,out);
            case SCREEN         -> blend_screen         (src,dst,out);
            case OVERLAY        -> blend_overlay        (src,dst,out);
            case DARKEN         -> blend_darken         (src,dst,out);
            case LIGHTEN        -> blend_lighten        (src,dst,out);
            case COLOR_DODGE    -> blend_color_dodge    (src,dst,out);
            case COLOR_BURN     -> blend_color_burn     (src,dst,out);
            case HARD_LIGHT     -> blend_hard_light     (src,dst,out);
            case SOFT_LIGHT     -> blend_soft_light     (src,dst,out);
            case DIFFERENCE     -> blend_difference     (src,dst,out);
            case EXCLUSION      -> blend_exclusion      (src,dst,out);
            case SUBTRACT       -> blend_subtract       (src,dst,out);
            case DIVIDE         -> blend_divide         (src,dst,out);
            case LINEAR_DODGE   -> blend_linear_dodge   (src,dst,out);
            case LINEAR_BURN    -> blend_linear_burn    (src,dst,out);
            case VIVID_LIGHT    -> blend_vivid_light    (src,dst,out);
            case LINEAR_LIGHT   -> blend_linear_light   (src,dst,out);
            case PIN_LIGHT      -> blend_pin_light      (src,dst,out);
            case HARD_MIX       -> blend_hard_mix       (src,dst,out);
            case DARKER_COLOR   -> blend_darker_color   (src,dst,out);
            case LIGHTER_COLOR  -> blend_lighter_color  (src,dst,out);
            case HUE            -> blend_hue            (src,dst,out);
            case SATURATION     -> blend_saturation     (src,dst,out);
            case COLOR          -> blend_color          (src,dst,out);
            case LUMINOSITY     -> blend_luminosity     (src,dst,out);
        }
        //@formatter:on
    }

    public static void blend_src_over(float[] src, float[] dst, float[] out) {
        float df = 1 - src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] + dst[i] * df;
        }
    }

    public static void blend_dst_over(float[] src, float[] dst, float[] out) {
        float sf = 1 - dst[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * sf + dst[i];
        }
    }

    public static void blend_src_in(float[] src, float[] dst, float[] out) {
        float sf = dst[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * sf;
        }
    }

    public static void blend_dst_in(float[] src, float[] dst, float[] out) {
        float df = src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = dst[i] * df;
        }
    }

    public static void blend_src_out(float[] src, float[] dst, float[] out) {
        float sf = 1 - dst[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * sf;
        }
    }

    public static void blend_dst_out(float[] src, float[] dst, float[] out) {
        float df = 1 - src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = dst[i] * df;
        }
    }

    public static void blend_src_atop(float[] src, float[] dst, float[] out) {
        float sf = dst[3];
        float df = 1 - src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * sf + dst[i] * df;
        }
    }

    public static void blend_dst_atop(float[] src, float[] dst, float[] out) {
        float sf = 1 - dst[3];
        float df = src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * sf + dst[i] * df;
        }
    }

    public static void blend_xor(float[] src, float[] dst, float[] out) {
        float sf = 1 - dst[3];
        float df = 1 - src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * sf + dst[i] * df;
        }
    }

    /**
     * <p>
     * Adds the source pixels to the destination pixels, without alpha blending.
     * For floating-point textures, color components may be greater than 1.0.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub></p>
     */
    public static void blend_plus(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] + dst[i];
        }
    }

    /**
     * <p>
     * Adds the source pixels to the destination pixels and saturates
     * the result, without alpha blending. For unsigned fixed-point textures,
     * this is the same as {@link #PLUS}. This is a special blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>src</sub> + a<sub>dst</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>src</sub> + C<sub>dst</sub>, 1))</p>
     */
    public static void blend_plus_clamped(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = Math.min(src[i] + dst[i], 1);
        }
    }

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels, without alpha blending.
     * For floating-point textures, color components may be less than 0.0.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> - a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> - C<sub>src</sub></p>
     */
    public static void blend_minus(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = dst[i] - src[i];
        }
    }

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels and saturates
     * the result, without alpha blending. For unsigned fixed-point textures,
     * this is the same as {@link #MINUS}. This is a special blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>dst</sub> - a<sub>src</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>dst</sub> - C<sub>src</sub>, 1))</p>
     */
    public static void blend_minus_clamped(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = Math.max(dst[i] - src[i], 0);
        }
    }

    /**
     * <p>
     * Multiplies the source and destination pixels, without alpha blending.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub></p>
     */
    public static void blend_modulate(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * dst[i];
        }
    }

    /**
     * <p>
     * Multiplies the source and destination pixels.
     * This is {@link #MODULATE} with alpha blending. If both the source and
     * destination are opaque, then this is the same as {@link #MODULATE}.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 -
     * a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    public static void blend_multiply(float[] src, float[] dst, float[] out) {
        float sf = 1 - dst[3];
        float df = 1 - src[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] * dst[i] + src[i] * sf + dst[i] * df;
        }
    }

    /**
     * <p>
     * Adds the source and destination pixels, then subtracts the
     * source pixels multiplied by the destination.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - C<sub>src</sub> * C<sub>dst</sub></p>
     */
    public static void blend_screen(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] + dst[i] - src[i] * dst[i];
        }
    }

    /**
     * <p>
     * Multiplies or screens the source and destination depending on the
     * destination color. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> &le; 0.5 * a<sub>dst</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> - 2 * (a<sub>src</sub> - C<sub>src</sub>) *
     * (a<sub>dst</sub> - C<sub>dst</sub>) + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) *
     * C<sub>dst</sub>
     * </p>
     */
    public static void blend_overlay(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = src[i] * (1 - da) + dst[i] * (1 - sa) +
                    (2 * dst[i] <= da
                            ? 2 * src[i] * dst[i]
                            : sa * da - 2 * (sa - src[i]) * (da - dst[i]));
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Retains the smallest component of the source and
     * destination pixels. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>
     * C<sub>out</sub> = min(C<sub>src</sub> / a<sub>src</sub>, C<sub>dst</sub> / a<sub>dst</sub>) *
     * a<sub>src</sub> * a<sub>dst</sub> +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_darken(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] + dst[i] - Math.max(src[i] * da, dst[i] * sa);
        }
    }

    /**
     * <p>
     * Retains the largest component of the source and
     * destination pixel. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>
     * C<sub>out</sub> = max(C<sub>src</sub> / a<sub>src</sub>, C<sub>dst</sub> / a<sub>dst</sub>) *
     * a<sub>src</sub> * a<sub>dst</sub> +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_lighten(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 4; i++) {
            out[i] = src[i] + dst[i] - Math.min(src[i] * da, dst[i] * sa);
        }
    }

    /**
     * <p>
     * Makes destination brighter to reflect source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> &le; 0:<br>
     * C<sub>out</sub> = C<sub>src</sub> * (1 - a<sub>dst</sub>)
     * </p>
     *
     * <p>if C<sub>src</sub> &ge; a<sub>src</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * min(a<sub>dst</sub>, C<sub>dst</sub> * a<sub>src</sub> /
     * (a<sub>src</sub> - C<sub>src</sub>)) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_color_dodge(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float s = src[i];
            float d = dst[i];
            if (d <= 0) {
                out[i] = s * (1 - da);
            } else if (s >= sa) {
                out[i] = sa * da + s * (1 - da) + d * (1 - sa);
            } else {
                out[i] = sa * Math.min(da, d * sa / (sa - s)) + s * (1 - da) + d * (1 - sa);
            }
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Makes destination darker to reflect source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> &ge; a<sub>dst</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if C<sub>src</sub> &le; 0:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * (1 - a<sub>src</sub>)
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * (a<sub>dst</sub> - min(a<sub>dst</sub>, (a<sub>dst</sub> -
     * C<sub>dst</sub>) * a<sub>src</sub> / C<sub>dst</sub>)) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_color_burn(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float s = src[i];
            float d = dst[i];
            if (d >= da) {
                out[i] = sa * da + s * (1 - da) + d * (1 - sa);
            } else if (s <= 0) {
                out[i] = d * (1 - sa);
            } else {
                out[i] = sa * Math.max(0, da - (da - d) * sa / s) + s * (1 - da) + d * (1 - sa);
            }
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Makes destination lighter or darker, depending on source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> &le; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> - 2 * (a<sub>src</sub> - C<sub>src</sub>) *
     * (a<sub>dst</sub> - C<sub>dst</sub>) + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) *
     * C<sub>dst</sub>
     * </p>
     */
    public static void blend_hard_light(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = src[i] * (1 - da) + dst[i] * (1 - sa) +
                    (2 * src[i] <= sa
                            ? 2 * src[i] * dst[i]
                            : sa * da - 2 * (sa - src[i]) * (da - dst[i]));
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Makes destination lighter or darker, depending on source.
     * This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> &le; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * C<sub>dst</sub> * (a<sub>src</sub> - 2 * C<sub>src</sub>) /
     * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> + C<sub>dst</sub> *
     * (2 * C<sub>src</sub> + 1 - a<sub>src</sub>)
     * </p>
     *
     * <p>if C<sub>dst</sub> &le; 0.25 * a<sub>dst</sub>:<br>
     * C<sub>out</sub> = (a<sub>dst</sub> * a<sub>dst</sub> * (C<sub>src</sub> + C<sub>dst</sub> *
     * (6 * C<sub>src</sub> - 3 * a<sub>src</sub> + 1)) + 12 * a<sub>dst</sub> * C<sub>dst</sub> * C<sub>dst</sub> *
     * (a<sub>src</sub> - 2 * C<sub>src</sub>) - 16 * C<sub>dst</sub> * C<sub>dst</sub> * C<sub>dst</sub> *
     * (a<sub>src</sub> - 2 * C<sub>src</sub>) - a<sub>dst</sub> * a<sub>dst</sub> * a<sub>dst</sub> *
     * C<sub>src</sub>) / a<sub>dst</sub> * a<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * (a<sub>src</sub> - 2 * C<sub>src</sub> + 1) + C<sub>src</sub> *
     * (1 - a<sub>dst</sub>) - sqrt(C<sub>dst</sub> * a<sub>dst</sub>) * (a<sub>src</sub> - 2 * C<sub>src</sub>)
     * </p>
     */
    public static void blend_soft_light(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float s = src[i];
            float d = dst[i];
            if (2 * s <= sa) {
                out[i] = d * d * (sa - 2 * s) / da + s * (1 - da) + d * (2 * s + 1 - sa);
            } else if (4 * d <= da) {
                float dd = d * d;
                float dada = da * da;
                out[i] = (dada * (s + d * (6 * s - 3 * sa + 1)) + 12 * da * dd * (sa - 2 * s) -
                        16 * dd * d * (sa - 2 * s) - dada * da * s) / dada;
            } else {
                out[i] = d * (sa - 2 * s + 1) + s * (1 - da) - (float) Math.sqrt(d * da) * (sa - 2 * s);
            }
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Subtracts darker from lighter with higher contrast.
     * This is an advanced blend equation.
     * </p>
     * <p>
     * a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     * <p>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - 2 * min(C<sub>src</sub> *
     * a<sub>dst</sub>, C<sub>dst</sub> * a<sub>src</sub>)
     * </p>
     */
    public static void blend_difference(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = src[i] + dst[i] - 2 * Math.min(src[i] * da, dst[i] * sa);
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Subtracts darker from lighter with lower contrast.
     * This is an advanced blend equation.
     * </p>
     * <p>
     * a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     * <p>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - 2 * C<sub>src</sub> * C<sub>dst</sub>
     * </p>
     */
    public static void blend_exclusion(float[] src, float[] dst, float[] out) {
        for (int i = 0; i < 3; i++) {
            out[i] = src[i] + dst[i] - 2 * (src[i] * dst[i]);
        }
        out[3] = src[3] + dst[3] * (1 - src[3]);
    }

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels and saturates
     * the result, with alpha blending. If both the source and destination are
     * opaque, then this is the same as {@link #MINUS_CLAMPED}. This is a custom
     * blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>dst</sub> / a<sub>dst</sub> - C<sub>src</sub> / a<sub>src</sub> &ge; 0:<br>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - 2 * C<sub>src</sub> * a<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_subtract(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = src[i] * (1 - da) + dst[i] - Math.min(src[i] * da, dst[i] * sa);
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Divides the destination pixels by the source pixels and saturates the result.
     * For negative and NaN values, the result color is black (XOR). This is a custom
     * blend equation.
     * </p>
     * <p>
     * a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     * <p>
     * C<sub>out</sub> = pin((C<sub>dst</sub> * a<sub>src</sub>) / (C<sub>src</sub> * a<sub>dst</sub>), 0, 1) *
     * a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_divide(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = MathUtil.pin((dst[i] * sa) / (src[i] * da), 0, 1) * sa * da +
                    src[i] * (1 - da) + dst[i] * (1 - sa);
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Lightens the destination pixels to reflect the source pixels while also increasing contrast.
     * This is {@link #PLUS_CLAMPED} with alpha blending. If both the source and
     * destination are opaque, then this is the same as {@link #PLUS_CLAMPED}.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &le; 1:<br>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_linear_dodge(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = Math.min(src[i] + dst[i], sa * da + src[i] * (1 - da) + dst[i] * (1 - sa));
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Darkens the destination pixels to reflect the source pixels while also increasing contrast.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &gt; 1:<br>
     * C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_linear_burn(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            out[i] = Math.max(src[i] + dst[i] - sa * da, src[i] * (1 - da) + dst[i] * (1 - sa));
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Burns or dodges colors by changing contrast, depending on the blend color.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> &le; 0:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * (1 - a<sub>src</sub>)
     * </p>
     *
     * <p>if C<sub>src</sub> &lt; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * (a<sub>dst</sub> - min(a<sub>dst</sub>, (a<sub>dst</sub> -
     * C<sub>dst</sub>) * a<sub>src</sub> / (2 * C<sub>dst</sub>))) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if C<sub>src</sub> &ge; a<sub>src</sub>:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * min(a<sub>dst</sub>, C<sub>dst</sub> * a<sub>src</sub> /
     * (2 * (a<sub>src</sub> - C<sub>src</sub>))) + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_vivid_light(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float s = src[i];
            float d = dst[i];
            if (2 * s < sa) {
                // burn
                if (s <= 0) {
                    out[i] = d * (1 - sa);
                } else {
                    out[i] = sa * Math.max(0, da - (da - d) * sa / (2 * s)) + s * (1 - da) + d * (1 - sa);
                }
            } else {
                // dodge
                if (s >= sa) {
                    out[i] = sa * da + s * (1 - da) + d * (1 - sa);
                } else {
                    out[i] = sa * Math.min(da, d * sa / (2 * (sa - s))) + s * (1 - da) + d * (1 - sa);
                }
            }
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Burns or dodges colors by changing brightness, depending on the blend color.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &gt; 2:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &le; 1:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * a<sub>dst</sub> + C<sub>dst</sub> * a<sub>src</sub> -
     * a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_linear_light(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float s = src[i];
            float d = dst[i];
            out[i] = MathUtil.clamp(2 * s * da + d * sa - sa * da, 0, sa * da) +
                    s * (1 - da) + d * (1 - sa);
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Conditionally replaces destination pixels with source pixels depending on the brightness of the source pixels.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> - C<sub>dst</sub> / a<sub>dst</sub> &gt; 1 &amp;&amp;
     * C<sub>src</sub> &lt; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> - C<sub>dst</sub> / a<sub>dst</sub> &gt; 1 &amp;&amp;
     * C<sub>src</sub> &ge; 0.5 * a<sub>src</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub> +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>if 2 * C<sub>src</sub> / a<sub>src</sub> - C<sub>dst</sub> / a<sub>dst</sub> &le; 1 &amp;&amp;
     * C<sub>src</sub> * a<sub>dst</sub> &lt; 0.5 * C<sub>dst</sub> * a<sub>src</sub>:<br>
     * C<sub>out</sub> = 2 * C<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = C<sub>dst</sub> * a<sub>src</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_pin_light(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float x = 2 * src[i] * da;
            float y = x - sa * da;
            float z = dst[i] * sa;
            out[i] = (y > z ? (2 * src[i] < sa ? 0 : y) : Math.min(x, z)) +
                    src[i] * (1 - da) + dst[i] * (1 - sa);
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Adds two images together, setting each color channel value to either 0 or 1.
     * This is an extended advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     *
     * <p>if C<sub>src</sub> / a<sub>src</sub> + C<sub>dst</sub> / a<sub>dst</sub> &lt; 1:<br>
     * C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     *
     * <p>otherwise:<br>
     * C<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> +
     * (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    public static void blend_hard_mix(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        for (int i = 0; i < 3; i++) {
            float b = src[i] * da + dst[i] * sa;
            float c = sa * da;
            out[i] = src[i] + dst[i] - b + (b < c ? 0 : c);
        }
        out[3] = sa + da * (1 - sa);
    }

    /**
     * <p>
     * Similar to {@link #DARKEN}, but darkens on the composite channel, instead of
     * separate RGB color channels. It compares the source and destination color,
     * and keep the one with lower luminosity between the two.
     * </p>
     * <p>if lum(C<sub>src</sub>) &le; lum(C<sub>dst</sub>):<br>
     * Equivalent to {@link #SRC_OVER}
     * </p>
     *
     * <p>otherwise:<br>
     * Equivalent to {@link #DST_OVER}
     * </p>
     */
    public static void blend_darker_color(float[] src, float[] dst, float[] out) {
        if (BlendMode.lum(src) <= BlendMode.lum(dst)) {
            // src_over
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i] * df;
            }
        } else {
            // dst_over
            float sf = 1 - dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf + dst[i];
            }
        }
    }

    /**
     * <p>
     * Similar to {@link #LIGHTEN}, but lightens on the composite channel, instead of
     * separate RGB color channels. It compares the source and destination color,
     * and keep the one with higher luminosity between the two.
     * </p>
     * <p>if lum(C<sub>src</sub>) &ge; lum(C<sub>dst</sub>):<br>
     * Equivalent to {@link #SRC_OVER}
     * </p>
     *
     * <p>otherwise:<br>
     * Equivalent to {@link #DST_OVER}
     * </p>
     */
    public static void blend_lighter_color(float[] src, float[] dst, float[] out) {
        if (BlendMode.lum(src) >= BlendMode.lum(dst)) {
            // src_over
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i] * df;
            }
        } else {
            // dst_over
            float sf = 1 - dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf + dst[i];
            }
        }
    }

    /**
     * <p>
     * Replaces hue of destination with hue of source, leaving saturation
     * and luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    public static void blend_hue(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        float alpha = sa * da;
        float[] c = {src[0] * da, src[1] * da, src[2] * da};
        BlendMode.set_lum_sat(c, dst, sa, dst, sa, alpha);
        for (int i = 0; i < 3; i++) {
            out[i] = c[i] + src[i] * (1 - da) + dst[i] * (1 - sa);
        }
        out[3] = sa + da - alpha;
    }

    /**
     * <p>
     * Replaces saturation of destination saturation hue of source, leaving hue and
     * luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    public static void blend_saturation(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        float alpha = sa * da;
        float[] c = {dst[0] * sa, dst[1] * sa, dst[2] * sa};
        BlendMode.set_lum_sat(c, src, da, dst, sa, alpha);
        for (int i = 0; i < 3; i++) {
            out[i] = c[i] + src[i] * (1 - da) + dst[i] * (1 - sa);
        }
        out[3] = sa + da - alpha;
    }

    /**
     * <p>
     * Replaces hue and saturation of destination with hue and saturation of source,
     * leaving luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    public static void blend_color(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        float alpha = sa * da;
        float[] c = {src[0] * da, src[1] * da, src[2] * da};
        BlendMode.set_lum(c, dst, sa, alpha);
        for (int i = 0; i < 3; i++) {
            out[i] = c[i] + src[i] * (1 - da) + dst[i] * (1 - sa);
        }
        out[3] = sa + da - alpha;
    }

    /**
     * <p>
     * Replaces luminosity of destination with luminosity of source, leaving hue and
     * saturation unchanged. This is an advanced blend equation.
     * </p>
     */
    public static void blend_luminosity(float[] src, float[] dst, float[] out) {
        float sa = src[3];
        float da = dst[3];
        float alpha = sa * da;
        float[] c = {dst[0] * sa, dst[1] * sa, dst[2] * sa};
        BlendMode.set_lum(c, src, da, alpha);
        for (int i = 0; i < 3; i++) {
            out[i] = c[i] + src[i] * (1 - da) + dst[i] * (1 - sa);
        }
        out[3] = sa + da - alpha;
    }

    private static float lum(float[] c) {
        // we know that Photoshop uses these values
        // instead of (0.3, 0.59, 0.11)
        return 0.299f * c[0] + 0.587f * c[1] + 0.114f * c[2];
    }

    private static void set_lum(float[] cbase,
                                float[] clum, float alum,
                                float alpha) {
        float ldiff = lum(clum) * alum - lum(cbase);
        for (int i = 0; i < 3; i++) {
            cbase[i] += ldiff;
        }
        float lum = lum(cbase);
        float mincol = MathUtil.min3(cbase);
        float maxcol = MathUtil.max3(cbase);
        if (mincol < 0 && lum != mincol) {
            for (int i = 0; i < 3; i++) {
                cbase[i] = lum + ((cbase[i] - lum) * lum) / (lum - mincol);
            }
        }
        if (maxcol > alpha && maxcol != lum) {
            for (int i = 0; i < 3; i++) {
                cbase[i] = lum + ((cbase[i] - lum) * (alpha - lum)) / (maxcol - lum);
            }
        }
    }

    private static void set_lum_sat(float[] cbase,
                                    float[] csat, float asat,
                                    float[] clum, float alum,
                                    float alpha) {
        float minbase = MathUtil.min3(cbase);
        float sbase = MathUtil.max3(cbase) - minbase;
        if (sbase > 0) {
            float ssat = (MathUtil.max3(csat) - MathUtil.min3(csat)) * asat;
            for (int i = 0; i < 3; i++) {
                cbase[i] = (cbase[i] - minbase) * ssat / sbase;
            }
        } else {
            for (int i = 0; i < 3; i++) {
                cbase[i] = 0;
            }
        }
        set_lum(cbase, clum, alum, alpha);
    }
}
