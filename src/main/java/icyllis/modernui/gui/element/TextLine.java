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

package icyllis.modernui.gui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.gui.font.IFontRenderer;
import icyllis.modernui.gui.font.StringRenderer;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;
import java.util.function.Supplier;

public class TextLine extends Base implements ITextLineBuilder {

    public static final TextLine DEFAULT = new TextLine();

    private IFontRenderer renderer = StringRenderer.STRING_RENDERER;

    private Supplier<String> text;

    private float align, scale;

    public float colorR, colorG, colorB;

    public TextLine() {

    }

    @Override
    public void draw() {
        RenderSystem.pushMatrix();
        GlStateManager.enableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        String text = this.text.get();
        RenderSystem.scalef(scale, scale, 1);
        renderer.drawString(text,  renderX / scale, renderY / scale, colorR, colorG, colorB, alpha, align / scale);
        RenderSystem.popMatrix();
    }

    public ITextLineBuilder style() {
        /*deco = sc -> {
            RenderSystem.color4f(120/255f, 190/255f, 230/255f, alpha.get());
            float s = sc * 0.5f;
            RenderSystem.scalef(0.5f, 0.5f, 1);
            textureManager.bindTexture(ReferenceLibrary.BUTTON);
            float x = this.x - length * align * 2;
            DrawTools.blit((x - 8) / s, (y + 0.5f) / s, 0, 8, 16, 16);
            DrawTools.blit((x + length * sc + 1) / s, (y + 0.5f) / s, 0, 8, 16, 16);
        };*/
        return this;
    }

    @Override
    public ITextLineBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, Supplier<String> text, float align, int RGBA, float scale) {
        this.fakeX = x;
        this.fakeY = y;
        this.text = text;
        this.align = align;
        this.alpha = (RGBA >> 24 & 255) / 255.0f;
        this.colorR = (RGBA >> 16 & 255) / 255.0f;
        this.colorG = (RGBA >> 8 & 255) / 255.0f;
        this.colorB = (RGBA & 255) / 255.0f;
        this.scale = scale;
        return this;
    }
}
