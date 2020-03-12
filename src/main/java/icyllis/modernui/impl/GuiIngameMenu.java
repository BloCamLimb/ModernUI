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

package icyllis.modernui.impl;

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.gui.element.MenuHomeBG;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.ModernUIScreen;
import icyllis.modernui.gui.widget.MenuButton;
import icyllis.modernui.impl.menu.SettingsHeader;
import icyllis.modernui.impl.menu.popup.ExitPopup;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.OptionsScreen;

import java.util.function.Consumer;

public class GuiIngameMenu extends ModernUIScreen {

    public GuiIngameMenu(boolean isFullMenu) {
        super(l -> {
            l.addModule(i -> true, Home::new);
            l.addModule(i -> i / 30 == 1, SettingsHeader::new);
            l.addPopupModule(10001, ExitPopup::new);
        });

    }

    private static class Home {

        private Minecraft minecraft;

        public Home(IModuleManager manager) {
            this.minecraft = Minecraft.getInstance();
            manager.addElement(new MenuHomeBG());
            manager.addElement(new MenuButton.A(w -> 8f, h -> 8f, "Back to Game", ReferenceLibrary.ICONS, 32, 32, 128, 0, 0.5f, () -> minecraft.displayGuiScreen(null)));
            manager.addElement(new MenuButton.B(w -> 8f, h -> 44f, "Advancements", ReferenceLibrary.ICONS, 32, 32, 32, 0, 0.5f, () -> {}, i -> i < 0));
            manager.addElement(new MenuButton.B(w -> 8f, h -> 72f, "Statistics", ReferenceLibrary.ICONS, 32, 32, 64, 0, 0.5f, () -> {}, i -> i == 1 || i == 2));
            manager.addElement(new MenuButton.B(w -> 8f, h -> h - 92f, "Forge Mods", ReferenceLibrary.ICONS, 32, 32, 192, 0, 0.5f, () -> minecraft.displayGuiScreen(new OptionsScreen(null, minecraft.gameSettings)), i -> false));
            manager.addElement(new MenuButton.B(w -> 8f, h -> h - 64f, "Settings", ReferenceLibrary.ICONS, 32, 32, 0, 0, 0.5f, () -> GlobalModuleManager.INSTANCE.switchTo(30), i -> i / 30 == 1));
            manager.addElement(new MenuButton.A(w -> 8f, h -> h - 28f, "Exit to Main Menu", ReferenceLibrary.ICONS, 32, 32, 160, 0, 0.5f, () -> GlobalModuleManager.INSTANCE.openPopup(4, 10001)));
        }

    }
}
