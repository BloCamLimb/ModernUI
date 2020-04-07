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

import icyllis.modernui.gui.master.IMouseListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScrollList<T extends ScrollGroup> implements IMouseListener {

    private final ScrollWindow<T> window;

    private List<T> groups = new ArrayList<>();

    private List<T> visible = new ArrayList<>();

    private float maxHeight;

    public ScrollList(ScrollWindow<T> window) {
        this.window = window;
    }

    protected void updateVisible(float topY, float yOffset, float bottomY) {
        topY += yOffset;
        bottomY += yOffset;
        visible.clear();
        for (T group : groups) {
            if (group.getTop() >= bottomY) {
                break;
            } else if (group.getBottom() > topY) {
                visible.add(group);
                group.updateVisible(topY, bottomY);
            }
        }
    }

    protected void draw(float time) {
        for (T group : visible) {
            group.draw(time);
        }
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        boolean result = false;
        for (T group : visible) {
            if (!result && group.updateMouseHover(mouseX, mouseY)) {
                result = true;
            } else {
                group.setMouseHoverExit();
            }
        }
        return result;
    }

    @Override
    public void setMouseHoverExit() {
        visible.forEach(IMouseListener::setMouseHoverExit);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (T group : visible) {
            if (group.isMouseHovered() && group.mouseClicked(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
        for (T group : visible) {
            if (group.isMouseHovered() && group.mouseReleased(mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double amount) {
        for (T group : visible) {
            if (group.isMouseHovered() && group.mouseScrolled(amount)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method shouldn't be called (
     */
    @Override
    public boolean isMouseHovered() {
        return window.isMouseHovered();
    }

    protected float getMaxHeight() {
        return maxHeight;
    }

    protected void addGroups(Collection<T> collection) {
        groups.addAll(collection);
        maxHeight += collection.stream().mapToDouble(ScrollGroup::getHeight).sum();
    }

    protected void layoutGroups(float x1, float x2, float baseY) {
        float ay = baseY;
        for (T group : groups) {
            group.setPos(x1, x2, ay);
            ay += group.getHeight();
        }
        maxHeight = (float) groups.stream().mapToDouble(ScrollGroup::getHeight).sum();
    }
}
