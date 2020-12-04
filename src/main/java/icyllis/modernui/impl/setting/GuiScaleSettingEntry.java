/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.setting;

import com.google.common.collect.Lists;
import icyllis.modernui.system.MuiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiScaleSettingEntry extends DropdownSettingEntry {

    public GuiScaleSettingEntry(SettingScrollWindow window) {
        super(window, I18n.format("options.guiScale"), Lists.newArrayList(""), 0, i -> {});
        this.saveOption = i -> {
            Minecraft.getInstance().gameSettings.guiScale = i == 0 ? 0 :
                    Integer.parseInt(this.optionNames.get(i).replace("x", ""));
            Minecraft.getInstance().updateWindowSize();
        };
    }

    public void onResized() {
        int r = MuiHooks.C.calcGuiScales();
        int min = r >> 8 & 0xf;
        int optimal = r >> 4 & 0xf;
        int max = r & 0xf;
        optionNames.clear();
        optionNames.add(I18n.format("options.guiScale.auto") + " (" + optimal + "x)");
        for (int i = min; i <= max; i++) {
            optionNames.add(i + "x");
        }
        boolean auto = Minecraft.getInstance().gameSettings.guiScale == 0;
        int c = (int) Minecraft.getInstance().getMainWindow().getGuiScaleFactor();
        lastOptionIndex = auto ? 0 : optionNames.indexOf(c + "x");
        updateValue(lastOptionIndex);
    }
}
