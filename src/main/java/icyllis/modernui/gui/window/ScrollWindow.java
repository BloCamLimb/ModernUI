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

package icyllis.modernui.gui.window;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.component.scroll.ScrollController;
import icyllis.modernui.gui.component.scroll.ScrollGroup;
import icyllis.modernui.gui.component.scroll.ScrollList;
import icyllis.modernui.gui.component.scroll.ScrollBar;
import icyllis.modernui.gui.element.Element;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.function.Function;

public class ScrollWindow<T extends ScrollGroup> extends Element implements IGuiEventListener {

    public int borderThickness = 6;

    protected Function<Integer, Float> wResizer, hResizer;

    protected float width, height;

    protected float right, bottom; // right = x + width | bottom = y + height

    protected float centerX;

    protected float visibleHeight; // except black fade out border * 2

    protected float scrollAmount = 0f; // scroll offset, > 0

    protected ScrollBar scrollbar;

    protected ScrollController controller;

    protected ScrollList<T> scrollList;

    public ScrollWindow(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, Function<Integer, Float> wResizer, Function<Integer, Float> hResizer) {
        super(xResizer, yResizer);
        this.wResizer = wResizer;
        this.hResizer = hResizer;
        this.scrollbar = new ScrollBar(this);
        this.controller = new ScrollController(this::callbackScrollAmount);
        this.scrollList = new ScrollList<>(this);
        //this.moduleManager.addEventListener(this);
    }

    @Override
    public final void draw(float currentTime) {
        controller.update(currentTime);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * scale), (int) (minecraft.getMainWindow().getFramebufferHeight() - (bottom * scale)),
                    (int) (width * scale), (int) (height * scale));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();

        RenderSystem.enableTexture();

        scrollList.draw(getCenterX(), y, getVisibleOffset(), bottom, currentTime);

        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(right, y + borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(right, y, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(0, 0, 0, 128).endVertex();
        tessellator.draw();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, bottom, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(right, bottom, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(right, bottom - borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x, bottom - borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        tessellator.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);

        scrollbar.draw(currentTime);

        RenderSystem.enableTexture();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        drawEndExtra();
    }

    public void drawEndExtra() {

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.width = wResizer.apply(width);
        this.height = hResizer.apply(height);
        this.right = x + this.width;
        this.bottom = y + this.height;
        this.centerX = x + this.width / 2f;
        this.visibleHeight = this.height - borderThickness * 2;
        this.scrollbar.setPos(this.right - scrollbar.barThickness - 1, y + 1);
        this.scrollbar.setMaxLength(this.height - 2);
        updateScrollBarLength();
        updateScrollBarOffset(); // scrollAmount() not always call updateScrollBarOffset()
        scrollSmoothly(0);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        scrollbar.mouseMoved(mouseX, mouseY);
        if (isMouseOver(mouseX, mouseY)) {
            scrollList.mouseMoved(y, getVisibleOffset(), mouseX - getCenterX(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseClicked(y, getVisibleOffset(), bottom, mouseX - getCenterX(), mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseReleased(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseReleased(y, getVisibleOffset(), bottom, mouseX - getCenterX(), mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY)) {
            return true;
        }
        if (isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseDragged(y, getVisibleOffset(), bottom, mouseX - getCenterX(), mouseX, mouseY, mouseButton, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            if (scrollList.mouseScrolled(y, this.scrollAmount, bottom, mouseX - (x + width / 2), mouseY, scrollAmount)) {
                return true;
            }
            scrollSmoothly(Math.round(scrollAmount * -20f));
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void scrollSmoothly(float delta) {
        controller.setTargetValue(MathHelper.clamp(controller.getTargetValue() + delta, 0, getMaxScrollAmount()));
    }

    public void scrollDirectly(float delta) {
        float amount = MathHelper.clamp(controller.getTargetValue() + delta, 0, getMaxScrollAmount());
        controller.setTargetValueDirect(amount);
        callbackScrollAmount(amount);
    }

    /**
     * Controlled by scroll controller, do not call this manually
     */
    private void callbackScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        updateScrollBarOffset();
        moduleManager.refreshMouse();
    }

    /**
     * Get total entries height
     */
    public float getTotalHeight() {
        return scrollList.getMaxHeight();
    }

    public float getVisibleHeight() {
        return visibleHeight;
    }

    public float getVisibleOffset() {
        return scrollAmount - borderThickness;
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

    public float getCenterX() {
        return centerX;
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

    public void onTotalHeightChanged() {
        updateScrollBarLength();
        updateScrollBarOffset();
    }

    public void addGroup(T entry) {
        scrollList.addGroup(entry);
    }

    public void addGroups(Collection<T> collection) {
        scrollList.addGroups(collection);
    }

    public void removeGroup(T entry) {
        scrollList.removeGroup(entry);
    }

    public void clearGroup() {
        scrollList.clearGroups();
    }
}
