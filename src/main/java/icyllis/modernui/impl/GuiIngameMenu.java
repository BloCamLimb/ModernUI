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

import icyllis.modernui.api.animation.MotionType;
import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.master.UniversalModernScreen;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
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
            builder.rectangle()
                    .setColor(0x000000)
                    .setPos(w -> 0f, h -> 0f)
                    .setSize(Float::valueOf, Float::valueOf)
                    .applyToA(a -> a.setTarget(0.3f).setTiming(4));
            builder.rectangle()
                    .setColor(0x000000)
                    .setAlpha(0.5f)
                    .setPos(w -> 0f, h -> 0f)
                    .setSize(w -> 0f, Float::valueOf)
                    .applyToW(a -> a.setTarget(32).setTiming(3).setMotion(MotionType.SINE));
            builder.widget()
                    .createTexture(a -> a
                            .setPos(w -> 8f, h -> h - 28f)
                            .setTexture(ReferenceLibrary.ICONS)
                            .setUV(160, 0)
                            .setSize(w -> 32f, h -> 32f)
                            .setTint(0.5f, 0.5f, 0.5f)
                            .setScale(0.5f))
                    .initEventListener(a -> a
                            .setPos(w -> 8f, h -> h - 28f)
                            .setRectShape(16, 16))
                    .onHoverOn(q -> q.getTexture().setTint(1.0f, 1.0f, 1.0f))
                    .onHoverOff(q -> q.getTexture().setTint(0.5f, 0.5f, 0.5f))
                    .onLeftClick(w -> {
                        Minecraft minecraft = Minecraft.getInstance();
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
