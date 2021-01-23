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
import net.minecraftforge.fml.*;
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

    static boolean interceptTipTheScales;

    static boolean production;
    static boolean developerMode;

    static {
        try {
            Class<?> clazz = Class.forName("optifine.Installer");
            String version = (String) clazz.getMethod("getOptiFineVersion").invoke(null);
            optiFineLoaded = true;
            ModernUI.LOGGER.debug(ModernUI.MARKER, "OptiFine found: {}", version);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    // mod-loading thread
    public ModernUIForge() {
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
            if (production) {
                FMLJavaModLoadingContext.get().getModEventBus().register(EventHandler.ModClient.class);
            }
        }

        ModernUI.LOGGER.debug(ModernUI.MARKER, "Modern UI initialized, signed: {}", ModernUIForge.class.getSigners() != null);
    }

    @Override
    public void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP, key, args));
    }

    private static void init() {
        // get '/run' parent
        Path path = FMLPaths.GAMEDIR.get().getParent();
        // the root directory of your project
        File dir = path.toFile();
        String[] r = dir.list((file, name) -> name.equals("build.gradle"));
        if (r != null && r.length > 0) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Working in production environment");
            production = true;
        }

        // TipTheScales doesn't work with OptiFine
        if (ModList.get().isLoaded("tipthescales") && !optiFineLoaded) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Intercepting TipTheScales");
            interceptTipTheScales = true;
        }
    }

    public static boolean isDeveloperMode() {
        return developerMode || production;
    }

    public static boolean isOptiFineLoaded() {
        return optiFineLoaded;
    }
}
