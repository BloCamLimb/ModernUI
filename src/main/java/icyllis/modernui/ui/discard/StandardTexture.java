/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.ui.discard;

/**
 * Example
 */
@Deprecated
public class StandardTexture extends Element {

    /*protected ResourceLocation res;

    protected float u, v;

    public float sizeW, sizeH;

    public float tintR, tintG, tintB, opacity;

    public float scale;

    public StandardTexture(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, int tintRGBA, float scale) {
        super(x, y);
        this.sizeW = w;
        this.sizeH = h;
        this.res = texture;
        this.u = u;
        this.v = v;
        this.opacity = (tintRGBA >> 24 & 255) / 255.0f;
        this.tintR = (tintRGBA >> 16 & 255) / 255.0f;
        this.tintG = (tintRGBA >> 8 & 255) / 255.0f;
        this.tintB = (tintRGBA & 255) / 255.0f;
        this.scale = scale;
    }

    @Override
    public void draw(float time) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.pushMatrix();
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.color4f(tintR, tintG, tintB, opacity);
        RenderSystem.scalef(scale, scale, 1);
        textureManager.bindTexture(res);
        DrawTools.blit(x / scale, y / scale, u, v, sizeW, sizeH);
        RenderSystem.popMatrix();
    }*/
}
