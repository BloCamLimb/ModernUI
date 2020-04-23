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
import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.impl.module.SettingResourcePack;
import icyllis.modernui.gui.scroll.UniformScrollEntry;
import icyllis.modernui.gui.math.Color3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.ClientResourcePackInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public class ResourcePackEntry extends UniformScrollEntry {

    private final TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    private final ClientResourcePackInfo resourcePack;

    private final SettingResourcePack module;

    private float width;

    private String title = "";

    private String[] desc = new String[0];

    public ResourcePackEntry(SettingResourcePack module, ScrollWindow<?> window, ClientResourcePackInfo resourcePack) {
        super(window, ResourcePackGroup.ENTRY_HEIGHT);
        this.module = module;
        this.resourcePack = resourcePack;
    }

    @Override
    public final void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        this.width = right - left;
        updateTitle();
        updateDescription();
    }

    @Override
    public final void draw(Canvas canvas, float time) {
        /*Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();*/

        if (module.getHighlightEntry() == this) {
            canvas.setRGBA(0.5f, 0.5f, 0.5f, 0.377f);
            canvas.drawRect(x1 + 1, y1, x2 - 1, y2);

            canvas.setLineAntiAliasing(true);
            canvas.setRGBA(1, 1, 1, 0.879f);
            canvas.drawRectLines(x1 + 1, y1, x2 - 1, y2);
            canvas.setLineAntiAliasing(false);
            /*RenderSystem.disableTexture();
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
            RenderSystem.enableTexture();*/
        } else if (isMouseHovered()) {
            canvas.setLineAntiAliasing(true);
            canvas.setRGBA(0.879f, 0.879f, 0.879f, 0.7f);
            canvas.drawRectLines(x1 + 1, y1, x2 - 1, y2);
            canvas.setLineAntiAliasing(false);
            /*RenderSystem.disableTexture();
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
            RenderSystem.enableTexture();*/
        }

        RenderSystem.enableTexture();
        bindTexture();
        blitIcon(x1 + 3, y1 + 2);

        canvas.resetColor();
        canvas.setTextAlign(TextAlign.LEFT);
        canvas.drawText(title, x1 + 39, y1 + 4);

        canvas.setColor(Color3f.GRAY);
        int i = 0;
        for (String d : desc) {
            canvas.drawText(d, x1 + 39, y1 + 14 + i * 10);
            i++;
            if (i > 1) {
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0) {
            module.setHighlightEntry(this);
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

    public void updateTitle() {
        title = resourcePack.getTitle().getFormattedText();
        if (!resourcePack.getCompatibility().isCompatible()) {
            title = TextFormatting.DARK_RED + "(" + I18n.format("resourcePack.incompatible") + ") " + TextFormatting.RESET + title;
        }
        float w = FontTools.getStringWidth(title);
        float cw = width - 39;
        if (w > cw) {
            float kw = cw - FontTools.getStringWidth("...");
            title = FontTools.trimStringToWidth(title, kw, false) + "...";
        }

    }

    public void updateDescription() {
        this.desc = FontTools.splitStringToWidth(resourcePack.getDescription().getFormattedText(), width - 39);
    }

    public void bindTexture() {
        resourcePack.func_195808_a(textureManager);
    }

    public boolean canIntoSelected() {
        return !module.getSelectedGroup().getEntries().contains(this);
    }

    public boolean canIntoAvailable() {
        return module.getSelectedGroup().getEntries().contains(this) && !resourcePack.isAlwaysEnabled();
    }

    public boolean canGoUp() {
        List<ResourcePackEntry> list = module.getSelectedGroup().getEntries();
        int i = list.indexOf(this);
        return i > 0 && !(list.get(i - 1)).resourcePack.isOrderLocked();
    }

    public boolean canGoDown() {
        List<ResourcePackEntry> list = module.getSelectedGroup().getEntries();
        int i = list.indexOf(this);
        return i >= 0 && i < list.size() - 1 && !(list.get(i + 1)).resourcePack.isOrderLocked();
    }

    public ClientResourcePackInfo getResourcePack() {
        return resourcePack;
    }

    public final void intoSelected(ResourcePackGroup selectedGroup) {
        resourcePack.getPriority().insert(selectedGroup.getEntries(), this, ResourcePackEntry::getResourcePack, true);
    }

    public void goUp() {
        List<ResourcePackEntry> list = module.getSelectedGroup().getEntries();
        int i = list.indexOf(this);
        list.remove(i);
        list.add(i - 1, this);
    }

    public void goDown() {
        List<ResourcePackEntry> list = module.getSelectedGroup().getEntries();
        int i = list.indexOf(this);
        list.remove(i);
        list.add(i + 1, this);
    }

    private static void blitIcon(float x, float y) {
        blitFinal(x, x + 32, y, y + 32);
    }

    private static void blitFinal(double x1, double x2, double y1, double y2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
        bufferbuilder.pos(x1, y2, 0.0D).color(255, 255, 255, 255).tex(0, 1).endVertex();
        bufferbuilder.pos(x2, y2, 0.0D).color(255, 255, 255, 255).tex(1, 1).endVertex();
        bufferbuilder.pos(x2, y1, 0.0D).color(255, 255, 255, 255).tex(1, 0).endVertex();
        bufferbuilder.pos(x1, y1, 0.0D).color(255, 255, 255, 255).tex(0, 0).endVertex();
        tessellator.draw();
    }

}
