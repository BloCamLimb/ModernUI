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

package icyllis.modernui.gui.component.option;

import com.google.common.collect.Lists;
import icyllis.modernui.font.FontRendererTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.component.scroll.ScrollEntry;

import java.util.List;

public class OptionCategory extends ScrollEntry {

    public static int ENTRY_HEIGHT = 21;

    private IFontRenderer fontRenderer = FontRendererTools.CURRENT_RENDERER;

    private String title;

    private List<OptionEntry> entries;

    public OptionCategory(String title, OptionEntry... entries) {
        this(title, Lists.newArrayList(entries));
    }

    public OptionCategory(String title, List<OptionEntry> entries) {
        super(0);
        this.title = title;
        this.entries = entries;
        height = 36 + entries.size() * ENTRY_HEIGHT;
    }

    @Override
    public void draw(float centerX, float y, float maxY, float currentTime) {
        fontRenderer.drawString(title, centerX - 160, y + 14, 1, 1, 1, 1, 0f);
        int maxSize = Math.min((int) Math.ceil((maxY - y) / ENTRY_HEIGHT), entries.size());
        for (int i = 0; i < maxSize; i++) {
            float cy = y + 30 + i * ENTRY_HEIGHT;
            entries.get(i).draw(centerX, cy, currentTime);
        }
    }

    @Override
    public void mouseMoved(double deltaCenterX, double deltaY, double mouseX, double mouseY) {
        //ModernUI.LOGGER.info("Category Mouse Move {} {}", deltaCenterX, deltaY);
        if (deltaCenterX >= -160 && deltaCenterX <= 160) {
            double rY = deltaY - 30;
            if (rY >= 0) {
                int pIndex = (int) (rY / ENTRY_HEIGHT);
                if (pIndex < entries.size()) {
                    for (int i = 0; i < entries.size(); i++) {
                        OptionEntry entry = entries.get(i);
                        if (i == pIndex) {
                            entry.mouseMoved(deltaCenterX, rY - pIndex * ENTRY_HEIGHT, mouseX, mouseY);
                            entry.setMouseHovered(true);
                        } else {
                            entry.setMouseHovered(false);
                        }
                    }
                } else {
                    entries.forEach(e -> e.setMouseHovered(false));
                }
            } else {
                entries.forEach(e -> e.setMouseHovered(false));
            }
        } else {
            entries.forEach(e -> e.setMouseHovered(false));
        }
    }

    @Override
    public boolean mouseClicked(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        double ry = deltaY - 30;
        /*if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                if (entries.get(pIndex).mouseClicked(deltaCenterX, ry - pIndex * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }*/
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).mouseClicked(deltaCenterX, ry - i * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return super.mouseClicked(deltaCenterX, deltaY, mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        double ry = deltaY - 30;
        /*if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                if (entries.get(pIndex).mouseReleased(deltaCenterX, ry - pIndex * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                    return true;
                }
            }
        }*/
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).mouseReleased(deltaCenterX, ry - i * ENTRY_HEIGHT, mouseX, mouseY, mouseButton)) {
                return true;
            }
        }
        return super.mouseReleased(deltaCenterX, deltaY, mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton, double deltaMouseX, double deltaMouseY) {
        double ry = deltaY - 30;
        /*if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                if (entries.get(pIndex).mouseDragged(deltaCenterX, ry - pIndex * ENTRY_HEIGHT, mouseX, mouseY, mouseButton, deltaMouseX, deltaMouseY)) {
                    return true;
                }
            }
        }*/
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).mouseDragged(deltaCenterX, ry - i * ENTRY_HEIGHT, mouseX, mouseY, mouseButton, deltaMouseX, deltaMouseY)) {
                return true;
            }
        }
        return super.mouseDragged(deltaCenterX, deltaY, mouseX, mouseY, mouseButton, deltaMouseX, deltaMouseY);
    }
}
