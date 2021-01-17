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

package icyllis.modernui.forge;

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.RenderCore;
import icyllis.modernui.mcimpl.ModernUIMod;
import icyllis.modernui.view.LayoutIO;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DatagenModLoader;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

@Mod(ModernUI.ID)
public final class ModernUIForge extends ModernUIMod {

    public static final IEventBus EVENT_BUS = BusBuilder.builder().build();

    private static boolean optiFineLoaded;

    static boolean alphaTest;
    static boolean developerMode;

    // mod-loading thread
    public ModernUIForge() {
        checkJava();

        final boolean isDataGen = DatagenModLoader.isRunningDataGen();

        init();
        Config.init();
        LayoutIO.init();
        LocalStorage.init();

        if (FMLEnvironment.dist.isClient()) {
            if (!isDataGen) {
                ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                        .registerReloadListener(
                                (ISelectiveResourceReloadListener) (r, t) -> {
                                    if (t.test(VanillaResourceType.SHADERS))
                                        RenderCore.compileShaders(r);
                                }
                        );
            }
            if (alphaTest) {
                FMLJavaModLoadingContext.get().getModEventBus().register(EventHandler.ModClient.class);
            }
        }

        ModernUI.LOGGER.debug(ModernUI.MARKER, "Modern UI initialized, signed: {}", ModernUIForge.class.getSigners() != null);
    }

    @Override
    public void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP,
                key, args));
    }

    private static void init() {
        // get '/run' parent
        Path path = FMLPaths.GAMEDIR.get().getParent();
        // the root directory of your project
        File dir = path.toFile();
        String[] r = dir.list((file, name) -> name.equals("build.gradle"));
        if (r != null && r.length > 0) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Working in development environment");
            alphaTest = true;
        }

        try {
            Class<?> clazz = Class.forName("optifine.Installer");
            String version = (String) clazz.getMethod("getOptiFineVersion").invoke(null);
            optiFineLoaded = true;
            ModernUI.LOGGER.debug(ModernUI.MARKER, "OptiFine installed: {}", version);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // Java 1.8.0_51 which is officially used by Mojang will produce bugs with Modern UI
    private static void checkJava() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion == null) {
            ModernUI.LOGGER.fatal(ModernUI.MARKER, "Java version is missing");
        } else if (javaVersion.startsWith("1.8")) {
            try {
                int update = Integer.parseInt(javaVersion.split("_")[1].split("-")[0]);
                if (update < 201) {
                    throw new RuntimeException(
                            "Java " + javaVersion + " is not compatible with Modern UI, " +
                                    "a minimum of java 1.8.0_271 or above is required");
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                ModernUI.LOGGER.warn(ModernUI.MARKER, "Failed to check java version: {}", javaVersion, e);
            }
        }
    }

    public static boolean isDeveloperMode() {
        return developerMode || alphaTest;
    }

    public static boolean isOptiFineLoaded() {
        return optiFineLoaded;
    }
}
