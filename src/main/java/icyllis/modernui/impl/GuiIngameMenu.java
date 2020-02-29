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

import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.api.global.MotionType;
import icyllis.modernui.gui.master.GlobalAnimationManager;
import icyllis.modernui.gui.master.UniversalModernScreen;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiIngameMenu extends UniversalModernScreen {

    public GuiIngameMenu(boolean isFullMenu) {
        super(l -> {
            Modules m = new Modules();
            l.add(m::createDefault);
        });
    }

    private static class Modules {

        void createDefault(IElementBuilder builder) {
            Minecraft minecraft = Minecraft.getInstance();
            /*builder.defaultBackground()
                    .alphaAnimation(0, 4);*/
            ModernUI.LOGGER.debug("Gui Scale Factor: {}", Minecraft.getInstance().getMainWindow().getGuiScaleFactor());
            builder.rectangle()
                    .setColor(0, 0, 0)
                    .setAlpha(0.7f)
                    .setPos(w -> 0f, h -> 0f)
                    .setSize(w -> 0f, Float::valueOf)
                    .buildToPool(t ->
                            GlobalAnimationManager.INSTANCE.create(a -> a
                                    .setInit(0)
                                    .setTarget(32)
                                    .setTiming(4)
                                    .setMotion(MotionType.SINE),
                                    r -> t.sizeW = r,
                                    rs -> t.fakeW = rs
                            )
                    );
            builder.buttonT1()
                    .createTexture(a -> a
                            .setAlpha(0)
                            .setPos(w -> 8f, h -> 8f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(128, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> 8f)
                            .setRectShape(16, 16))
                    .onLeftClick(() -> Minecraft.getInstance().displayGuiScreen(null));
            builder.buttonT1()
                    .createTexture(a -> a
                            .setAlpha(0)
                            .setPos(w -> 8f, h -> h / 4f - 16f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(32, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> h / 4f - 16f)
                            .setRectShape(16, 16))
                    .onLeftClick(() -> minecraft.displayGuiScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancementManager())));
            builder.buttonT1()
                    .createTexture(a -> a
                            .setAlpha(0)
                            .setPos(w -> 8f, h -> h / 4f + 4f + h / 32f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(64, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> h / 4f + 4f + h / 32f)
                            .setRectShape(16, 16))
                    .onLeftClick(() -> {});
            builder.buttonT1()
                    .createTexture(a -> a
                            .setAlpha(0)
                            .setPos(w -> 8f, h -> h * 0.75f - 20 - h / 32f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(96, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> h * 0.75f - 20 - h / 32f)
                            .setRectShape(16, 16))
                    .onLeftClick(() -> {});
            builder.buttonT1()
                    .createTexture(a -> a
                            .setAlpha(0)
                            .setPos(w -> 8f, h -> h * 0.75f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(0, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> h * 0.75f)
                            .setRectShape(16, 16))
                    .onLeftClick(() -> {
                        if (Minecraft.getInstance().currentScreen != null)
                            Minecraft.getInstance().displayGuiScreen(new OptionsScreen(Minecraft.getInstance().currentScreen, Minecraft.getInstance().gameSettings));
                    });
            builder.buttonT1()
                    .createTexture(a -> a
                            .setAlpha(0)
                            .setPos(w -> 8f, h -> h - 28f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(160, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> h - 28f)
                            .setRectShape(16, 16))
                    .onLeftClick(() -> {
                        if (minecraft.world == null) {
                            return;
                        }
                        boolean flag = minecraft.isIntegratedServerRunning();
                        boolean flag1 = minecraft.isConnectedToRealms();
                        minecraft.world.sendQuittingDisconnectingPacket();
                        if (flag) {
                            minecraft.unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
                        } else {
                            minecraft.unloadWorld();
                        }

                        if (flag) {
                            minecraft.displayGuiScreen(new MainMenuScreen());
                        } else if (flag1) {
                            RealmsBridge realmsbridge = new RealmsBridge();
                            realmsbridge.switchToRealms(new MainMenuScreen());
                        } else {
                            minecraft.displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
                        }
                    });
        }
    }
}
