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

package icyllis.arc3d.sketch;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Typeface class specifies the typeface (font face) and intrinsic style of a font.
 * This is used in the paint, along with optionally algorithmic settings like
 * textSize, textScaleX, textShearX, kFakeBoldText_Mask, to specify
 * how text appears when drawn (and measured).
 * <p>
 * Typeface objects are immutable, and so they can be shared between threads.
 */
public abstract class Typeface {

    public Typeface() {
    }

    @NonNull
    public final ScalerContext createScalerContext(StrikeDesc desc) {
        return Objects.requireNonNull(onCreateScalerContext(desc));
    }

    @NonNull
    protected abstract ScalerContext onCreateScalerContext(StrikeDesc desc);

    protected abstract void onFilterStrikeDesc(StrikeDesc.Lookup desc);
}
