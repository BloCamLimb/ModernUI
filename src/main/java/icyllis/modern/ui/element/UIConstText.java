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
import icyllis.modern.api.element.IConstTextBuilder;
import icyllis.modern.api.element.IVarTextBuilder;
import icyllis.modern.system.ReferenceLibrary;
import icyllis.modern.ui.font.IFontRenderer;
import icyllis.modern.ui.font.StringRenderer;
import icyllis.modern.ui.master.DrawTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import org.lwjgl.opengl.GL11;

public class UIConstText implements IConstTextBuilder, IElement {

    private IFontRenderer renderer;
    protected TextureManager textureManager;

    private float bx, by;
    private float x, y;
    private int color;
    private int alpha;
    private float align;
    private String text;

    public UIConstText() {
        renderer = StringRenderer.STRING_RENDERER;
        textureManager = Minecraft.getInstance().textureManager;
        color = 0xffffff;
        alpha = 0xff;
        text = "";
    }

    @Override
    public void draw() {
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        float l = renderer.getStringWidth(text);
        GlStateManager.scaled(0.8, 0.8, 1);
        renderer.drawString(text, x / 0.8f, y / 0.8f, color, alpha, align);
        textureManager.bindTexture(ReferenceLibrary.BUTTON);
        GlStateManager.scaled(0.625, 0.625, 1);
        DrawTools.blit(x * 2 - 70, y * 2 + 1, 0, 8, 16, 16);
        GlStateManager.popMatrix();
    }

    @Override
    public void resize(int width, int height) {
        x = width / 2f + bx;
        y = height / 2f + by;
    }

    @Override
    public IConstTextBuilder text(String c) {
        text = c;
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
    public IConstTextBuilder color(int color) {
        this.color = color;
        return this;
    }
}
