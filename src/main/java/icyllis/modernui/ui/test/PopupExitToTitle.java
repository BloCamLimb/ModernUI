/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

@Deprecated
public class PopupExitToTitle implements IModule {

    /*private Minecraft minecraft;

    private List<IElement> elements = new ArrayList<>();

    private List<IGuiEventListener> listeners = new ArrayList<>();

    public PopupExitToTitle() {
        this.minecraft = Minecraft.getInstance();
        elements.add(new Background(4));
        Consumer<IGuiEventListener> consumer = s -> listeners.add(s);
        elements.add(new ConfirmWindow(consumer, "Confirm Exit", "Are you sure you want to exit to main menu?", "Exit", this::exit));
    }

    private void exit(boolean exit) {
        GlobalModuleManager.INSTANCE.closePopup();
        if (minecraft.world == null || !exit) {
            return;
        }
        boolean singlePlayer = minecraft.isIntegratedServerRunning();
        boolean realmsConnected = minecraft.isConnectedToRealms();
        minecraft.world.sendQuittingDisconnectingPacket();
        if (singlePlayer) {
            minecraft.unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
        } else {
            minecraft.unloadWorld();
        }

        if (singlePlayer) {
            minecraft.displayGuiScreen(new MainMenuScreen());
        } else if (realmsConnected) {
            RealmsBridge realmsbridge = new RealmsBridge();
            realmsbridge.switchToRealms(new MainMenuScreen());
        } else {
            minecraft.displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
        }
    }

    @Override
    public List<IElement> getElements() {
        return elements;
    }

    @Override
    public List<? extends IGuiEventListener> getEventListeners() {
        return listeners;
    }*/
}
