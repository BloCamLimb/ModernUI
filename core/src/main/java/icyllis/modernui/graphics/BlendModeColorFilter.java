/*
 * Modern UI.
 * Copyright (C) 2024-2026 BloCamLimb. All rights reserved.
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

import icyllis.arc3d.core.ColorSpace;
import icyllis.modernui.annotation.ColorInt;
import icyllis.modernui.annotation.ColorLong;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.annotation.Size;
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
    private final long mColor;
    private final BlendMode mMode;

    @Nullable
    private final icyllis.arc3d.sketch.effects.BlendModeColorFilter mColorFilter;

    public BlendModeColorFilter(@ColorInt int color, @NonNull BlendMode mode) {
        this(
                ((color >> 16) & 0xff) * (1 / 255.0f),
                ((color >> 8) & 0xff) * (1 / 255.0f),
                (color & 0xff) * (1 / 255.0f),
                (color >>> 24) * (1 / 255.0f),
                mode
        );
    }

    public BlendModeColorFilter(@ColorLong long color, @NonNull BlendMode mode) {
        Objects.requireNonNull(mode);
        mColor = color;
        mMode = mode;

        mColorFilter = icyllis.arc3d.sketch.effects.BlendModeColorFilter.make(
                new float[]{Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)},
                null, mode.getNativeBlendMode()
        );
    }

    /**
     * Blend color is non-premultiplied and in sRGB space.
     */
    public BlendModeColorFilter(float r, float g, float b, float a, @NonNull BlendMode mode) {
        Objects.requireNonNull(mode);
        mColor = Color.pack(r, g, b, a);
        mMode = mode;

        mColorFilter = icyllis.arc3d.sketch.effects.BlendModeColorFilter.make(
                new float[]{r, g, b, a}, null, mode.getNativeBlendMode()
        );
    }

    public BlendModeColorFilter(@NonNull @Size(4) float[] color,
                                @Nullable ColorSpace colorSpace, @NonNull BlendMode mode) {
        Objects.requireNonNull(mode);

        if (colorSpace != null && !colorSpace.isSrgb() &&
                colorSpace != ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)) {
            color = color.clone();
            ColorSpace.connect(colorSpace,
                            ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB),
                            ColorSpace.RenderIntent.RELATIVE)
                    .transform(color);
        }

        mColor = Color.pack(color[0], color[1], color[2], color[color.length - 1]);
        mMode = mode;

        mColorFilter = icyllis.arc3d.sketch.effects.BlendModeColorFilter.make(
                color, null, mode.getNativeBlendMode()
        );
    }

    public BlendModeColorFilter(float r, float g, float b, float a,
                                @Nullable ColorSpace colorSpace, @NonNull BlendMode mode) {
        this(new float[]{r, g, b, a}, colorSpace, mode);
    }

    public long getColor() {
        return mColor;
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
    public icyllis.arc3d.sketch.effects.ColorFilter getNativeColorFilter() {
        return mColorFilter;
    }
}
