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

import icyllis.modernui.api.ModernUI_API;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.shader.BlurHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.Field;

@Mod(ModernUI.MODID)
public class ModernUI {

    public static final String MODID = "modernui";

    public static final Logger LOGGER = LogManager.getLogger("ModernUI");

    public static final Marker MARKER = MarkerManager.getMarker("MAIN");

    public ModernUI() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.8")) {
            String[] s = javaVersion.split("_");
            if (Integer.parseInt(s[1]) < 60) {
                ModernUI.LOGGER.fatal(MARKER, "You're using java {} which is an outdated java version, please update it to java 1.8.0_200 or above", javaVersion);
                throw new RuntimeException("Outdated java version found, please update it to java 1.8.0_200 or above");
            }
        }
        ConfigManager.register();
        try {
            Field f = ModernUI_API.class.getDeclaredField("network");
            f.setAccessible(true);
            f.set(ModernUI_API.INSTANCE, NetworkManager.INSTANCE);
            if (FMLEnvironment.dist == Dist.CLIENT) {
                f = ModernUI_API.class.getDeclaredField("gui");
                f.setAccessible(true);
                f.set(ModernUI_API.INSTANCE, GuiManager.INSTANCE);
            }
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

}
