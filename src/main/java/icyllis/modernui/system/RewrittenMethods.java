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

package icyllis.modernui.system;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.module.IngameMenuHome;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Objects;

@SuppressWarnings("unused")
public class RewrittenMethods {

    /** MainWindow **/
    public static int calcGuiScale(int guiScaleIn) {
        int r = RewrittenMethods.calcGuiScales();
        return guiScaleIn > 0 ? MathHelper.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }

    /** Minecraft **/
    public static void displayInGameMenu(boolean pauseGame) {
        if (RenderSystem.isOnRenderThread()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.currentScreen == null) {
                // If press F3 + Esc and is single player and not open LAN world
                if (pauseGame && minecraft.isSingleplayer() && !Objects.requireNonNull(minecraft.getIntegratedServer()).getPublic()) {
                    minecraft.displayGuiScreen(new IngameMenuScreen(false));
                    minecraft.getSoundHandler().pause();
                } else {
                    GlobalModuleManager.INSTANCE.openGuiScreen(new TranslationTextComponent("menu.game"), IngameMenuHome::new);
                }
            }
        }
    }

    public static int calcGuiScales() {
        MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
        int framebufferWidth = mainWindow.getFramebufferWidth();
        int framebufferHeight = mainWindow.getFramebufferHeight();
        return calcGuiScales(framebufferWidth, framebufferHeight);
    }

    private static int calcGuiScales(int framebufferWidth, int framebufferHeight) {

        double a1 = Math.floor(framebufferWidth / 16.0d);
        double a2 = Math.floor(framebufferHeight / 9.0d);

        if (a1 % 2 != 0) {
            a1++;
        }
        if (a2 % 2 != 0) {
            a2++;
        }

        double base = Math.min(a1, a2);
        double top = Math.max(a1, a2);

        int min;
        int max = MathHelper.clamp((int) (base / 27), 1, 10);
        if (max > 1) {
            int i = (int) (base / 64);
            int j = (int) (top / 64);
            min = MathHelper.clamp(j > i ? i + 1 : i, 2, 10);
        } else {
            min = 1;
        }

        int best;
        if (min > 1) {
            int i = (int) (base / 32);
            int j = (int) (top / 32);
            double v1 = base / (i * 32);
            if (v1 > 1.25 || j > i) {
                best = Math.min(max, i + 1);
            } else {
                best = i;
            }
        } else {
            best = 1;
        }

        return min << 8 | best << 4 | max;
    }
}
