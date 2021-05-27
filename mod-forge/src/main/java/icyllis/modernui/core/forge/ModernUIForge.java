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
import icyllis.modernui.graphics.font.GlyphManager;
import icyllis.modernui.graphics.shader.ShaderProgram;
import icyllis.modernui.view.LayoutIO;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
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
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

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
                                (ISelectiveResourceReloadListener) (manager, predicate) -> {
                                    if (predicate.test(VanillaResourceType.SHADERS)) {
                                        ShaderProgram.recompile(manager);
                                    }
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

    @Override
    public void loadFont(String cfgFont, Consumer<Font> setter) {
        try (Resource resource = Minecraft.getInstance().getResourceManager()
                .getResource(new ResourceLocation(cfgFont))) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
            setter.accept(f);
            ModernUI.LOGGER.debug(GlyphManager.MARKER, "Preferred font {} was loaded", f.getFamily(Locale.ROOT));
        } catch (Exception e) {
            ModernUI.LOGGER.warn(GlyphManager.MARKER, "Preferred font {} failed to load", cfgFont, e);
        }
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

    public <T extends Event & IModBusEvent> boolean post(@Nullable String modid, @Nonnull T event) {
        if (modid == null) {
            for (IEventBus bus : mModEventBuses.values())
                if (bus.post(event))
                    return true;
        } else {
            IEventBus bus = mModEventBuses.get(modid);
            return bus != null && bus.post(event);
        }
        return false;
    }
}
