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

package icyllis.modernui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.util.Color3I;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

public class DropDownMenu extends Widget {

    private static int ENTRY_HEIGHT = 13;

    private final List<String> list;

    private final int selectedIndex;

    private final float reservedSpace;

    private final Consumer<Integer> receiver;

    private float heightOffset;

    private boolean upward = false;

    private int hoveredIndex = -1;

    private float textAlpha = 0;

    private boolean init = false;

    public DropDownMenu(List<String> contents, int selected, float reservedSpace, Consumer<Integer> receiver) {
        this.list = contents;
        this.selectedIndex = selected;
        float width = list.stream().distinct().mapToInt(s -> (int) Math.ceil(fontRenderer.getStringWidth(s))).max().orElse(0) + 7;
        float height = list.size() * ENTRY_HEIGHT;
        area = new WidgetArea.Rect(width, height);
        this.reservedSpace = reservedSpace;
        this.receiver = receiver;
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(4, true)
                .applyTo(new Applier(0, height, value -> heightOffset = value)));
        GlobalModuleManager.INSTANCE.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> textAlpha = value))
                .withDelay(3));
    }

    @Override
    public void draw(float time) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        float left = x1;
        float bottom = upward ? y2 : y1 + heightOffset;

        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (upward) {
            bufferBuilder.pos(left, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, bottom - heightOffset, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(left, bottom - heightOffset, 0.0D).color(8, 8, 8, 160).endVertex();
        } else {
            bufferBuilder.pos(left, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(x2, y1, 0.0D).color(8, 8, 8, 160).endVertex();
            bufferBuilder.pos(left, y1, 0.0D).color(8, 8, 8, 160).endVertex();
        }
        tessellator.draw();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        GL11.glLineWidth(1.0F);
        if (upward) {
            bufferBuilder.pos(left, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, bottom - heightOffset, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(left, bottom - heightOffset, 0.0D).color(255, 255, 255, 80).endVertex();
        } else {
            bufferBuilder.pos(left, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(x2, y1, 0.0D).color(255, 255, 255, 80).endVertex();
            bufferBuilder.pos(left, y1, 0.0D).color(255, 255, 255, 80).endVertex();
        }
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.enableTexture();
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i);
            float cy = y1 + ENTRY_HEIGHT * i;
            if (i == hoveredIndex) {
                RenderSystem.disableTexture();
                bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                bufferBuilder.pos(left, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x2, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x2, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(left, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                tessellator.draw();
                RenderSystem.enableTexture();
            }
            if (selectedIndex == i) {
                fontRenderer.drawString(text, x2 - 3, cy + 2, Color3I.BLUE_C, textAlpha, TextAlign.RIGHT);
            } else {
                fontRenderer.drawString(text, x2 - 3, cy + 2, Color3I.WHILE, textAlpha, TextAlign.RIGHT);
            }
        }
    }

    @Override
    public void setPos(float x2, float y) {
        this.x1 = x2 - area.getWidth();
        this.x2 = x2;
        this.y1 = y;
        this.y2 = y + area.getHeight();
        float vH = area.getHeight() + reservedSpace;
        upward = y + vH >= GlobalModuleManager.INSTANCE.getWindowHeight();
        if (upward) {
            this.y1 -= vH;
            this.y2 -= vH;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (!init) {
            init = true;
            return;
        }
        GlobalModuleManager.INSTANCE.closePopup();
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            int pIndex = (int) ((mouseY - y1) / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < list.size()) {
                hoveredIndex = pIndex;
            } else {
                hoveredIndex = -1;
            }
            return true;
        }
        hoveredIndex = -1;
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseButton) {
        if (listening && mouseButton == 0 && hoveredIndex != -1) {
            receiver.accept(hoveredIndex);
        }
        return true;
    }
}
