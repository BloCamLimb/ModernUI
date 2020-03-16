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

public class ScrollList<T extends ScrollEntry> {

    private final ScrollWindow<T> window;

    private List<T> entries = new ArrayList<>();

    private float maxHeight;

    public ScrollList(ScrollWindow<T> window) {
        this.window = window;
    }

    public void draw(float centerX, float topY, float yOffset, float bottomY, float currentTime) {
        float baseY = topY - yOffset;
        int accHeight = 0;
        boolean startDraw = false;
        for (T entry : entries) {
            if (!startDraw) {
                accHeight += entry.height;
                if (accHeight >= yOffset) {
                    startDraw = true;
                }
            }
            if (startDraw) {
                entry.draw(centerX, baseY, bottomY, currentTime);
            }
            baseY += entry.height;
            if (startDraw && baseY >= bottomY) {
                break;
            }
        }
    }

    /**
     * Check if mouse is in scroll window width first!
     */
    public void mouseMoved(float topY, float yOffset, float bottomY, double rcx, double mouseY) {
        float baseY = topY - yOffset;
        boolean finish = false;
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
        }
    }

    public boolean mouseClicked(float topY, float yOffset, float bottomY, double rcx, double mouseY, int mouseButton) {
        float baseY = topY - yOffset;
        for (T entry : entries) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseClicked(rcx, mouseY - baseY, mouseButton)) {
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

    public boolean mouseReleased(float topY, float yOffset, float bottomY, double rcx, double mouseY, int mouseButton) {
        float baseY = topY - yOffset;
        for (T entry : entries) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseReleased(rcx, mouseY - baseY, mouseButton)) {
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

    public boolean mouseDragged(float topY, float yOffset, float bottomY, double rcx, double mouseY, int mouseButton, double rmx, double rmy) {
        float baseY = topY - yOffset;
        for (T entry : entries) {
            if (mouseY >= baseY && mouseY <= baseY + entry.height) {
                if (entry.mouseDragged(rcx, mouseY - baseY, mouseButton, rmx, rmy)) {
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

    public boolean mouseScrolled(float topY, float yOffset, float bottomY, double rcx, double mouseY, double scroll) {
        float baseY = topY - yOffset;
        for (T entry : entries) {
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

    public void addEntry(T entry) {
        entries.add(entry);
        maxHeight += entry.height;
        window.onTotalHeightChanged();
    }

    public void addEntries(Collection<T> collection) {
        entries.addAll(collection);
        maxHeight += collection.stream().mapToInt(t -> t.height).sum();
        window.onTotalHeightChanged();
    }

    public void removeEntry(T entry) {
        if (entries.remove(entry)) {
            maxHeight -= entry.height;
        }
        window.onTotalHeightChanged();
    }

    public void clearEntries() {
        entries.clear();
        maxHeight = 0;
        window.onTotalHeightChanged();
    }
}
