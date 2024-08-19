/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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
 * Descriptor to create image views.
 */
public final class ImageViewDesc {

    /**
     * @see Engine.SurfaceOrigin
     */
    public int mOrigin;
    /**
     * @see Swizzle
     */
    public short mSwizzle;

    public ImageViewDesc() {
        mOrigin = Engine.SurfaceOrigin.kUpperLeft;
        mSwizzle = Swizzle.RGBA;
    }

    public ImageViewDesc(int origin, short swizzle) {
        mOrigin = origin;
        mSwizzle = swizzle;
    }

    /**
     * @see Engine.SurfaceOrigin
     */
    public int getOrigin() {
        return mOrigin;
    }

    /**
     * @see Swizzle
     */
    public short getSwizzle() {
        return mSwizzle;
    }

    /**
     * Concat swizzle.
     */
    public void concat(short swizzle) {
        mSwizzle = Swizzle.concat(mSwizzle, swizzle);
    }
}
