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

/**
 * Types for interacting with GL resources created externally to pipeline. BackendObjects for GL
 * textures are really const GLTexture*. The mFormat here should be a sized, internal format
 * for the texture. We use the sized format since the base internal formats are deprecated.
 */
public final class GLFramebufferInfo {

    public int mFramebuffer;
    public int mFormat;

    public void set(GLFramebufferInfo info) {
        mFramebuffer = info.mFramebuffer;
        mFormat = info.mFormat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GLFramebufferInfo that = (GLFramebufferInfo) o;
        if (mFramebuffer != that.mFramebuffer) return false;
        return mFormat == that.mFormat;
    }

    @Override
    public int hashCode() {
        int result = mFramebuffer;
        result = 31 * result + mFormat;
        return result;
    }
}
