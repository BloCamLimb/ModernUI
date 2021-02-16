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

package icyllis.modernui.graphics.math;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Icon is a cached full texture or a part of texture specially used in UI
 */
public class Icon {

    @Expose
    private final ResourceLocation resource;

    @Nullable
    private AbstractTexture texture;

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
     * @param u1       texture left pos [0,1]
     * @param v1       texture top pos [0,1]
     * @param u2       texture right pos [0,1]
     * @param v2       texture bottom pos [0,1]
     * @param aa       enable anti-aliasing for HD textures
     */
    public Icon(ResourceLocation resource, float u1, float v1, float u2, float v2, boolean aa) {
        this.resource = resource;
        this.p = u1;
        this.q = v1;
        this.s = u2;
        this.t = v2;
        this.aa = aa;
    }

    private void loadTexture() {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        AbstractTexture texture = textureManager.getTexture(resource);
        if (texture == null) {
            texture = new SimpleTexture(resource);
            textureManager.register(resource, texture);
        }
        this.texture = texture;
    }

    public void bindTexture() {
        if (texture == null) {
            loadTexture();
            texture.bind();
            if (aa) {
                texture.setFilter(true, true);
            } else {
                texture.setFilter(false, false);
            }
        } else {
            texture.bind();
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
