/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

import icyllis.modernui.graphics.RenderCore;
import icyllis.modernui.view.LayoutIO;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DatagenModLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

/**
 * The Modern UI mod class for INTERNAL USE ONLY
 */
@Mod(ModernUI.MODID)
public final class ModernUI {

    public static final String MODID = "modernui";
    public static final String NAME_CPT = "ModernUI";

    public static final Logger LOGGER = LogManager.getLogger(NAME_CPT);
    public static final Marker MARKER = MarkerManager.getMarker("System");

    public static final IEventBus EVENT_BUS = BusBuilder.builder().build();

    private static boolean optiFineLoaded;

    static boolean development;
    static boolean developerMode;

    // mod-loading thread
    public ModernUI() {
        checkJava();

        final boolean isDataGen = DatagenModLoader.isRunningDataGen();

        init();
        Config.init();
        LayoutIO.init();
        LocalStorage.init();

        if (FMLEnvironment.dist.isClient()) {
            if (!isDataGen) {
                RenderCore.init();
            }
            if (development) {
                FMLJavaModLoadingContext.get().getModEventBus().register(EventHandler.ModClient.class);
            }
        }

        LOGGER.debug(MARKER, "Modern UI initialized, signed: {}", ModernUI.class.getSigners() != null);
    }

    private static void init() {
        // get '/run' parent
        Path path = FMLPaths.GAMEDIR.get().getParent();
        // the root directory of your project
        File dir = path.toFile();
        String[] r = dir.list((file, name) -> name.equals("build.gradle"));
        development = r != null && r.length > 0;
        if (development) {
            LOGGER.debug(MARKER, "Working in development environment");
        }

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
        if (javaVersion == null) {
            LOGGER.fatal(MARKER, "Java version is missing");
        } else if (javaVersion.startsWith("1.8")) {
            try {
                int update = Integer.parseInt(javaVersion.split("_")[1].split("-")[0]);
                if (update < 201) {
                    throw new RuntimeException(
                            "Java " + javaVersion + " is not compatible with Modern UI, " +
                                    "a minimum of java 1.8.0_271 or above is required");
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                LOGGER.warn(MARKER, "Failed to check java version: {}", javaVersion, e);
            }
        }
    }

    public static boolean isDeveloperMode() {
        return developerMode || development;
    }

    public static boolean isOptiFineLoaded() {
        return optiFineLoaded;
    }
}
