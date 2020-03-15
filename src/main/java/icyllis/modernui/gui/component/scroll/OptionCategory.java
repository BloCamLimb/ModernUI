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

import icyllis.modernui.gui.master.DrawTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptionCategory extends ScrollEntry {

    private String title;

    private List<OptionEntry> entries = new ArrayList<>();

    public OptionCategory(String title, OptionEntry... entries) {
        super(0);
        this.title = title;
        Collections.addAll(this.entries, entries);
        height = 36 + entries.length * 25;
    }

    @Override
    public void draw(float centerX, float y, float maxY, float currentTime) {
        fontRenderer.drawString(title, centerX - 160, y + 14, 1, 1, 1, 1, 0f);
        int maxSize = Math.min((int) Math.ceil((maxY - y) / 25), entries.size());
        for (int i = 0; i < maxSize; i++) {
            float cy = y + 30 + i * 25;
            entries.get(i).draw(centerX, cy, maxY, currentTime);
            DrawTools.fillRectWithColor(centerX - 160, cy + 24.49f, centerX + 160, cy + 25, 0.55f, 0.55f, 0.55f, 0.9f);
        }
    }
}
