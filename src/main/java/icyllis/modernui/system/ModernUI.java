/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system;

import icyllis.modernui.graphics.BlurHandler;
import icyllis.modernui.graphics.shader.ShaderTools;
import icyllis.modernui.ui.data.LayoutResourceManager;
import icyllis.modernui.ui.master.UIEditor;
import icyllis.modernui.ui.master.UIManager;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@SuppressWarnings("unused")
@Mod(ModernUI.MODID)
public class ModernUI {

    public static final String MODID = "modernui";

    public static final Logger LOGGER = LogManager.getLogger("ModernUI");

    public static final Marker MARKER = MarkerManager.getMarker("SYSTEM");

    public ModernUI() {
        checkJava();

        Config.init();
        ModIntegration.init();
        StorageManager.init();

        if (FMLEnvironment.dist.isClient()) {
            LayoutResourceManager.init();
            ShaderTools.init();
            MinecraftForge.EVENT_BUS.register(BlurHandler.INSTANCE);
            MinecraftForge.EVENT_BUS.register(UIEditor.INSTANCE);
            MinecraftForge.EVENT_BUS.register(UIManager.INSTANCE);
        }
    }

    // Java 1.8.0_51 which is officially used by Mojang will produce bugs with Modern UI
    private void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.8")) {
            String[] s = javaVersion.split("_");
            if (Integer.parseInt(s[1]) < 60) {
                throw new RuntimeException(
                        "You're using java " + javaVersion + " which is not compatible with Modern UI, " +
                                "a minimum of java 1.8.0_200 or above is required");
            }
        }
    }

    /* MainWindow */
    public static int calcGuiScale(int guiScaleIn) {
        int r = ModernUI.calcGuiScales();
        return guiScaleIn > 0 ? MathHelper.clamp(guiScaleIn, r >> 8 & 0xf, r & 0xf) : r >> 4 & 0xf;
    }

    /* Minecraft */
    public static void displayInGameMenu(boolean usePauseScreen) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.currentScreen == null) {
            // If press F3 + Esc and is single player and not open LAN world
            if (usePauseScreen && minecraft.isIntegratedServerRunning() && minecraft.getIntegratedServer() != null && !minecraft.getIntegratedServer().getPublic()) {
                minecraft.displayGuiScreen(new IngameMenuScreen(false));
                minecraft.getSoundHandler().pause();
            } else {
                //UIManager.INSTANCE.openGuiScreen(new TranslationTextComponent("menu.game"), IngameMenuHome::new);
                minecraft.displayGuiScreen(new IngameMenuScreen(true));
            }
        }
    }

    /* Screen */
    public static int getScreenBackgroundColor() {
        return (int) (BlurHandler.INSTANCE.getBackgroundAlpha() * 255.0f) << 24;
    }

    /* Core function */
    public static int calcGuiScales() {
        MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
        return calcGuiScales(mainWindow.getFramebufferWidth(), mainWindow.getFramebufferHeight());
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
