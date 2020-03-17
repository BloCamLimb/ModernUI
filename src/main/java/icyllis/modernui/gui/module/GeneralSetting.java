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

import com.google.common.collect.Lists;
import icyllis.modernui.gui.component.option.*;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.window.SettingScrollWindow;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.OptionsScreen;

import java.util.ArrayList;
import java.util.List;

public class GeneralSetting implements IGuiModule {

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    public GeneralSetting() {
        SettingScrollWindow window = new SettingScrollWindow();
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        OptionCategory category = new OptionCategory("Game",
                new SelectiveOptionEntry(window, "Difficulty", Lists.newArrayList("Peaceful", "Easy", "Normal", "Hard"), 1),
                new BooleanOptionEntry(window, "Lock World Difficulty", false),
                new SliderOptionEntry(window, "FOV", 30, 110, (float) gameSettings.fov, 1, v -> gameSettings.fov = v));
        /*OptionCategory category1 = new OptionCategory("Rule",
                new OptionEntry("Keep Inventory"),
                new OptionEntry("Send Commands Feedback"),
                new OptionEntry("Allow Fire Spread"),
                new OptionEntry("Allow Daylight Cycle"));*/
        window.addEntry(category);
        //window.addEntry(category1);
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
