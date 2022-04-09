/*
 * Arc UI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arc UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcui;

/**
 * Common constant values. These may be enum class in C++, but since Java's enums
 * are objects, we directly use int values.
 */
public final class Types {

    /**
     * The type of texture. Backends other than GL currently only use the 2D value but the type must
     * still be known at the API-neutral layer as it used to determine whether MIP maps, render-ability,
     * and sampling parameters are legal for proxies that will be instantiated with wrapped textures.
     */
    public static final int
            TEXTURE_TYPE_NONE = 0,
            TEXTURE_TYPE_2D = 1,
            TEXTURE_TYPE_RECTANGLE = 2,
            TEXTURE_TYPE_EXTERNAL = 3;
}
