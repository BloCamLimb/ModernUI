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
import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.IMouseListener;
import icyllis.modernui.gui.module.SettingLanguage;
import icyllis.modernui.gui.scroll.UniformScrollEntry;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.Language;
import org.lwjgl.opengl.GL11;

public class LanguageEntry extends UniformScrollEntry {

    private final SettingLanguage module;

    private final Language language;

    public LanguageEntry(SettingLanguage module, Language language) {
        super(LanguageGroup.ENTRY_HEIGHT);
        this.module = module;
        this.language = language;
    }

    @Override
    public final void draw(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        if (module.getHighlight() == this) {
            RenderSystem.disableTexture();
            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            bufferBuilder.pos(x1 + 1, y2, 0.0D).color(128, 128, 128, 96).endVertex();
            bufferBuilder.pos(x2 - 1, y2, 0.0D).color(128, 128, 128, 96).endVertex();
            bufferBuilder.pos(x2 - 1, y1, 0.0D).color(128, 128, 128, 96).endVertex();
            bufferBuilder.pos(x1 + 1, y1, 0.0D).color(128, 128, 128, 96).endVertex();
            tessellator.draw();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glLineWidth(1.0F);
            bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            bufferBuilder.pos(x1 + 1, y2, 0.0D).color(255, 255, 255, 224).endVertex();
            bufferBuilder.pos(x2 - 1, y2, 0.0D).color(255, 255, 255, 224).endVertex();
            bufferBuilder.pos(x2 - 1, y1, 0.0D).color(255, 255, 255, 224).endVertex();
            bufferBuilder.pos(x1 + 1, y1, 0.0D).color(255, 255, 255, 224).endVertex();
            tessellator.draw();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            RenderSystem.enableTexture();
        } else if (mouseHovered) {
            RenderSystem.disableTexture();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glLineWidth(1.0F);
            bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            bufferBuilder.pos(x1 + 1, y2, 0.0D).color(224, 224, 224, 180).endVertex();
            bufferBuilder.pos(x2 - 1, y2, 0.0D).color(224, 224, 224, 180).endVertex();
            bufferBuilder.pos(x2 - 1, y1, 0.0D).color(224, 224, 224, 180).endVertex();
            bufferBuilder.pos(x1 + 1, y1, 0.0D).color(224, 224, 224, 180).endVertex();
            tessellator.draw();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            RenderSystem.enableTexture();
        }

        fontRenderer.drawString(language.toString(), centerX, y1 + 4, TextAlign.CENTER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            module.setHighlight(this);
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter() {

    }

    @Override
    protected void onMouseHoverExit() {

    }

    public Language getLanguage() {
        return language;
    }

}
