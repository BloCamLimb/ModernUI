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

package icyllis.arc3d.engine;

/**
 * BlendInfo is an immutable object holding info for setting-up
 * GPU blend states.
 * <p>
 * Note: constant color is premultiplied
 */
public record BlendInfo(
        int equation,
        int srcFactor,
        int dstFactor,
        float constantR,
        float constantG,
        float constantB,
        float constantA,
        boolean writeColor
) {

    public static final BlendInfo SRC = new BlendInfo(
            Blend.EQUATION_ADD,
            Blend.FACTOR_ONE,
            Blend.FACTOR_ZERO,
            0, 0, 0, 0,
            true
    );
}
