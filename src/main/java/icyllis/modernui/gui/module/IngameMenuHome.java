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

import icyllis.modernui.gui.background.MenuHomeBG;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.IModule;
import icyllis.modernui.gui.master.ModuleGroup;
import icyllis.modernui.gui.popup.ConfirmCallback;
import icyllis.modernui.gui.popup.PopupConfirm;
import icyllis.modernui.gui.widget.MenuButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IngameMenuHome extends ModuleGroup {

    private Minecraft minecraft = Minecraft.getInstance();

    private List<MenuButton> buttons = new ArrayList<>();

    public IngameMenuHome() {
        addBackground(new MenuHomeBG());
        Consumer<MenuButton> consumer = s -> {
            addWidget(s);
            buttons.add(s);
        };
        consumer.accept(new MenuButton(w -> 8f, h -> 8f, I18n.format("gui.modernui.menu.back"), 4,
                GlobalModuleManager.INSTANCE::closeGuiScreen, -1));
        consumer.accept(new MenuButton(w -> 8f, h -> 44f, I18n.format("gui.advancements"), 1,
                () -> minecraft.displayGuiScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancementManager())), 1));
        consumer.accept(new MenuButton(w -> 8f, h -> 72f, I18n.format("gui.stats"), 2,
                () -> switchChildModule(2), 2));
        consumer.accept(new MenuButton(w -> 8f, h -> h - 92f, I18n.format("gui.modernui.menu.mods") + " (WIP)", 6,
                () -> {}, 3));
        consumer.accept(new MenuButton(w -> 8f, h -> h - 64f, I18n.format("gui.modernui.menu.settings"), 0,
                () -> switchChildModule(4), 4));
        consumer.accept(new MenuButton(w -> 8f, h -> h - 28f, I18n.format("gui.modernui.menu.exit"), 5,
                this::exit, -1));

        addChildModule(2, IngameMenuStats::new);
        addChildModule(4, IngameMenuSettings::new);

        // always draw at the top
        makeOverDraw();
    }

    @Override
    public boolean onBack() {
        if (super.onBack()) {
            return true;
        }
        if (getCid() != 0) {
            if (getCid() == 4) {
                minecraft.gameSettings.saveOptions();
            }
            switchChildModule(0);
            return true;
        }
        return false;
    }

    private void exit() {
        IModule popup = new PopupConfirm(this::confirmExit)
                .setConfirmTitle(I18n.format("gui.modernui.button.exit"))
                .setDescription(I18n.format("gui.modernui.popup.exit"));
        GlobalModuleManager.INSTANCE.openPopup(popup, true);
    }

    private void confirmExit(int callback) {
        if (minecraft.world == null || callback != ConfirmCallback.CONFIRM) {
            return;
        }
        boolean singleplayer = minecraft.isIntegratedServerRunning();
        boolean realmsConnected = minecraft.isConnectedToRealms();
        minecraft.world.sendQuittingDisconnectingPacket();
        if (singleplayer) {
            minecraft.unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
        } else {
            minecraft.unloadWorld();
        }

        if (singleplayer) {
            minecraft.displayGuiScreen(new MainMenuScreen());
        } else if (realmsConnected) {
            RealmsBridge realmsbridge = new RealmsBridge();
            realmsbridge.switchToRealms(new MainMenuScreen());
        } else {
            minecraft.displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
        }
    }

    @Override
    public void moduleChanged(int id) {
        super.moduleChanged(id);
        buttons.forEach(e -> e.onModuleChanged(id));
    }
}
