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
import icyllis.modernui.gui.component.option.MenuOptionEntry;
import icyllis.modernui.gui.component.option.OptionCategory;
import icyllis.modernui.gui.component.option.OptionEntry;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.window.SettingScrollWindow;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.resources.I18n;

import java.util.ArrayList;
import java.util.List;

public class SettingVideo implements IGuiModule {

    private Minecraft minecraft;

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    private SettingScrollWindow window;

    public SettingVideo() {
        this.minecraft = Minecraft.getInstance();
        this.window = new SettingScrollWindow();
        addGameCategory();
        elements.add(window);
        listeners.add(window);
    }

    private void addGameCategory() {
        List<OptionEntry> list = new ArrayList<>();
        GameSettings gameSettings = minecraft.gameSettings;

        list.add(new MenuOptionEntry(window, I18n.format("options.graphics"),
                        Lists.newArrayList(I18n.format("options.graphics.fancy"), I18n.format("options.graphics.fast")),
                        gameSettings.fancyGraphics ? 0 : 1, i -> {
            gameSettings.fancyGraphics = i == 0;
            minecraft.worldRenderer.loadRenderers();
        }));

        OptionCategory category = new OptionCategory("Video", list);
        window.addGroup(category);
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
