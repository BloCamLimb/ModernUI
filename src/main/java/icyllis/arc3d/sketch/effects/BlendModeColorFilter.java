/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2025 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.sketch.effects;

import icyllis.arc3d.sketch.BlendMode;
import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.Size;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class BlendModeColorFilter implements ColorFilter {

    // non-premultiplied blend source color in sRGB space
    private final float mR;
    private final float mG;
    private final float mB;
    private final float mA;
    private final BlendMode mMode;

    BlendModeColorFilter(@Size(4) float[] color, BlendMode mode) {
        mR = color[0];
        mG = color[1];
        mB = color[2];
        mA = color[3];
        mMode = mode;
    }

    @Nullable
    public static BlendModeColorFilter make(@Size(4) float[] color,
                                            @Nullable ColorSpace colorSpace,
                                            BlendMode mode) {
        if (mode == null) {
            return null;
        }

        // First map to sRGB to simplify storage in the actual ColorFilter instance, staying unpremul
        // until the final dst color space is known when actually filtering.
        float[] srgb = Arrays.copyOfRange(color, 0, 4);
        if (colorSpace != null && !colorSpace.isSrgb()) {
            ColorSpace.connect(colorSpace).transform(srgb);
        }

        // Next collapse some modes if possible
        float alpha = srgb[3];
        if (mode == BlendMode.CLEAR) {
            Arrays.fill(srgb, 0);
            mode = BlendMode.SRC;
        } else if (mode == BlendMode.SRC_OVER) {
            if (alpha == 0.f) {
                mode = BlendMode.DST;
            } else if (alpha == 1.f) {
                mode = BlendMode.SRC;
            }
            // else just stay src_over
        }

        // Finally weed out combinations that are no-ops, and just return null
        if (mode == BlendMode.DST) {
            return null;
        }
        if (alpha == 0.f) {
            switch (mode) {
                //case SRC_OVER:
                case DST_OVER:
                case DST_OUT:
                case SRC_ATOP:
                case XOR:
                case PLUS:
                case PLUS_CLAMPED:
                case MINUS:
                case MINUS_CLAMPED:
                    return null;
            }
            // All advanced blend modes are SrcOver-like
            if (mode.isAdvanced()) {
                return null;
            }
        }
        if (alpha == 1.f && mode == BlendMode.DST_IN) {
            return null;
        }

        return new BlendModeColorFilter(srgb, mode);
    }

    /**
     * @return a copy of non-premultiplied source color in sRGB space.
     */
    @Contract(value = " -> new", pure = true)
    public float @NonNull [] getColor() {
        return new float[]{mR, mG, mB, mA};
    }

    public BlendMode getMode() {
        return mMode;
    }

    @Override
    public boolean isAlphaUnchanged() {
        return switch (mMode) {
            case DST, SRC_ATOP -> true;
            default -> false;
        };
    }

    @Override
    public void filterColor4f(float[] col, float[] out, ColorSpace dstCS) {
        float[] blendColor = getColor();
        if (dstCS != null && !dstCS.isSrgb()) {
            ColorSpace.connect(ColorSpace.get(ColorSpace.Named.SRGB), dstCS)
                    .transform(blendColor);
        }
        for (int i = 0; i < 3; i++) {
            blendColor[i] *= blendColor[3];
        }
        mMode.apply(blendColor, col, out);
    }
}
