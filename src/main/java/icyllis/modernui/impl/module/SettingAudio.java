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

package icyllis.modernui.impl.module;

import com.google.common.collect.Lists;
import icyllis.modernui.gui.master.Module;
import icyllis.modernui.impl.setting.SettingCategoryGroup;
import icyllis.modernui.impl.setting.SettingEntry;
import icyllis.modernui.impl.setting.SSliderSettingEntry;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.SoundCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Really simple module (
 */
public class SettingAudio extends Module {

    public SettingAudio() {
        Minecraft minecraft = Minecraft.getInstance();
        SettingScrollWindow window = new SettingScrollWindow(this);
        GameSettings gameSettings = minecraft.gameSettings;

        List<SettingEntry> list = new ArrayList<>();

        for (SoundCategory soundCategory : SoundCategory.values()) {
            SSliderSettingEntry entry = new SSliderSettingEntry(window, I18n.format("soundCategory." + soundCategory.getName()),
                    0, 1, 0.01f, gameSettings.getSoundLevel(soundCategory),
                    d -> gameSettings.setSoundLevel(soundCategory, d.floatValue()), ConstantsLibrary.PERCENTAGE_STRING_FUNC, true);
            list.add(entry);
        }
        SettingCategoryGroup category = new SettingCategoryGroup(window, I18n.format("gui.modernui.settings.category.sounds"), list);
        window.addGroups(Lists.newArrayList(category));

        addElements(window);
        addMouseListener(window);
    }

}
