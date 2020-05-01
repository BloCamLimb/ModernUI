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

package icyllis.modernui.gui.master;

import com.google.gson.annotations.Expose;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.math.Align3V;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Almost all gui elements extends this.
 *
 * Widget has its own precise position and a rect area
 * which is used for listening mouse and keyboard events.
 *
 * All widgets should be created by Builder or Json
 *
 * @since 1.6 reworked
 */
public abstract class Widget implements IWidget {

    private final IHost host;

    @Nullable
    private final Locator locator;

    protected final Align9D align;

    protected float x1, y1;

    protected float x2, y2;

    protected float width, height;

    private WidgetStatus status = WidgetStatus.ACTIVE;

    private boolean mouseHovered = false;

    private int dClickTime = -10;

    /**
     * A mandatory alpha, for global shader uniform
     */
    private float widgetAlpha = 1.0f;

    public Widget(IHost host, @Nonnull Builder builder) {
        this.host = host;
        this.width = builder.width;
        this.height = builder.height;
        this.locator = builder.locator;
        this.align = builder.align;
    }

    @Override
    public final void draw(Canvas canvas, float time) {
        if (status.isDrawing()) {
            onDraw(canvas, time);
        }
    }

    protected abstract void onDraw(Canvas canvas, float time);

    @Override
    public void locate(float px, float py) {
        Align3H horizontalAlign = align.getAlign3H();
        switch (horizontalAlign) {
            case LEFT:
                x1 = px;
                break;
            case CENTER:
                x1 = px - width / 2f;
                break;
            case RIGHT:
                x1 = px - width;
                break;
        }
        x2 = x1 + width;

        Align3V verticalAlign = align.getAlign3V();
        switch (verticalAlign) {
            case TOP:
                y1 = py;
                break;
            case CENTER:
                y1 = py - height / 2f;
                break;
            case BOTTOM:
                y1 = py - height;
                break;
        }
        y2 = y1 + height;
    }

    @Override
    public final float getWidth() {
        return width;
    }

    @Override
    public final float getHeight() {
        return height;
    }

    @Override
    public final float getLeft() {
        return x1;
    }

    @Override
    public final float getRight() {
        return x2;
    }

    @Override
    public final float getTop() {
        return y1;
    }

    @Override
    public final float getBottom() {
        return y2;
    }

    @Override
    public void resize(int width, int height) {
        if (locator != null) {
            locator.locate(this, width, height);
        }
    }

    @Override
    public float getAlpha() {
        return widgetAlpha;
    }

    @Override
    public void setAlpha(float alpha) {
        this.widgetAlpha = alpha;
    }

    /**
     * Change status maybe creates some animations
     * But builder gives you a default status, so you
     * shouldn't call this in constructor
     */
    public void setStatus(WidgetStatus status) {
        WidgetStatus prev = this.status;
        this.status = status;
        if (prev != status) {
            onStatusChanged(status);
        }
    }

    protected void onStatusChanged(WidgetStatus status) {}

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (status.isListening()) {
            boolean prev = mouseHovered;
            mouseHovered = isMouseInArea(mouseX, mouseY);
            if (prev != mouseHovered) {
                if (mouseHovered) {
                    onMouseHoverEnter(mouseX, mouseY);
                } else {
                    onMouseHoverExit();
                }
            }
            return mouseHovered;
        }
        return false;
    }

    @Override
    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    private boolean isMouseInArea(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    @Override
    public final boolean isMouseHovered() {
        return mouseHovered;
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (status.isListening()) {
            if (onMouseClick(mouseX, mouseY, mouseButton)) {
                return true;
            }
            if (mouseButton == 0) {
                int c = getHost().getElapsedTicks();
                int d = c - dClickTime;
                dClickTime = c;
                if (d < 10) {
                    if (onMouseDoubleClick(mouseX, mouseY)) {
                        return true;
                    }
                }
                return onMouseLeftClick(mouseX, mouseY);
            } else if (mouseButton == 1) {
                return onMouseRightClick(mouseX, mouseY);
            }
        }
        return false;
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        if (status.isListening()) {
            if (onMouseRelease(mouseX, mouseY, mouseButton)) {
                return true;
            }
            if (mouseButton == 0) {
                return onMouseLeftRelease(mouseX, mouseY);
            } else if (mouseButton == 1) {
                return onMouseRightRelease(mouseX, mouseY);
            }
        }
        return false;
    }

    @Override
    public final boolean mouseScrolled(double amount) {
        if (status.isListening()) {
            return onMouseScrolled(amount);
        }
        return false;
    }

    protected boolean onMouseClick(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    protected boolean onMouseRelease(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        return false;
    }

    protected boolean onMouseLeftRelease(double mouseX, double mouseY) {
        return false;
    }

    protected boolean onMouseDoubleClick(double mouseX, double mouseY) {
        return false;
    }

    protected boolean onMouseRightClick(double mouseX, double mouseY) {
        return false;
    }

    protected boolean onMouseRightRelease(double mouseX, double mouseY) {
        return false;
    }

    protected boolean onMouseScrolled(double amount) {
        return false;
    }

    protected void onMouseHoverEnter(double mouseX, double mouseY) {}

    protected void onMouseHoverExit() {}

    public final IHost getHost() {
        return host;
    }

    public final WidgetStatus getStatus() {
        return status;
    }

    /**
     * Called if width or height changed after build
     */
    protected final void relocate() {
        if (locator != null) {
            locator.locate(this, getHost().getWindowWidth(), getHost().getWindowHeight());
        }
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
        relocate();
    }

    public void setWidth(float width) {
        this.width = width;
        relocate();
    }

    public void setHeight(float height) {
        this.height = height;
        relocate();
    }

    @Nonnull
    public abstract Class<? extends Builder> getBuilder();

    public static class Builder {

        @Expose
        protected float width = 16;

        @Expose
        protected float height = 16;

        @Expose
        protected Locator locator;

        @Expose
        protected Align9D align = Align9D.TOP_LEFT;

        public Builder() {

        }

        public Builder setWidth(float width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(float height) {
            this.height = height;
            return this;
        }

        public Builder setLocator(@Nonnull Locator locator) {
            this.locator = locator;
            return this;
        }

        public Builder setAlign(@Nonnull Align9D align) {
            this.align = align;
            return this;
        }

        @Nonnull
        public Widget build(IHost host) {
            throw new RuntimeException();
        }
    }
}
