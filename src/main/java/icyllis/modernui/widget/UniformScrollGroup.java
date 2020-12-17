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

package icyllis.modernui.widget;

import icyllis.modernui.graphics.renderer.Plotter;
import icyllis.modernui.ui.discard.IScrollHost;
import icyllis.modernui.ui.discard.ScrollGroup;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains uniform entries with same height
 */
public abstract class UniformScrollGroup<T extends UniformScrollEntry> extends ScrollGroup {

    protected List<T> entries = new ArrayList<>();

    protected List<T> visible = new ArrayList<>();

    protected final int entryHeight;

    private boolean mouseHovered = false;

    public UniformScrollGroup(IScrollHost window, int entryHeight) {
        super(window);
        this.entryHeight = entryHeight;
    }

    @Override
    public void updateVisible(float top, float bottom) {
        visible.clear();
        for (T entry : entries) {
            if (entry.getTop() >= bottom) {
                break;
            } else if (entry.getBottom() > top) {
                visible.add(entry);
            }
        }
    }

    @Override
    public void draw(@Nonnull Plotter plotter, float time) {
        for (UniformScrollEntry entry : visible) {
            entry.draw(plotter, time);
        }
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        height = entries.size() * entryHeight;
        y2 = y1 + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (UniformScrollEntry entry : visible) {
            if (entry.isMouseHovered() && entry.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (UniformScrollEntry entry : visible) {
            if (entry.isMouseHovered() && entry.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (UniformScrollEntry entry : visible) {
            if (entry.isMouseHovered() && entry.mouseScrolled(, amount)) {
                return true;
            }
        }
        return false;
    }*/

    @Override
    public final boolean updateMouseHover(double mouseX, double mouseY) {
        boolean prev = mouseHovered;
        mouseHovered = isMouseInArea(mouseY);
        if (prev != mouseHovered) {
            if (mouseHovered) {
                onMouseHoverEnter(mouseX, mouseY);
            } else {
                onMouseHoverExit();
            }
        }
        if (mouseHovered) {
            boolean result = false;
            for (UniformScrollEntry entry : visible) {
                if (!result && entry.updateMouseHover(mouseX, mouseY)) {
                    result = true;
                } else {
                    entry.setMouseHoverExit();
                }
            }
        }
        return mouseHovered;
    }

    private boolean isMouseInArea(double mouseY) {
        return mouseY >= y1 && mouseY <= y2;
    }

    /*@Override
    public boolean isMouseHovered() {
        return mouseHovered;
    }

    @Override
    public final void setMouseHoverExit() {
        if (mouseHovered) {
            mouseHovered = false;
            onMouseHoverExit();
        }
    }

    protected void onMouseHoverEnter(double mouseX, double mouseY) {

    }

    protected void onMouseHoverExit() {
        visible.forEach(IMouseListener::setMouseHoverExit);
    }*/

    public List<T> getEntries() {
        return entries;
    }

    /**
     * Follow and focus an entry to make sure it's visible on scroll window
     * Fixed in 1.4.2
     *
     * @param entry entry to follow
     */
    public void followEntry(@Nonnull T entry) {
        float c = entry.getTop() - window.getTop() - window.getVisibleOffset() - window.getMargin();
        if (c < 0) {
            if (c < -240) {
                //window.getScrollController().scrollDirectBy(c);
            } else {
                //window.getScrollController().scrollSmoothBy(c);
            }
            return;
        }
        float d = entry.getBottom() - window.getBottom() - window.getVisibleOffset() + window.getMargin();
        if (d > 0) {
            if (d > 240) {
                //window.getScrollController().scrollDirectBy(d);
            } else {
                //window.getScrollController().scrollSmoothBy(d);
            }
        }
    }

}
