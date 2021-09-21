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

package icyllis.modernui.forge;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Image;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

@Deprecated
public final class ContextClient extends Context {

    private final String mNamespace;
    private ResourceManager mResourceManager;

    public ContextClient(String namespace) {
        mNamespace = namespace;
        mResourceManager = Minecraft.getInstance().getResourceManager();
    }

    @Override
    public ReadableByteChannel getResource(@Nonnull Path path) throws IOException {
        return Channels.newChannel(mResourceManager.getResource(new ResourceLocation(mNamespace, path.toString()
                .replace('\\', '/'))).getInputStream());
    }

    @Nullable
    @Override
    public Image getImage(@Nonnull Path path, boolean aa) {
        /*Image.Source source = mImageMap.get(path);
        if (source != null) {
            return new Image(source);
        }
        try (Resource resource = mResourceManager.getResource(new ResourceLocation(mNamespace, path.toString()))) {
            Bitmap bitmap = Bitmap.decode(Bitmap.Format.RGBA, resource.getInputStream());
            GLTexture texture = new GLTexture(GL_TEXTURE_2D);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            RenderCore.recordRenderCall(() -> {
                texture.allocate2D(GL_RGBA8, width, height, aa ? 4 : 0);
                texture.upload(0, 0, 0, width, height, 0,
                        0, 0, 1, GL_RGBA, GL_UNSIGNED_BYTE, bitmap.getPixels());
                if (aa) {
                    texture.setFilter(true, true);
                    texture.generateMipmap();
                } else {
                    texture.setFilter(false, false);
                }
            });
            source = null;
            mImageMap.put(path, source);
            return new Image(source);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return null;
    }
}
