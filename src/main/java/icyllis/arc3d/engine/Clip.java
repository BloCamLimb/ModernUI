/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.Rect2f;
import icyllis.arc3d.core.Rect2i;

/**
 * {@link Clip} is an abstract base class for producing a clip. It constructs a
 * clip mask if necessary, and fills out a {@link ClipResult} instructing the
 * caller on how to set up the draw state.
 */
public abstract class Clip {

    public abstract int apply(SurfaceDrawContext sdc,
                              ClipResult out,
                              Rect2f bounds);

    public static void getPixelBounds(Rect2f bounds, boolean aa,
                                      boolean exterior, Rect2i out) {
    }
}
