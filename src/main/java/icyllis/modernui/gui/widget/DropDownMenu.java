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
import java.util.function.IntConsumer;

/**
 * Provide multiple options, and must select one of them at the same time
 */
public class DropDownMenu extends FlexibleWidget {

    private static int ENTRY_HEIGHT = 13;

    private final List<String> list;

    private final int selected;

    private int hovered = -1;

    /**
     * Reserve space if upward
     */
    private final float space;

    private boolean upward = false;

    private final Align align;

    private final IntConsumer callback;

    private float heightOffset = 0;

    private float textAlpha = 0;

    public DropDownMenu(List<String> list, int index, float space, IntConsumer callback, Align align) {
        this.list = list;
        this.selected = index;
        this.width = this.list.stream().distinct().mapToInt(s -> (int) Math.ceil(fontRenderer.getStringWidth(s))).max().orElse(0) + 7;
        this.height = this.list.size() * ENTRY_HEIGHT;
        this.space = space;
        this.align = align;
        this.callback = callback;
        manager.addAnimation(new Animation(4, true)
                .applyTo(new Applier(0, height, value -> heightOffset = value)));
        manager.addAnimation(new Animation(3)
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

        float top = upward ? y2 - heightOffset : y1;
        float bottom = upward ? y2 : y1 + heightOffset;

        // list background
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
        bufferBuilder.pos(x2, bottom, 0.0D).color(8, 8, 8, 160).endVertex();
        bufferBuilder.pos(x2, top, 0.0D).color(8, 8, 8, 160).endVertex();
        bufferBuilder.pos(x1, top, 0.0D).color(8, 8, 8, 160).endVertex();
        tessellator.draw();

        // list frame line
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(1.0F);
        bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
        bufferBuilder.pos(x2, bottom, 0.0D).color(255, 255, 255, 80).endVertex();
        bufferBuilder.pos(x2, top, 0.0D).color(255, 255, 255, 80).endVertex();
        bufferBuilder.pos(x1, top, 0.0D).color(255, 255, 255, 80).endVertex();
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.enableTexture();
        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i);
            float cy = y1 + ENTRY_HEIGHT * i;
            if (i == hovered) {
                RenderSystem.disableTexture();
                bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                bufferBuilder.pos(x1, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x2, cy + ENTRY_HEIGHT, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x2, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                bufferBuilder.pos(x1, cy, 0.0D).color(128, 128, 128, 80).endVertex();
                tessellator.draw();
                RenderSystem.enableTexture();
            }
            if (i == selected) {
                if (align == Align.LEFT) {
                    fontRenderer.drawString(text, x1 + 3, cy + 2, Color3I.BLUE_C, textAlpha, TextAlign.LEFT);
                } else {
                    fontRenderer.drawString(text, x2 - 3, cy + 2, Color3I.BLUE_C, textAlpha, TextAlign.RIGHT);
                }
            } else {
                if (align == Align.LEFT) {
                    fontRenderer.drawString(text, x1 + 3, cy + 2, Color3I.WHILE, textAlpha, TextAlign.LEFT);
                } else {
                    fontRenderer.drawString(text, x2 - 3, cy + 2, Color3I.WHILE, textAlpha, TextAlign.RIGHT);
                }
            }
        }
    }

    @Override
    public void setPos(float x, float y) {
        int gWidth = GlobalModuleManager.INSTANCE.getWindowWidth();
        int gHeight = GlobalModuleManager.INSTANCE.getWindowHeight();
        if (align == Align.LEFT) {
            this.x2 = Math.min(x + width, gWidth);
            this.x1 = x2 - width;
        } else {
            this.x1 = Math.max(x - width, 0);
            this.x2 = x1 + width;
        }
        this.y1 = y;
        this.y2 = y + height;
        float vH = height + space;
        this.upward = y + vH >= gHeight;
        if (upward) {
            this.y1 -= vH;
            this.y2 -= vH;
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            int index = (int) ((mouseY - y1) / ENTRY_HEIGHT);
            if (index >= 0 && index < list.size()) {
                hovered = index;
            } else {
                hovered = -1;
            }
            return true;
        }
        hovered = -1;
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (listening && mouseButton == 0 && hovered != -1) {
            callback.accept(hovered);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    public enum Align {
        LEFT,
        RIGHT
    }
}
