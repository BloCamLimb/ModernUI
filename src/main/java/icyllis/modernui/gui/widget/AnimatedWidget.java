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

import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.master.IWidget;

public abstract class AnimatedWidget extends AnimatedElement implements IWidget {

    protected IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    protected float x1, y1;

    protected float x2, y2;

    protected boolean listening = true;

    protected boolean mouseHovered = false;

    protected WidgetArea.Rect area;

    public AnimatedWidget() {

    }

    public AnimatedWidget(WidgetArea.Rect area) {
        this.area = area;
    }

    @Override
    public final void setPos(float x, float y) {
        this.x1 = x;
        this.x2 = x + area.getWidth();
        this.y1 = y;
        this.y2 = y + area.getHeight();
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
    public final float getWidth() {
        return area.getWidth();
    }

    @Override
    public final float getHeight() {
        return area.getHeight();
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (listening) {
            boolean prev = mouseHovered;
            mouseHovered = area.isMouseInArea(x1, y1, mouseX, mouseY);
            if (prev != mouseHovered) {
                if (mouseHovered) {
                    onMouseHoverEnter();
                } else {
                    onMouseHoverExit();
                }
            }
            return mouseHovered;
        }
        return false;
    }

    @Override
    public final boolean isMouseHovered() {
        return mouseHovered;
    }

    protected void onMouseHoverEnter() {
        this.startOpenAnimation();
    }

    protected void onMouseHoverExit() {
        this.startCloseAnimation();
    }
}
