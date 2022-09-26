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

package icyllis.arcticgi.vulkan;

import icyllis.arcticgi.engine.*;

import javax.annotation.Nonnull;

/**
 * Represents Vulkan 2D images, can be used as textures and attachments.
 */
public final class VkTexture extends Texture {

    public VkTexture(VkServer server, int width, int height) {
        super(server, width, height);
    }

    @Override
    public long getMemorySize() {
        return 0;
    }

    @Override
    protected void onFree() {

    }

    @Override
    protected void onDrop() {

    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return null;
    }

    @Override
    public int getTextureType() {
        return 0;
    }

    @Nonnull
    @Override
    public BackendTexture getBackendTexture() {
        return null;
    }

    @Override
    public int getMaxMipmapLevel() {
        return 0;
    }
}
