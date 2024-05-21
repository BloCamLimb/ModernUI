/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.core.effects;

import icyllis.arc3d.core.*;

import javax.annotation.Nullable;
import java.util.Objects;

public class BlendModeColorFilter extends ColorFilter {

    private static final BlendModeColorFilter CLEAR = new BlendModeColorFilter(BlendMode.SRC);

    // premultiplied blend color in sRGB space
    private final float[] mColor;
    private final BlendMode mMode;

    public BlendModeColorFilter(int color, BlendMode mode) {
        mColor = Color.load_and_premul(color);
        mMode = mode;
    }

    public BlendModeColorFilter(@Size(4) float[] color, boolean srcIsPremul, BlendMode mode) {
        this(mode);
        if (srcIsPremul) {
            System.arraycopy(color, 0, mColor, 0, 4);
        } else {
            float alpha = mColor[3] = color[3];
            mColor[0] = color[0] * alpha;
            mColor[1] = color[1] * alpha;
            mColor[2] = color[2] * alpha;
        }
    }

    private BlendModeColorFilter(BlendMode mode) {
        mColor = new float[4];
        mMode = mode;
    }

    @Nullable
    public static BlendModeColorFilter make(int color, BlendMode mode) {
        Objects.requireNonNull(mode);
        // Next collapse some modes if possible
        if (mode == BlendMode.CLEAR) {
            return CLEAR;
        }
        int alpha = color >>> 24;
        if (mode == BlendMode.SRC_OVER) {
            if (alpha == 0) {
                mode = BlendMode.DST;
            } else if (alpha == 0xFF) {
                mode = BlendMode.SRC;
            }
            // else just stay src_over
        }

        // Finally weed out combinations that are no-ops, and just return null
        if (mode == BlendMode.DST) {
            return null;
        }
        if (alpha == 0) {
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
            if (mode.isAdvanced()) {
                return null;
            }
        }
        if (alpha == 0xFF && mode == BlendMode.DST_IN) {
            return null;
        }

        return new BlendModeColorFilter(color, mode);
    }

    @Nullable
    public static BlendModeColorFilter make(@Size(4) float[] color, boolean srcIsPremul, BlendMode mode) {
        Objects.requireNonNull(mode);
        // Next collapse some modes if possible
        if (mode == BlendMode.CLEAR) {
            return CLEAR;
        }
        float alpha = color[3];
        if (mode == BlendMode.SRC_OVER) {
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
            if (mode.isAdvanced()) {
                return null;
            }
        }
        if (alpha == 1.f && mode == BlendMode.DST_IN) {
            return null;
        }

        return new BlendModeColorFilter(color, srcIsPremul, mode);
    }

    /**
     * @return premultiplied source color in sRGB space, unmodifiable.
     */
    public float[] getColor4f() {
        return mColor;
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
    public void filterColor4f(float[] col, float[] out) {
        mMode.apply(mColor, col, out);
    }
}
