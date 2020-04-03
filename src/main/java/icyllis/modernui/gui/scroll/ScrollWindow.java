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

package icyllis.modernui.gui.scroll;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.trash.Element;
import icyllis.modernui.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.function.Function;

public class ScrollWindow<T extends ScrollGroup> extends Widget {

    public int borderThickness = 6;

    private Minecraft minecraft;

    protected Function<Integer, Float> xResizer, yResizer;

    protected Function<Integer, Float> wResizer, hResizer;

    protected float width, height;

    protected float centerX;

    protected float visibleHeight; // except black fade out border * 2

    protected float scrollAmount = 0f; // scroll offset, > 0

    private boolean mouseMoving = false;

    protected ScrollBar scrollbar;

    protected ScrollController controller;

    protected ScrollList<T> scrollList;

    public ScrollWindow(Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, Function<Integer, Float> wResizer, Function<Integer, Float> hResizer) {
        this.minecraft = Minecraft.getInstance();
        this.xResizer = xResizer;
        this.yResizer = yResizer;
        this.wResizer = wResizer;
        this.hResizer = hResizer;
        this.scrollbar = new ScrollBar(this);
        this.controller = new ScrollController(this::callbackScrollAmount);
        this.scrollList = new ScrollList<>(this);
    }

    @Override
    public final void draw(float time) {
        controller.update(time);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x1 * scale), (int) (minecraft.getMainWindow().getFramebufferHeight() - (y2 * scale)),
                    (int) (width * scale), (int) (height * scale));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();

        RenderSystem.enableTexture();

        scrollList.draw(time);

        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, y1 + borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x2, y1 + borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x2, y1, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x1, y1, 0.0D).color(0, 0, 0, 128).endVertex();
        tessellator.draw();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, y2, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x2, y2, 0.0D).color(0, 0, 0, 128).endVertex();
        bufferBuilder.pos(x2, y2 - borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        bufferBuilder.pos(x1, y2 - borderThickness, 0.0D).color(0, 0, 0, 0).endVertex();
        tessellator.draw();
        RenderSystem.shadeModel(GL11.GL_FLAT);

        scrollbar.draw(time);

        RenderSystem.enableTexture();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        drawEndExtra();
    }

    public void drawEndExtra() {

    }

    @Override
    public void setPos(float x, float y) {
        throw new RuntimeException("Scroll window doesn't allow to set pos");
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.x1 = xResizer.apply(width);
        this.y1 = yResizer.apply(height);
        this.width = wResizer.apply(width);
        this.height = hResizer.apply(height);
        this.x2 = x1 + this.width;
        this.y2 = y1 + this.height;
        this.centerX = x1 + this.width / 2f;
        this.visibleHeight = this.height - borderThickness * 2;
        this.scrollbar.setPos(this.x2 - scrollbar.barThickness - 1, y1 + 1);
        this.scrollbar.setMaxLength(this.height - 2);
        updateScrollBarLength();
        updateScrollBarOffset(); // scrollSmoothly() not always call updateScrollBarOffset()
        updateScrollList();
        scrollSmoothly(0);
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        mouseHovered = isMouseOver(mouseX, mouseY);
        scrollbar.mouseMoved(mouseX, mouseY);
        if (mouseHovered) {
            scrollList.mouseMoved(mouseX, mouseY);
            mouseMoving = true;
        } else if (mouseMoving) {
            scrollList.mouseMoved(mouseX, -1);
            mouseMoving = false;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseReleased(mouseX, mouseY, mouseButton)) {
            return true;
        }
        if (isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseReleased(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        if (scrollbar.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY)) {
            return true;
        }
        if (isMouseOver(mouseX, mouseY)) {
            return scrollList.mouseDragged(mouseX, mouseY, mouseButton, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            if (scrollList.mouseScrolled(mouseX, mouseY, scrollAmount)) {
                return true;
            }
            scrollSmoothly(Math.round(scrollAmount * -20f));
            return true;
        }
        return false;
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public void scrollSmoothly(float delta) {
        float amount = MathHelper.clamp(controller.getTargetValue() + delta, 0, getMaxScrollAmount());
        controller.setTargetValue(amount);
    }

    public void scrollDirectly(float delta) {
        float amount = Math.round(MathHelper.clamp(controller.getTargetValue() + delta, 0, getMaxScrollAmount()));
        controller.setTargetValueDirect(amount);
        callbackScrollAmount(amount);
    }

    /**
     * Controlled by scroll controller, do not call this manually
     */
    private void callbackScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        updateScrollBarOffset();
        updateScrollList();
        GlobalModuleManager.INSTANCE.refreshMouse();
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

    public void updateScrollList() {
        scrollList.updateVisible(getCenterX(), y1, getVisibleOffset(), y2);
    }

    public void onTotalHeightChanged() {
        updateScrollBarLength();
        updateScrollBarOffset();
        updateScrollList();
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
