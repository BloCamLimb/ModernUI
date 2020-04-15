/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.master;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

/**
 * Icon is a full texture or a part of texture specially used in gui
 */
public class Icon {

    private final Texture texture;

    private final float p;

    private final float q;

    private final float s;

    private final float t;

    /**
     * Constructor
     * @param resource texture location
     * @param p texture left pos [0,1]
     * @param q texture top pos [0,1]
     * @param s texture right pos [0,1]
     * @param t texture bottom pos [0,1]
     * @param aa enable anti-aliasing for HD textures (Notice: anti-aliasing will apply on whole texture, not icon)
     */
    public Icon(ResourceLocation resource, float p, float q, float s, float t, boolean aa) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Texture texture = textureManager.getTexture(resource);
        if (texture == null) {
            texture = new SimpleTexture(resource);
            textureManager.loadTexture(resource, texture);
        }
        this.texture = texture;
        this.p = p;
        this.q = q;
        this.s = s;
        this.t = t;
        if (aa) {
            texture.setBlurMipmapDirect(true, true);
        } else {
            texture.setBlurMipmapDirect(false, false);
        }
    }

    public void loadTexture() {
        texture.bindTexture();
    }

    public float getLeft() {
        return p;
    }

    public float getTop() {
        return q;
    }

    public float getRight() {
        return s;
    }

    public float getBottom() {
        return t;
    }
}
