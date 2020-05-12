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

import icyllis.modernui.gui.master.*;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Light-weighted scroll window with fixed size, and can only use single uniform scroll group
 */
public class ScrollPanel<E extends UniformScrollEntry, G extends UniformScrollGroup<E>> extends Widget implements IScrollHost {

    @Nonnull
    protected final G group;

    protected float scrollAmount = 0f;

    protected final ScrollBar scrollbar;

    protected final ScrollController controller;

    public ScrollPanel(IHost host, @Nonnull Builder builder, @Nonnull Function<ScrollPanel<E, G>, G> group) {
        super(host, builder);
        this.group = group.apply(this);
        this.scrollbar = new ScrollBar(this);
        this.controller = new ScrollController(this::callbackScrollAmount);
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, float time) {
        controller.update(time);

        canvas.clipStart(x1, y1, width, height);

        canvas.save();
        canvas.translate(0, -getVisibleOffset());
        group.draw(canvas, time);
        canvas.restore();

        scrollbar.draw(canvas, time);

        canvas.clipEnd();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.scrollbar.setPos(this.x2 - scrollbar.barThickness - 1, y1 + 1);
        this.scrollbar.setHeight(this.height - 2);
        this.layoutList();
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            if (scrollbar.updateMouseHover(mouseX, mouseY)) {
                group.setMouseHoverExit();
            } else {
                group.updateMouseHover(mouseX, mouseY + getVisibleOffset());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return group.mouseClicked(mouseX, mouseY + getVisibleOffset(), mouseButton);
    }

    @Override
    public boolean onMouseRelease(double mouseX, double mouseY, int mouseButton) {
        if (scrollbar.mouseReleased(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return group.mouseReleased(mouseX, mouseY + getVisibleOffset(), mouseButton);
    }

    @Override
    protected boolean onMouseScrolled(double amount) {
        if (scrollbar.mouseScrolled(amount)) {
            return true;
        }
        if (group.mouseScrolled(amount)) {
            return true;
        }
        onScrollPanel(amount);
        return true;
    }

    protected void onScrollPanel(double amount) {
        scrollSmooth(Math.round(amount * -20f));
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        scrollbar.setMouseHoverExit();
        group.setMouseHoverExit();
    }

    protected void callbackScrollAmount(float scrollAmount) {
        this.scrollAmount = scrollAmount;
        updateScrollBarOffset();
        updateScrollList();
        refocusMouseCursor();
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

    @Override
    public void scrollSmooth(float delta) {
        float amount = MathHelper.clamp(controller.getTargetValue() + delta, 0, getMaxScrollAmount());
        controller.setTargetValue(amount);
    }

    @Override
    public void scrollDirect(float delta) {
        float amount = Math.round(MathHelper.clamp(controller.getTargetValue() + delta, 0, getMaxScrollAmount()));
        controller.setTargetValueDirect(amount);
        callbackScrollAmount(amount);
    }

    @Override
    public float getMaxScrollAmount() {
        return Math.max(0, group.getHeight() - getHeight());
    }

    @Override
    public float getVisibleOffset() {
        return scrollAmount;
    }

    @Override
    public float getMargin() {
        return 0;
    }

    @Override
    public void layoutList() {
        group.locate((x1 + x2) / 2f, y1);
        updateScrollBarLength();
        updateScrollBarOffset();
        updateScrollList();
        scrollSmooth(0);
    }

    public void updateScrollList() {
        group.updateVisible(y1 + getVisibleOffset(), y2 + getVisibleOffset());
    }

    public void updateScrollBarLength() {
        float v = getHeight();
        float t = group.getHeight();
        boolean renderBar = t > v;
        scrollbar.setVisible(renderBar);
        if (renderBar) {
            float p = v / t;
            scrollbar.setBarLength(p);
        }
    }

    @Override
    public int getWindowWidth() {
        return getHost().getWindowWidth();
    }

    @Override
    public int getWindowHeight() {
        return getHost().getWindowHeight();
    }

    @Override
    public double getAbsoluteMouseX() {
        return getHost().getAbsoluteMouseX();
    }

    @Override
    public double getAbsoluteMouseY() {
        return getHost().getAbsoluteMouseY();
    }

    @Override
    public double getRelativeMouseX() {
        return getHost().getRelativeMouseX();
    }

    @Override
    public double getRelativeMouseY() {
        return getHost().getRelativeMouseY() + getVisibleOffset();
    }

    @Override
    public float toAbsoluteX(float rx) {
        return getHost().toAbsoluteX(rx);
    }

    @Override
    public float toAbsoluteY(float ry) {
        return getHost().toAbsoluteY(ry) - getVisibleOffset();
    }

    @Override
    public int getElapsedTicks() {
        return getHost().getElapsedTicks();
    }

    @Override
    public void refocusMouseCursor() {
        getHost().refocusMouseCursor();
    }

    @Override
    public void setDraggable(@Nonnull IDraggable draggable) {
        getHost().setDraggable(draggable);
    }

    @Nullable
    @Override
    public IDraggable getDraggable() {
        return getHost().getDraggable();
    }

    @Override
    public void setKeyboardListener(@Nullable IKeyboardListener keyboardListener) {
        getHost().setKeyboardListener(keyboardListener);
    }

    @Nullable
    @Override
    public IKeyboardListener getKeyboardListener() {
        return getHost().getKeyboardListener();
    }
}
