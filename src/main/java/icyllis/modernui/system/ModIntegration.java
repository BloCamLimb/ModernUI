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

package icyllis.modernui.system;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;
import net.optifine.shaders.gui.GuiShaders;

import java.lang.reflect.InvocationTargetException;

public class ModIntegration {

    public static boolean optifineLoaded = false;

    static {

    }

    public static void init() {
        ModList modList = ModList.get();
        try {
            Class<?> clazz = Class.forName("optifine.Installer");
            optifineLoaded = true;
            String ver = (String) clazz.getMethod("getOptiFineVersion").invoke(null);
            ModernUI.LOGGER.debug(ModernUI.MARKER, "OptiFine loaded, version : {}", ver);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static class OptiFine {

        public static void openShadersGui() {
            Minecraft.getInstance().displayGuiScreen(
                    new GuiShaders(Minecraft.getInstance().currentScreen, Minecraft.getInstance().gameSettings));
        }
    }
}
