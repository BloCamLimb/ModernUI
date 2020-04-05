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

package icyllis.modernui.gui.option;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.master.IMouseListener;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Single option line in settings interface
 */
public abstract class OptionEntry implements IMouseListener {

    protected final IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    protected final SettingScrollWindow window;

    protected float x1, y1;

    protected float x2, y2;

    protected float centerX;

    protected float height;

    protected boolean mouseHovered = false;

    public String title;

    protected float titleBrightness = 0.85f;

    public OptionEntry(SettingScrollWindow window, String title) {
        this.window = window;
        this.title = title;
        this.height = OptionCategory.ENTRY_HEIGHT;
        //TODO tooltip description lines
        /*if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);*/
    }

    /**
     * Called when layout
     */
    public void setPos(float x1, float x2, float y) {
        this.x1 = x1;
        this.x2 = x2;
        this.centerX = (x1 + x2) / 2f;
        this.y1 = y;
        this.y2 = y + height;
    }

    public final float getHeight() {
        return height;
    }

    public final float getTop() {
        return y1;
    }

    public final float getBottom() {
        return y2;
    }

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

    protected void drawExtra(float time) {

    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        boolean prev = mouseHovered;
        mouseHovered = isMouseInArea(mouseX, mouseY);
        if (prev != mouseHovered) {
            if (mouseHovered) {
                onMouseHoverEnter();
            } else {
                onMouseHoverExit();
            }
        }
        return mouseHovered;
    }

    private boolean isMouseInArea(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    protected void onMouseHoverEnter() {
        titleBrightness = 1.0f;
    }

    protected void onMouseHoverExit() {
        titleBrightness = 0.85f;
    }

    @Override
    public final boolean isMouseHovered() {
        return mouseHovered;
    }
}
