/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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
    CLEAR {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = 0;
            }
        }
    },

    /**
     * <p>
     * The source pixels replace the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub></p>
     */
    SRC {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            System.arraycopy(src, 0, out, 0, 4);
        }
    },

    /**
     * <p>
     * The source pixels are discarded, leaving the destination intact.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub></p>
     */
    DST {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            System.arraycopy(dst, 0, out, 0, 4);
        }
    },

    /**
     * <p>
     * The source pixels are drawn over the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    SRC_OVER {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i] * df;
            }
        }
    },

    /**
     * <p>
     * The source pixels are drawn behind the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> + (1 - a<sub>dst</sub>) * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    DST_OVER {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = 1 - dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf + dst[i];
            }
        }
    },

    /**
     * <p>
     * Keeps the source pixels that cover the destination pixels,
     * discards the remaining source and destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * a<sub>dst</sub></p>
     */
    SRC_IN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf;
            }
        }
    },

    /**
     * <p>
     * Keeps the destination pixels that cover source pixels,
     * discards the remaining source and destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> * a<sub>src</sub></p>
     */
    DST_IN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float df = src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = dst[i] * df;
            }
        }
    },

    /**
     * <p>
     * Keeps the source pixels that do not cover destination pixels.
     * Discards source pixels that cover destination pixels. Discards all
     * destination pixels.
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>dst</sub>) * a<sub>src</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    SRC_OUT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = 1 - dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf;
            }
        }
    },

    /**
     * <p>
     * Keeps the destination pixels that are not covered by source pixels.
     * Discards destination pixels that are covered by source pixels. Discards all
     * source pixels.
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    DST_OUT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = dst[i] * df;
            }
        }
    },

    /**
     * <p>
     * Discards the source pixels that do not cover destination pixels.
     * Draws remaining source pixels over destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = a<sub>dst</sub> * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    SRC_ATOP {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = dst[3];
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf + dst[i] * df;
            }
        }
    },

    /**
     * <p>
     * Discards the destination pixels that are not covered by source pixels.
     * Draws remaining destination pixels over source pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub></p>
     * <p>C<sub>out</sub> = a<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub></p>
     */
    DST_ATOP {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = 1 - dst[3];
            float df = src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf + dst[i] * df;
            }
        }
    },

    /**
     * <p>
     * Discards the source and destination pixels where source pixels
     * cover destination pixels. Draws remaining source pixels.
     * </p>
     * <p>a<sub>out</sub> = (1 - a<sub>dst</sub>) * a<sub>src</sub> + (1 - a<sub>src</sub>) * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub></p>
     */
    XOR {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = 1 - dst[3];
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * sf + dst[i] * df;
            }
        }
    },

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
    PLUS {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i];
            }
        }
    },

    /**
     * <p>
     * Adds the source pixels to the destination pixels and saturates
     * the result, without alpha blending. For unsigned fixed-point textures,
     * this is the same as {@link #PLUS}. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>src</sub> + a<sub>dst</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>src</sub> + C<sub>dst</sub>, 1))</p>
     *
     * @see #PLUS
     * @see #LINEAR_DODGE
     */
    PLUS_CLAMPED {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = Math.min(src[i] + dst[i], 1);
            }
        }
    },

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
    MINUS {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = dst[i] - src[i];
            }
        }
    },

    /**
     * <p>
     * Subtracts the source pixels from the destination pixels and saturates
     * the result, without alpha blending. For unsigned fixed-point textures,
     * this is the same as {@link #MINUS}. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>dst</sub> - a<sub>src</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>dst</sub> - C<sub>src</sub>, 1))</p>
     *
     * @see #MINUS
     * @see #SUBTRACT
     */
    MINUS_CLAMPED {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = Math.max(dst[i] - src[i], 0);
            }
        }
    },

    /**
     * <p>
     * Multiplies the source and destination pixels, without alpha blending.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub></p>
     *
     * @see #MULTIPLY
     */
    MODULATE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * dst[i];
            }
        }
    },

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
    MULTIPLY {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sf = 1 - dst[3];
            float df = 1 - src[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * dst[i] + src[i] * sf + dst[i] * df;
            }
        }
    },

    /**
     * <p>
     * Adds the source and destination pixels, then subtracts the
     * source pixels multiplied by the destination.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub> - C<sub>src</sub> * C<sub>dst</sub></p>
     */
    SCREEN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i] - src[i] * dst[i];
            }
        }
    },

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
    OVERLAY {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

    /**
     * <p>
     * Retains the smallest component of the source and
     * destination pixels. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>
     * C<sub>out</sub> = min(C<sub>src</sub>, C<sub>dst</sub>) +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    DARKEN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i] - Math.max(src[i] * da, dst[i] * sa);
            }
        }
    },

    /**
     * <p>
     * Retains the largest component of the source and
     * destination pixel. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>
     * C<sub>out</sub> = max(C<sub>src</sub>, C<sub>dst</sub>) +
     * (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 - a<sub>src</sub>) * C<sub>dst</sub>
     * </p>
     */
    LIGHTEN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] + dst[i] - Math.min(src[i] * da, dst[i] * sa);
            }
        }
    },

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
    COLOR_DODGE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    COLOR_BURN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    HARD_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    SOFT_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    DIFFERENCE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 3; i++) {
                out[i] = src[i] + dst[i] - 2 * Math.min(src[i] * da, dst[i] * sa);
            }
            out[3] = sa + da * (1 - sa);
        }
    },

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
    EXCLUSION {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            for (int i = 0; i < 3; i++) {
                out[i] = src[i] + dst[i] - 2 * (src[i] * dst[i]);
            }
            out[3] = src[3] + dst[3] * (1 - src[3]);
        }
    },

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
    SUBTRACT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 3; i++) {
                out[i] = src[i] * (1 - da) + dst[i] - Math.min(src[i] * da, dst[i] * sa);
            }
            out[3] = sa + da * (1 - sa);
        }
    },

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
    DIVIDE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 3; i++) {
                out[i] = MathUtil.pin((dst[i] * sa) / (src[i] * da), 0, 1) * sa * da +
                        src[i] * (1 - da) + dst[i] * (1 - sa);
            }
            out[3] = sa + da * (1 - sa);
        }
    },

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
    LINEAR_DODGE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 3; i++) {
                out[i] = Math.min(src[i] + dst[i], sa * da + src[i] * (1 - da) + dst[i] * (1 - sa));
            }
            out[3] = sa + da * (1 - sa);
        }
    },

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
    LINEAR_BURN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 3; i++) {
                out[i] = Math.max(src[i] + dst[i] - sa * da, src[i] * (1 - da) + dst[i] * (1 - sa));
            }
            out[3] = sa + da * (1 - sa);
        }
    },

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
    VIVID_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    LINEAR_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    PIN_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    HARD_MIX {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
            float sa = src[3];
            float da = dst[3];
            for (int i = 0; i < 3; i++) {
                float b = src[i] * da + dst[i] * sa;
                float c = sa * da;
                out[i] = src[i] + dst[i] - b + (b < c ? 0 : c);
            }
            out[3] = sa + da * (1 - sa);
        }
    },

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
    DARKER_COLOR {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

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
    LIGHTER_COLOR {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

    /**
     * <p>
     * Replaces hue of destination with hue of source, leaving saturation
     * and luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    HUE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

    /**
     * <p>
     * Replaces saturation of destination saturation hue of source, leaving hue and
     * luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    SATURATION {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

    /**
     * <p>
     * Replaces hue and saturation of destination with hue and saturation of source,
     * leaving luminosity unchanged. This is an advanced blend equation.
     * </p>
     */
    COLOR {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    },

    /**
     * <p>
     * Replaces luminosity of destination with luminosity of source, leaving hue and
     * saturation unchanged. This is an advanced blend equation.
     * </p>
     */
    LUMINOSITY {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {
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
    };

    /**
     * Name alias of {@link #LINEAR_DODGE}.
     */
    public static final BlendMode ADD = LINEAR_DODGE;

    private static final BlendMode[] VALUES = values();
    /**
     * Number of blend modes.
     */
    public static final int COUNT = VALUES.length;

    /**
     * Returns the value at the given index.
     *
     * @param index the {@link BlendMode#ordinal()}
     * @return the blend mode
     */
    @Nonnull
    public static BlendMode mode(int index) {
        return VALUES[index];
    }

    @Override
    public BlendMode asBlendMode() {
        return this;
    }

    /**
     * Applies this blend mode with RGBA colors. src, dst and out store premultiplied
     * R,G,B,A components from index 0 to 3. src, dst and out can be the same pointer.
     */
    public abstract void apply(@Size(4) float[] src,
                               @Size(4) float[] dst,
                               @Size(4) float[] out);

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
