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

package icyllis.modernui.gui.component.scroll;

import icyllis.modernui.gui.window.ScrollWindow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScrollList<T extends ScrollGroup> {

    private final ScrollWindow<T> window;

    private List<T> groups = new ArrayList<>();

    private List<T> visible = new ArrayList<>();

    private float maxHeight;

    public ScrollList(ScrollWindow<T> window) {
        this.window = window;
    }

    public void draw(float centerX, float topY, float yOffset, float bottomY, float currentTime) {
        float baseY = topY - yOffset;
        float maxHeight = bottomY - baseY;
        boolean startDraw = false;
        float accHeight;
        visible.clear();
        for (T entry : groups) {
            if (!startDraw) {
                accHeight = entry.height + entry.lastHeight;
                if (accHeight >= yOffset) {
                    startDraw = true;
                }
            }
            if (startDraw) {
                if (entry.lastHeight >= maxHeight) {
                    break;
                } else {
                    visible.add(entry);
                }
            }
        }
        for (T entry : visible) {
            entry.draw(centerX, baseY + entry.lastHeight, bottomY, currentTime);
        }
    }

    /**
     * Check if mouse is in scroll window width first!
     */
    public void mouseMoved(float topY, float yOffset, double deltaCenterX, double mouseX, double mouseY) {
        float baseY = topY - yOffset;
        double rMouseY = mouseY - baseY;
        /*boolean finish = false;
        for (T entry : entries) {
            if (!finish) {
                if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                    entry.mouseMoved(rcx, mouseY - baseY);
                    entry.setMouseHovered(true);
                    finish = true;
                } else {
                    baseY += entry.height;
                    entry.setMouseHovered(false);
                }
            } else {
                entry.setMouseHovered(false);
            }
        }*/
        visible.forEach(e -> e.mouseMoved(deltaCenterX, rMouseY - e.lastHeight, mouseX, mouseY));
    }

    public boolean mouseClicked(float topY, float yOffset, float bottomY, double deltaCenterX, double mouseX, double mouseY, int mouseButton) {
        float baseY = topY - yOffset;
        for (T entry : groups) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseClicked(deltaCenterX, mouseY - baseY, mouseX, mouseY, mouseButton)) {
                    return true;
                }
                break;
            } else {
                baseY += entry.height;
            }
            if (baseY >= bottomY) {
                break;
            }
        }
        return false;
    }

    public boolean mouseReleased(float topY, float yOffset, float bottomY, double deltaCenterX, double mouseX, double mouseY, int mouseButton) {
        float baseY = topY - yOffset;
        double rMouseY = mouseY - baseY;
        /*for (T entry : entries) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseReleased(rcx, mouseY - baseY, mouseX, mouseY, mouseButton)) {
                    return true;
                }
                break;
            } else {
                baseY += entry.height;
            }
            if (baseY >= bottomY) {
                break;
            }
        }*/
        for (T entry : visible) {
            if (entry.mouseReleased(deltaCenterX, rMouseY - entry.lastHeight, mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(float topY, float yOffset, float bottomY, double deltaCenterX, double mouseX, double mouseY, int mouseButton, double deltaX, double deltaY) {
        float baseY = topY - yOffset;
        double rMouseY = mouseY - baseY;
        /*for (T entry : entries) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseDragged(deltaCenterX, mouseY - baseY, mouseX, mouseY, mouseButton, deltaX, deltaY)) {
                    return true;
                }
                break;
            } else {
                baseY += entry.height;
            }
            if (baseY >= bottomY) {
                break;
            }
        }*/
        for (T entry : visible) {
            if (entry.mouseDragged(deltaCenterX, rMouseY - entry.lastHeight, mouseX, mouseY, mouseButton, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(float topY, float yOffset, float bottomY, double rcx, double mouseY, double scroll) {
        float baseY = topY - yOffset;
        for (T entry : groups) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseScrolled(rcx, mouseY - baseY, scroll)) {
                    return true;
                }
                break;
            } else {
                baseY += entry.height;
            }
            if (baseY >= bottomY) {
                break;
            }
        }
        return false;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public void addGroup(T entry) {
        groups.add(entry);
        maxHeight += entry.height;
        resizeGroups();
        window.onTotalHeightChanged();
    }

    public void addGroups(Collection<T> collection) {
        groups.addAll(collection);
        maxHeight += collection.stream().mapToInt(t -> t.height).sum();
        resizeGroups();
        window.onTotalHeightChanged();
    }

    public void removeGroup(T entry) {
        if (groups.remove(entry)) {
            maxHeight -= entry.height;
        }
        resizeGroups();
        window.onTotalHeightChanged();
    }

    private void resizeGroups() {
        groups.forEach(e -> e.lastHeight = 0);
        int size = groups.size();
        if (size > 1) {
            for (int i = 1; i < size; i++) {
                T entry = groups.get(i);
                T lastEntry = groups.get(i - 1);
                entry.lastHeight = lastEntry.height + lastEntry.lastHeight;
            }
        }
    }

    public void clearGroups() {
        groups.clear();
        maxHeight = 0;
        window.onTotalHeightChanged();
    }
}
