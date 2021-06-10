/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.core;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

public final class ContextImpl extends Context {

    private final String mNamespace;
    private ResourceManager mResourceManager;

    public ContextImpl(String namespace) {
        mNamespace = namespace;
        if (FMLEnvironment.dist.isClient()) {
            mResourceManager = Minecraft.getInstance().getResourceManager();
        }
    }

    @Override
    public ReadableByteChannel getResource(@Nonnull Path path) throws IOException {
        return Channels.newChannel(mResourceManager.getResource(new ResourceLocation(mNamespace, path.toString())).getInputStream());
    }
}
