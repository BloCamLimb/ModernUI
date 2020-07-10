/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.ui.master.View;
import icyllis.modernui.ui.widget.Scroller;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.Function;

@Deprecated
public class ScrollWindow<T extends ScrollGroup> extends Window implements IScrollHost {

    protected final int borderThickness = 6;

    protected float centerX;

    protected float visibleHeight;

    protected float scrollAmount = 0f;

    protected View.ScrollBar scrollbar;

    protected Scroller controller;

    protected ScrollList<T> scrollList;

    public ScrollWindow(Module module, Function<Integer, Float> xResizer, Function<Integer, Float> yResizer, Function<Integer, Float> wResizer, Function<Integer, Float> hResizer) {
        super(module, xResizer, yResizer, wResizer, hResizer);
        /*this.scrollbar = new ScrollBar(this);
        this.controller = new ScrollController(this);
        this.scrollList = new ScrollList<>(this);*/
    }

    @Override
    public final void draw(@Nonnull Canvas canvas, float time) {
        //controller.update(time);

        /* For horizontal transition animation */
        canvas.clipStart(0, y1, getGameWidth(), height);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();

        RenderSystem.enableTexture();

        canvas.save();
        canvas.translate(0, -getVisibleOffset());
        scrollList.draw(canvas, time);
        canvas.restore();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

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

        //scrollbar.draw(canvas, time);

        RenderSystem.enableTexture();

        canvas.clipEnd();
    }

    /*@Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.centerX = x1 + this.width / 2f;
        this.visibleHeight = this.height - borderThickness * 2;
        this.scrollbar.setPos(this.x2 - scrollbar.barThickness - 1, y1 + 1);
        this.scrollbar.setHeight(this.height - 2);
        this.layoutList();
        // Am I cute?
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            if (scrollbar.updateMouseHover(mouseX, mouseY)) {
                scrollList.setMouseHoverExit();
            } else {
                scrollList.updateMouseHover(mouseX, mouseY + getVisibleOffset());
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        scrollbar.setMouseHoverExit();
        scrollList.setMouseHoverExit();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return scrollList.mouseClicked(mouseX, mouseY + getVisibleOffset(), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return scrollList.mouseReleased(mouseX, mouseY + getVisibleOffset(), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (scrollbar.mouseScrolled(, amount)) {
            return true;
        }
        if (scrollList.mouseScrolled(, amount)) {
            return true;
        }
        controller.scrollSmoothBy(Math.round(amount * -20f));
        return true;
    }*/

    @Override
    public double getRelativeMouseY() {
        return super.getRelativeMouseY() + getVisibleOffset();
    }

    @Override
    public float toAbsoluteY(float ry) {
        return super.toAbsoluteY(ry) - getVisibleOffset();
    }

    /**
     * Controlled by scroll controller, do not call this manually
     */
    @Override
    public void callbackScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        updateScrollBarOffset();
        scrollList.updateVisible(y1, getVisibleOffset(), y2);
        refocusMouseCursor();
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

    /**
     * Get scroll amount for drawing
     */
    @Override
    public float getVisibleOffset() {
        return scrollAmount - borderThickness;
    }

    @Override
    public float getMargin() {
        return borderThickness;
    }

    @Override
    public float getMaxScrollAmount() {
        return Math.max(0, getTotalHeight() - getVisibleHeight());
    }

    @Override
    public Scroller getScrollController() {
        return controller;
    }

    public float getScrollPercentage() {
        float max = getMaxScrollAmount();
        if (max == 0) {
            return 0;
        }
        return scrollAmount / max;
    }

    private void updateScrollBarOffset() {
        //scrollbar.setBarOffset(getScrollPercentage());
    }

    public void updateScrollBarLength() {
        float v = getVisibleHeight();
        float t = getTotalHeight();
        boolean renderBar = t > v;
        //scrollbar.setVisibility(renderBar ? View.VISIBLE : View.INVISIBLE);
        if (renderBar) {
            float p = v / t;
            //scrollbar.setBarLength(p);
        }
    }

    @Override
    public void layoutList() {
        scrollList.layoutGroups(centerX, getTop());
        updateScrollBarLength();
        // update all scroll data
        //controller.scrollDirectBy(0);
    }

    public void addGroups(Collection<T> collection) {
        scrollList.addGroups(collection);
    }

}
