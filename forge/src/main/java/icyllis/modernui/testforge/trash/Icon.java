/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.testforge.trash;

import icyllis.modernui.graphics.opengl.GLTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Icon is a cached full texture or a part of texture specially used in UI
 */
@Deprecated
public class Icon {

    private final ResourceLocation location;

    @Nullable
    private GLTexture texture;

    private final float u1;
    private final float v1;
    private final float u2;
    private final float v2;

    private final boolean aa;

    /**
     * Constructor
     *
     * @param location texture location
     * @param u1       texture left pos [0,1]
     * @param v1       texture top pos [0,1]
     * @param u2       texture right pos [0,1]
     * @param v2       texture bottom pos [0,1]
     * @param aa       enable anti-aliasing for HD textures
     */
    public Icon(ResourceLocation location, float u1, float v1, float u2, float v2, boolean aa) {
        this.location = location;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
        this.aa = aa;
    }

    /*private void loadTexture() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        try (Resource resource = manager.getResource(location);
             Bitmap bitmap = Bitmap.decode(Bitmap.Format.RGBA, resource.getInputStream())) {
            Texture2D texture = new Texture2D();
            texture.allocate2D(GL_RGBA8, bitmap.getWidth(), bitmap.getHeight(), aa ? 4 : 0);
            texture.upload(0, 0, 0, bitmap.getWidth(), bitmap.getHeight(), 0,
                    0, 0, 1, GL_RGBA, GL_UNSIGNED_BYTE, bitmap.getPixels());
            if (aa) {
                texture.setFilter(true, true);
                texture.generateMipmap();
            } else {
                texture.setFilter(false, false);
            }
            this.texture = texture;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public void bindTexture() {
        /*if (texture == null)
            loadTexture();
        else
            texture.bind();*/
    }

    public float getLeft() {
        return u1;
    }

    public float getTop() {
        return v1;
    }

    public float getRight() {
        return u2;
    }

    public float getBottom() {
        return v2;
    }
}
