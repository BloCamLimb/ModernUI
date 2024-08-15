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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.ImageMutableState;
import icyllis.arc3d.engine.Swizzle;

/**
 * Only used when OpenGL 4.3 texture view is unavailable.
 */
public final class GLTextureMutableState extends ImageMutableState {

    // Texture parameter state that is not overridden by a bound sampler object.
    public int mBaseMipmapLevel;
    public int mMaxMipmapLevel;
    // The read swizzle, identity by default.
    public short mSwizzle = Swizzle.RGBA;

    public GLTextureMutableState() {
        // These are the OpenGL defaults.
        mBaseMipmapLevel = 0;
        mMaxMipmapLevel = 1000;
    }

    /**
     * Makes parameters invalid, forces GLContext to refresh.
     */
    public void invalidate() {
        mBaseMipmapLevel = ~0;
        mMaxMipmapLevel = ~0;
        mSwizzle = ~0;
    }

    @Override
    public String toString() {
        return '{' +
                "mBaseMipmapLevel=" + mBaseMipmapLevel +
                ", mMaxMipmapLevel=" + mMaxMipmapLevel +
                ", mSwizzle=" + Swizzle.toString(mSwizzle) +
                '}';
    }
}
