/*
 * The Arctic Graphics Engine.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Arctic is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arctic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arctic. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arcticgi.opengl;

public final class GLTextureParameters {

    // Texture parameter state that is not overridden by a bound sampler object.
    public int mBaseMipMapLevel;
    public int mMaxMipmapLevel;
    public boolean mSwizzleIsRGBA;

    public GLTextureParameters() {
        // These are the OpenGL defaults.
        mBaseMipMapLevel = 0;
        mMaxMipmapLevel = 1000;
        mSwizzleIsRGBA = true;
    }

    /**
     * Makes parameters invalid, forces GLServer to refresh.
     */
    public void invalidate() {
        mBaseMipMapLevel = ~0;
        mMaxMipmapLevel = ~0;
        mSwizzleIsRGBA = false;
    }
}
