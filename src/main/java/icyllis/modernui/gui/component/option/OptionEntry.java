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

package icyllis.modernui.gui.component.option;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.window.SettingScrollWindow;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Single option line in settings interface
 */
public abstract class OptionEntry {

    protected IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    protected final SettingScrollWindow window;

    public String title;

    //public String[] desc = new String[0];

    protected boolean mouseHovered;

    protected float titleGrayscale = 0.85f;

    /*public OptionEntry(String optionName, T originalOption, List<T> options) {
        this(optionName, originalOption, options, null);
    }*/

    protected boolean autoApply = true;

    public OptionEntry(SettingScrollWindow window, String title) {
        this.window = window;
        this.title = title;
        /*if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);*/

    }

    public void disableAutoApply() {
        autoApply = false;
    }

    public final void draw(float centerX, float y, float currentTime) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        fontRenderer.drawString(title, centerX - 160, y + 6, titleGrayscale, titleGrayscale, titleGrayscale, 1, 0);
        /*if (desc.length > 0) {
            //TODO
        }*/
        drawExtra(centerX, y, currentTime);
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0f);
        bufferBuilder.pos(centerX - 160, y + OptionCategory.ENTRY_HEIGHT, 0.0D).color(140, 140, 140, 220).endVertex();
        bufferBuilder.pos(centerX + 160, y + OptionCategory.ENTRY_HEIGHT, 0.0D).color(140, 140, 140, 220).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
    }

    public void drawExtra(float centerX, float y, float currentTime) {

    }

    public void mouseMoved(double deltaCenterX, double deltaY, double mouseX, double mouseY) {

    }

    public void setMouseHovered(boolean mouseHovered) {
        boolean prev = this.mouseHovered;
        this.mouseHovered = mouseHovered;
        if (prev != mouseHovered) {
            if (mouseHovered) {
                onMouseHoverOn();
            } else {
                onMouseHoverOff();
            }
        }
    }

    protected void onMouseHoverOn() {
        titleGrayscale = 1.0f;
    }

    protected void onMouseHoverOff() {
        titleGrayscale = 0.85f;
    }

    public boolean mouseClicked(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseReleased(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseDragged(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton, double deltaMouseX, double deltaMouseY) {
        return false;
    }

}
