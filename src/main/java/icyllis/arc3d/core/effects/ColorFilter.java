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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * ColorFilters are optional objects in the drawing pipeline. When present in
 * a paint, they are called with the "src" colors, and return new colors, which
 * are then passed onto the next stage (either ImageFilter or Blender).
 * <p>
 * All subclasses are required to be reentrant-safe : it must be legal to share
 * the same instance between several threads.
 */
public sealed interface ColorFilter extends RefCounted
        permits BlendModeColorFilter, ColorMatrixColorFilter, ComposeColorFilter {

    /**
     * Returns the flags for this filter. Override in subclasses to return custom flags.
     */
    default boolean isAlphaUnchanged() {
        return false;
    }

    /**
     * Applies this color filter with ARGB color, un-premultiplied.
     *
     * @param col base color
     * @return resulting color
     */
    @ColorInt
    default int filterColor(@ColorInt int col) {
        float[] dst4 = Color.load_and_premul(col);
        filterColor4f(dst4, dst4, null);
        float a = MathUtil.clamp(dst4[3], 0, 1);
        int result = (int) (a * 255.0f + 0.5f) << 24;
        if (result == 0) {
            return Color.TRANSPARENT;
        }
        // unpremul and store
        a = 255.0f / a;
        for (int i = 0; i < 3; i++) {
            result |= (int) MathUtil.clamp(dst4[2 - i] * a + 0.5f, 0, 255) << (i << 3);
        }
        return result;
    }

    /**
     * Applies this color filter with RGBA colors. col and out store premultiplied
     * R,G,B,A components from index 0 to 3. col and out can be the same pointer.
     * col is read-only, out may be written multiple times.
     *
     * @param col   base color
     * @param out   resulting color
     * @param dstCS destination color space
     */
    void filterColor4f(@Size(4) float[] col, @Size(4) float[] out, ColorSpace dstCS);

    /**
     * Returns a composed color filter that first applies the <var>before</var> filter
     * and then applies <code>this</code> filter.
     *
     * @param before the filter to apply before this filter is applied, can be null
     * @return a composed color filter
     */
    @NonNull
    @SharedPtr
    default ColorFilter compose(@Nullable @SharedPtr ColorFilter before) {
        ref();
        if (before == null) {
            return this;
        }
        return new ComposeColorFilter(before, this);
    }

    /**
     * Returns a composed color filter that first applies <code>this</code> filter
     * and then applies the <var>after</var> filter.
     *
     * @param after the filter to apply after this filter is applied, can be null
     * @return a composed color filter
     */
    @NonNull
    @SharedPtr
    default ColorFilter andThen(@Nullable @SharedPtr ColorFilter after) {
        ref();
        if (after == null) {
            return this;
        }
        return new ComposeColorFilter(this, after);
    }

    // Currently all the subclasses are trivially counted
    @Override
    default void ref() {
    }

    @Override
    default void unref() {
    }

    @Override
    default boolean isTriviallyCounted() {
        return true;
    }
}
