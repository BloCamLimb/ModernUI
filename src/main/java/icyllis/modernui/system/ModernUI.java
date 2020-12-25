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
import icyllis.modernui.view.LayoutInflater;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.lang.reflect.InvocationTargetException;

/**
 * The Modern UI mod class for INTERNAL USE ONLY
 */
@Mod(ModernUI.MODID)
public final class ModernUI {

    public static final String MODID = "modernui";
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("System");

    private static final IEventBus EVENT_BUS = BusBuilder.builder().build();

    private static boolean optiFineLoaded;

    static boolean developerMode;

    // mod-loading thread
    public ModernUI() {
        checkJava();

        init();
        Config.init();
        StorageManager.init();

        if (FMLEnvironment.dist.isClient()) {
            LayoutInflater.init();
            RenderTools.init();
        }

        EVENT_BUS.register(EventHandler.Internal.class);
        LOGGER.debug(MARKER, "Modern UI initialized, {}", LOGGER);
    }

    private static void init() {
        try {
            Class<?> clazz = Class.forName("optifine.Installer");
            String version = (String) clazz.getMethod("getOptiFineVersion").invoke(null);
            optiFineLoaded = true;
            LOGGER.debug(MARKER, "OptiFine installed: {}", version);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // Java 1.8.0_51 which is officially used by Mojang will produce bugs with Modern UI
    private static void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null && javaVersion.startsWith("1.8")) {
            int update = Integer.parseInt(javaVersion.split("_")[1]);
            if (update < 60) {
                throw new RuntimeException(
                        "Java " + javaVersion + " is not compatible with Modern UI, " +
                                "a minimum of java 1.8.0_251 or above is required");
            }
        }
    }

    public static boolean post(Event event) {
        return EVENT_BUS.post(event);
    }

    public static boolean isDeveloperMode() {
        return developerMode;
    }

    public static boolean isOptiFineLoaded() {
        return optiFineLoaded;
    }
}
