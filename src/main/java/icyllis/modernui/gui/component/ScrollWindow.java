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
import icyllis.modernui.gui.component.scroll.ScrollController;
import icyllis.modernui.gui.component.scroll.Scrollbar;
import icyllis.modernui.gui.element.Element;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

public class ScrollWindow extends Element implements IGuiEventListener {

    public static int EDGE_BLACK_THICKNESS = 6;

    protected Function<Integer, Float> wResizer, hResizer;

    protected float width;

    protected float height;

    protected float visibleHeight;

    protected float scrollAmount = 0f;

    protected Scrollbar scrollbar;

    protected ScrollController controller;

    int totalHeight = 600;

    public ScrollWindow(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, Function<Integer, Float> wResizer, Function<Integer, Float> hResizer) {
        super(xResizer, yResizer);
        this.wResizer = wResizer;
        this.hResizer = hResizer;
        this.scrollbar = new Scrollbar(this);
        this.controller = new ScrollController(this::callbackScrollAmount);
        this.moduleManager.addEventListener(this);
    }

    @Override
    public void draw(float currentTime) {
        controller.update(currentTime);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (minecraft.getMainWindow().getFramebufferHeight() - ((y + height) * scale)),
                    (int) (width * scale), (int) (height * scale));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();

        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + EDGE_BLACK_THICKNESS, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x + width, y + EDGE_BLACK_THICKNESS, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x + width, y, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(0, 0, 0, 128).endVertex();
        tessellator.draw();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + height, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x + width, y + height, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x + width, y + height - EDGE_BLACK_THICKNESS, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x, y + height - EDGE_BLACK_THICKNESS, 0.0D).color(0, 0, 0, 0).endVertex();
        tessellator.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);

        scrollbar.draw(currentTime);

        RenderSystem.enableTexture();

        fontRenderer.drawString("Yes", x + 1, y - scrollAmount + 10, 1, 1, 1, 1, 0);

        fontRenderer.drawString("Peach!", x + 1, y - scrollAmount + 200, 1, 1, 1, 1, 0);

        fontRenderer.drawString("Hey, you are here", x + 1, y - scrollAmount + 400, 1, 1, 1, 1, 0);

        fontRenderer.drawString("TQL", x + 1, y - scrollAmount + 595, 1, 1, 1, 1, 0);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.width = wResizer.apply(width);
        this.height = hResizer.apply(height);
        this.visibleHeight = this.height - EDGE_BLACK_THICKNESS * 2;;
        this.scrollbar.setPos(x + this.width - Scrollbar.BAR_THICKNESS - 1, y + 1);
        this.scrollbar.setMaxLength(this.height - 2);
        updateScrollBarLength();
        updateScrollBarOffset();
        scrollAmount(0);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        scrollbar.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        scrollbar.mouseReleased(mouseX, mouseY, mouseButton);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double rmx, double rmy) {
        if (scrollbar.mouseDragged(mouseX, mouseY, mouseButton, rmx, rmy)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            scrollAmount(Math.round(scrollAmount * -20f));
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void scrollAmount(float change) {
        controller.setTargetValue(MathHelper.clamp(controller.getTargetValue() + change, 0, getMaxScrollAmount()));
    }

    public void scrollWithoutAnimation(float change) {
        float amount = MathHelper.clamp(controller.getTargetValue() + change, 0, getMaxScrollAmount());
        controller.setTargetValueDirect(amount);
        callbackScrollAmount(amount);
    }

    /**
     * Controlled by scroll controller, do not call this manually
     */
    private void callbackScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        updateScrollBarOffset();
    }

    public int getTotalHeight() {
        return totalHeight;
    }

    public float getVisibleHeight() {
        return visibleHeight;
    }

    public float getMaxScrollAmount() {
        return Math.max(0, getTotalHeight() - getVisibleHeight());
    }

    public float getScrollPercentage() {
        float max = getMaxScrollAmount();
        if (max == 0) {
            return 0;
        }
        return scrollAmount / max;
    }

    public void updateScrollBarOffset() {
        scrollbar.setBarOffset(getScrollPercentage());
    }

    public void updateScrollBarLength() {

        float v = getVisibleHeight();
        float t = getTotalHeight();
        boolean renderBar = t > v;
        scrollbar.setVisible(renderBar);
        if (renderBar) {
            float p = v / t;
            scrollbar.setBarLength(p);
        }
    }
}
