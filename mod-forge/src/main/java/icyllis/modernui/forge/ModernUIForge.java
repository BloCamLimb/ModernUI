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
import icyllis.modernui.graphics.texture.TextureManager;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.textmc.TextLayoutEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModLoadingWarning;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fmllegacy.DatagenModLoader;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

@Mod(ModernUI.ID)
public final class ModernUIForge extends ModernUI {

    private static boolean sOptiFineLoaded;

    static boolean sInterceptTipTheScales;

    static boolean sDevelopment;
    static boolean sDeveloperMode;

    static {
        try {
            Class<?> clazz = Class.forName("optifine.Installer");
            String version = (String) clazz.getMethod("getOptiFineVersion").invoke(null);
            sOptiFineLoaded = true;
            ModernUI.LOGGER.debug(ModernUI.MARKER, "OptiFine installed: {}", version);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {

        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static final Map<String, IEventBus> sModEventBuses = new HashMap<>();

    private volatile Typeface mTypeface;

    // mod-loading thread
    @SuppressWarnings("deprecation")
    public ModernUIForge() {
        final boolean isDataGen = DatagenModLoader.isRunningDataGen();

        init();
        Config.init();
        LocalStorage.init();

        if (FMLEnvironment.dist.isClient()) {
            if (!isDataGen) {
                ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                        .registerReloadListener(
                                (ResourceManagerReloadListener) (manager) -> {
                                    ShaderManager.getInstance().reload();
                                    TextureManager.getInstance().reload();
                                    TextLayoutEngine.getInstance().reload();
                                }
                        );
            }
            if (sDevelopment) {
                FMLJavaModLoadingContext.get().getModEventBus().register(EventHandler.ModClientDev.class);
            }
        }

        ModList.get().forEachModContainer((modid, container) -> {
            if (container instanceof FMLModContainer) {
                sModEventBuses.put(container.getNamespace(), ((FMLModContainer) container).getEventBus());
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
            sDevelopment = true;
        } else if (ModernUI.class.getSigners() == null) {
            ModernUI.LOGGER.debug(MARKER, "Signature is missing");
        }

        // TipTheScales doesn't work with OptiFine
        if (ModList.get().isLoaded("tipthescales") && !sOptiFineLoaded) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Intercepting TipTheScales");
            sInterceptTipTheScales = true;
        }
    }

    public static void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP, key, args));
    }

    @Nonnull
    @Override
    public Locale getSelectedLocale() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
    }

    @Nonnull
    @Override
    public Typeface getSelectedTypeface() {
        if (mTypeface == null) {
            synchronized (this) {
                if (mTypeface == null) {
                    Set<Font> set = new LinkedHashSet<>();
                    List<? extends String> configs = Config.CLIENT.fontFamily.get();
                    if (configs != null) {
                        loadFonts(configs, set);
                    }
                    mTypeface = Typeface.createTypeface(set.toArray(new Font[0]));
                    ModernUI.LOGGER.info(MARKER, "Active: {}", mTypeface);
                }
            }
        }
        return mTypeface;
    }

    private static void loadFonts(@Nonnull List<? extends String> configs, @Nonnull Set<Font> selected) {
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
                        ModernUI.LOGGER.debug(MARKER, "Font {} was loaded with config value {}",
                                f.getFamily(Locale.ROOT), cfg);
                    } catch (Exception e) {
                        ModernUI.LOGGER.warn(MARKER, "Font {} failed to load", cfg);
                    }
                } else if (cfg.contains(":")) {
                    try (Resource resource = Minecraft.getInstance().getResourceManager()
                            .getResource(new ResourceLocation(cfg))) {
                        Font f = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
                        selected.add(f);
                        ModernUI.LOGGER.debug(MARKER, "Font {} was loaded with config value {}",
                                f.getFamily(Locale.ROOT), cfg);
                    } catch (Exception e) {
                        ModernUI.LOGGER.warn(MARKER, "Font {} failed to load", cfg);
                    }
                } else {
                    ModernUI.LOGGER.warn(MARKER, "Font {} is invalid", cfg);
                }
            } else {
                Optional<Font> font =
                        Typeface.sAllFontFamilies.stream().filter(f -> f.getFamily(Locale.ROOT).equalsIgnoreCase(cfg)).findFirst();
                if (font.isPresent()) {
                    selected.add(font.get());
                    ModernUI.LOGGER.debug(MARKER, "Font {} was loaded", cfg);
                } else {
                    ModernUI.LOGGER.warn(MARKER, "Font {} cannot found or invalid", cfg);
                }
            }
        }
    }

    @Nonnull
    @Override
    public InputStream getResourceAsStream(@Nonnull String namespace, @Nonnull String path) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(namespace, path)).getInputStream();
    }

    @Nonnull
    @Override
    public ReadableByteChannel getResourceAsChannel(@Nonnull String namespace, @Nonnull String path) throws IOException {
        return Channels.newChannel(getResourceAsStream(namespace, path));
    }

    public static boolean isDeveloperMode() {
        return sDeveloperMode || sDevelopment;
    }

    public static boolean isOptiFineLoaded() {
        return sOptiFineLoaded;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T extends Event & IModBusEvent> boolean fire(@Nullable String namespace, @Nonnull T event) {
        if (namespace == null) {
            for (IEventBus bus : sModEventBuses.values()) {
                if (bus.post(event)) {
                    return true;
                }
            }
        } else {
            IEventBus bus = sModEventBuses.get(namespace);
            return bus != null && bus.post(event);
        }
        return false;
    }
}
