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

import icyllis.modernui.gui.master.Module;
import icyllis.modernui.gui.scroll.ScrollWindow;
import icyllis.modernui.impl.setting.SettingCategoryGroup;

public class SettingScrollWindow extends ScrollWindow<SettingCategoryGroup> {

    public SettingScrollWindow(Module module) {
        super(module, w -> 40f, h -> 36f, w -> w - 80f, h -> h - 72f);
    }
}
