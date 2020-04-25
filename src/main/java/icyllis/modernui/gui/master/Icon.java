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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Icon is a full texture or a part of texture specially used in gui
 */
public class Icon {

    @Expose
    private final ResourceLocation resource;

    @Nullable
    private Texture texture;

    @Expose
    private final float p;

    @Expose
    private final float q;

    @Expose
    private final float s;

    @Expose
    private final float t;

    @Expose
    @SerializedName("antiAliasing")
    private final boolean aa;

    /**
     * Constructor
     *
     * @param resource texture location
     * @param p texture left pos [0,1]
     * @param q texture top pos [0,1]
     * @param s texture right pos [0,1]
     * @param t texture bottom pos [0,1]
     * @param aa enable anti-aliasing for HD textures
     */
    public Icon(ResourceLocation resource, float p, float q, float s, float t, boolean aa) {
        this.resource = resource;
        this.p = p;
        this.q = q;
        this.s = s;
        this.t = t;
        this.aa = aa;
    }

    private void loadTexture() {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        Texture texture = textureManager.getTexture(resource);
        if (texture == null) {
            texture = new SimpleTexture(resource);
            textureManager.loadTexture(resource, texture);
        }
        this.texture = texture;
    }

    public void bindTexture() {
        if (texture == null) {
            loadTexture();
        }
        texture.bindTexture();
        if (aa) {
            texture.setBlurMipmapDirect(true, true);
        } else {
            texture.setBlurMipmapDirect(false, false);
        }
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
