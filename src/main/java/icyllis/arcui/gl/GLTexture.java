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

import icyllis.arcui.hgi.*;

import javax.annotation.Nonnull;

/**
 * Represents OpenGL 2D textures, can be used as textures and attachments.
 */
public final class GLTexture extends Texture {

    public GLTexture(GLServer server,
                     int width, int height,
                     GLFormat format,
                     int texture,
                     boolean mipmapped,
                     boolean budgeted,
                     boolean ownership) {
        super(server, width, height, false);
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Override
    protected void onRelease() {

    }

    @Override
    protected void onDiscard() {

    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return null;
    }

    @Override
    public int getSampleCount() {
        // We have no multisample textures
        return 1;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Nonnull
    @Override
    public BackendTexture getBackendTexture() {
        return null;
    }

    @Override
    public boolean isMipmapped() {
        return false;
    }
}
