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
import icyllis.modernui.gui.element.MenuHomeBG;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IGuiModule;
import icyllis.modernui.gui.widget.MenuButton;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.StatsScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IngameMenuHome implements IGuiModule {

    private Minecraft minecraft;

    private List<IElement> elements = new ArrayList<>();

    private List<MenuButton> buttons = new ArrayList<>();

    public IngameMenuHome() {
        minecraft = Minecraft.getInstance();
        elements.add(new MenuHomeBG());
        Consumer<MenuButton> consumer = s -> {
            elements.add(s);
            buttons.add(s);
        };
        consumer.accept(new MenuButton.A(w -> 8f, h -> 8f, "Back to Game", ReferenceLibrary.ICONS, 32, 32, 128, 0, 0.5f, () -> minecraft.displayGuiScreen(null)));
        consumer.accept(new MenuButton.B(w -> 8f, h -> 44f, "Advancements", ReferenceLibrary.ICONS, 32, 32, 32, 0, 0.5f, () -> {}, i -> i < 0));
        consumer.accept(new MenuButton.B(w -> 8f, h -> 72f, "Statistics", ReferenceLibrary.ICONS, 32, 32, 64, 0, 0.5f, () -> minecraft.displayGuiScreen(new StatsScreen(null, minecraft.player.getStats())), i -> i == 1 || i == 2));
        consumer.accept(new MenuButton.B(w -> 8f, h -> h - 92f, "Forge Mods", ReferenceLibrary.ICONS, 32, 32, 192, 0, 0.5f, () -> minecraft.displayGuiScreen(new OptionsScreen(null, minecraft.gameSettings)), i -> false));
        consumer.accept(new MenuButton.B(w -> 8f, h -> h - 64f, "Settings", ReferenceLibrary.ICONS, 32, 32, 0, 0, 0.5f, () -> GlobalModuleManager.INSTANCE.switchModule(30), i -> i / 30 == 1));
        consumer.accept(new MenuButton.A(w -> 8f, h -> h - 28f, "Exit to Main Menu", ReferenceLibrary.ICONS, 32, 32, 160, 0, 0.5f, () -> GlobalModuleManager.INSTANCE.openPopup(new PopupExitTitle())));
    }

    @Override
    public List<IElement> getElements() {
        return elements;
    }

    @Override
    public List<? extends IGuiEventListener> getEventListeners() {
        return buttons;
    }

    @Override
    public void onModuleChanged(int newID) {
        buttons.forEach(e -> e.onModuleChanged(newID));
    }
}
