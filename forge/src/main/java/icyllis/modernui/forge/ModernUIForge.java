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
import icyllis.modernui.graphics.font.*;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.textmc.ModernUITextMC;
import icyllis.modernui.view.ViewManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
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
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static icyllis.modernui.ModernUI.*;

/**
 * Mod class. INTERNAL.
 *
 * @author BloCamLimb
 */
@Mod(ModernUI.ID)
public final class ModernUIForge {

    // false to disable extensions
    public static final int BOOTSTRAP_TEXT_ENGINE = 0x1;
    public static final int BOOTSTRAP_SMOOTH_SCROLLING = 0x2;

    private static boolean sOptiFineLoaded;

    static volatile boolean sInterceptTipTheScales;

    static volatile boolean sDevelopment;
    static volatile boolean sDeveloperMode;

    static volatile Integer sBootstrapLevel;

    public static boolean sInventoryScreenPausesGame;

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

    // mod-loading thread
    public ModernUIForge() {
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
            LOGGER.debug(MARKER, "Auto detected in FML development environment");
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

        // the 'new' method is in another class, so it's class-loading-safe
        DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> Client::new);

        ModList.get().forEachModContainer((modid, container) -> {
            if (container instanceof FMLModContainer) {
                final String namespace = container.getNamespace();
                if (!namespace.equals("forge")) {
                    sModEventBuses.put(namespace, ((FMLModContainer) container).getEventBus());
                }
            }
        });

        LOGGER.info(MARKER, "Initialized Modern UI, FML mods: {}", sModEventBuses.size());
    }

    // INTERNAL HOOK
    @OnlyIn(Dist.CLIENT)
    public static void dispatchOnWindowResize(int width, int height, int guiScale, int oldGuiScale) {
        for (var l : MuiForgeApi.sOnWindowResizeListeners) {
            l.onWindowResize(width, height, guiScale, oldGuiScale);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void dispatchOnDebugDump(@Nonnull PrintWriter writer) {
        for (var l : MuiForgeApi.sOnDebugDumpListeners) {
            l.onDebugDump(writer);
        }
    }

    // INTERNAL
    public static int getBootstrapLevel() {
        if (sBootstrapLevel != null) {
            return sBootstrapLevel;
        }
        synchronized (ModernUIForge.class) {
            if (sBootstrapLevel == null) {
                Path path = FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(NAME_CPT),
                        NAME_CPT).resolve("bootstrap");
                if (Files.exists(path)) {
                    try {
                        sBootstrapLevel = Integer.parseUnsignedInt(Files.readString(path, StandardCharsets.UTF_8));
                        LOGGER.debug(MARKER, "Bootstrap level: 0x{}", Integer.toHexString(sBootstrapLevel));
                    } catch (Exception ignored) {
                    }
                } else {
                    try {
                        Files.createFile(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (sBootstrapLevel == null) {
                setBootstrapLevel(0);
            }
        }
        return sBootstrapLevel;
    }

    public static void setBootstrapLevel(int level) {
        sBootstrapLevel = level;
        Path path = FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve(NAME_CPT),
                NAME_CPT).resolve("bootstrap");
        if (!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.writeString(path, Integer.toString(level), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public static void warnSetup(String key, Object... args) {
        ModLoader.get().addWarning(new ModLoadingWarning(null, ModLoadingStage.SIDED_SETUP, key, args));
    }*/

    private static void loadFonts(@Nonnull List<? extends String> configs, @Nonnull Set<Font> selected) {
        boolean hasFail = false;
        for (String cfg : configs) {
            if (StringUtils.isEmpty(cfg)) {
                continue;
            }
            try {
                Font f = Font.createFont(Font.TRUETYPE_FONT, new File(
                        cfg.replaceAll("\\\\", "/")));
                selected.add(f);
                LOGGER.debug(MARKER, "Font {} was loaded with config value {} as LOCAL FILE",
                        f.getFamily(Locale.ROOT), cfg);
                continue;
            } catch (Exception ignored) {
            }
            try (Resource resource = Minecraft.getInstance().getResourceManager()
                    .getResource(new ResourceLocation(cfg))) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
                selected.add(f);
                LOGGER.debug(MARKER, "Font {} was loaded with config value {} as RESOURCE PACK",
                        f.getFamily(Locale.ROOT), cfg);
                continue;
            } catch (Exception ignored) {
            }
            Optional<Font> font = FontCollection.sAllFontFamilies.stream()
                    .filter(f -> f.getFamily(Locale.ROOT).equalsIgnoreCase(cfg))
                    .findFirst();
            if (font.isPresent()) {
                Font f = font.get();
                selected.add(f);
                LOGGER.debug(MARKER, "Font {} was loaded with config value {} as SYSTEM FONT",
                        f.getFamily(Locale.ROOT), cfg);
                continue;
            }
            hasFail = true;
            LOGGER.info(MARKER, "Font {} failed to load or invalid", cfg);
        }
        if (hasFail && isDeveloperMode()) {
            LOGGER.debug(MARKER, "Available system font names: {}",
                    FontCollection.sAllFontFamilies.stream().map(f -> f.getFamily(Locale.ROOT))
                            .collect(Collectors.joining(",")));
        }
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

    // should not make OnlyIn Dist.CLIENT, we require the constructor method
    public static class Client extends ModernUI {

        static {
            assert FMLEnvironment.dist.isClient();
        }

        private volatile Typeface mTypeface;

        private Client() {
            super();
            if ((getBootstrapLevel() & BOOTSTRAP_TEXT_ENGINE) == 0) {
                ModernUITextMC.init();
                LOGGER.info(MARKER, "Initialized Modern UI text engine");
            }
            if (sDevelopment) {
                FMLJavaModLoadingContext.get().getModEventBus().register(Registration.ModClientDev.class);
            }
            LOGGER.info(MARKER, "Initialized Modern UI client");
        }

        @Nonnull
        @Override
        protected Locale onGetSelectedLocale() {
            return Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
        }

        @Nonnull
        @Override
        protected Typeface onGetSelectedTypeface() {
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
                    // do some warm-up
                    Minecraft.getInstance().tell(() -> LayoutCache.getOrCreate(ID, 0, 1, false,
                            new FontPaint(), false, false));
                    LOGGER.info(MARKER, "Loaded typeface: {}", mTypeface);
                }
            }
            return mTypeface;
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
            return UIManager.getInstance().getDecorView();
        }
    }
}
