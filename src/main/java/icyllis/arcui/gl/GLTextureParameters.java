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

package icyllis.arcui.gl;

import static org.lwjgl.opengl.GL45C.*;

public final class GLTextureParameters {

    // Texture parameter state that is overridden when a non-zero sampler object is bound.
    // This is optional because we don't track it when we're using sampler objects.
    public int mMinFilter;
    public int mMagFilter;
    public int mWrapS;
    public int mWrapT;
    public float mMinLOD;
    public float mMaxLOD;
    // We always want the border color to be transparent black, so no need to store 4 floats.
    // Just track if it's been invalidated and no longer the default
    public boolean mBorderColorInvalid;

    // Texture parameter state that is not overridden by a bound sampler object.
    public int mBaseMipMapLevel;
    public int mMaxMipmapLevel;
    public boolean mSwizzleIsRGBA;

    public GLTextureParameters() {
        // These are the OpenGL defaults.
        mMinFilter = GL_NEAREST_MIPMAP_LINEAR;
        mMagFilter = GL_LINEAR;
        mWrapS = GL_REPEAT;
        mWrapT = GL_REPEAT;
        mMinLOD = -1000.f;
        mMaxLOD = 1000.f;
        mBorderColorInvalid = false;
        // These are the OpenGL defaults.
        mBaseMipMapLevel = 0;
        mMaxMipmapLevel = 1000;
        mSwizzleIsRGBA = true;
    }

    public void invalidate() {
        mMinFilter = ~0;
        mMagFilter = ~0;
        mWrapS = ~0;
        mWrapT = ~0;
        mMinLOD = Float.NaN;
        mMaxLOD = Float.NaN;
        mBorderColorInvalid = true;

        mBaseMipMapLevel = ~0;
        mMaxMipmapLevel = ~0;
        mSwizzleIsRGBA = false;
    }
}
