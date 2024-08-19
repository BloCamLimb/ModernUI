/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.MathUtil;
import icyllis.modernui.annotation.*;
import icyllis.modernui.core.Core;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * A color filter that can be used to tint the source pixels using a single
 * color and a specific {@link BlendMode}.
 *
 * @since 3.11
 */
public final class BlendModeColorFilter extends ColorFilter {

    // non-premultiplied blend color in sRGB space
    private final float[] mColor;
    private final BlendMode mMode;

    // closed by cleaner
    @Nullable
    private final icyllis.arc3d.core.effects.BlendModeColorFilter mColorFilter;

    public BlendModeColorFilter(@ColorInt int color, @NonNull BlendMode mode) {
        this(
                ((color >> 16) & 0xff) * (1 / 255.0f),
                ((color >> 8) & 0xff) * (1 / 255.0f),
                (color & 0xff) * (1 / 255.0f),
                (color >>> 24) * (1 / 255.0f),
                mode
        );
    }

    /**
     * Blend color is non-premultiplied and in sRGB space.
     */
    public BlendModeColorFilter(float r, float g, float b, float a, @NonNull BlendMode mode) {
        Objects.requireNonNull(mode);
        mColor = new float[]{
                MathUtil.pin(r, 0.0f, 1.0f),
                MathUtil.pin(g, 0.0f, 1.0f),
                MathUtil.pin(b, 0.0f, 1.0f),
                MathUtil.pin(a, 0.0f, 1.0f)
        };
        mMode = mode;

        mColorFilter = icyllis.arc3d.core.effects.BlendModeColorFilter.make(
                mColor, null, mode.getNativeBlendMode()
        );
        if (mColorFilter != null) {
            Core.registerNativeResource(this, mColorFilter);
        }
    }

    public int getColor() {
        return ((int) (mColor[0] * 255.0f + 0.5f) << 16) |
                ((int) (mColor[1] * 255.0f + 0.5f) << 8) |
                (int) (mColor[2] * 255.0f + 0.5f) |
                ((int) (mColor[3] * 255.0f + 0.5f) << 24);
    }

    @NonNull
    public float[] getColor4f() {
        return mColor.clone();
    }

    @NonNull
    public BlendMode getMode() {
        return mMode;
    }

    /**
     * @hidden
     */
    @ApiStatus.Internal
    @Override
    public icyllis.arc3d.core.effects.ColorFilter getNativeColorFilter() {
        return mColorFilter;
    }
}
