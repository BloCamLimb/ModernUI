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
import icyllis.modernui.font.FontRendererTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.window.SettingScrollWindow;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Single option line in settings interface
 */
public class OptionEntry<T> {

    protected IFontRenderer fontRenderer = FontRendererTools.CURRENT_RENDERER;

    protected final SettingScrollWindow window;

    public String optionName;

    public String[] desc = new String[0];

    public final int originalOption;

    public int currentOption;

    public List<T> options;

    public boolean mouseHovered;

    /*public OptionEntry(String optionName, T originalOption, List<T> options) {
        this(optionName, originalOption, options, null);
    }*/

    public OptionEntry(SettingScrollWindow windowString, String optionName, List<T> options, int originalIndex) {
        this.window = windowString;
        this.optionName = optionName;
        this.currentOption = this.originalOption = originalIndex;
        this.options = options;
        /*if (desc != null)
            this.desc = FontRendererTools.splitStringToWidth(desc, 150);*/

    }

    public final void draw(float centerX, float y, float currentTime) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        fontRenderer.drawString(optionName, centerX - 160, y + 6, 0.75f, 0.75f, 0.75f, 1, 0);
        if (desc.length > 0) {
            //TODO
        }
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

    public void setMouseHovered(boolean mouseHovered) {
        this.mouseHovered = mouseHovered;
    }

    public boolean mouseClicked(double rcx, double rty, int mouseButton) {
        return false;
    }

}
