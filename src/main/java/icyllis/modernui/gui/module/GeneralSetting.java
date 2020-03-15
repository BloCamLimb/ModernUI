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

package icyllis.modernui.gui.module;

import icyllis.modernui.gui.component.ScrollWindow;
import icyllis.modernui.gui.component.scroll.OptionCategory;
import icyllis.modernui.gui.component.scroll.OptionEntry;
import icyllis.modernui.gui.master.IGuiModule;

public class GeneralSetting implements IGuiModule {

    private ScrollWindow<OptionCategory> window;

    public GeneralSetting() {
        window = new ScrollWindow<>(w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);
        OptionCategory category = new OptionCategory("Game",
                new OptionEntry("Difficulty"),
                new OptionEntry("Lock Difficulty"),
                new OptionEntry("Enable PVP"));
        OptionCategory category1 = new OptionCategory("Rule",
                new OptionEntry("Keep Inventory"),
                new OptionEntry("Send Commands Feedback"),
                new OptionEntry("Allow Fire Spread"),
                new OptionEntry("Allow Daylight Cycle"));
        window.addEntry(category);
        window.addEntry(category1);
    }

    @Override
    public void draw(float currentTime) {
        window.draw(currentTime);
    }

    @Override
    public void resize(int width, int height) {
        window.resize(width, height);
    }
}
