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

import icyllis.arc3d.core.ColorSpace;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;

import java.util.Objects;

/**
 * @see ColorFilter#compose(ColorFilter)
 */
public final class ComposeColorFilter implements ColorFilter {

    @SharedPtr
    private final ColorFilter mAfter;
    @SharedPtr
    private final ColorFilter mBefore;

    ComposeColorFilter(@SharedPtr ColorFilter before,
                       @SharedPtr ColorFilter after) {
        mBefore = Objects.requireNonNull(before);
        mAfter = Objects.requireNonNull(after);
    }

    @RawPtr
    public ColorFilter getBefore() {
        return mBefore;
    }

    @RawPtr
    public ColorFilter getAfter() {
        return mAfter;
    }

    @Override
    public boolean isAlphaUnchanged() {
        return mAfter.isAlphaUnchanged() && mBefore.isAlphaUnchanged();
    }

    @Override
    public void filterColor4f(float[] col, float[] out, ColorSpace dstCS) {
        mBefore.filterColor4f(col, out, dstCS); // col -> out
        mAfter.filterColor4f(out, out, dstCS);  // out -> out
    }
}
