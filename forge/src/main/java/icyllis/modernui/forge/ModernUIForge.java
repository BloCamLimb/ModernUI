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
import icyllis.modernui.graphics.font.FontCollection;
import icyllis.modernui.graphics.opengl.ShaderManager;
import icyllis.modernui.graphics.opengl.TextureManager;
import icyllis.modernui.graphics.font.FontPaint;
import icyllis.modernui.graphics.font.LayoutCache;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.textmc.ModernUITextMC;
import icyllis.modernui.view.ViewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.data.loading.DatagenModLoader;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * Mod class. INTERNAL.
 */
@Mod(ModernUI.ID)
public final class ModernUIForge extends ModernUI {

    private static boolean sOptiFineLoaded;

    static volatile boolean sInterceptTipTheScales;

    static volatile boolean sDevelopment;
    static volatile boolean sDeveloperMode;

    static {
        try {
            Class<?> clazz = Class.forName("optifine.Installer");
            sOptiFineLoaded = true;
            try {
                String version = (String) clazz.getMethod("getOptiFineVersion").invoke(null);
                LOGGER.info(MARKER, "OptiFine installed: {}", version);
            } catch (Exception e) {
                LOGGER.info(MARKER, "OptiFine installed...");
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    private static final Map<String, IEventBus> sModEventBuses = new HashMap<>();

    private volatile Typeface mTypeface;

    // mod-loading thread
    public ModernUIForge() {
        final boolean isDataGen = DatagenModLoader.isRunningDataGen();

        // get '/run' parent
        Path path = FMLPaths.GAMEDIR.get().getParent();
        // the root directory of your project
        File dir = path.toFile();
        String[] r = dir.list((file, name) -> name.equals("build.gradle"));
        if (r != null && r.length > 0 && dir.getName().equals(NAME_CPT)) {
            sDevelopment = true;
            LOGGER.debug(MARKER, "Auto detected in development environment");
        } else if (!FMLEnvironment.production) {
            sDevelopment = true;
        } else if (ModernUI.class.getSigners() == null) {
            LOGGER.warn(MARKER, "Signature is missing");
        }

        // TipTheScales doesn't work with OptiFine
        if (ModList.get().isLoaded("tipthescales") && !sOptiFineLoaded) {
            sInterceptTipTheScales = true;
            LOGGER.info(MARKER, "Intercepting TipTheScales");
        }

        Config.init();
        LocalStorage.init();

        if (FMLEnvironment.dist.isClient()) {
            if (!isDataGen) {
                ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                        .registerReloadListener(
                                (ResourceManagerReloadListener) (manager) -> {
                                    ShaderManager.getInstance().reload();
                                    TextureManager.getInstance().reload();
                                    if (UIManager.sInstance != null) {
                                        UIManager.sInstance.updateLayoutDir();
                                    }
                                }
                        );
            }
            ModernUITextMC.init();
            if (sDevelopment) {
                FMLJavaModLoadingContext.get().getModEventBus().register(Registration.ModClientDev.class);
            }
        }

        ModList.get().forEachModContainer((modid, container) -> {
            if (container instanceof FMLModContainer) {
                final String namespace = container.getNamespace();
                if (!namespace.equals("forge")) {
                    sModEventBuses.put(namespace, ((FMLModContainer) container).getEventBus());
                }
            }
        });

        LOGGER.info(MARKER, "Modern UI initialized, FML mods: {}", sModEventBuses.size());
    }

    // INTERNAL HOOK
    @OnlyIn(Dist.CLIENT)
    public static void dispatchOnWindowResize(int width, int height, int guiScale, int oldGuiScale) {
        for (var l : MuiForgeApi.sOnWindowResizeListeners) {
            l.onWindowResize(width, height, guiScale, oldGuiScale);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static void dispatchOnDebugDump(@Nonnull PrintWriter writer) {
        for (var l : MuiForgeApi.sOnDebugDumpListeners) {
            l.onDebugDump(writer);
        }
    }

    /*public static void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP, key, args));
    }*/

    @Nonnull
    @Override
    public Locale getSelectedLocale() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
    }

    @Nonnull
    @Override
    public Typeface getSelectedTypeface() {
        if (mTypeface != null) {
            return mTypeface;
        }
        synchronized (this) {
            // should be a worker thread
            if (mTypeface == null) {
                Set<Font> set = new LinkedHashSet<>();
                List<? extends String> configs = Config.CLIENT.fontFamily.get();
                if (configs != null) {
                    loadFonts(configs, set);
                }
                mTypeface = Typeface.createTypeface(set.toArray(new Font[0]));
                Minecraft.getInstance().tell(() -> LayoutCache.getOrCreate(ID, 0, 1, false,
                        new FontPaint(), false, false));
                LOGGER.info(MARKER, "Loaded typeface: {}", mTypeface);
            }
        }
        return mTypeface;
    }

    private static void loadFonts(@Nonnull List<? extends String> configs, @Nonnull Set<Font> selected) {
        for (String cfg : configs) {
            if (StringUtils.isEmpty(cfg)) {
                continue;
            }
            if (cfg.endsWith(".ttf") || cfg.endsWith(".otf") ||
                    cfg.endsWith(".TTF") || cfg.endsWith(".OTF")) {
                try {
                    Font f = Font.createFont(Font.TRUETYPE_FONT, new File(
                            cfg.replaceAll("\\\\", "/")));
                    selected.add(f);
                    LOGGER.debug(MARKER, "Font {} was loaded with config value {}",
                            f.getFamily(Locale.ROOT), cfg);
                    continue;
                } catch (Exception ignored) {
                }
                try (Resource resource = Minecraft.getInstance().getResourceManager()
                        .getResource(new ResourceLocation(cfg))) {
                    Font f = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
                    selected.add(f);
                    LOGGER.debug(MARKER, "Font {} was loaded with config value {}",
                            f.getFamily(Locale.ROOT), cfg);
                    continue;
                } catch (Exception ignored) {
                }
                LOGGER.warn(MARKER, "Font {} failed to load or invalid", cfg);
            } else {
                Optional<Font> font = FontCollection.sAllFontFamilies.stream()
                        .filter(f -> f.getFamily(Locale.ROOT).equalsIgnoreCase(cfg))
                        .findFirst();
                if (font.isPresent()) {
                    selected.add(font.get());
                    LOGGER.debug(MARKER, "Font {} was loaded", cfg);
                }
            }
        }
    }

    @Nonnull
    @Override
    public InputStream getResourceStream(@Nonnull String res, @Nonnull String path) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(res, path)).getInputStream();
    }

    @Nonnull
    @Override
    public ReadableByteChannel getResourceChannel(@Nonnull String res, @Nonnull String path) throws IOException {
        return Channels.newChannel(getResourceStream(res, path));
    }

    @Override
    public ViewManager getViewManager() {
        return UIManager.sInstance.getDecorView();
    }

    public static boolean isDeveloperMode() {
        return sDeveloperMode || sDevelopment;
    }

    public static boolean isOptiFineLoaded() {
        return sOptiFineLoaded;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static <T extends Event & IModBusEvent> boolean post(@Nullable String s, @Nonnull T e) {
        if (s == null) {
            boolean handled = false;
            for (IEventBus bus : sModEventBuses.values()) {
                handled |= bus.post(e);
            }
            return handled;
        } else {
            IEventBus bus = sModEventBuses.get(s);
            return bus != null && bus.post(e);
        }
    }
}
