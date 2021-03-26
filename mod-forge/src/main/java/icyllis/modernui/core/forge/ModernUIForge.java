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

package icyllis.modernui.core.forge;

import icyllis.modernui.ModernUI;
import icyllis.modernui.graphics.RenderCore;
import icyllis.modernui.view.LayoutIO;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;

@Mod(ModernUI.ID)
public final class ModernUIForge extends ModernUI {

    private static boolean optiFineLoaded;

    static boolean interceptTipTheScales;

    static boolean production;
    static boolean developerMode;

    static {
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

    private final Object2ObjectMap<String, IEventBus> mModEventBuses = new Object2ObjectOpenHashMap<>();

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
                                (ISelectiveResourceReloadListener) (resourceManager, typePredicate) -> {
                                    if (typePredicate.test(VanillaResourceType.SHADERS))
                                        RenderCore.compileShaders(resourceManager);
                                }
                        );
            }
            if (production) {
                FMLJavaModLoadingContext.get().getModEventBus().register(EventHandler.ModClientExp.class);
            }
        }

        ModernUI.LOGGER.debug(ModernUI.MARKER, "Modern UI initialized");
    }

    private void init() {
        // get '/run' parent
        Path path = FMLPaths.GAMEDIR.get().getParent();
        // the root directory of your project
        File dir = path.toFile();
        String[] r = dir.list((file, name) -> name.equals("build.gradle"));
        if (r != null && r.length > 0 && dir.getName().equals(ModernUI.NAME_CPT)) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Working in production environment");
            production = true;
        } else if (ModernUI.class.getSigners() == null) {
            ModernUI.LOGGER.debug(MARKER, "Signature is missing");
        }

        // TipTheScales doesn't work with OptiFine
        if (ModList.get().isLoaded("tipthescales") && !optiFineLoaded) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Intercepting TipTheScales");
            interceptTipTheScales = true;
        }

        ModList.get().forEachModContainer((modid, container) -> {
            if (container instanceof FMLModContainer)
                mModEventBuses.put(modid, ((FMLModContainer) container).getEventBus());
        });
    }

    @Override
    public void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP, key, args));
    }

    @Nonnull
    @Override
    public Locale getSelectedLocale() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
    }

    public static boolean isDeveloperMode() {
        return developerMode || production;
    }

    public static boolean isOptiFineLoaded() {
        return optiFineLoaded;
    }

    public static ModernUIForge get() {
        return (ModernUIForge) sInstance;
    }

    public boolean post(@Nullable String modid, @Nonnull Event event) {
        if (event instanceof IModBusEvent)
            if (modid == null) {
                for (IEventBus bus : mModEventBuses.values())
                    if (bus.post(event))
                        return true;
            } else {
                IEventBus bus = mModEventBuses.get(modid);
                return bus != null && bus.post(event);
            }
        else
            LOGGER.warn(MARKER, "Posting a event that not implements IModBusEvent, {}", event);
        return false;
    }
}
