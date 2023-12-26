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
 * Blend modes.
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
                out[i] = dst[i] * df + src[i];
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
     * Adds the source pixels to the destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> + C<sub>dst</sub></p>
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
     * the result. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>src</sub> + a<sub>dst</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>src</sub> + C<sub>dst</sub>, 1))</p>
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
     * Subtracts the destination pixels from the source pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>dst</sub> - a<sub>src</sub></p>
     * <p>C<sub>out</sub> = C<sub>dst</sub> - C<sub>src</sub></p>
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
     * Subtracts the destination pixels from the source pixels and saturates
     * the result. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = max(0, min(a<sub>dst</sub> - a<sub>src</sub>, 1))</p>
     * <p>C<sub>out</sub> = max(0, min(C<sub>dst</sub> - C<sub>src</sub>, 1))</p>
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
     * Multiplies the source and destination pixels.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub></p>
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
     * Multiplies the source and destination pixels. This is an advanced blend equation.
     * </p>
     * <p>a<sub>out</sub> = a<sub>src</sub> + a<sub>dst</sub> - a<sub>src</sub> * a<sub>dst</sub></p>
     * <p>C<sub>out</sub> = C<sub>src</sub> * C<sub>dst</sub> + (1 - a<sub>dst</sub>) * C<sub>src</sub> + (1 -
     * a<sub>src</sub>) * C<sub>dst</sub></p>
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
                out[i] = src[i] + dst[i] * (1 - src[i]);
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
            for (int i = 0; i < 4; i++) {
                out[i] = src[i] * (1 - da) + dst[i] * (1 - sa) +
                        (2 * dst[i] <= da
                                ? 2 * src[i] * dst[i]
                                : sa * da - 2 * (sa - src[i]) * (da - dst[i]));
            }
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
     * C<sub>out</sub> = C<sub>src</sub> * (1 - a<sub>dst</sub>)
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

        }
    },

    /**
     * <p>
     * Makes destination lighter or darker, depending on source.
     * This is an advanced blend equation.
     * </p>
     */
    HARD_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

        }
    },

    /**
     * <p>
     * Makes destination lighter or darker, depending on source.
     * This is an advanced blend equation.
     * </p>
     */
    SOFT_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

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
     * Lightens the destination pixels to reflect the source pixels while also increasing contrast.
     * This is an extended advanced blend equation.
     * </p>
     */
    LINEAR_DODGE {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

        }
    },

    /**
     * <p>
     * Darkens the destination pixels to reflect the source pixels while also increasing contrast.
     * This is an extended advanced blend equation.
     * </p>
     */
    LINEAR_BURN {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

        }
    },

    /**
     * <p>
     * Burns or dodges colors by changing contrast, depending on the blend color.
     * This is an extended advanced blend equation.
     * </p>
     */
    VIVID_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

        }
    },

    /**
     * <p>
     * Burns or dodges colors by changing brightness, depending on the blend color.
     * This is an extended advanced blend equation.
     * </p>
     */
    LINEAR_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

        }
    },

    /**
     * <p>
     * Conditionally replaces destination pixels with source pixels depending on the brightness of the source pixels.
     * This is an extended advanced blend equation.
     * </p>
     */
    PIN_LIGHT {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

        }
    },

    /**
     * <p>
     * Adds two images together, setting each color channel value to either 0 or 1.
     * This is an extended advanced blend equation.
     * </p>
     */
    HARD_MIX {
        @Override
        public void apply(float[] src, float[] dst, float[] out) {

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

        }
    };

    private static final BlendMode[] BLEND_MODES = values();
    public static final int COUNT = BLEND_MODES.length;

    @Nonnull
    public static BlendMode mode(int value) {
        return BLEND_MODES[value];
    }

    @Override
    public BlendMode asBlendMode() {
        return this;
    }

    /**
     * Applies this blend mode with RGBA colors. src, dst and out are all premultiplied.
     */
    public abstract void apply(@Size(min = 4) float[] src,
                               @Size(min = 4) float[] dst,
                               @Size(min = 4) float[] out);
}
