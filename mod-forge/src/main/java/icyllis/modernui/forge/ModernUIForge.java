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
import icyllis.modernui.graphics.shader.ShaderManager;
import icyllis.modernui.screen.LayoutIO;
import icyllis.modernui.text.Typeface;
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
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

@Mod(ModernUI.ID)
public final class ModernUIForge extends ModernUI {

    private static boolean optiFineLoaded;

    static boolean interceptTipTheScales;

    static boolean development;
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

    private static final Map<String, IEventBus> sModEventBuses = new HashMap<>();

    private Typeface mTypeface;

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
                                        Minecraft.getInstance().submit(ShaderManager.getInstance()::reload);
                                    }
                                }
                        );
                attachBaseContext(new ContextClient(ID));
            }
            if (development) {
                FMLJavaModLoadingContext.get().getModEventBus().register(EventHandler.ModClientExp.class);
            }
        }

        ModList.get().forEachModContainer((modid, container) -> {
            if (container instanceof FMLModContainer) {
                sModEventBuses.put(modid, ((FMLModContainer) container).getEventBus());
            }
        });

        ModernUI.LOGGER.debug(ModernUI.MARKER, "Modern UI initialized");
    }

    private static void init() {
        // get '/run' parent
        Path path = FMLPaths.GAMEDIR.get().getParent();
        // the root directory of your project
        File dir = path.toFile();
        String[] r = dir.list((file, name) -> name.equals("build.gradle"));
        if (r != null && r.length > 0 && dir.getName().equals(ModernUI.NAME_CPT)) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Working in development environment");
            development = true;
        } else if (ModernUI.class.getSigners() == null) {
            ModernUI.LOGGER.debug(MARKER, "Signature is missing");
        }

        // TipTheScales doesn't work with OptiFine
        if (ModList.get().isLoaded("tipthescales") && !optiFineLoaded) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Intercepting TipTheScales");
            interceptTipTheScales = true;
        }
    }

    public void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP, key, args));
    }

    @Nonnull
    @Override
    public Locale getSelectedLocale() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
    }

    @Nonnull
    @Override
    public Typeface getPreferredTypeface() {
        if (mTypeface == null) {
            synchronized (this) {
                if (mTypeface == null) {
                    List<Font> list = new ArrayList<>();
                    List<? extends String> configs = Config.CLIENT.fontFamily.get();
                    if (configs != null) {
                        loadFonts(configs, list);
                    }
                    mTypeface = Typeface.createTypeface(list.toArray(new Font[0]));
                    ModernUI.LOGGER.info(MARKER, "Loaded typeface: {}", mTypeface);
                }
            }
        }
        return mTypeface;
    }

    private static void loadFonts(@Nonnull List<? extends String> configs, @Nonnull List<Font> selected) {
        for (String cfg : configs) {
            if (StringUtils.isEmpty(cfg)) {
                continue;
            }
            if (cfg.endsWith(".ttf") || cfg.endsWith(".otf")
                    || cfg.endsWith(".TTF") || cfg.endsWith(".OTF")) {
                if (cfg.contains(":/") || cfg.contains(":\\")) {
                    try {
                        Font f = Font.createFont(Font.TRUETYPE_FONT, new File(
                                cfg.replaceAll("\\\\", "/")));
                        selected.add(f);
                        ModernUI.LOGGER.debug(MARKER, "Preferred font {} was loaded with config value {}",
                                f.getFamily(Locale.ROOT), cfg);
                    } catch (Exception e) {
                        ModernUI.LOGGER.warn(MARKER, "Preferred font {} failed to load", cfg, e);
                    }
                } else if (cfg.contains(":")) {
                    try (Resource resource = Minecraft.getInstance().getResourceManager()
                            .getResource(new ResourceLocation(cfg))) {
                        Font f = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
                        selected.add(f);
                        ModernUI.LOGGER.debug(MARKER, "Preferred font {} was loaded with config value {}",
                                f.getFamily(Locale.ROOT), cfg);
                    } catch (Exception e) {
                        ModernUI.LOGGER.warn(MARKER, "Preferred font {} failed to load", cfg, e);
                    }
                } else {
                    ModernUI.LOGGER.warn(MARKER, "Preferred font {} is invalid", cfg);
                }
            } else {
                Optional<Font> font =
                        Typeface.sAllFontFamilies.stream().filter(f -> f.getFamily(Locale.ROOT).equals(cfg)).findFirst();
                if (font.isPresent()) {
                    selected.add(font.get());
                    ModernUI.LOGGER.debug(MARKER, "Preferred font {} was loaded", cfg);
                } else {
                    ModernUI.LOGGER.warn(MARKER, "Preferred font {} cannot found or invalid", cfg);
                }
            }
        }
    }

    public static boolean isDeveloperMode() {
        return developerMode || development;
    }

    public static boolean isOptiFineLoaded() {
        return optiFineLoaded;
    }

    public static <T extends Event & IModBusEvent> boolean fire(@Nullable String modid, @Nonnull T event) {
        if (modid == null) {
            for (IEventBus bus : sModEventBuses.values()) {
                if (bus.post(event)) {
                    return true;
                }
            }
        } else {
            IEventBus bus = sModEventBuses.get(modid);
            return bus != null && bus.post(event);
        }
        return false;
    }
}
