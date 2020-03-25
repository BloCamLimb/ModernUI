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

import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.scroll.option.OptionCategory;
import icyllis.modernui.gui.scroll.option.OptionEntry;
import icyllis.modernui.gui.scroll.option.SSliderOptionEntry;
import icyllis.modernui.gui.window.SettingScrollWindow;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.SoundCategory;

import java.util.ArrayList;
import java.util.List;

public class SettingAudio implements IGuiModule {

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    public SettingAudio() {
        Minecraft minecraft = Minecraft.getInstance();
        SettingScrollWindow window = new SettingScrollWindow();
        GameSettings gameSettings = minecraft.gameSettings;

        List<OptionEntry> list = new ArrayList<>();

        for (SoundCategory soundCategory : SoundCategory.values()) {
            SSliderOptionEntry entry = new SSliderOptionEntry(window, I18n.format("soundCategory." + soundCategory.getName()),
                    0, 1, 0.01f, gameSettings.getSoundLevel(soundCategory),
                    d -> gameSettings.setSoundLevel(soundCategory, d.floatValue()), ConstantsLibrary.PERCENTAGE_STRING_FUNC);
            list.add(entry);
        }
        OptionCategory category = new OptionCategory("Sounds", list);
        window.addGroup(category);

        elements.add(window);
        listeners.add(window);
    }

    @Override
    public List<? extends IElement> getElements() {
        return elements;
    }

    @Override
    public List<? extends IGuiEventListener> getEventListeners() {
        return listeners;
    }
}
