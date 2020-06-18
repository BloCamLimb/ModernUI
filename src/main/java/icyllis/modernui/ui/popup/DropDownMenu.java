/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.popup;

import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.master.UITools;
import icyllis.modernui.ui.animation.IInterpolator;
import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.animation.Applier;
import icyllis.modernui.ui.test.Align9D;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.Locator;
import icyllis.modernui.ui.test.Widget;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Provide multiple options, and must select one of them at the same time
 */
public class DropDownMenu extends Widget {

    private static int ENTRY_HEIGHT = 13;

    private final List<String> list;

    private final int selected;

    private int hovered = -1;

    /**
     * Reserve space if upward
     */
    private final float space;

    private boolean upward = false;

    @Nullable
    private IntConsumer callback;

    private float heightOffset = 0;

    private float textAlpha = 0;

    public DropDownMenu(IHost host, Builder builder) {
        super(host, builder);
        this.list = builder.list;
        this.selected = builder.index;
        this.width = this.list.stream().distinct().mapToInt(s -> (int) Math.ceil(UITools.getStringWidth(s))).max().orElse(0) + 7;
        this.height = this.list.size() * ENTRY_HEIGHT;
        this.space = 16;
        new Animation(200)
                .applyTo(
                        new Applier(0, height, () -> heightOffset, value -> heightOffset = value)
                                .setInterpolator(IInterpolator.SINE))
                .start();
        new Animation(150)
                .applyTo(new Applier(0, 1, () -> textAlpha, value -> textAlpha = value))
                .withDelay(150)
                .start();
    }

    public DropDownMenu buildCallback(@Nullable IntConsumer callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        float top = upward ? y2 - heightOffset : y1;
        float bottom = upward ? y2 : y1 + heightOffset;

        canvas.setRGBA(0.032f, 0.032f, 0.032f, 0.63f);
        canvas.drawRect(x1, top, x2, bottom);

        canvas.setLineAntiAliasing(true);
        canvas.setColor(Color3i.WHITE, 0.315f);
        canvas.drawRectLines(x1, top, x2, bottom);
        canvas.setLineAntiAliasing(false);

        for (int i = 0; i < list.size(); i++) {
            String text = list.get(i);
            float cy = y1 + ENTRY_HEIGHT * i;
            if (i == hovered) {
                canvas.setRGBA(0.5f, 0.5f, 0.5f, 0.315f);
                canvas.drawRect(x1, cy, x2, cy + ENTRY_HEIGHT);
            }
            canvas.setAlpha(textAlpha);
            if (i == selected) {
                canvas.setColor(Color3i.BLUE_C);
            } else {
                canvas.setColor(Color3i.WHITE);
            }
            if (align.isLeft()) {
                canvas.setTextAlign(TextAlign.LEFT);
                canvas.drawText(text, x1 + 3, cy + 2);
            } else {
                canvas.setTextAlign(TextAlign.RIGHT);
                canvas.drawText(text, x2 - 3, cy + 2);
            }
        }
    }

    /*@Override
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
                    fontRenderer.drawString(text, x1 + 3, cy + 2, Color3f.BLUE_C, textAlpha, TextAlign.LEFT);
                } else {
                    fontRenderer.drawString(text, x2 - 3, cy + 2, Color3f.BLUE_C, textAlpha, TextAlign.RIGHT);
                }
            } else {
                if (align == Align.LEFT) {
                    fontRenderer.drawString(text, x1 + 3, cy + 2, Color3f.WHILE, textAlpha, TextAlign.LEFT);
                } else {
                    fontRenderer.drawString(text, x2 - 3, cy + 2, Color3f.WHILE, textAlpha, TextAlign.RIGHT);
                }
            }
        }
    }*/

    @Override
    public void locate(float px, float py) {
        int gWidth = getParent().getGameWidth();
        int gHeight = getParent().getGameHeight();
        if (align.isLeft()) {
            this.x2 = Math.min(px + width, gWidth);
            this.x1 = x2 - width;
        } else {
            this.x1 = Math.max(px - width, 0);
            this.x2 = x1 + width;
        }
        this.y1 = py;
        this.y2 = py + height;
        float vH = height + space;
        this.upward = py + vH >= gHeight;
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
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (hovered != -1 && hovered != selected) {
            if (callback != null) {
                callback.accept(hovered);
            }
            return true;
        }
        return super.onMouseLeftClick(mouseX, mouseY);
    }

    /*@Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        return false;
    }*/

    public static class Builder extends Widget.Builder {

        protected final List<String> list;
        protected final int index;

        public Builder(List<String> list, int index) {
            this.list = list;
            this.index = index;
        }

        @Deprecated
        @Override
        public Builder setWidth(float width) {
            super.setWidth(width);
            return this;
        }

        @Deprecated
        @Override
        public Builder setHeight(float height) {
            super.setHeight(height);
            return this;
        }

        @Deprecated
        @Override
        public Builder setLocator(@Nonnull Locator locator) {
            super.setLocator(locator);
            return this;
        }

        @Override
        public Builder setAlign(@Nonnull Align9D align) {
            super.setAlign(align);
            return this;
        }

        @Nonnull
        @Override
        public DropDownMenu build(IHost host) {
            return new DropDownMenu(host, this);
        }
    }
}
