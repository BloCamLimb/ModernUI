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

import icyllis.modernui.graphics.renderer.RenderTools;
import icyllis.modernui.ui.storage.LayoutInflater;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@Mod(ModernUI.MODID)
public class ModernUI {

    public static final String MODID        = "modernui";
    public static final String NAME_COMPACT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_COMPACT);
    public static final Marker MARKER = MarkerManager.getMarker("System");

    public ModernUI() {
        checkJava();

        Config.init();
        ModIntegration.init();
        StorageManager.init();

        if (FMLEnvironment.dist.isClient()) {
            LayoutInflater.init();
            RenderTools.init();
        }
    }

    // Java 1.8.0_51 which is officially used by Mojang will produce bugs with Modern UI
    private static void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.8")) {
            String[] s = javaVersion.split("_");
            if (Integer.parseInt(s[1]) < 60) {
                throw new IllegalStateException(
                        "You're using java " + javaVersion + " which is not compatible with Modern UI, " +
                                "a minimum of java 1.8.0_251 or above is required");
            }
        }
    }
}
