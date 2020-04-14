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

package icyllis.modernui.impl.setting;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.gui.scroll.UniformScrollEntry;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Single option line in settings interface
 */
public abstract class SettingEntry extends UniformScrollEntry {

    protected final SettingScrollWindow window;

    public String title;

    protected float titleBrightness = 0.85f;

    public SettingEntry(SettingScrollWindow window, String title) {
        super(SettingCategoryGroup.ENTRY_HEIGHT);
        this.window = window;
        this.title = title;
        //TODO tooltip description lines
        /*if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);*/
    }

    @Override
    public final void draw(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        fontRenderer.drawString(title, x1, y1 + 6, titleBrightness, 1.0f, TextAlign.LEFT);
        /*if (desc.length > 0) {

        }*/
        drawExtra(time);
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0f);
        bufferBuilder.pos(x1, y2, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferBuilder.pos(x2, y2, 0.0D).color(140, 140, 140, 220).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    protected abstract void drawExtra(float time);

    @Override
    protected void onMouseHoverEnter() {
        titleBrightness = 1.0f;
    }

    @Override
    protected void onMouseHoverExit() {
        titleBrightness = 0.85f;
    }

}
