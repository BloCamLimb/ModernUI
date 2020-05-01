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

import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.math.Locator;
import icyllis.modernui.impl.background.MenuHomeBG;
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
import net.minecraftforge.fml.client.gui.screen.ModListScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

public class IngameMenuHome extends ModuleGroup {

    private Minecraft minecraft = Minecraft.getInstance();

    private List<MenuButton> buttons = new ArrayList<>();

    // true = right, false = left
    private boolean moduleTransitionDirection = true;

    private Random random = new Random();

    public IngameMenuHome() {
        // add background layer
        addDrawable(new MenuHomeBG());

        Consumer<MenuButton> consumer = s -> {
            addWidget(s);
            buttons.add(s);
        };
        consumer.accept(
                new MenuButton.Builder(I18n.format("gui.modernui.menu.back"), 4, -1)
                        .setLocator(new Locator(Align9D.TOP_LEFT, 8, 8))
                        .build(this)
                        .buildCallback(true, GlobalModuleManager.INSTANCE::closeGuiScreen, false)
        );
        consumer.accept(
                new MenuButton.Builder(I18n.format("gui.advancements") + " (WIP)", 1, 1)
                        .setLocator(new Locator(Align9D.TOP_LEFT, 8, 44))
                        .build(this)
                        .buildCallback(true,
                                () -> minecraft.displayGuiScreen(
                                        new AdvancementsScreen(Objects.requireNonNull(minecraft.player).connection.getAdvancementManager())),
                                false)
        );
        consumer.accept(
                new MenuButton.Builder(I18n.format("gui.stats"), 2, 2)
                        .setLocator(new Locator(Align9D.TOP_LEFT, 8, 72))
                        .build(this)
                        .buildCallback(true,
                                () -> switchChildModule(2),
                                false)
        );
        consumer.accept(
                new MenuButton.Builder(I18n.format("gui.modernui.menu.mods"), 6, 3)
                        .setLocator(new Locator(Align9D.BOTTOM_LEFT, 8, -92))
                        .build(this)
                        .buildCallback(true,
                                () -> minecraft.displayGuiScreen(new ModListScreen(null)),
                                false)
        ); // Forge's GUI is buggy, so null
        consumer.accept(
                new MenuButton.Builder(I18n.format("gui.modernui.menu.settings"), 0, 4)
                        .setLocator(new Locator(Align9D.BOTTOM_LEFT, 8, -64))
                        .build(this)
                        .buildCallback(true,
                                () -> switchChildModule(4),
                                false)
        );
        consumer.accept(
                new MenuButton.Builder(I18n.format("gui.modernui.menu.exit"), 5, -1)
                        .setLocator(new Locator(Align9D.BOTTOM_LEFT, 8, -28))
                        .build(this)
                        .buildCallback(true, this::exitToTitle, false)
        );

        // advancements
        addChildModule(2, () -> new IngameMenuStats(this));
        // mod list
        addChildModule(4, () -> new IngameMenuSettings(this));

        // always draw at the top
        makeOverDraw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    /**
     * Get transition animation direction
     * @param constr is in constructor
     */
    public boolean getTransitionDirection(boolean constr) {
        boolean b = moduleTransitionDirection;
        if (constr) {
            moduleTransitionDirection = random.nextBoolean();
        }
        return b;
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

    private void exitToTitle() {
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
    public void onChildModuleChanged(int id) {
        super.onChildModuleChanged(id);
        buttons.forEach(e -> e.onModuleChanged(id));
    }
}
