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

package icyllis.modernui.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.FontRendererTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.GlobalModuleManager;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

public class DropDownList implements IGuiEventListener {

    private static int ENTRY_HEIGHT = 13;

    private IFontRenderer fontRenderer = FontRendererTools.CURRENT_RENDERER;

    private final List<String> list;

    private final float vHeight;

    private final int selectedIndex;

    private final float reservedSpace;

    private final Consumer<Integer> receiver;

    private float x, y;

    private float width, height;

    private boolean upward = false;

    private int hoveredIndex = -1;

    private float textAlpha = 0;

    public DropDownList(List<String> contents, int selectedIndex, float reservedSpace, Consumer<Integer> receiver) {
        list = contents;
        this.selectedIndex = selectedIndex;
        width = list.stream().distinct().mapToInt(s -> (int) fontRenderer.getStringWidth(s)).max().orElse(0) + 7;
        vHeight = list.size() * ENTRY_HEIGHT;
        this.reservedSpace = reservedSpace;
        this.receiver = receiver;
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                .applyTo(new Applier(0, vHeight, value -> height = value)));
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> textAlpha = value))
                .withDelay(3));
    }

    public void draw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        float right = x + width;
        float bottom = upward ? y + vHeight : y + height;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (upward) {
            bufferBuilder.pos(x, bottom, 0.0D).color(8, 8, 8, 80).endVertex();
            bufferBuilder.pos(right, bottom, 0.0D).color(8, 8, 8, 80).endVertex();
            bufferBuilder.pos(right, bottom - height, 0.0D).color(8, 8, 8, 80).endVertex();
            bufferBuilder.pos(x, bottom - height, 0.0D).color(8, 8, 8, 80).endVertex();
        } else {
            bufferBuilder.pos(x, bottom, 0.0D).color(8, 8, 8, 80).endVertex();
            bufferBuilder.pos(right, bottom, 0.0D).color(8, 8, 8, 80).endVertex();
            bufferBuilder.pos(right, y, 0.0D).color(8, 8, 8, 80).endVertex();
            bufferBuilder.pos(x, y, 0.0D).color(8, 8, 8, 80).endVertex();
        }
        tessellator.draw();

        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0F);
        if (upward) {
            bufferBuilder.pos(x, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(right, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(right, bottom - height, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x, bottom - height, 0.0D).color(255, 255, 255, 80).endVertex();
        } else {
            bufferBuilder.pos(x, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(right, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(right, y, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x, y, 0.0D).color(255, 255, 255, 80).endVertex();
        }
        tessellator.draw();

        RenderSystem.enableTexture();
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i);
            float cy = y + ENTRY_HEIGHT * i;
            if (i == hoveredIndex) {
                RenderSystem.disableTexture();
                bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                bufferBuilder.pos(x, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(right, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(right, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                tessellator.draw();
                RenderSystem.enableTexture();
            }
            if (selectedIndex == i) {
                fontRenderer.drawString(text, right - 3, cy + 2, 0.6f, 0.87f, 0.94f, textAlpha, 0.5f);
            } else {
                fontRenderer.drawString(text, right - 3, cy + 2, 1, 1, 1, textAlpha, 0.5f);
            }
        }
    }

    public void setPos(float x, float y, float height) {
        this.x = x;
        this.y = y;
        float vH = vHeight + reservedSpace;
        upward = y + vH >= height;
        if (upward) {
            this.y -= vH;
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            int pIndex = (int) ((mouseY - y) / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < list.size()) {
                hoveredIndex = pIndex;
            } else {
                hoveredIndex = -1;
            }
        } else {
            hoveredIndex = -1;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (mouseButton == 0 && hoveredIndex != -1) {
            receiver.accept(hoveredIndex);
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
