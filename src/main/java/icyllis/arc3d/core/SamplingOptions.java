/*
 * Arc 3D.
 * Copyright (C) 2022-2023 BloCamLimb. All rights reserved.
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

//TODO
public final class SamplingOptions {

    /**
     * Filter modes.
     */
    public static final int
            FILTER_MODE_NEAREST = 0,    // single sample point (nearest neighbor)
            FILTER_MODE_LINEAR = 1;     // interpolate between 2x2 sample points (bilinear interpolation)

    /**
     * Mipmap modes.
     */
    public static final int
            MIPMAP_MODE_NONE = 0,       // ignore mipmap levels, sample from the "base"
            MIPMAP_MODE_NEAREST = 1,    // sample from the nearest level
            MIPMAP_MODE_LINEAR = 2;     // interpolate between the two nearest levels
}
