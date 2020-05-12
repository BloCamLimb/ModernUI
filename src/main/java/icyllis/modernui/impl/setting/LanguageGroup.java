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

package icyllis.modernui.impl.setting;

import icyllis.modernui.impl.module.SettingLanguage;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.gui.scroll.UniformScrollGroup;
import net.minecraft.client.resources.LanguageManager;

public class LanguageGroup extends UniformScrollGroup<LanguageEntry> {

    public static int ENTRY_HEIGHT = 18;

    private final SettingLanguage module;

    public LanguageGroup(SettingLanguage module, ScrollWindow<?> window, LanguageManager manager) {
        super(window, ENTRY_HEIGHT);
        this.module = module;
        manager.getLanguages().forEach(l -> {
            LanguageEntry entry = new LanguageEntry(module, window, l);
            entries.add(entry);
            if (l.getCode().equals(manager.getCurrentLanguage().getCode())) {
                module.setHighlight(entry);
            }
        });
    }

    @Override
    public void locate(float px, float py) {
        super.locate(px, py);
        int i = 0;
        for (LanguageEntry entry : entries) {
            float cy = py + i * entryHeight;
            entry.locate(px, cy);
            i++;
        }

        followEntry(module.getHighlight());
    }

    /*@Override
    public void onLayout(float left, float right, float y) {
        super.onLayout(left, right, y);
        left = centerX - 120;
        right = centerX + 120;
        int i = 0;
        for (LanguageEntry entry : entries) {
            float cy = y + i * entryHeight;
            entry.onLayout(left, right, cy);
            i++;
        }

        followEntry(module.getHighlight());
    }*/

}
