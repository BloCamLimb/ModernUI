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

import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.IMouseListener;
import icyllis.modernui.system.ModernUI;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains uniform entries with same height
 */
public abstract class UniformScrollGroup<T extends UniformScrollEntry> extends ScrollGroup {

    protected List<T> entries = new ArrayList<>();

    private List<T> visible = new ArrayList<>();

    protected final int entryHeight;

    public UniformScrollGroup(ScrollWindow<?> window, int entryHeight) {
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
    public void draw(Canvas canvas, float time) {
        for (UniformScrollEntry entry : visible) {
            entry.draw(canvas, time);
        }
    }

    @Override
    public void drawForegroundLayer(Canvas canvas, float mouseX, float mouseY, float time) {
        for (UniformScrollEntry entry : visible) {
            entry.drawForegroundLayer(canvas, mouseX, mouseY, time);
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            boolean result = false;
            for (UniformScrollEntry entry : visible) {
                if (!result && entry.updateMouseHover(mouseX, mouseY)) {
                    result = true;
                } else {
                    entry.setMouseHoverExit();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (UniformScrollEntry entry : visible) {
            if (entry.isMouseHovered() && entry.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (UniformScrollEntry entry : visible) {
            if (entry.isMouseHovered() && entry.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double amount) {
        for (UniformScrollEntry entry : visible) {
            if (entry.isMouseHovered() && entry.mouseScrolled(amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        visible.forEach(IMouseListener::setMouseHoverExit);
    }

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
        float c = entry.getTop() - window.getTop() - window.getVisibleOffset() - window.borderThickness;
        if (c < 0) {
            if (c < -240) {
                window.scrollDirect(c);
            } else {
                window.scrollSmooth(c);
            }
            return;
        }
        float d = entry.getBottom() - window.getBottom() - window.getVisibleOffset() + window.borderThickness;
        if (d > 0) {
            if (d > 240) {
                window.scrollDirect(d);
            } else {
                window.scrollSmooth(d);
            }
        }
    }

}
