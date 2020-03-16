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

import icyllis.modernui.gui.component.scroll.ScrollEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptionCategory extends ScrollEntry {

    public static int ENTRY_HEIGHT = 21;

    private String title;

    private List<OptionEntry<?>> entries = new ArrayList<>();

    public OptionCategory(String title, OptionEntry<?>... entries) {
        super(0);
        this.title = title;
        Collections.addAll(this.entries, entries);
        height = 36 + entries.length * ENTRY_HEIGHT;
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
    public void mouseMoved(double rcx, double rty) {
        double ry = rty - 30;
        if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                for (int i = 0; i < entries.size(); i++) {
                    OptionEntry<?> entry = entries.get(i);
                    if (i == pIndex) {
                        entry.setMouseHovered(true);
                    } else {
                        entry.setMouseHovered(false);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double rcx, double rty, int mouseButton) {
        double ry = rty - 30;
        if (ry >= 0) {
            int pIndex = (int) (ry / ENTRY_HEIGHT);
            if (pIndex >= 0 && pIndex < entries.size()) {
                if (entries.get(pIndex).mouseClicked(rcx, ry - pIndex * ENTRY_HEIGHT, mouseButton)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(rcx, rty, mouseButton);
    }
}
