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

package icyllis.modernui.test.discard;

import com.google.gson.annotations.Expose;
import icyllis.modernui.graphics.renderer.Canvas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Almost all gui elements extends this.
 * <p>
 * Widget has its own precise position and a rect area
 * which is used for listening mouse and keyboard events.
 *
 * @since 1.5 reworked
 */
@Deprecated
public abstract class Widget implements IWidget {

    @Nonnull
    private final IHost host;

    @Nullable
    private final Locator locator;

    /**
     * read-only in onDraw, or write-only in the constructor
     */
    @Nonnull
    protected Align9D align;

    /**
     * read-only in onDraw
     */
    protected float x1, y1;

    /**
     * read-only in onDraw
     */
    protected float x2, y2;

    /**
     * read-only in onDraw, or write-only in the constructor
     */
    protected float width, height;

    private WidgetStatus status = WidgetStatus.ACTIVE;

    private boolean mouseHovered = false;

    private int dClickTime = -10;

    /**
     * A mandatory alpha, for global shader uniform
     */
    private float systemAlpha = 1.0f;

    public Widget(@Nonnull IHost host, @Nonnull Builder builder) {
        this.host = host;
        this.width = builder.width;
        this.height = builder.height;
        this.locator = builder.locator;
        this.align = builder.align;
    }

    @Override
    public final void draw(@Nonnull Canvas canvas, float time) {
        if (status.isDrawing()) {
            onDraw(canvas, time);
        }
    }

    protected abstract void onDraw(@Nonnull Canvas canvas, float time);

    /**
     * Set widget position
     *
     * @param px pivot x pos
     * @param py pivot y pos
     */
    public void locate(float px, float py) {
        x1 = align.getAlignedX(px, -width);
        x2 = x1 + width;

        y1 = align.getAlignedY(py, -height);
        y2 = y1 + height;
    }

    /**
     * Get current locator
     * It's Nullable because not all widgets are supported
     *
     * @return Current locator
     */
    @Nullable
    public Locator getLocator() {
        return locator;
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

    @Deprecated
    public void resize(int width, int height) {
        if (locator != null) {
            locator.locate(this, width, height);
        }
    }

    /*@Override
    public final float getAlpha() {
        return systemAlpha;
    }

    @Override
    public final void setAlpha(float alpha) {
        this.systemAlpha = alpha;
    }*/

    /**
     * Change status maybe creates some animations
     * But builder gives you a default status, so you
     * shouldn't call this in constructor
     */
    public void setStatus(WidgetStatus status, boolean allowAnimation) {
        if (this.status != status) {
            this.status = status;
            onStatusChanged(status, allowAnimation);
        }
    }

    protected void onStatusChanged(WidgetStatus status, boolean allowAnimation) {
    }

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

    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    private boolean isMouseInArea(double mouseX, double mouseY) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    public final boolean isMouseHovered() {
        return mouseHovered;
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (status.isListening()) {
            if (dispatchMouseClick(mouseX, mouseY, button)) {
                return true;
            }
            if (button == 0) {
                int c = getParent().getElapsedTicks();
                int d = c - dClickTime;
                dClickTime = c;
                if (d < 10) {
                    if (onMouseDoubleClick(mouseX, mouseY)) {
                        dClickTime = -10;
                        return true;
                    }
                }
                return onMouseLeftClick(mouseX, mouseY);
            } else if (button == 1) {
                return onMouseRightClick(mouseX, mouseY);
            }
        }
        return false;
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (status.isListening()) {
            if (dispatchMouseRelease(mouseX, mouseY, button)) {
                return true;
            }
            if (button == 0) {
                return onMouseLeftRelease(mouseX, mouseY);
            } else if (button == 1) {
                return onMouseRightRelease(mouseX, mouseY);
            }
        }
        return false;
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (status.isListening()) {
            return onMouseScrolled(amount);
        }
        return false;
    }

    protected boolean dispatchMouseClick(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    protected boolean dispatchMouseRelease(double mouseX, double mouseY, int mouseButton) {
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

    /**
     * Called when widget is listening and mouse hovered in area
     */
    public void onMouseHoverEnter(double mouseX, double mouseY) {
    }

    /**
     * Called whenever widget is not mouse hovered
     */
    public void onMouseHoverExit() {
        dClickTime = -10;
    }

    @Nonnull
    public final IHost getParent() {
        return host;
    }

    public final WidgetStatus getStatus() {
        return status;
    }

    /**
     * Called if width or height changed after build
     */
    public final void relocate() {
        resize(getParent().getGameWidth(), getParent().getGameHeight());
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

    public void setAlign(@Nonnull Align9D align) {
        this.align = align;
        relocate();
    }

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
