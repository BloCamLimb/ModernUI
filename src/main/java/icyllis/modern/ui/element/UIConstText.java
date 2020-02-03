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

package icyllis.modern.ui.element;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modern.api.animation.IAlphaAnimation;
import icyllis.modern.api.element.IConstTextAnimator;
import icyllis.modern.api.element.IConstTextBuilder;
import icyllis.modern.system.ReferenceLibrary;
import icyllis.modern.ui.animation.AlphaAnimation;
import icyllis.modern.ui.font.IFontRenderer;
import icyllis.modern.ui.font.StringRenderer;
import icyllis.modern.ui.master.DrawTools;
import icyllis.modern.ui.master.GlobalAnimationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIConstText implements IConstTextBuilder, IConstTextAnimator, IElement {

    private IFontRenderer renderer;
    private TextureManager textureManager;

    private float bx, by;
    private float x, y;
    private float align;
    private String text;
    private float length;

    private Supplier<Integer> color;
    private Supplier<Float> alpha;
    private Supplier<Float> scale;

    private Consumer<Float> deco;

    public UIConstText() {
        renderer = StringRenderer.STRING_RENDERER;
        textureManager = Minecraft.getInstance().textureManager;
        text = "";
        color = () -> 0xffffff;
        alpha = () -> 1.0f;
        scale = () -> 1.0f;
        deco = s -> {};
    }

    @Override
    public void draw() {
        RenderSystem.pushMatrix();
        GlStateManager.enableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        float s = scale.get();
        if(s < 1) {
            RenderSystem.scalef(s, s, 1);
            renderer.drawString(text, x / s, y / s, color.get(), (int) (alpha.get() * 0xff), align / s);
        } else {
            renderer.drawString(text, x, y, color.get(), (int) (alpha.get() * 0xff), align);
        }
        deco.accept(s);
        RenderSystem.popMatrix();
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
    }

    @Override
    public IConstTextBuilder text(String c) {
        text = c;
        length = renderer.getStringWidth(c);
        return this;
    }

    @Override
    public IConstTextBuilder pos(float x, float y) {
        bx = x;
        by = y;
        return this;
    }

    @Override
    public IConstTextBuilder align(float align) {
        this.align = align;
        return this;
    }

    @Override
    public IConstTextBuilder color(Supplier<Integer> color) {
        this.color = color;
        return this;
    }

    @Override
    public IConstTextBuilder scale(Supplier<Float> scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public IConstTextBuilder style() {
        deco = sc -> {
            RenderSystem.color4f(120/255f, 190/255f, 230/255f, alpha.get());
            float s = sc * 0.5f;
            RenderSystem.scalef(0.5f, 0.5f, 1);
            textureManager.bindTexture(ReferenceLibrary.BUTTON);
            float x = this.x - length * align * 2;
            DrawTools.blit((x - 8) / s, (y + 0.5f) / s, 0, 8, 16, 16);
            DrawTools.blit((x + length * sc + 1) / s, (y + 0.5f) / s, 0, 8, 16, 16);
        };
        return this;
    }

    @Override
    public IConstTextAnimator animated() {
        return this;
    }

    @Override
    public IConstTextAnimator alpha(Consumer<IAlphaAnimation> a) {
        AlphaAnimation i = GlobalAnimationManager.INSTANCE.newAlpha(alpha.get());
        a.accept(i);
        alpha = i;
        return this;
    }
}
