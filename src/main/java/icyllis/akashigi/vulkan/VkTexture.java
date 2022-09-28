/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.vulkan;

import icyllis.akashigi.engine.*;

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
    protected void onRelease() {

    }

    @Override
    protected void onDiscard() {

    }

    @Override
    public int getSampleCount() {
        return 0;
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return null;
    }

    @Override
    public boolean isExternal() {
        return false;
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
