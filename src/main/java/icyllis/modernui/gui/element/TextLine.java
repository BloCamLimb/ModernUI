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
import icyllis.modernui.api.element.IRectangleBuilder;
import icyllis.modernui.api.element.ITextLineBuilder;
import icyllis.modernui.gui.font.IFontRenderer;
import icyllis.modernui.gui.font.StringRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;
import java.util.function.Supplier;

public class TextLine extends Base implements ITextLineBuilder {

    public static final TextLine DEFAULT = new TextLine();

    private IFontRenderer renderer;
    private TextureManager textureManager;

    private float align;
    private Supplier<String> text;

    private Supplier<Integer> color;
    private Supplier<Float> scale;

    public TextLine() {
        renderer = StringRenderer.STRING_RENDERER;
        textureManager = Minecraft.getInstance().textureManager;
        text = () -> "";
        color = () -> 0xffffff;
        scale = () -> 1.0f;
    }

    @Override
    public void draw() {
        RenderSystem.pushMatrix();
        GlStateManager.enableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        String text = this.text.get();
        float s = scale.get();
        if(s < 1) {
            RenderSystem.scalef(s, s, 1);
            renderer.drawString(text,  renderX / s, renderY / s, color.get(), (int) (alpha * 0xff), align / s);
        } else {
            renderer.drawString(text, renderX, renderY, color.get(), (int) (alpha * 0xff), align);
        }
        RenderSystem.popMatrix();
    }

    @Override
    public ITextLineBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y) {
        GWtBX = x;
        GWtBY = y;
        return this;
    }

    @Override
    public ITextLineBuilder setPos(float x, float y) {
        GWtBX = w -> w / 2f + x;
        GWtBY = h -> h / 2f + y;
        return this;
    }

    @Override
    public ITextLineBuilder setAlpha(float a) {
        alpha = a;
        return this;
    }

    @Override
    public ITextLineBuilder text(Supplier<String> text) {
        this.text = text;
        return this;
    }

    @Override
    public ITextLineBuilder align(float align) {
        this.align = align;
        return this;
    }

    @Override
    public ITextLineBuilder color(Supplier<Integer> color) {
        this.color = color;
        return this;
    }

    @Override
    public ITextLineBuilder scale(Supplier<Float> scale) {
        this.scale = scale;
        return this;
    }

    @Override
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
}
