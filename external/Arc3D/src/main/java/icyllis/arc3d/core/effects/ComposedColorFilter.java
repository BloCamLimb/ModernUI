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

package icyllis.arc3d.core.effects;

import icyllis.arc3d.core.ColorFilter;

import java.util.Objects;

/**
 * @see ColorFilter#compose(ColorFilter)
 */
public class ComposedColorFilter extends ColorFilter {

    private final ColorFilter mAfter;
    private final ColorFilter mBefore;

    public ComposedColorFilter(ColorFilter before, ColorFilter after) {
        mBefore = Objects.requireNonNull(before);
        mAfter = Objects.requireNonNull(after);
    }

    public ColorFilter getBefore() {
        return mBefore;
    }

    public ColorFilter getAfter() {
        return mAfter;
    }

    @Override
    public boolean isAlphaUnchanged() {
        return mAfter.isAlphaUnchanged() && mBefore.isAlphaUnchanged();
    }

    @Override
    public void filterColor4f(float[] col, float[] out) {
        mBefore.filterColor4f(col, out); // col -> out
        mAfter.filterColor4f(out, out);  // out -> out
    }
}
