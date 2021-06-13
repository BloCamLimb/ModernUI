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

import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.texture.Texture2D;
import icyllis.modernui.platform.Bitmap;
import icyllis.modernui.platform.RenderCore;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;

public final class ContextClient extends Context {

    private final String mNamespace;
    private ResourceManager mResourceManager;

    private Map<Path, Image.Source> mImageMap = new HashMap<>();

    public ContextClient(String namespace) {
        mNamespace = namespace;
        mResourceManager = Minecraft.getInstance().getResourceManager();
        ((ReloadableResourceManager) mResourceManager)
                .registerReloadListener((ISelectiveResourceReloadListener) (manager, predicate) -> {
                    if (predicate.test(VanillaResourceType.TEXTURES)) {
                        mImageMap.clear();
                    }
                });
    }

    @Override
    public ReadableByteChannel getResource(@Nonnull Path path) throws IOException {
        return Channels.newChannel(mResourceManager.getResource(new ResourceLocation(mNamespace, path.toString())).getInputStream());
    }

    @Nullable
    @Override
    public Image getImage(@Nonnull Path path, boolean aa) {
        Image.Source source = mImageMap.get(path);
        if (source != null) {
            return new Image(source);
        }
        try (Resource resource = mResourceManager.getResource(new ResourceLocation(mNamespace, path.toString()))) {
            Bitmap bitmap = Bitmap.decode(Bitmap.Format.RGBA, resource.getInputStream());
            Texture2D texture2D = new Texture2D();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            RenderCore.recordRenderCall(() -> {
                texture2D.init(GL_RGBA8, width, height, aa ? 4 : 0);
                texture2D.upload(0, 0, 0, width, height, 0,
                        0, 0, 1, GL_RGBA, GL_UNSIGNED_BYTE, bitmap.getPixels());
                if (aa) {
                    texture2D.setFilter(true, true);
                    texture2D.generateMipmap();
                } else {
                    texture2D.setFilter(false, false);
                }
            });
            source = new Image.Source(width, height, texture2D);
            mImageMap.put(path, source);
            return new Image(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
